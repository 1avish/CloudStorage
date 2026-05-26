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

// ────────────────────────────────────────────────
// 视频信息卡片（标题、时间、大小、格式）
// ────────────────────────────────────────────────

@Composable
internal fun VideoInfoCard(title: String) {
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
                Text("修改时间：2026-05-24 18:36", fontSize = 12.ws.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.w.dp))
                Text("文件大小：428.6 MB", fontSize = 12.ws.sp, color = TextSecondary)
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
