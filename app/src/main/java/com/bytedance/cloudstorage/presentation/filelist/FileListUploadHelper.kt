package com.bytedance.cloudstorage.presentation.filelist

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import com.bytedance.cloudstorage.domain.model.CloudFile
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

// ────────────────────────────────────────────────
// 文件上传辅助函数
// ────────────────────────────────────────────────

/** MIME 类型 → 文件分类类型键 */
internal fun mimeTypeToFileType(mimeType: String?): String = when {
    mimeType == null                -> "other"
    mimeType.startsWith("video/")  -> "video"
    mimeType.startsWith("text/")   -> "txt"
    else                           -> "other"
}

/**
 * 生成去重后的文件名。
 */
internal fun generateUniqueName(
    desiredName: String,
    existingNames: Set<String>
): String {
    if (desiredName !in existingNames) return desiredName

    val dotIndex = desiredName.lastIndexOf('.')
    val baseName: String
    val extension: String
    if (dotIndex > 0) {
        baseName = desiredName.substring(0, dotIndex)
        extension = desiredName.substring(dotIndex)
    } else {
        baseName = desiredName
        extension = ""
    }

    var counter = 1
    var candidate: String
    do {
        candidate = "${baseName}(${counter})$extension"
        counter++
    } while (candidate in existingNames)

    return candidate
}

// ────────────────────────────────────────────────
// 文件名清理
// ────────────────────────────────────────────────

/** 文件系统不允许出现在文件名中的字符 */
private val ILLEGAL_NAME_CHARS = Regex("[\\\\/:*?\"<>|\\x00-\\x1f]")

/**
 * 清理文件名：移除非法字符、截断超长名称、去除首尾空白和点号。
 *
 * @param name      原始文件名
 * @param maxLength 最大字符数（含扩展名），默认 50
 * @return 清理后的合法文件名
 */
internal fun sanitizeFileName(name: String, maxLength: Int = 50): String {
    // 1. 移除非法字符
    var cleaned = name.replace(ILLEGAL_NAME_CHARS, "")
    // 2. 去除首尾空格和点号（Windows 不允许）
    cleaned = cleaned.trim().trimEnd('.')
    // 3. 空字符串兜底
    if (cleaned.isBlank()) return "未命名文件"
    // 4. 截断超长，保留扩展名
    if (cleaned.length <= maxLength) return cleaned

    val dotIndex = cleaned.lastIndexOf('.')
    return if (dotIndex > 0 && dotIndex > maxLength - 10) {
        // 扩展名较短，保留扩展名截断主体
        val ext = cleaned.substring(dotIndex)
        val keep = maxLength - ext.length
        cleaned.substring(0, keep.coerceAtLeast(1)) + ext
    } else {
        cleaned.substring(0, maxLength)
    }
}

/**
 * 从 Content URI 选取文件后，拷贝到应用本地目录，写入数据库。
 *
 * 流程：
 * 1. 从 content:// URI 读取元数据（文件名、大小）
 * 2. 将文件内容拷贝到 app 私有目录 files/uploads/
 * 3. 以本地 file:// 路径存入数据库
 *
 * 为什么拷贝到本地目录？content:// URI 的读权限是临时的，
 * ExoPlayer 等后台组件无法持续访问，拷贝到本地目录彻底解决权限问题。
 */
internal fun uploadSelectedFile(
    context: Context,
    uri: Uri,
    viewModel: FileListViewModel,
    existingFiles: List<CloudFile>
) {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri)
    val fileType = mimeTypeToFileType(mimeType)
    var fileName = "未命名文件"
    var fileSize = 0L

    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) fileName = cursor.getString(nameIndex) ?: fileName
            if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
        }
    }

    val cleanName = sanitizeFileName(fileName)
    val uniqueName = generateUniqueName(cleanName, existingFiles.map { it.name }.toSet())

    // 拷贝到应用本地目录（失败时提示用户）
    val localUri = try {
        copyToAppStorage(context, uri, uniqueName)
    } catch (_: Exception) {
        Toast.makeText(context, "文件保存失败，可能磁盘空间不足", Toast.LENGTH_SHORT).show()
        return
    }
    val coverUri = if (fileType == "video") {
        createVideoCover(context, localUri)?.toString()
    } else {
        null
    }

    viewModel.uploadFile(
        name = uniqueName,
        size = fileSize,
        uri = localUri.toString(),
        coverUri = coverUri,
        type = fileType
    )
}

/**
 * 将 content:// 文件拷贝到 app 私有目录 files/uploads/
 * @return 本地文件的 file:// URI
 * @throws IllegalStateException 拷贝失败时抛出（磁盘满、权限不足等）
 */
private fun copyToAppStorage(context: Context, sourceUri: Uri, fileName: String): Uri {
    val uploadDir = File(context.filesDir, "uploads")
    if (!uploadDir.exists()) uploadDir.mkdirs()

    val localFile = File(uploadDir, fileName)

    context.contentResolver.openInputStream(sourceUri)?.use { input ->
        localFile.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: throw IllegalStateException("无法读取源文件")

    return Uri.fromFile(localFile)
}

internal fun createVideoCover(context: Context, videoUri: Uri): Uri? {
    val coverDir = File(context.filesDir, "covers")
    if (!coverDir.exists()) coverDir.mkdirs()

    val coverFile = File(coverDir, "${UUID.randomUUID()}.jpg")
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, videoUri)
        val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: return null
        val cover = frame.scaleToCover(maxWidth = 320)
        coverFile.outputStream().use { output ->
            cover.compress(Bitmap.CompressFormat.JPEG, 78, output)
        }
        Uri.fromFile(coverFile)
    } catch (_: Exception) {
        null
    } finally {
        retriever.release()
    }
}

private fun Bitmap.scaleToCover(maxWidth: Int): Bitmap {
    if (width <= maxWidth) return this
    val ratio = maxWidth.toFloat() / width
    val targetHeight = (height * ratio).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, maxWidth, targetHeight, true)
}
