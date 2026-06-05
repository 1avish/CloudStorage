package com.bytedance.cloudstorage.data.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Base64
import com.bytedance.cloudstorage.data.local.database.AppDatabase
import com.bytedance.cloudstorage.data.local.entity.ShareLinkEntity
import com.bytedance.cloudstorage.data.local.entity.ShareLinkFileEntity
import java.security.SecureRandom

// ────────────────────────────────────────────────
// 分享链接数据模型
// ────────────────────────────────────────────────

/**
 * 创建分享链接的返回结果。
 *
 * @property token 分享令牌，用于解析文件列表
 * @property url   完整的自定义 scheme 链接，如 cloudstorage://share/<token>
 */
data class CreatedShareLink(
    val token: String,
    val url: String,
)

// ────────────────────────────────────────────────
// 分享链接本地存储（基于 Room）
// ────────────────────────────────────────────────

/**
 * 分享链接本地持久化存储。
 *
 * 数据流：
 * 1. 用户在文件列表页选中文件 → createShare(fileIds) → 生成 token + 写入 Room + 返回链接
 * 2. 外部通过 deeplink 打开 → parseToken(uri) → getFileIds(token) → 还原文件列表
 */
class ShareLinkStore(context: Context) {
    private val appContext = context.applicationContext
    private val shareLinkDao = AppDatabase.getInstance(appContext).shareLinkDao()

    /**
     * 创建分享链接。
     *
     * 生成随机 token，将去重后的文件 ID 列表写入数据库，
     * 返回包含 token 和完整 URL 的 [CreatedShareLink]。
     */
    suspend fun createShare(fileIds: List<String>): CreatedShareLink {
        val token = generateToken()
        val distinctIds = fileIds.distinct()
        val now = System.currentTimeMillis()
        shareLinkDao.createShare(
            link = ShareLinkEntity(token = token, createdAt = now),
            files = distinctIds.mapIndexed { index, fileId ->
                ShareLinkFileEntity(
                    token = token,
                    fileId = fileId,
                    sortOrder = index,
                )
            }
        )
        return CreatedShareLink(token = token, url = buildUrl(token))
    }

    /**
     * 根据 token 还原文件 ID 列表，token 无效或数据损坏时返回 null。
     */
    suspend fun getFileIds(token: String): List<String>? {
        if (!hasShare(token)) return null
        return shareLinkDao.getFileIds(token).takeIf { it.isNotEmpty() }
    }

    /**
     * 检查指定 token 是否存在有效分享记录。
     */
    suspend fun hasShare(token: String): Boolean =
        shareLinkDao.getActiveShareCount(token) > 0

    suspend fun shouldAutoPrompt(token: String): Boolean =
        shareLinkDao.getAutoPromptableShareCount(token) > 0

    suspend fun markHandled(token: String, action: ShareLinkHandledAction) {
        shareLinkDao.markHandled(
            token = token,
            action = action.value,
            handledAt = System.currentTimeMillis(),
        )
    }

    /**
     * 将分享链接复制到系统剪贴板。
     */
    fun copyToClipboard(link: CreatedShareLink) {
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("资源分享链接", link.url))
    }

    /**
     * 从系统剪贴板读取并解析分享链接，未匹配到有效链接时返回 null。
     * 部分 ROM 对 coerceToText 会抛异常，此处统一捕获。
     */
    fun readTokenFromClipboard(): String? {
        return try {
            val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (!clipboard.hasPrimaryClip()) return null
            val clip = clipboard.primaryClip ?: return null
            if (clip.itemCount == 0) return null
            val item = clip.getItemAt(0)
            val text = item.coerceToText(appContext)?.toString()?.trim().orEmpty()
            parseToken(text)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val SCHEME = "cloudstorage"
        private const val HOST = "share"

        /**
         * 根据 token 构建自定义 scheme 链接：cloudstorage://share/<token>
         */
        fun buildUrl(token: String): String = "$SCHEME://$HOST/$token"

        /**
         * 从原始文本中提取 token，支持完整 URL 和纯 token 两种格式。
         */
        fun parseToken(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val trimmed = raw.trim()
            // 先尝试作为 URI 解析，失败则当作纯 token 处理
            val tokenFromUri = runCatching { Uri.parse(trimmed) }
                .getOrNull()
                ?.let(::parseToken)
            return tokenFromUri ?: trimmed.takeIf { it.matches(Regex("[A-Za-z0-9_-]{24}")) }
        }

        /**
         * 从 URI 中提取 token。
         *
         * 优先从 path segment 提取（cloudstorage://share/<token>），
         * 其次从 query parameter 提取（cloudstorage://share?token=<token>）。
         */
        fun parseToken(uri: Uri?): String? {
            if (uri == null) return null
            if (uri.scheme != SCHEME || uri.host != HOST) return null
            return uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: uri.getQueryParameter("token")?.takeIf { it.isNotBlank() }
        }

        /**
         * 生成 24 字符的 URL 安全随机 token。
         */
        private fun generateToken(): String {
            val bytes = ByteArray(18)
            SecureRandom().nextBytes(bytes)
            return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }
    }
}

enum class ShareLinkHandledAction(val value: String) {
    Opened("opened"),
    Saved("saved"),
    Dismissed("dismissed"),
}
