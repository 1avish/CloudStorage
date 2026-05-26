package com.bytedance.cloudstorage.presentation.videoplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws

// ────────────────────────────────────────────────
// 选集列表（横向滚动）
// ────────────────────────────────────────────────

@Composable
internal fun EpisodeSection(
    episodes: List<Episode>,
    activeEpisode: Episode,
    onEpisodeClick: (Episode) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.w.dp, vertical = 12.w.dp)
            .shadow(4.w.dp, RoundedCornerShape(16.w.dp))
            .clip(RoundedCornerShape(16.w.dp))
            .background(Color.White)
            .padding(top = 16.w.dp, bottom = 16.w.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.w.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("选集", fontSize = 15.ws.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.width(8.w.dp))
            Text("同目录视频，按文件名排序", fontSize = 11.ws.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(14.w.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.w.dp),
            horizontalArrangement = Arrangement.spacedBy(10.w.dp)
        ) {
            items(episodes, key = { it.id }) { ep ->
                EpisodeCard(ep, isActive = ep.id == activeEpisode.id, onClick = { onEpisodeClick(ep) })
            }
        }
    }
}

// ────────────────────────────────────────────────
// 选集卡片（缩略图 + 标题 + 播放状态）
// ────────────────────────────────────────────────

@Composable
private fun EpisodeCard(episode: Episode, isActive: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.w.dp)
    Column(
        modifier = Modifier
            .width(104.w.dp)
            .clip(shape)
            .border(if (isActive) 2.w.dp else 1.w.dp, if (isActive) ProgressBlue else BorderGray, shape)
            .clickable { onClick() }
    ) {
        // 缩略图区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Brush.linearGradient(listOf(Color(0xFF0D2240), Color(0xFF163552)))),
            contentAlignment = Alignment.Center
        ) {
            // 时长标签
            if (episode.duration.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 6.w.dp, bottom = 6.w.dp)
                        .clip(RoundedCornerShape(4.w.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 4.w.dp, vertical = 2.w.dp)
                ) {
                    Text(episode.duration, fontSize = 9.ws.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
            // 播放中标签
            if (isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 6.w.dp, top = 6.w.dp)
                        .clip(RoundedCornerShape(5.w.dp))
                        .background(ProgressBlue)
                        .padding(horizontal = 6.w.dp, vertical = 2.w.dp)
                ) {
                    Text("播放中", fontSize = 9.ws.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isActive) BlueSoft else Color(0xFFF8F9FB))
                .padding(horizontal = 8.w.dp, vertical = 8.w.dp)
        ) {
            Text(
                episode.title,
                fontSize = 11.ws.sp,
                lineHeight = 15.ws.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) ProgressBlue else TextPrimary
            )
        }
    }
}
