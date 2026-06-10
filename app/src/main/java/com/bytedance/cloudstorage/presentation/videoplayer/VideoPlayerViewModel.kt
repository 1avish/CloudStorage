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
import com.bytedance.cloudstorage.data.remote.datasource.MockFileRemoteDataSource
import com.bytedance.cloudstorage.data.repository.FileRepository
import com.bytedance.cloudstorage.data.share.CreatedShareLink
import com.bytedance.cloudstorage.data.share.ShareLinkHandledAction
import com.bytedance.cloudstorage.data.share.ShareLinkStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ────────────────────────────────────────────────
// 视频播放页 ViewModel
// ────────────────────────────────────────────────

/**
 * 选集项数据模型
 *
 * 描述一个可播放的视频剧集，用于选集列表和播放器状态管理。
 *
 * @property id        文件 ID
 * @property title     显示名称
 * @property duration  时长文本
 * @property uri       播放地址
 * @property size      文件大小（字节）
 * @property updatedAt 更新时间戳
 * @property coverUri  封面图片本地 URI
 */
data class Episode(
    val id: String,
    val title: String,
    val duration: String = "",
    val uri: String = "",
    val size: Long = 0L,
    val updatedAt: Long = 0L,
    val coverUri: String? = null,
    val lastOpenedAt: Long? = null,
)

/**
 * 视频播放 ViewModel
 *
 * 管理 ExoPlayer 实例、播放状态、进度、选集列表，
 * 同时处理视频的删除、重命名、下载等文件操作。
 */
class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {

    var exoPlayer: ExoPlayer? = null
        private set

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _isPlayerReady = MutableStateFlow(false)
    val isPlayerReady: StateFlow<Boolean> = _isPlayerReady.asStateFlow()

    private val _playbackError = MutableStateFlow(false)
    val playbackError: StateFlow<Boolean> = _playbackError.asStateFlow()

    private val _activeEpisode = MutableStateFlow(Episode("", "", ""))
    val activeEpisode: StateFlow<Episode> = _activeEpisode.asStateFlow()

    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes: StateFlow<List<Episode>> = _episodes.asStateFlow()

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage

    private val _createdShareLink = MutableSharedFlow<CreatedShareLink?>(extraBufferCapacity = 1)
    val createdShareLink: SharedFlow<CreatedShareLink?> = _createdShareLink

    private var initialized = false
    private var fileId: String = ""
    private var episodeJob: Job? = null
    private val shareLinkStore = ShareLinkStore(application)
    private val db = AppDatabase.getInstance(application)
    private val repository = FileRepository(
        fileDao = db.fileDao(),
        remoteDataSource = MockFileRemoteDataSource(application),
    )

    fun initVideo(id: String, fileName: String, fileUri: String) {
        if (initialized) return
        initialized = true
        fileId = id

        val episode = Episode(id, fileName, uri = fileUri)
        _activeEpisode.value = episode
        _episodes.value = listOf(episode)
        loadEpisodes(id)

        if (fileUri.isNotEmpty()) {
            initPlayer(fileUri)
        }
    }

    private fun loadEpisodes(id: String) {
        episodeJob?.cancel()
        episodeJob = viewModelScope.launch {
            val currentFile = withContext(Dispatchers.IO) {
                db.fileDao().getFileById(id)
            } ?: return@launch
            db.fileDao().getVideoFilesByParent(currentFile.parentId).collect { files ->
                val episodes = files.map { file ->
                    Episode(
                        id = file.fileId,
                        title = file.name,
                        uri = file.uri.orEmpty(),
                        size = file.size,
                        updatedAt = file.updatedAt,
                        coverUri = file.coverUri,
                        lastOpenedAt = file.lastOpenedAt,
                    )
                }
                _episodes.value = episodes

                val activeId = _activeEpisode.value.id.ifEmpty { id }
                val active = episodes.firstOrNull { it.id == activeId }
                    ?: episodes.firstOrNull { it.id == id }
                if (active != null) {
                    _activeEpisode.value = active
                }
            }
        }
    }

    private fun initPlayer(uriString: String): ExoPlayer {
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
                _isPlayerReady.value = false
                _playbackError.value = true
                _toastMessage.tryEmit("视频播放失败")
            }
        })
        return player
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun selectEpisode(episode: Episode) {
        fileId = episode.id
        _activeEpisode.value = episode
        _playbackError.value = false
        _isPlayerReady.value = false
        if (episode.uri.isNotEmpty()) {
            val player = exoPlayer ?: initPlayer(episode.uri)
            player.setMediaItem(MediaItem.fromUri(Uri.parse(episode.uri)))
            player.prepare()
            player.play()
        }
    }

    /** 播放出错后重试：清除错误状态，重新初始化播放器 */
    fun retryPlay() {
        val uri = _activeEpisode.value.uri
        if (uri.isNotEmpty()) {
            _playbackError.value = false
            initPlayer(uri)
        }
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


    // ── 下载到系统相册/下载目录 ──

    fun downloadToDevice() {
        val episode = _activeEpisode.value
        if (episode.uri.isEmpty()) return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val sourceFile = File(Uri.parse(episode.uri).path ?: return@withContext)
                    if (!sourceFile.exists()) {
                        _toastMessage.tryEmit("源文件不存在")
                        return@withContext
                    }

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
                _toastMessage.tryEmit("已保存到系统相册")
            } catch (_: Exception) {
                _toastMessage.tryEmit("下载失败")
            }
        }
    }

    // ── 删除文件 ──

    fun deleteVideo() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteFiles(listOf(fileId))
                }
                // 删除本地文件
                val episode = _activeEpisode.value
                if (episode.uri.isNotEmpty()) {
                    val localFile = File(Uri.parse(episode.uri).path ?: "")
                    if (localFile.exists()) localFile.delete()
                }
                // 删除视频封面文件
                if (!episode.coverUri.isNullOrEmpty()) {
                    val coverFile = File(Uri.parse(episode.coverUri).path ?: "")
                    if (coverFile.exists()) coverFile.delete()
                }
                withContext(Dispatchers.Main) {
                    exoPlayer?.release()
                    exoPlayer = null
                    _isDeleted.value = true
                }
            } catch (_: Exception) {
                _toastMessage.tryEmit("删除失败")
            }
        }
    }

    // ── 重命名文件 ──

    fun renameVideo(newName: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.renameFile(fileId, newName)
                }
                _activeEpisode.value = _activeEpisode.value.copy(title = newName)
                _toastMessage.tryEmit("已重命名")
            } catch (_: Exception) {
                _toastMessage.tryEmit("重命名失败")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }

    fun markFileOpened(id: String) {
        viewModelScope.launch { withContext(Dispatchers.IO) {
            repository.markFileOpened(id)
        } }
    }

    fun createShareLink() {
        val id = _activeEpisode.value.id.ifEmpty { fileId }
        if (id.isEmpty()) {
            viewModelScope.launch { _createdShareLink.emit(null) }
            return
        }

        viewModelScope.launch {
            val link = shareLinkStore.createShare(listOf(id))
            shareLinkStore.copyToClipboard(link)
            _createdShareLink.emit(link)
        }
    }

    fun markShareLinkHandled(token: String, action: ShareLinkHandledAction) {
        viewModelScope.launch {
            shareLinkStore.markHandled(token, action)
        }
    }
}
