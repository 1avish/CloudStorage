package com.bytedance.cloudstorage.presentation.filelist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bytedance.cloudstorage.data.local.database.AppDatabase
import com.bytedance.cloudstorage.data.remote.datasource.MockFileRemoteDataSource
import com.bytedance.cloudstorage.data.repository.FileRepository
import com.bytedance.cloudstorage.domain.model.CloudFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 文件列表 ViewModel
 *
 * 管理当前文件夹路径和筛选类型，驱动 Repository 的 Flow 查询。
 */
class FileListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FileRepository

    // ── 当前文件夹层级（null = 根目录） ──
    private val _currentFolderId = MutableStateFlow<String?>(null)

    // ── 胶囊筛选类型：null = 全部，"folder"/"video"/"txt" ──
    private val _filterType = MutableStateFlow<String?>(null)

    // ── 文件列表（folderId 或 filterType 任一变化时自动重新查询） ──
    @OptIn(ExperimentalCoroutinesApi::class)
    val files: StateFlow<List<CloudFile>> = combine(
        _currentFolderId, _filterType
    ) { folderId, type -> folderId to type }
        .flatMapLatest { (folderId, type) ->
            if (type == null) {
                repository.observeFilesByParent(folderId)
            } else {
                repository.observeFilesByType(type)
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        val db = AppDatabase.getInstance(application)
        repository = FileRepository(
            fileDao = db.fileDao(),
            remoteDataSource = MockFileRemoteDataSource(),
        )
        viewModelScope.launch {
            repository.initializeDataIfEmpty()
        }
    }

    fun setFilter(type: String?) {
        _filterType.value = type
    }

    fun navigateToFolder(folderId: String?) {
        _currentFolderId.value = folderId
        _filterType.value = null
    }
}
