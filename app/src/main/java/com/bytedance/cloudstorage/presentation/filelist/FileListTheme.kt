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
internal val BgGray           = Color.White
internal val PrimaryBlue      = Color(0xFF3370FF)
internal val PrimaryBlueBg    = Color(0xFFEBF0FF)
internal val CapsuleBg        = Color(0xFFEBEDF0)
internal val TextPrimary      = Color(0xFF1D2129)
internal val TextSecondary    = Color(0xFF8C93A4)
internal val DividerColor     = Color(0xFFF0F2F5)
internal val IconFolderBg     = Color(0xFFF0F5FF)
internal val IconFolderTint   = Color(0xFF6366F1)
internal val IconVideoBg      = Color(0xFFFFF0F5)
internal val IconVideoTint    = Color(0xFFEB2F96)
internal val IconDocBg        = Color(0xFFE6F7FF)
internal val IconDocTint      = Color(0xFF1890FF)
internal val EmptyIconBg      = Color(0xFFF0F2F5)
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
