package com.bytedance.cloudstorage.presentation.videoplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ────────────────────────────────────────────────
// 视频信息卡片（标题、时间、大小、格式）
// ────────────────────────────────────────────────

@Composable
internal fun VideoInfoCard(
    title: String,
    updatedAt: Long,
    size: Long,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.w.dp)
            .shadow(4.w.dp, RoundedCornerShape(16.w.dp))
            .clip(RoundedCornerShape(16.w.dp))
            .background(Color.White)
            .padding(16.w.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.w.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 15.ws.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 21.ws.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.w.dp))
                Text("修改时间：${formatVideoTimestamp(updatedAt)}", fontSize = 12.ws.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.w.dp))
                Text("文件大小：${formatVideoFileSize(size)}", fontSize = 12.ws.sp, color = TextSecondary)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.w.dp))
                    .background(BlueSoft)
                    .padding(horizontal = 8.w.dp, vertical = 4.w.dp)
            ) {
                Text("MP4", fontSize = 11.ws.sp, fontWeight = FontWeight.Medium, color = ProgressBlue)
            }
        }
    }
}

private fun formatVideoFileSize(bytes: Long): String = when {
    bytes <= 0L -> "--"
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "%.1fKB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1fMB".format(bytes / (1024.0 * 1024))
    else -> "%.1fGB".format(bytes / (1024.0 * 1024 * 1024))
}

private fun formatVideoTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "--"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
