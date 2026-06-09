package com.bytedance.cloudstorage.presentation.videoplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytedance.cloudstorage.presentation.common.VideoCoverThumbnail
import com.bytedance.cloudstorage.presentation.filelist.formatFileSize
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws

@Composable
internal fun EpisodeSection(
    episodes: List<Episode>,
    activeEpisode: Episode,
    onEpisodeClick: (Episode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PageBg)
            .padding(horizontal = 18.w.dp)
            .padding(top = 34.w.dp)
    ) {
        Text(
            "目录 · 共${episodes.size}个",
            fontSize = 18.ws.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary.copy(alpha = 0.88f)
        )
        Spacer(modifier = Modifier.height(18.w.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.w.dp)
        ) {
            items(episodes, key = { it.id }) { episode ->
                EpisodeRow(
                    episode = episode,
                    isActive = episode.id == activeEpisode.id,
                    onClick = { onEpisodeClick(episode) }
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, isActive: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(145.w.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.w.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF121212), Color(0xFF242424)))),
            contentAlignment = Alignment.Center
        ) {
            if (!episode.coverUri.isNullOrEmpty()) {
                VideoCoverThumbnail(
                    coverUri = episode.coverUri,
                    modifier = Modifier.matchParentSize(),
                    cornerRadiusDp = 0,
                    showPlayIcon = false,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.68f),
                    modifier = Modifier.size(30.w.dp)
                )
            }

            if (isActive) {
                Icon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = "播放中",
                    tint = Color.White,
                    modifier = Modifier.size(36.w.dp)
                )
            }

            if (episode.duration.isNotEmpty()) {
                Text(
                    episode.duration,
                    fontSize = 14.ws.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.w.dp, bottom = 6.w.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.w.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .height(82.w.dp)
                .padding(top = 1.w.dp, bottom = 2.w.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                episode.title,
                fontSize = 14.ws.sp,
                lineHeight = 20.ws.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                color = if (isActive) TextPrimary else TextPrimary.copy(alpha = 0.66f)
            )
            Text(
                buildString {
                    append(formatFileSize(episode.size))
                },
                fontSize = 10.ws.sp,
                color = TextSecondary.copy(alpha = if (isActive) 0.7f else 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
