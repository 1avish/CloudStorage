package com.bytedance.cloudstorage.presentation.filelist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytedance.cloudstorage.domain.model.CloudFile
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws

// ────────────────────────────────────────────────
// 底部操作栏：非模态版本（选择模式下固定在底部，不遮挡背景）
// ────────────────────────────────────────────────

/**
 * 选择模式底部操作栏
 *
 * 顶部横滑展示已选文件标签，底部排列操作按钮（下载、分享、删除、移动、重命名）。
 * 重命名在多选状态下禁用，仅单选时可用。
 *
 * @param selectedFiles 当前已选中的文件列表
 * @param onDownload    下载回调
 * @param onShare       分享回调
 * @param onDelete      删除回调
 * @param onMove        移动回调（打开移动目标选择器）
 * @param onRename      重命名回调（传入选中的文件）
 * @param onToggleFile  取消选中回调（传入文件 ID）
 */
@Composable
internal fun FileActionBar(
    selectedFiles: List<CloudFile>,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onRename: (CloudFile) -> Unit,
    onToggleFile: (String) -> Unit,
) {
    val multiSelect = selectedFiles.size > 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.w.dp)
            .padding(top = 8.w.dp, bottom = 16.w.dp)
    ) {
        // ── 操作按钮行 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.w.dp)
        ) {
            ActionButton(
                icon = Icons.Default.ArrowDownward,
                label = "下载",
                onClick = onDownload
            )
            ActionButton(
                icon = Icons.Default.Share,
                label = "分享",
                onClick = onShare
            )
            ActionButton(
                icon = Icons.Default.Delete,
                label = "删除",
                onClick = onDelete
            )
            ActionButton(
                icon = Icons.Default.DriveFileMove,
                label = "移动",
                onClick = onMove
            )
            ActionButton(
                icon = Icons.Default.Edit,
                label = "重命名",
                enabled = !multiSelect,
                onClick = {
                    if (!multiSelect && selectedFiles.isNotEmpty()) {
                        onRename(selectedFiles.first())
                    }
                }
            )
        }
    }
}

// ── 已选文件标签（横滑列表中的每一项） ──

@Composable
internal fun SelectedFileChip(
    file: CloudFile,
    onRemove: () -> Unit,
) {
    val (icon, _, iconTint) = remember(file.type) { fileStyle(file.type) }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.w.dp))
            .background(Color(0xFFF5F7FA))
            .padding(horizontal = 10.w.dp, vertical = 8.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.w.dp)
        )
        Spacer(modifier = Modifier.width(6.w.dp))
        Text(
            text = file.name,
            fontSize = 14.ws.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 100.w.dp)
        )
        Spacer(modifier = Modifier.width(6.w.dp))
        Box(
            modifier = Modifier
                .size(18.w.dp)
                .clip(CircleShape)
                .background(EmptyIconTint.copy(alpha = 0.3f))
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "取消选择",
                tint = TextSecondary,
                modifier = Modifier.size(12.w.dp)
            )
        }
    }
}

// ── 操作按钮 ──

@Composable
internal fun RowScope.ActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(8.w.dp))
            .background(Color(0xFFF0F0F0))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 7.w.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF111111),
            modifier = Modifier.size(22.w.dp)
        )
        Spacer(modifier = Modifier.height(3.w.dp))
        Text(
            text = label,
            fontSize = 10.ws.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) TextPrimary else TextSecondary
        )
    }
}
