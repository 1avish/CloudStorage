package com.bytedance.cloudstorage.presentation.videoplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun FullscreenPlayer(
    onBack: () -> Unit,
    viewModel: VideoPlayerViewModel
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val durationMs by viewModel.duration.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val isPlayerReady by viewModel.isPlayerReady.collectAsStateWithLifecycle()
    val player = viewModel.exoPlayer

    var showControls by remember { mutableStateOf(true) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    LaunchedEffect(showControls, showSpeedMenu) {
        if (showControls && !showSpeedMenu) {
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
        if (isPlayerReady && player != null) {
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

        if (showControls || showSpeedMenu) {
            FullscreenTopControls(onBack = onBack)
            FullscreenBottomControls(
                isPlaying = isPlaying,
                playbackSpeed = playbackSpeed,
                durationMs = durationMs,
                currentPosition = currentPosition,
                showSpeedMenu = showSpeedMenu,
                onShowSpeedMenu = { showSpeedMenu = true },
                onDismissSpeedMenu = { showSpeedMenu = false },
                onSpeedSelected = { speed ->
                    showSpeedMenu = false
                    showControls = true
                    viewModel.setPlaybackSpeed(speed)
                },
                onTogglePlay = {
                    showControls = true
                    viewModel.togglePlayPause()
                },
                onSeek = {
                    showControls = true
                    viewModel.seekTo(it)
                },
                viewModel = viewModel
            )
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
    playbackSpeed: Float,
    durationMs: Long,
    currentPosition: Long,
    showSpeedMenu: Boolean,
    onShowSpeedMenu: () -> Unit,
    onDismissSpeedMenu: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
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
                PlayPauseButton(
                    isPlaying = isPlaying,
                    onTogglePlay = onTogglePlay
                )

                SpeedSelector(
                    playbackSpeed = playbackSpeed,
                    expanded = showSpeedMenu,
                    onShow = onShowSpeedMenu,
                    onDismiss = onDismissSpeedMenu,
                    onSpeedSelected = onSpeedSelected
                )
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
            FullscreenProgressBar(viewModel, durationMs, onSeek)
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

@Composable
private fun SpeedSelector(
    playbackSpeed: Float,
    expanded: Boolean,
    onShow: () -> Unit,
    onDismiss: () -> Unit,
    onSpeedSelected: (Float) -> Unit
) {
    Box {
        Row(
            modifier = Modifier
                .clickable { onShow() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                formatSpeed(playbackSpeed),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                "选择倍速",
                tint = Color.White.copy(alpha = 0.82f),
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            containerColor = Color.White,
            shape = RoundedCornerShape(14.dp)
        ) {
            PlaybackSpeeds.forEach { speed ->
                DropdownMenuItem(
                    text = {
                        Text(
                            formatSpeed(speed),
                            fontSize = 13.sp,
                            fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal,
                            color = if (speed == playbackSpeed) ProgressBlue else TextPrimary
                        )
                    },
                    onClick = { onSpeedSelected(speed) }
                )
            }
        }
    }
}

@Composable
private fun FullscreenProgressBar(
    viewModel: VideoPlayerViewModel,
    durationMs: Long,
    onSeek: (Float) -> Unit
) {
    val currentPos by viewModel.currentPosition.collectAsStateWithLifecycle()
    val progress = if (durationMs > 0) (currentPos.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    var trackWidthPx by remember { mutableStateOf(0f) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var pendingSeekFraction by remember { mutableStateOf<Float?>(null) }
    val displayProgress = dragFraction ?: pendingSeekFraction ?: progress

    LaunchedEffect(currentPos, durationMs, pendingSeekFraction, dragFraction) {
        val targetFraction = pendingSeekFraction ?: return@LaunchedEffect
        if (dragFraction != null || durationMs <= 0L) return@LaunchedEffect

        val targetPos = (targetFraction * durationMs).toLong()
        if (abs(currentPos - targetPos) <= 500L) {
            pendingSeekFraction = null
        }
    }

    LaunchedEffect(pendingSeekFraction) {
        if (pendingSeekFraction != null) {
            delay(700)
            pendingSeekFraction = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (trackWidthPx > 0f) {
                            dragFraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        }
                    },
                    onDragEnd = {
                        dragFraction?.let {
                            pendingSeekFraction = it
                            onSeek(it)
                        }
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                    onHorizontalDrag = { change, _ ->
                        if (trackWidthPx > 0f) {
                            dragFraction = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                        }
                        change.consume()
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.28f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayProgress)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ProgressBlue)
            )
        }
        if (trackWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset((trackWidthPx * displayProgress - 7.dp.toPx()).roundToInt(), 0) }
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
