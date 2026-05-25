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
import kotlinx.coroutines.flow.map
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

    // ── 路径面包屑（从根到当前文件夹的名称列表） ──
    private val _pathStack = MutableStateFlow<List<String>>(emptyList())
    val pathStack: StateFlow<List<String>> = _pathStack.asStateFlow()

    val atRoot: StateFlow<Boolean> = _pathStack
        .map { it.isEmpty() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val hasBack: StateFlow<Boolean> = _pathStack
        .map { it.isNotEmpty() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 当前文件夹名称，null = 根目录（显示"我的网盘"） */
    val currentFolderName: StateFlow<String?> = _pathStack
        .map { stack -> stack.lastOrNull() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── 文件列表：始终在当前目录下筛选 ──
    @OptIn(ExperimentalCoroutinesApi::class)
    val files: StateFlow<List<CloudFile>> = combine(
        _currentFolderId, _filterType
    ) { folderId, type -> folderId to type }
        .flatMapLatest { (folderId, type) ->
            if (type == null) {
                repository.observeFilesByParent(folderId)
            } else {
                repository.observeFilesByParentAndType(folderId, type)
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

    /** 创建新文件夹（在当前目录下） */
    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.createFolder(name, _currentFolderId.value)
        }
    }

    fun navigateToFolder(folderId: String?) {
        _currentFolderId.value = folderId
        _filterType.value = null
    }

    /** 进入子文件夹 */
    fun navigateIntoFolder(folderId: String, folderName: String) {
        _pathStack.value = _pathStack.value + folderName
        _currentFolderId.value = folderId
        _filterType.value = null
    }

    /** 返回上一级目录 */
    fun navigateBack() {
        val stack = _pathStack.value
        if (stack.isEmpty()) return
        navigateToPathIndex(stack.size - 1)
    }

    /** 跳转到面包屑指定层级（index = 0 表示根目录） */
    fun navigateToPathIndex(index: Int) {
        val stack = _pathStack.value
        if (index >= stack.size) return
        val newStack = stack.take(index)
        _pathStack.value = newStack
        viewModelScope.launch {
            if (newStack.isEmpty()) {
                _currentFolderId.value = null
            } else {
                _currentFolderId.value = findFolderIdByPath(newStack)
            }
        }
        _filterType.value = null
    }

    /** 根据路径名称栈，逐级查找文件夹 ID */
    private suspend fun findFolderIdByPath(pathNames: List<String>): String? {
        var parentId: String? = null
        for (name in pathNames) {
            val id = repository.findFolderIdByName(name, parentId) ?: return null
            parentId = id
        }
        return parentId
    }
}
