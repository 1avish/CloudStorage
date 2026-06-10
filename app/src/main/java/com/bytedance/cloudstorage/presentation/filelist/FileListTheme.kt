package com.bytedance.cloudstorage.presentation.filelist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.bytedance.cloudstorage.domain.model.FileType

// ────────────────────────────────────────────────
// 颜色常量，严格对齐设计稿 App.tsx
// ────────────────────────────────────────────────
/** 背景色 */
internal val BgGray           = Color.White
/** 主色（黄） */
internal val PrimaryBlue      = Color(0xFFFFE36A)
/** 主色浅底 */
internal val PrimaryBlueBg    = Color(0xFFFFF4BA)
/** 胶囊筛选背景 */
internal val CapsuleBg        = Color(0xFFEBEDF0)
/** 标题文字色 */
internal val TextPrimary      = Color(0xFF1D2129)
/** 次要文字色 */
internal val TextSecondary    = Color(0xFF8C93A4)
/** 分隔线色 */
internal val DividerColor     = Color(0xFFF0F2F5)
/** 文件夹图标背景 */
internal val IconFolderBg     = Color(0xFFF0F5FF)
/** 文件夹图标色 */
internal val IconFolderTint   = Color(0xFF6366F1)
/** 视频图标背景 */
internal val IconVideoBg      = Color(0xFFFFF0F5)
/** 视频图标色 */
internal val IconVideoTint    = Color(0xFFEB2F96)
/** 文档图标背景 */
internal val IconDocBg        = Color(0xFFE6F7FF)
/** 文档图标色 */
internal val IconDocTint      = Color(0xFF1890FF)
/** 空文件图标背景 */
internal val EmptyIconBg      = Color(0xFFF0F2F5)
/** 空文件图标色 */
internal val EmptyIconTint    = Color(0xFFC0C4D0)

// ────────────────────────────────────────────────
// 胶囊筛选项（不含文件夹）
// ────────────────────────────────────────────────

internal enum class FileFilter(val label: String, val typeKey: String?) {
    All("全部", null),
    Video("视频", "video"),
    Doc("文档", "txt")
}

// ────────────────────────────────────────────────
// 辅助函数
// ────────────────────────────────────────────────

internal fun fileStyle(type: FileType): Triple<ImageVector, Color, Color> = when (type) {
    FileType.Folder -> Triple(Icons.Default.Folder, IconFolderBg, IconFolderTint)
    FileType.Video  -> Triple(Icons.Default.OndemandVideo, IconVideoBg, IconVideoTint)
    FileType.Txt    -> Triple(Icons.Default.Description, IconDocBg, IconDocTint)
    FileType.Other  -> Triple(Icons.Default.Description, IconDocBg, IconDocTint)
}
