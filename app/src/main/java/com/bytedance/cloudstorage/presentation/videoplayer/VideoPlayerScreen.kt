package com.bytedance.cloudstorage.presentation.videoplayer

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.bytedance.cloudstorage.data.share.ShareLinkHandledAction
import com.bytedance.cloudstorage.presentation.share.ShareLinkPromptDialog
import com.bytedance.cloudstorage.utils.w
import kotlinx.coroutines.delay

fun formatTime(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    val minutes = seconds / 60
    val remainSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainSeconds)
}

@UnstableApi
@Composable
fun VideoPlayerScreen(
    fileId: String,
    fileName: String,
    fileUri: String,
    onBack: () -> Unit,
    onOpenShareLink: (String) -> Unit = {},
    viewModel: VideoPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(fileId, fileName, fileUri) {
        viewModel.initVideo(fileId, fileName, fileUri)
        viewModel.markFileOpened(fileId)
    }

    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val activeEpisode by viewModel.activeEpisode.collectAsStateWithLifecycle()
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()
    val durationMs by viewModel.duration.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val isDeleted by viewModel.isDeleted.collectAsStateWithLifecycle()
    val isPlayerReady by viewModel.isPlayerReady.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var copiedShareToken by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(isDeleted) {
        if (isDeleted) onBack()
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.createdShareLink.collect { link ->
            if (link == null) {
                Toast.makeText(context, "请先选择文件", Toast.LENGTH_SHORT).show()
            } else {
                copiedShareToken = link.token
                Toast.makeText(context, "分享链接已复制", Toast.LENGTH_SHORT).show()
            }
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

    val displayEpisodes = episodes.ifEmpty { listOf(activeEpisode) }.filter { it.id.isNotEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 28.w.dp)
        ) {
            PlayerCard(
                fileUri = activeEpisode.uri.ifEmpty { fileUri },
                isPlayerReady = isPlayerReady,
                isPlaying = isPlaying,
                durationMs = durationMs,
                currentPosition = currentPosition,
                playbackError = playbackError,
                onTogglePlay = { viewModel.togglePlayPause() },
                onSeek = { viewModel.seekTo(it) },
                onBack = onBack,
                onDownload = { viewModel.downloadToDevice() },
                onShare = { viewModel.createShareLink() },
                onFullscreen = { isFullscreen = true },
                onRetry = { viewModel.retryPlay() },
                viewModel = viewModel
            )

            VideoInfoCard(
                title = activeEpisode.title.ifEmpty { fileName },
                updatedAt = activeEpisode.updatedAt,
                size = activeEpisode.size,
                onDownload = { viewModel.downloadToDevice() },
                onShare = { viewModel.createShareLink() },
            )

            EpisodeSection(
                episodes = displayEpisodes,
                activeEpisode = activeEpisode,
                onEpisodeClick = { viewModel.selectEpisode(it) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    copiedShareToken?.let { token ->
        ShareLinkPromptDialog(
            onDismiss = {
                viewModel.markShareLinkHandled(token, ShareLinkHandledAction.Dismissed)
                copiedShareToken = null
            },
            onViewNow = {
                viewModel.markShareLinkHandled(token, ShareLinkHandledAction.Opened)
                copiedShareToken = null
                onOpenShareLink(token)
            }
        )
    }
}
