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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws
import kotlinx.coroutines.delay

// ────────────────────────────────────────────────
// 播放器卡片（视频画面 + 控制栏）
// ────────────────────────────────────────────────

/**
 * 视频播放卡片，包含视频画面、顶部导航/下载/分享遮罩、底部进度条与播放控制栏。
 *
 * 点击画面切换控制栏显隐，双击切换播放/暂停；控制栏显�后 2.6s 自动隐藏。
 *
 * @param fileUri        视频播放地址
 * @param isPlayerReady  ExoPlayer 是否已进入可播放状态
 * @param isPlaying      是否正在播放
 * @param durationMs     视频总时长（毫秒）
 * @param currentPosition 当前播放进度（毫秒）
 * @param playbackError  是否播放失败
 * @param onTogglePlay   播放/暂停切换回调
 * @param onSeek         跳转进度回调（0f–1f 百分比）
 * @param onBack         返回按钮回调
 * @param onDownload     下载按钮回调
 * @param onShare        分享按钮回调
 * @param onFullscreen   全屏按钮回调
 * @param onRetry        播放失败重试回调
 * @param viewModel      播放器 ViewModel
 */
@UnstableApi
@Composable
internal fun PlayerCard(
    fileUri: String,
    isPlayerReady: Boolean,
    isPlaying: Boolean,
    durationMs: Long,
    currentPosition: Long,
    playbackError: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onFullscreen: () -> Unit,
    onRetry: () -> Unit,
    viewModel: VideoPlayerViewModel
) {
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(2600)
            showControls = false
        }
    }

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

        when {
            playbackError -> PlaybackErrorState(onRetry = onRetry)
            !hasUri -> VideoPlaceholder("无法播放此视频")
            !isPlayerReady -> LoadingState()
            player != null -> PlayerSurface(player = player)
        }

        if (showControls) {
            TopOverlay(
                onBack = onBack,
                onDownload = onDownload,
                onShare = onShare
            )
            BottomOverlay(
                isPlaying = isPlaying,
                durationMs = durationMs,
                currentPosition = currentPosition,
                onTogglePlay = onTogglePlay,
                onSeek = onSeek,
                onFullscreen = onFullscreen,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun BoxScope.TopOverlay(
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.w.dp)
            .align(Alignment.TopCenter)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.58f), Color.Transparent)))
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(horizontal = 18.w.dp, vertical = 16.w.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.w.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                "返回",
                tint = ControlWhite,
                modifier = Modifier.size(31.w.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDownload, modifier = Modifier.size(40.w.dp)) {
                Icon(
                    Icons.Filled.ArrowDownward,
                    "下载",
                    tint = ControlWhite,
                    modifier = Modifier.size(30.w.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.w.dp))
            IconButton(onClick = onShare, modifier = Modifier.size(40.w.dp)) {
                Icon(
                    Icons.Filled.Share,
                    "分享",
                    tint = ControlWhite,
                    modifier = Modifier.size(28.w.dp)
                )
            }
        }
    }
}

@Composable
private fun BoxScope.BottomOverlay(
    isPlaying: Boolean,
    durationMs: Long,
    currentPosition: Long,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onFullscreen: () -> Unit,
    viewModel: VideoPlayerViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.w.dp)
            .align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f))))
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(start = 18.w.dp, end = 14.w.dp, bottom = 12.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onTogglePlay, modifier = Modifier.size(42.w.dp)) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (isPlaying) "暂停" else "播放",
                tint = ControlWhite,
                modifier = Modifier.size(38.w.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.w.dp))
        Text(
            formatTime(currentPosition),
            fontSize = 16.ws.sp,
            fontWeight = FontWeight.Medium,
            color = ControlWhite
        )
        Spacer(modifier = Modifier.width(10.w.dp))
        Box(modifier = Modifier.weight(1f)) {
            PlayerProgressRow(viewModel, durationMs, onSeek)
        }
        Spacer(modifier = Modifier.width(10.w.dp))
        Text(
            formatTime(durationMs),
            fontSize = 16.ws.sp,
            fontWeight = FontWeight.Medium,
            color = ControlWhite
        )
        Spacer(modifier = Modifier.width(8.w.dp))
        IconButton(onClick = onFullscreen, modifier = Modifier.size(38.w.dp)) {
            Icon(Icons.Filled.Fullscreen, "全屏", tint = ControlWhite, modifier = Modifier.size(30.w.dp))
        }
    }
}

@Composable
private fun PlayerSurface(player: androidx.media3.exoplayer.ExoPlayer) {
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
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerBg),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = ControlWhite,
            strokeWidth = 2.w.dp,
            modifier = Modifier.size(32.w.dp)
        )
    }
}

@Composable
private fun PlaybackErrorState(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF0A1624), Color(0xFF101820)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("视频加载失败", fontSize = 14.ws.sp, color = ControlWhite.copy(alpha = 0.72f))
            Spacer(modifier = Modifier.height(12.w.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.w.dp))
                    .background(ControlWhite.copy(alpha = 0.16f))
                    .clickable { onRetry() }
                    .padding(horizontal = 18.w.dp, vertical = 8.w.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Refresh, "重试", tint = ControlWhite, modifier = Modifier.size(16.w.dp))
                Spacer(modifier = Modifier.width(6.w.dp))
                Text("重试", fontSize = 13.ws.sp, color = ControlWhite)
            }
        }
    }
}

@Composable
private fun VideoPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF0A1624), Color(0xFF101820)))),
        contentAlignment = Alignment.Center
    ) {
        Text(message, fontSize = 14.ws.sp, color = ControlWhite.copy(alpha = 0.72f))
    }
}

@Composable
internal fun PlayerProgressRow(viewModel: VideoPlayerViewModel, durationMs: Long, onSeek: (Float) -> Unit) {
    VideoProgressBar(
        viewModel = viewModel,
        durationMs = durationMs,
        onSeek = onSeek,
        touchTargetHeight = 28.w.dp,
        trackHeight = 3.w.dp,
        trackColor = ControlWhite.copy(alpha = 0.36f),
        thumbColor = ControlWhite,
        thumbSize = 13.w.dp,
    )
}
