package com.bytedance.cloudstorage.presentation.filelist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytedance.cloudstorage.domain.model.CloudFile
import com.bytedance.cloudstorage.domain.model.FileType
import com.bytedance.cloudstorage.presentation.common.VideoCoverThumbnail
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ────────────────────────────────────────────────
// 文件列表项（严格对齐设计稿）
// ────────────────────────────────────────────────

@Stable
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FileListItem(
    file: CloudFile,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    showDivider: Boolean = true,
    onCircleClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onFolderClick: ((String) -> Unit)? = null,
    onFileClick: (() -> Unit)? = null,
) {
    val (icon, iconBg, iconTint) = remember(file.type) { fileStyle(file.type) }
    val formattedSize = remember(file.size, file.type) {
        if (file.type != FileType.Folder && file.size > 0) formatFileSize(file.size) else null
    }
    val formattedTime = remember(file.updatedAt) { formatTimestamp(file.updatedAt) }

    // 圆圈选中静态样式
    val circleBg = if (isSelected) Color(0xFF3370FF) else Color.Transparent
    val circleBorder = if (isSelected) Color(0xFF3370FF) else EmptyIconTint
    val rowBg = if (isSelected) Color(0xFFEBF0FF) else Color.Transparent

    Column(modifier = Modifier.height(74.w.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.w.dp)
                .background(rowBg)
                .then(
                    if (isSelectionMode) {
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onCircleClick?.invoke() }
                    } else if (onLongPress != null) {
                        Modifier.combinedClickable(
                            onClick = {
                                if (file.type == FileType.Folder && onFolderClick != null) {
                                    onFolderClick(file.id)
                                } else if (file.type != FileType.Folder && onFileClick != null) {
                                    onFileClick()
                                }
                            },
                            onLongClick = { onLongPress() }
                        )
                    } else if (file.type == FileType.Folder && onFolderClick != null) {
                        Modifier.clickable { onFolderClick(file.id) }
                    } else if (file.type != FileType.Folder && onFileClick != null) {
                        Modifier.clickable { onFileClick() }
                    } else Modifier
                )
                .padding(horizontal = 16.w.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (file.type == FileType.Video && !file.coverUri.isNullOrEmpty()) {
                VideoCoverThumbnail(
                    coverUri = file.coverUri,
                    modifier = Modifier.size(46.w.dp),
                    cornerRadiusDp = 14,
                    showPlayIcon = false,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(46.w.dp)
                        .clip(RoundedCornerShape(14.w.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.w.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.w.dp))

            // 文件名 + 元信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 16.ws.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.w.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (formattedSize != null) {
                        Text(
                            text = formattedSize,
                            fontSize = 12.ws.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = " | ",
                            fontSize = 12.ws.sp,
                            color = DividerColor
                        )
                    }
                    Text(
                        text = formattedTime,
                        fontSize = 12.ws.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }

            // 右侧圆形选择按钮
            Box(
                modifier = Modifier
                    .size(40.w.dp, 72.w.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onCircleClick?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.w.dp)
                        .border(
                            width = if (isSelected) 0.dp else 1.5f.w.dp,
                            color = circleBorder,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .background(circleBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "已选中",
                            tint = Color.White,
                            modifier = Modifier.size(14.w.dp)
                        )
                    }
                }
            }
        }

        // 底部分割线（仅 n-1 条）
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.w.dp)
                    .height((2f).w.dp)
                    .background(DividerColor)
            )
        }
    }
}

// ────────────────────────────────────────────────
// 空状态（大文件夹图标 + 标题 + 副标题）
// ────────────────────────────────────────────────

@Composable
internal fun EmptyFileList() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.w.dp)
                    .clip(CircleShape)
                    .background(EmptyIconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = EmptyIconTint,
                    modifier = Modifier.size(48.w.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.w.dp))
            Text(
                text = "暂无文件",
                fontSize = 16.ws.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.w.dp))
        }
    }
}

// ────────────────────────────────────────────────
// 辅助函数
// ────────────────────────────────────────────────

internal fun formatFileSize(bytes: Long): String = when {
    bytes <= 0L                      -> "--"
    bytes < 1024                   -> "${bytes}B"
    bytes < 1024 * 1024            -> "%.1fKB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024    -> "%.1fMB".format(bytes / (1024.0 * 1024))
    else                           -> "%.1fGB".format(bytes / (1024.0 * 1024 * 1024))
}

internal fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "--"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
