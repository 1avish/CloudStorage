package com.bytedance.cloudstorage.presentation.videoplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@UnstableApi
@Composable
fun FullscreenPlayer(
    onBack: () -> Unit,
    viewModel: VideoPlayerViewModel
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val durationMs by viewModel.duration.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val isPlayerReady by viewModel.isPlayerReady.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
    val player = viewModel.exoPlayer

    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(2500)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { viewModel.togglePlayPause() }
                )
            }
    ) {
        when {
            isPlayerReady && player != null -> FullscreenVideoSurface(player = player)
            playbackError -> FullscreenError(onRetry = { viewModel.retryPlay() })
        }

        if (showControls) {
            FullscreenTopControls(onBack = onBack)
            FullscreenBottomControls(
                isPlaying = isPlaying,
                durationMs = durationMs,
                currentPosition = currentPosition,
                onTogglePlay = {
                    showControls = true
                    viewModel.togglePlayPause()
                },
                onSeek = {
                    showControls = true
                    viewModel.seekTo(it)
                },
                onExitFullscreen = onBack,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun FullscreenVideoSurface(player: androidx.media3.exoplayer.ExoPlayer) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                controllerAutoShow = false
                controllerHideOnTouch = false
                hideController()
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            view.useController = false
            view.controllerAutoShow = false
            view.controllerHideOnTouch = false
            if (view.player !== player) {
                view.player = player
            }
            view.hideController()
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun FullscreenError(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("视频加载失败", fontSize = 15.sp, color = Color.White.copy(alpha = 0.65f))
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onRetry() }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Refresh, "重试", tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("重试", fontSize = 14.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun BoxScope.FullscreenTopControls(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .align(Alignment.TopCenter)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.68f), Color.Transparent)))
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 10.dp)
                .size(44.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                "返回",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun BoxScope.FullscreenBottomControls(
    isPlaying: Boolean,
    durationMs: Long,
    currentPosition: Long,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onExitFullscreen: () -> Unit,
    viewModel: VideoPlayerViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FullscreenProgressRow(
                currentPosition = currentPosition,
                durationMs = durationMs,
                viewModel = viewModel,
                onSeek = onSeek
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayPauseButton(isPlaying = isPlaying, onTogglePlay = onTogglePlay)

                IconButton(onClick = onExitFullscreen, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Filled.FullscreenExit,
                        "退出全屏",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FullscreenProgressRow(
    currentPosition: Long,
    durationMs: Long,
    viewModel: VideoPlayerViewModel,
    onSeek: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            formatTime(currentPosition),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            VideoProgressBar(
                viewModel = viewModel,
                durationMs = durationMs,
                onSeek = onSeek,
            )
        }
        Text(
            formatTime(durationMs),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.68f)
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable { onTogglePlay() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            if (isPlaying) "暂停" else "播放",
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}
