package com.bytedance.cloudstorage.presentation.videoplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytedance.cloudstorage.presentation.filelist.formatFileSize
import com.bytedance.cloudstorage.presentation.filelist.formatTimestamp
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws

// ────────────────────────────────────────────────
// 视频信息卡片（标题 + 大小/时间 + 操作按钮）
// ────────────────────────────────────────────────

/**
 * 视频信息卡片，展示当前视频的标题、更新时间、文件大小，以及下载/分享操作按钮。
 *
 * @param title     视频标题
 * @param updatedAt 更新时间戳（毫秒）
 * @param size      文件大小（字节）
 * @param onDownload 下载按钮回调
 * @param onShare    分享按钮回调
 */
@Composable
internal fun VideoInfoCard(
    title: String,
    updatedAt: Long,
    size: Long,
    onDownload: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PageBg)
            .padding(horizontal = 18.w.dp)
            .padding(top = 28.w.dp)
    ) {
        Text(
            title,
            fontSize = 18.ws.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            lineHeight = 24.ws.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(14.w.dp))
        Text(
            "${formatTimestamp(updatedAt)} | ${formatFileSize(size)}",
            fontSize = 16.ws.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(24.w.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.w.dp)
        ) {
            VideoActionButton(
                label = "下载",
                icon = Icons.Filled.ArrowDownward,
                onClick = onDownload,
                modifier = Modifier.weight(1f)
            )
            VideoActionButton(
                label = "分享",
                icon = Icons.Filled.Share,
                onClick = onShare,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VideoActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(50.w.dp)
            .clip(RoundedCornerShape(18.w.dp))
            .background(Color(0xFF151515))
            .clickable { onClick() }
            .padding(horizontal = 18.w.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = TextPrimary, modifier = Modifier.size(26.w.dp))
        Spacer(modifier = Modifier.size(13.w.dp))
        Text(label, fontSize = 18.ws.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}
