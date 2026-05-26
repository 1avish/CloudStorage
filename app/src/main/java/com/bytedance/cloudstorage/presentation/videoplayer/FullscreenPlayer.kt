package com.bytedance.cloudstorage.presentation.videoplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun FullscreenPlayer(
    onBack: () -> Unit,
    viewModel: VideoPlayerViewModel
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val durationMs by viewModel.duration.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val isPlayerReady by viewModel.isPlayerReady.collectAsStateWithLifecycle()
    val player = viewModel.exoPlayer

    var showControls by remember { mutableStateOf(true) }
    LaunchedEffect(showControls) {
        if (showControls) { delay(2000); showControls = false }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 播放器画面
        if (isPlayerReady && player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = true },
                            onDoubleTap = { viewModel.togglePlayPause() }
                        )
                    }
            )
        }

        // ── 顶部控制栏 ──
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(64.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "退出全屏",
                            tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(28.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { /* 投屏 */ }, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Filled.Cast, "投屏",
                                tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // ── 底部控制栏 ──
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).align(Alignment.BottomCenter)
                ) {
                    FullscreenProgressBar(viewModel, durationMs) { viewModel.seekTo(it) }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.togglePlayPause() }, modifier = Modifier.size(36.dp)) {
                                Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    if (isPlaying) "暂停" else "播放",
                                    tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                "${formatTime(currentPosition)} / ${formatTime(durationMs)}",
                                fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f), letterSpacing = 0.5.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            IconButton(onClick = { /* 音量 */ }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, "音量",
                                    tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.FullscreenExit, "退出全屏",
                                    tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenProgressBar(viewModel: VideoPlayerViewModel, durationMs: Long, onSeek: (Float) -> Unit) {
    val currentPos by viewModel.currentPosition.collectAsStateWithLifecycle()
    val progress = if (durationMs > 0) (currentPos.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    var trackWidthPx by remember { mutableStateOf(0f) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val displayProgress = dragFraction ?: progress

    Box(
        modifier = Modifier
            .fillMaxWidth().height(32.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (trackWidthPx > 0f) dragFraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                    },
                    onDragEnd = { dragFraction?.let { onSeek(it) }; dragFraction = null },
                    onDragCancel = { dragFraction = null },
                    onHorizontalDrag = { _, dragAmount ->
                        if (trackWidthPx > 0f) {
                            val delta = dragAmount / trackWidthPx
                            dragFraction = ((dragFraction ?: progress) + delta).coerceIn(0f, 1f)
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(displayProgress).height(3.dp).clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF2979FF))
            )
        }
        if (trackWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset((trackWidthPx * displayProgress - 6.dp.toPx()).roundToInt(), 0) }
                    .size(12.dp).clip(CircleShape).background(Color.White)
            )
        }
    }
}
