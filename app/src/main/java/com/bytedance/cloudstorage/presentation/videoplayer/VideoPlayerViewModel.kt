package com.bytedance.cloudstorage.presentation.videoplayer

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bytedance.cloudstorage.data.local.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class Episode(
    val id: String,
    val title: String,
    val duration: String = "",
    val uri: String = ""
)

class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {

    var exoPlayer: ExoPlayer? = null
        private set

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _isPlayerReady = MutableStateFlow(false)
    val isPlayerReady: StateFlow<Boolean> = _isPlayerReady.asStateFlow()

    private val _activeEpisode = MutableStateFlow(Episode("", "", ""))
    val activeEpisode: StateFlow<Episode> = _activeEpisode.asStateFlow()

    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes: StateFlow<List<Episode>> = _episodes.asStateFlow()

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var initialized = false
    private var fileId: String = ""
    private val fileDao = AppDatabase.getInstance(application).fileDao()

    fun initVideo(id: String, fileName: String, fileUri: String) {
        if (initialized) return
        initialized = true
        fileId = id

        val episode = Episode(id, fileName, uri = fileUri)
        _activeEpisode.value = episode
        _episodes.value = listOf(episode)

        if (fileUri.isNotEmpty()) {
            initPlayer(fileUri)
        }
    }

    private fun initPlayer(uriString: String) {
        val player = ExoPlayer.Builder(getApplication()).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uriString)))
            prepare()
            playWhenReady = false
        }
        exoPlayer = player

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0)
                    _isPlayerReady.value = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                player.release()
                exoPlayer = null
            }
        })
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun selectEpisode(episode: Episode) {
        _activeEpisode.value = episode
        exoPlayer?.let { player ->
            if (episode.uri.isNotEmpty()) {
                player.setMediaItem(MediaItem.fromUri(Uri.parse(episode.uri)))
                player.prepare()
                player.setPlaybackSpeed(_playbackSpeed.value)
                player.play()
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.25f, 3f)
        _playbackSpeed.value = safeSpeed
        exoPlayer?.setPlaybackSpeed(safeSpeed)
    }

    fun seekTo(fraction: Float) {
        val player = exoPlayer ?: return
        val durationMs = when {
            player.duration > 0L -> player.duration
            _duration.value > 0L -> _duration.value
            else -> return
        }
        val pos = (fraction.coerceIn(0f, 1f) * durationMs).toLong()
        _currentPosition.value = pos
        player.seekTo(pos)
    }

    fun updateProgress() {
        exoPlayer?.let { _currentPosition.value = it.currentPosition }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // ── 下载到系统相册/下载目录 ──

    fun downloadToDevice() {
        val episode = _activeEpisode.value
        if (episode.uri.isEmpty()) return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val sourceFile = File(Uri.parse(episode.uri).path ?: return@withContext)
                    if (!sourceFile.exists()) return@withContext

                    val context = getApplication<Application>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = ContentValues().apply {
                            put(MediaStore.Video.Media.DISPLAY_NAME, episode.title)
                            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
                        }
                        val uri = context.contentResolver.insert(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
                        )
                        uri?.let {
                            context.contentResolver.openOutputStream(it)?.use { out ->
                                sourceFile.inputStream().use { inp -> inp.copyTo(out) }
                            }
                        }
                    } else {
                        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                        val dest = File(dir, episode.title)
                        sourceFile.copyTo(dest, overwrite = true)
                    }
                }
                _toastMessage.value = "已保存到系统相册"
            } catch (_: Exception) {
                _toastMessage.value = "下载失败"
            }
        }
    }

    // ── 删除文件 ──

    fun deleteVideo() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    fileDao.deleteFiles(listOf(fileId))
                }
                // 删除本地文件
                val episode = _activeEpisode.value
                if (episode.uri.isNotEmpty()) {
                    val localFile = File(Uri.parse(episode.uri).path ?: "")
                    if (localFile.exists()) localFile.delete()
                }
                withContext(Dispatchers.Main) {
                    exoPlayer?.release()
                    exoPlayer = null
                    _isDeleted.value = true
                }
            } catch (_: Exception) {
                _toastMessage.value = "删除失败"
            }
        }
    }

    // ── 重命名文件 ──

    fun renameVideo(newName: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    fileDao.renameFile(fileId, newName, System.currentTimeMillis())
                }
                _activeEpisode.value = _activeEpisode.value.copy(title = newName)
                _toastMessage.value = "已重命名"
            } catch (_: Exception) {
                _toastMessage.value = "重命名失败"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}
