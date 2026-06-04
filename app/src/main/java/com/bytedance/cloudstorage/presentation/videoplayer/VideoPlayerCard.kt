package com.bytedance.cloudstorage.presentation.videoplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws
import kotlinx.coroutines.delay

// ────────────────────────────────────────────────
// 视频播放器卡片（含覆盖层 + 控制栏）
// ────────────────────────────────────────────────

/**
 * 播放器卡片：ExoPlayer 画面 + 顶部/底部渐变覆盖层 + 底部控制栏。
 * 覆盖层（进度条、返回、更多按钮）2 秒无操作后自动隐藏，单击画面可重新显示。
 */
@UnstableApi
@Composable
internal fun PlayerCard(
    fileUri: String,
    isPlayerReady: Boolean,
    isPlaying: Boolean,
    playbackSpeed: Float,
    durationMs: Long,
    currentPosition: Long,
    playbackError: Boolean,
    showSpeedMenu: Boolean,
    onShowSpeedMenu: () -> Unit,
    onDismissSpeedMenu: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onBack: () -> Unit,
    onMoreClick: () -> Unit,
    onFullscreen: () -> Unit,
    onRetry: () -> Unit,
    viewModel: VideoPlayerViewModel
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.w.dp, vertical = 12.w.dp)
            .shadow(4.w.dp, RoundedCornerShape(16.w.dp))
            .clip(RoundedCornerShape(16.w.dp))
            .background(Color.White)
    ) {
        // 控制层自动隐藏（2 秒无操作）
        var showControls by remember { mutableStateOf(true) }
        LaunchedEffect(showControls) {
            if (showControls) {
                delay(2000)
                showControls = false
            }
        }

        // 播放器画面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(PlayerBg)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls },
                        onDoubleTap = { onTogglePlay() }
                    )
                }
        ) {
            val player = viewModel.exoPlayer
            val hasUri = fileUri.isNotEmpty()

            if (!hasUri) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF0B1E35), Color(0xFF12273D), Color(0xFF081520)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("无法播放此视频", fontSize = 14.ws.sp, color = Color.White.copy(alpha = 0.65f))
                }
            } else if (!isPlayerReady) {
                Box(modifier = Modifier.fillMaxSize().background(PlayerBg), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = ProgressBlue,
                        strokeWidth = 2.w.dp,
                        modifier = Modifier.size(36.w.dp)
                    )
                }
            } else if (player != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            controllerAutoShow = false
                            controllerHideOnTouch = false
                            this.player = player
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
            } else if (playbackError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF0B1E35), Color(0xFF12273D), Color(0xFF081520)))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("视频加载失败", fontSize = 14.ws.sp, color = Color.White.copy(alpha = 0.65f))
                        Spacer(modifier = Modifier.height(12.w.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.w.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { onRetry() }
                                .padding(horizontal = 20.w.dp, vertical = 8.w.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, "重试", tint = Color.White, modifier = Modifier.size(16.w.dp))
                                Spacer(modifier = Modifier.width(4.w.dp))
                                Text("重试", fontSize = 13.ws.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 顶部渐变遮罩 + 按钮
            if (showControls) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.w.dp)
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.w.dp, vertical = 10.w.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(38.w.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White, modifier = Modifier.size(28.w.dp))
                        }
                        IconButton(onClick = onMoreClick, modifier = Modifier.size(38.w.dp)) {
                            Icon(Icons.Filled.MoreHoriz, "更多", tint = Color.White, modifier = Modifier.size(28.w.dp))
                        }
                    }
                }
            }

            // 底部渐变遮罩 + 进度条
            if (showControls) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.w.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 14.w.dp, vertical = 10.w.dp)
                    ) {
                        PlayerProgressRow(viewModel, durationMs, onSeek)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(formatTime(currentPosition), fontSize = 11.ws.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Text(formatTime(durationMs), fontSize = 11.ws.sp, color = Color.White.copy(alpha = 0.62f))
                        }
                    }
                }
            }
        }

        // ── 底部控制栏：倍速 | 播放/暂停 | 全屏 ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.w.dp, vertical = 16.w.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    modifier = Modifier.clickable { onShowSpeedMenu() }.padding(vertical = 8.w.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatSpeed(playbackSpeed), fontSize = 16.ws.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Icon(Icons.Filled.KeyboardArrowDown, "选择倍速", tint = TextSecondary, modifier = Modifier.size(18.w.dp))
                }
                DropdownMenu(
                    expanded = showSpeedMenu,
                    onDismissRequest = onDismissSpeedMenu,
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.w.dp)
                ) {
                    PlaybackSpeeds.forEach { speed ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    formatSpeed(speed),
                                    fontSize = 13.ws.sp,
                                    fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal,
                                    color = if (speed == playbackSpeed) ProgressBlue else TextPrimary
                                )
                            },
                            onClick = { onSpeedSelected(speed) }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(42.w.dp)
                    .shadow(8.w.dp, CircleShape)
                    .clip(CircleShape)
                    .background(ProgressBlue)
                    .clickable { onTogglePlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(24.w.dp)
                )
            }

            IconButton(onClick = onFullscreen, modifier = Modifier.size(40.w.dp)) {
                Icon(Icons.Filled.Fullscreen, "全屏", tint = TextPrimary, modifier = Modifier.size(28.w.dp))
            }
        }
    }
}

// ────────────────────────────────────────────────
// 播放器进度条（拖拽 + 预览位置）
// ────────────────────────────────────────────────

@Composable
internal fun PlayerProgressRow(viewModel: VideoPlayerViewModel, durationMs: Long, onSeek: (Float) -> Unit) {
    VideoProgressBar(
        viewModel = viewModel,
        durationMs = durationMs,
        onSeek = onSeek,
        touchTargetHeight = 22.w.dp,
        trackHeight = 3.w.dp,
        trackColor = ControlWhite.copy(alpha = 0.25f),
        thumbColor = ControlWhite,
        thumbSize = 14.w.dp,
    )
}
