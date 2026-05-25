package com.bytedance.cloudstorage.presentation.filelist

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.bytedance.cloudstorage.domain.model.CloudFile

// ────────────────────────────────────────────────
// 文件上传辅助函数
// ────────────────────────────────────────────────

/** MIME 类型 → 文件分类类型键 */
internal fun mimeTypeToFileType(mimeType: String?): String = when {
    mimeType == null                -> "other"
    mimeType.startsWith("video/")  -> "video"
    mimeType.startsWith("text/")   -> "txt"
    mimeType == "application/pdf"  -> "txt"
    else                           -> "other"
}

/**
 * 生成去重后的文件名。
 *
 * 如果 [desiredName] 不在 [existingNames] 中，直接返回原名；
 * 否则在扩展名前追加 "（1）"、"（2）" 等序号直到不冲突。
 *
 * 示例：
 * - "hello.txt" 已存在 → "hello（1）.txt"
 * - "README" 已存在   → "README（1）"
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
        extension = desiredName.substring(dotIndex) // 含 "."
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

/**
 * 从 Content URI 读取文件元数据并写入本地数据库。
 *
 * 流程：
 * 1. ContentResolver.query() 读取 DISPLAY_NAME 和 SIZE
 * 2. 对文件名做重名检测，必要时追加序号
 * 3. 将数据封装为 FileEntity，通过 ViewModel 存入 Room
 * 4. 数据库变更自动触发 Room Flow → UI 即时刷新
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

    val uniqueName = generateUniqueName(fileName, existingFiles.map { it.name }.toSet())

    viewModel.uploadFile(
        name = uniqueName,
        size = fileSize,
        uri = uri.toString(),
        type = fileType
    )
}
