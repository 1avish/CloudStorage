package com.bytedance.cloudstorage.presentation.common

// ────────────────────────────────────────────────
// 视频封面缩略图组件（支持播放图标叠加）
// ────────────────────────────────────────────────

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * 视频封面缩略图，支持 Coil 加载封面图片，可叠加半透明播放图标。
 *
 * @param coverUri      封面图片本地 URI，为 null 时仅显示深色渐变背景
 * @param cornerRadiusDp 圆角半径（dp）
 * @param showPlayIcon  是否叠加播放图标
 */
@Composable
internal fun VideoCoverThumbnail(
    coverUri: String?,
    modifier: Modifier = Modifier,
    cornerRadiusDp: Int = 12,
    showPlayIcon: Boolean = true,
) {
    val shape = RoundedCornerShape(cornerRadiusDp.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF0D2240), Color(0xFF163552)))),
        contentAlignment = Alignment.Center
    ) {
        if (!coverUri.isNullOrEmpty()) {
            AsyncImage(
                model = coverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
        if (showPlayIcon) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.38f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
