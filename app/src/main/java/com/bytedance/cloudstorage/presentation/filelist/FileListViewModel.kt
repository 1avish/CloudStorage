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

    // ── 多选状态 ──
    private val _selectedFileIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFileIds: StateFlow<Set<String>> = _selectedFileIds.asStateFlow()

    private val _showActionSheet = MutableStateFlow(false)
    val showActionSheet: StateFlow<Boolean> = _showActionSheet.asStateFlow()

    /** 当前选中的文件列表（派生自 files + selectedFileIds） */
    val selectedFiles: StateFlow<List<CloudFile>> = combine(
        files, _selectedFileIds
    ) { allFiles, ids ->
        allFiles.filter { it.id in ids }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 是否处于选择模式（有文件被选中） */
    val isSelectionMode: StateFlow<Boolean> = _selectedFileIds
        .map { it.isNotEmpty() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 已选中文件数量 */
    val selectedCount: StateFlow<Int> = _selectedFileIds
        .map { it.size }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

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

    /**
     * 上传文件：接收系统文件选择器返回的元数据，创建本地数据库记录。
     *
     * @param name 文件名（来自 ContentResolver 查询）
     * @param size 文件大小（字节）
     * @param uri  content:// URI 字符串
     * @param type 文件类型键："video" / "txt" / "other"
     */
    fun uploadFile(name: String, size: Long, uri: String, type: String) {
        viewModelScope.launch {
            repository.uploadFile(
                name = name,
                size = size,
                uri = uri,
                type = type,
                parentId = _currentFolderId.value,
            )
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

    // ── 多选与操作 ──

    /** 切换单个文件的选中状态 */
    fun toggleFileSelection(fileId: String) {
        val current = _selectedFileIds.value
        _selectedFileIds.value = if (fileId in current) current - fileId else current + fileId
    }

    /** 长按进入选中模式（清空旧选中，仅保留当前文件） */
    fun enterSelectionMode(fileId: String) {
        _selectedFileIds.value = setOf(fileId)
    }

    /** 取消全部选中，退出选择模式 */
    fun exitSelectionMode() {
        _selectedFileIds.value = emptySet()
    }

    /** 全选当前目录下所有文件 */
    fun selectAllFiles() {
        _selectedFileIds.value = files.value.map { it.id }.toSet()
    }

    /** 批量删除选中文件 */
    fun deleteSelectedFiles() {
        viewModelScope.launch {
            repository.deleteFiles(_selectedFileIds.value.toList())
            exitSelectionMode()
        }
    }

    /** 重命名文件 */
    fun renameFile(fileId: String, newName: String) {
        viewModelScope.launch {
            repository.renameFile(fileId, newName)
        }
    }
}
