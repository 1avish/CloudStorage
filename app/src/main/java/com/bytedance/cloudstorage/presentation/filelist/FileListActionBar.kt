package com.bytedance.cloudstorage.presentation.filelist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
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

@Composable
internal fun FileActionBar(
    selectedFiles: List<CloudFile>,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRename: (CloudFile) -> Unit,
    onToggleFile: (String) -> Unit,
) {
    val multiSelect = selectedFiles.size > 1
    val primaryBlue = Color(0xFF3370FF)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.w.dp)
            .padding(top = 12.w.dp, bottom = 24.w.dp)
    ) {
        // ── 已选文件横滑列表 ──
        if (selectedFiles.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.w.dp),
                contentPadding = PaddingValues(horizontal = 2.w.dp)
            ) {
                items(selectedFiles, key = { it.id }) { file ->
                    SelectedFileChip(
                        file = file,
                        onRemove = { onToggleFile(file.id) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.w.dp))
            HorizontalDivider(color = DividerColor, thickness = 1.w.dp)
            Spacer(modifier = Modifier.height(16.w.dp))
        }

        // ── 操作按钮行 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(
                icon = Icons.Default.ArrowDownward,
                label = "下载",
                iconTint = primaryBlue,
                bgColor = Color(0xFFEBF0FF),
                onClick = onDownload
            )
            ActionButton(
                icon = Icons.Default.Share,
                label = "分享",
                iconTint = Color(0xFF34C759),
                bgColor = Color(0xFFE8F8ED),
                onClick = onShare
            )
            ActionButton(
                icon = Icons.Default.Delete,
                label = "删除",
                iconTint = Color(0xFFFF3B30),
                bgColor = Color(0xFFFFEBEE),
                onClick = onDelete
            )
            ActionButton(
                icon = Icons.Default.Edit,
                label = "重命名",
                iconTint = if (multiSelect) EmptyIconTint else Color(0xFF6366F1),
                bgColor = if (multiSelect) Color(0xFFF0F2F5) else Color(0xFFF0F5FF),
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
internal fun ActionButton(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    bgColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.w.dp)
                .clip(RoundedCornerShape(14.w.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.w.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.w.dp))
        Text(
            text = label,
            fontSize = 13.ws.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) TextPrimary else TextSecondary
        )
    }
}
