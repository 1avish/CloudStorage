package com.bytedance.cloudstorage.presentation.videoplayer

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws
import kotlinx.coroutines.delay

// ── 工具函数

internal fun formatSpeed(speed: Float): String {
    return if (speed % 1f == 0f) {
        "${speed.toInt()}.0x"
    } else {
        "${speed}x"
    }
}

fun formatTime(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    val minutes = seconds / 60
    val remainSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainSeconds)
}

// ────────────────────────────────────────────────
// 视频播放页
// ────────────────────────────────────────────────

/**
 * 视频播放页主入口，包含播放器卡片、视频信息、选集列表。
 * 支持全屏切换、倍速播放、删除/重命名/下载操作。
 *
 * @param fileId 文件 ID
 * @param fileName 文件名
 * @param fileUri 文件 URI
 * @param onBack 返回回调
 */
@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    fileId: String,
    fileName: String,
    fileUri: String,
    onBack: () -> Unit,
    viewModel: VideoPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(fileId, fileName, fileUri) {
        viewModel.initVideo(fileId, fileName, fileUri)
        viewModel.markFileOpened(fileId)
    }

    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val activeEpisode by viewModel.activeEpisode.collectAsStateWithLifecycle()
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()
    val durationMs by viewModel.duration.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val isDeleted by viewModel.isDeleted.collectAsStateWithLifecycle()
    val isPlayerReady by viewModel.isPlayerReady.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()

    LaunchedEffect(isDeleted) {
        if (isDeleted) onBack()
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                viewModel.updateProgress()
                delay(200)
            }
        }
    }

    // ── 弹窗 / 菜单状态 ──
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRenameSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // ── 全屏时切换横屏 ──
    LaunchedEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    DisposableEffect(activity) {
        val window = activity?.window
        val insetsController = window?.let {
            WindowCompat.getInsetsController(it, it.decorView)
        }

        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
        }
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    LaunchedEffect(activity, isFullscreen) {
        val window = activity?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullscreen) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    if (isFullscreen) {
        FullscreenPlayer(
            onBack = { isFullscreen = false },
            viewModel = viewModel
        )
        return
    }

    // ── 更多操作弹窗 ──
    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.w.dp, topEnd = 16.w.dp),
            dragHandle = null
        ) {
            VideoMoreSheet(
                onDownload = {
                    showMoreSheet = false
                    viewModel.downloadToDevice()
                },
                onShare = {
                    showMoreSheet = false
                    Toast.makeText(context, "分享（待实现）", Toast.LENGTH_SHORT).show()
                },
                onRename = {
                    showMoreSheet = false
                    showRenameSheet = true
                },
                onDelete = {
                    showMoreSheet = false
                    showDeleteConfirm = true
                }
            )
        }
    }

    // ── 删除 / 重命名弹窗 ──
    if (showDeleteConfirm) {
        ModalBottomSheet(
            onDismissRequest = { showDeleteConfirm = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.w.dp, topEnd = 16.w.dp),
            dragHandle = null
        ) {
            VideoDeleteSheet(
                fileName = activeEpisode.title.ifEmpty { fileName },
                onDismiss = { showDeleteConfirm = false },
                onConfirm = {
                    showDeleteConfirm = false
                    viewModel.deleteVideo()
                }
            )
        }
    }

    if (showRenameSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRenameSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.w.dp, topEnd = 16.w.dp),
            dragHandle = null
        ) {
            VideoRenameSheet(
                currentName = activeEpisode.title.ifEmpty { fileName },
                onDismiss = { showRenameSheet = false },
                onConfirm = { newName ->
                    viewModel.renameVideo(newName)
                    showRenameSheet = false
                }
            )
        }
    }

    val displayEpisodes = episodes.ifEmpty { listOf(activeEpisode) }.filter { it.id.isNotEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .statusBarsPadding()
    ) {
        // ── 页面主体：播放器 + 信息 + 选集 ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 12.w.dp, bottom = 28.w.dp)
        ) {
            PlayerCard(
                fileUri = fileUri,
                isPlayerReady = isPlayerReady,
                isPlaying = isPlaying,
                playbackSpeed = playbackSpeed,
                durationMs = durationMs,
                currentPosition = currentPosition,
                playbackError = playbackError,
                showSpeedMenu = showSpeedMenu,
                onShowSpeedMenu = { showSpeedMenu = true },
                onDismissSpeedMenu = { showSpeedMenu = false },
                onSpeedSelected = {
                    showSpeedMenu = false
                    viewModel.setPlaybackSpeed(it)
                },
                onTogglePlay = { viewModel.togglePlayPause() },
                onSeek = { viewModel.seekTo(it) },
                onBack = onBack,
                onMoreClick = { showMoreSheet = true },
                onFullscreen = { isFullscreen = true },
                onRetry = { viewModel.retryPlay() },
                viewModel = viewModel
            )

            VideoInfoCard(
                title = activeEpisode.title.ifEmpty { fileName },
                updatedAt = activeEpisode.updatedAt,
                size = activeEpisode.size,
            )

            EpisodeSection(
                episodes = displayEpisodes,
                activeEpisode = activeEpisode,
                onEpisodeClick = { viewModel.selectEpisode(it) }
            )
        }
    }
}
