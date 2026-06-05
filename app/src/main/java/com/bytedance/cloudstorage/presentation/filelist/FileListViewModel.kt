package com.bytedance.cloudstorage.presentation.filelist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bytedance.cloudstorage.data.local.database.AppDatabase
import com.bytedance.cloudstorage.data.remote.datasource.MockFileRemoteDataSource
import com.bytedance.cloudstorage.data.repository.FileRepository
import com.bytedance.cloudstorage.data.share.CreatedShareLink
import com.bytedance.cloudstorage.data.share.ShareLinkHandledAction
import com.bytedance.cloudstorage.data.share.ShareLinkStore
import com.bytedance.cloudstorage.domain.model.CloudFile
import com.bytedance.cloudstorage.domain.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortType { TIME, NAME }
enum class SortDirection { ASC, DESC }

/**
 * 文件列表 ViewModel
 *
 * 管理当前文件夹路径、筛选类型和排序方式，驱动 Repository 的 Flow 查询。
 */
class FileListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = FileRepository(
        fileDao = db.fileDao(),
        remoteDataSource = MockFileRemoteDataSource(application),
    )
    private val shareLinkStore = ShareLinkStore(application)

    private val _createdShareLink = MutableSharedFlow<CreatedShareLink?>(extraBufferCapacity = 1)
    val createdShareLink: SharedFlow<CreatedShareLink?> = _createdShareLink

    private val _shareTokenLookupResult = MutableSharedFlow<String?>(extraBufferCapacity = 1)
    val shareTokenLookupResult: SharedFlow<String?> = _shareTokenLookupResult

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage

    // ── 当前文件夹层级（null = 根目录） ──
    private val _currentFolderId = MutableStateFlow<String?>(null)

    // ── 胶囊筛选类型：null = 全部，"folder"/"video"/"txt" ──
    private val _filterType = MutableStateFlow<String?>(null)

    // ── 排序状态 ──
    private val _sortType = MutableStateFlow(SortType.TIME)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.DESC)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

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

    // ── 文件列表：始终在当前目录下筛选并排序 ──
    @OptIn(ExperimentalCoroutinesApi::class)
    val files: StateFlow<List<CloudFile>> = combine(
        _currentFolderId, _filterType, _sortType, _sortDirection
    ) { folderId, type, sortType, sortDir ->
        FileQuery(folderId, type, sortType, sortDir)
    }
        .flatMapLatest { query ->
            val flow = if (query.filterType == null) {
                repository.observeFilesByParent(query.folderId)
            } else {
                repository.observeFilesByParentAndType(query.folderId, query.filterType)
            }
            flow
                .map { files -> sortFiles(files, query.sortType, query.sortDir) }
                .flowOn(Dispatchers.Default)
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    // ── 文件移动 ──
    /** 是否显示移动目标文件夹选择器 */
    private val _showMoveSheet = MutableStateFlow(false)
    val showMoveSheet: StateFlow<Boolean> = _showMoveSheet.asStateFlow()
    /** 移动目标文件夹 ID（null = 根目录） */
    private val _moveTargetFolderId = MutableStateFlow<String?>(null)
    /** 移动目标文件夹路径面包屑（从根到当前层级的名称列表） */
    private val _moveTargetPathStack = MutableStateFlow<List<String>>(emptyList())
    val moveTargetPathStack: StateFlow<List<String>> = _moveTargetPathStack.asStateFlow()
    /** 目标文件夹下的子文件夹列表（供选择器展示，排除已选中的文件） */
    private val _moveTargetFolders = MutableStateFlow<List<CloudFile>>(emptyList())
    val moveTargetFolders: StateFlow<List<CloudFile>> = _moveTargetFolders.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDataIfEmpty()
        }
    }

    fun setFilter(type: String?) {
        _filterType.value = type
    }

    /** 切换排序：同类型则颠倒方向，不同类型则切换并设默认方向 */
    fun toggleSort(type: SortType) {
        if (_sortType.value == type) {
            _sortDirection.value = if (_sortDirection.value == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
        } else {
            _sortType.value = type
            _sortDirection.value = when (type) {
                SortType.TIME -> SortDirection.DESC
                SortType.NAME -> SortDirection.ASC
            }
        }
    }

    /** 创建新文件夹（在当前目录下） */
    fun createFolder(name: String) {
        viewModelScope.launch {
            try {
                repository.createFolder(name, _currentFolderId.value)
            } catch (_: Exception) {
                _toastMessage.tryEmit("创建文件夹失败")
            }
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
    fun uploadFile(name: String, size: Long, uri: String, coverUri: String?, type: String) {
        viewModelScope.launch {
            try {
                repository.uploadFile(
                    name = name,
                    size = size,
                    uri = uri,
                    coverUri = coverUri,
                    type = type,
                    parentId = _currentFolderId.value,
                )
            } catch (_: Exception) {
                _toastMessage.tryEmit("上传失败")
            }
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
            try {
                repository.deleteFiles(_selectedFileIds.value.toList())
                exitSelectionMode()
                _toastMessage.tryEmit("已删除")
            } catch (_: Exception) {
                _toastMessage.tryEmit("删除失败")
            }
        }
    }

    /** 重命名文件 */
    fun renameFile(fileId: String, newName: String) {
        viewModelScope.launch {
            try {
                repository.renameFile(fileId, newName)
            } catch (_: Exception) {
                _toastMessage.tryEmit("重命名失败")
            }
        }
    }

    /**
     * 为当前选中的文件创建分享链接。
     *
     * 结果通过 [createdShareLink] 发送，创建成功时已自动复制到剪贴板。
     */
    fun createShareLink() {
        val selectedIds = _selectedFileIds.value
        val ids = files.value.map { it.id }.filter { it in selectedIds }
        if (ids.isEmpty()) {
            viewModelScope.launch { _createdShareLink.emit(null) }
            return
        }
        viewModelScope.launch {
            val link = shareLinkStore.createShare(ids)
            shareLinkStore.copyToClipboard(link)
            _createdShareLink.emit(link)
        }
    }

    /**
     * 根据用户输入的原始链接文本查找本地已存在的分享 token。
     *
     * @param rawLink 用户粘贴的链接文本（支持完整 URL 或纯 token）
     * 结果通过 [shareTokenLookupResult] 发送，链接不存在时发送 null。
     */
    fun findExistingShareToken(rawLink: String) {
        val token = ShareLinkStore.parseToken(rawLink)
        if (token == null) {
            viewModelScope.launch { _shareTokenLookupResult.emit(null) }
            return
        }
        viewModelScope.launch {
            _shareTokenLookupResult.emit(token.takeIf { shareLinkStore.hasShare(it) })
        }
    }

    fun markShareLinkHandled(token: String, action: ShareLinkHandledAction) {
        viewModelScope.launch {
            shareLinkStore.markHandled(token, action)
        }
    }

    /** 打开移动目标选择器，重置到根目录 */
    fun openMoveSheet() {
        _showMoveSheet.value = true
        _moveTargetFolderId.value = null
        _moveTargetPathStack.value = emptyList()
        loadMoveTargetFolders(null)
    }

    /** 关闭移动目标选择器 */
    fun closeMoveSheet() {
        _showMoveSheet.value = false
    }

    /** 重新打开移动目标选择器（新建文件夹后返回时使用） */
    fun reopenMoveSheet() {
        _showMoveSheet.value = true
        loadMoveTargetFolders(_moveTargetFolderId.value)
    }

    /** 在移动目标选择器中进入子文件夹 */
    fun navigateMoveIntoFolder(folderId: String, folderName: String) {
        _moveTargetPathStack.value = _moveTargetPathStack.value + folderName
        _moveTargetFolderId.value = folderId
        loadMoveTargetFolders(folderId)
    }

    /** 在移动目标选择器中返回上一级 */
    fun navigateMoveBack() {
        val stack = _moveTargetPathStack.value
        if (stack.isEmpty()) return
        navigateMoveToPathIndex(stack.size - 1)
    }

    /** 在移动目标选择器中跳转到面包屑指定层级（index = 0 表示根目录） */
    fun navigateMoveToPathIndex(index: Int) {
        val stack = _moveTargetPathStack.value
        if (index > stack.size) return
        val newStack = stack.take(index)
        _moveTargetPathStack.value = newStack
        viewModelScope.launch {
            val newParentId = if (newStack.isEmpty()) null else findFolderIdByPath(newStack)
            _moveTargetFolderId.value = newParentId
            loadMoveTargetFolders(newParentId)
        }
    }

    /** 加载指定目录下的子文件夹列表，自动排除当前已选中的文件 */
    private fun loadMoveTargetFolders(parentId: String?) {
        viewModelScope.launch {
            val selectedIds = _selectedFileIds.value
            _moveTargetFolders.value = repository.getFilesByParent(parentId)
                .filter { it.type == FileType.Folder && it.id !in selectedIds }
        }
    }

    /** 在移动目标目录下新建文件夹（重名时自动追加编号） */
    fun createFolderInMoveTarget(name: String) {
        viewModelScope.launch {
            val targetId = _moveTargetFolderId.value
            val existingNames = repository.getFilesByParent(targetId).map { it.name }.toSet()
            repository.createFolder(generateUniqueName(name, existingNames), targetId)
            loadMoveTargetFolders(targetId)
        }
    }

    /** 确认移动：将已选文件移至当前目标文件夹，然后退出选择模式 */
    fun confirmMove() {
        val ids = _selectedFileIds.value.toList()
        val targetId = _moveTargetFolderId.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                repository.moveFiles(ids, targetId)
                exitSelectionMode()
                _showMoveSheet.value = false
            } catch (_: Exception) {
                _toastMessage.tryEmit("移动失败")
            }
        }
    }


    // ── 内部类型与方法 ──

    private data class FileQuery(
        val folderId: String?,
        val filterType: String?,
        val sortType: SortType,
        val sortDir: SortDirection,
    )

    private fun sortFiles(files: List<CloudFile>, type: SortType, direction: SortDirection): List<CloudFile> {
        val comparator: Comparator<CloudFile> = when (type) {
            SortType.TIME -> compareBy { it.updatedAt }
            SortType.NAME -> compareBy { it.name.lowercase() }
        }
        return if (direction == SortDirection.DESC) files.sortedWith(comparator.reversed()) else files.sortedWith(comparator)
    }
}
