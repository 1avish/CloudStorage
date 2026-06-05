package com.bytedance.cloudstorage.presentation.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bytedance.cloudstorage.data.local.database.AppDatabase
import com.bytedance.cloudstorage.data.remote.datasource.MockFileRemoteDataSource
import com.bytedance.cloudstorage.data.repository.FileRepository
import com.bytedance.cloudstorage.data.share.ShareLinkHandledAction
import com.bytedance.cloudstorage.data.share.ShareLinkStore
import com.bytedance.cloudstorage.domain.model.CloudFile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ────────────────────────────────────────────────
// UI 状态数据类
// ────────────────────────────────────────────────

/**
 * 分享文件列表页的 UI 状态。
 *
 * @property isLoading       正在加载分享数据
 * @property files           当前展示的文件列表（可能为根级或子文件夹）
 * @property selectedIds     已选中的文件 ID 集合，默认全选
 * @property invalid         分享链接无效（token 不存在或文件已清空）
 * @property canNavigateBack 当前是否在子文件夹内，控制返回按钮行为
 * @property isSaving        正在保存文件到网盘，按钮显示「保存中」
 */
data class ShareListUiState(
    val isLoading: Boolean = true,
    val files: List<CloudFile> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val invalid: Boolean = false,
    val canNavigateBack: Boolean = false,
    val isSaving: Boolean = false,
)

/**
 * 保存操作结果，通过 [SharedFlow] 一次性发送给 UI 层展示 Toast。
 *
 * @property message    提示文字
 * @property shouldExit 保存成功后是否退出分享页返回云盘首页
 */
data class ShareSaveResult(
    val message: String,
    val shouldExit: Boolean,
)

// ────────────────────────────────────────────────
// 分享文件列表 ViewModel
// ────────────────────────────────────────────────

/**
 * 分享文件列表页的 ViewModel。
 *
 * 核心流程：
 * 1. [load] — 根据 token 从 [ShareLinkStore] 获取文件 ID 列表，再通过 [FileRepository] 查询文件
 * 2. [navigateIntoFolder] / [navigateBack] — 文件夹内浏览，维护 folderStack 栈
 * 3. [toggleSelection] / [selectAll] / [clearSelection] — 文件多选操作
 * 4. [saveSelectedFiles] — 将选中的文件复制到当前用户网盘根目录
 *
 * 状态通过 [uiState] (StateFlow) 驱动 UI，一次性事件通过 [saveResult] (SharedFlow) 发送。
 */
class ShareListViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = FileRepository(
        fileDao = db.fileDao(),
        remoteDataSource = MockFileRemoteDataSource(application),
        transferRecordDao = db.transferRecordDao(),
    )
    private val shareLinkStore = ShareLinkStore(application)

    private val _uiState = MutableStateFlow(ShareListUiState())
    val uiState: StateFlow<ShareListUiState> = _uiState.asStateFlow()

    private val _saveResult = MutableSharedFlow<ShareSaveResult>(extraBufferCapacity = 1)
    val saveResult: SharedFlow<ShareSaveResult> = _saveResult

    /** 已加载的 token，用于防止重复加载 */
    private var loadedToken: String? = null
    private var currentToken: String? = null
    /** 分享根级文件列表（token 对应的原始文件） */
    private var rootFiles: List<CloudFile> = emptyList()
    /** 文件夹导航栈，记录进入过的文件夹 ID */
    private var folderStack: List<String> = emptyList()

    /**
     * 根据分享 token 加载文件列表。
     *
     * 同一 token 重复调用时跳过，避免重复请求。
     * token 无效或文件列表为空时设置 [ShareListUiState.invalid] = true。
     */
    fun load(token: String) {
        if (loadedToken == token) return
        loadedToken = token
        currentToken = token
        folderStack = emptyList()
        _uiState.value = ShareListUiState(isLoading = true)

        viewModelScope.launch {
            try {
                // 首次进入时确保本地有 Mock 数据
                repository.initializeDataIfEmpty()
                val fileIds = shareLinkStore.getFileIds(token)
                if (fileIds.isNullOrEmpty()) {
                    _uiState.value = ShareListUiState(isLoading = false, invalid = true)
                    return@launch
                }

                rootFiles = repository.getFilesByIds(fileIds)
                // 默认全选所有文件
                _uiState.value = ShareListUiState(
                    isLoading = false,
                    files = rootFiles,
                    selectedIds = rootFiles.map { it.id }.toSet(),
                    invalid = rootFiles.isEmpty(),
                )
            } catch (_: Exception) {
                _uiState.value = ShareListUiState(isLoading = false, invalid = true)
            }
        }
    }

    /**
     * 进入子文件夹，查询该文件夹下的文件列表，默认全选。
     */
    fun navigateIntoFolder(folderId: String) {
        viewModelScope.launch {
            try {
                folderStack = folderStack + folderId
                _uiState.value = _uiState.value.copy(isLoading = true)
                val files = repository.getFilesByParent(folderId)
                _uiState.value = ShareListUiState(
                    isLoading = false,
                    files = files,
                    selectedIds = files.map { it.id }.toSet(),
                    invalid = false,
                    canNavigateBack = folderStack.isNotEmpty(),
                )
            } catch (_: Exception) {
                folderStack = folderStack.dropLast(1)
                _uiState.value = _uiState.value.copy(isLoading = false)
                _saveResult.tryEmit(ShareSaveResult("加载失败", shouldExit = false))
            }
        }
    }

    /**
     * 返回上一级文件夹。
     *
     * @return true 表示成功返回上一级，false 表示已在根级
     */
    fun navigateBack(): Boolean {
        if (folderStack.isEmpty()) return false
        folderStack = folderStack.dropLast(1)
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val files = folderStack.lastOrNull()?.let { folderId ->
                    repository.getFilesByParent(folderId)
                } ?: rootFiles
                _uiState.value = ShareListUiState(
                    isLoading = false,
                    files = files,
                    selectedIds = files.map { it.id }.toSet(),
                    invalid = false,
                    canNavigateBack = folderStack.isNotEmpty(),
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _saveResult.tryEmit(ShareSaveResult("加载失败", shouldExit = false))
            }
        }
        return true
    }

    /**
     * 切换单个文件的选中状态。
     */
    fun toggleSelection(fileId: String) {
        val current = _uiState.value
        val selectedIds = if (fileId in current.selectedIds) {
            current.selectedIds - fileId
        } else {
            current.selectedIds + fileId
        }
        _uiState.value = current.copy(selectedIds = selectedIds)
    }

    /**
     * 全选当前文件列表中的所有文件。
     */
    fun selectAll() {
        val current = _uiState.value
        _uiState.value = current.copy(selectedIds = current.files.map { it.id }.toSet())
    }

    /**
     * 取消全部选中。
     */
    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet())
    }

    /**
     * 将当前选中的文件保存到用户网盘根目录。
     *
     * 未选中任何文件时通过 [saveResult] 发送提示。
     * 防止重复点击：保存中 ([isSaving]) 时忽略后续调用。
     */
    fun saveSelectedFiles() {
        val selectedIds = _uiState.value.selectedIds.toList()
        if (selectedIds.isEmpty()) {
            _saveResult.tryEmit(ShareSaveResult("请先选择文件", shouldExit = false))
            return
        }
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val savedCount = repository.saveFilesToRoot(selectedIds)
                _uiState.value = _uiState.value.copy(isSaving = false)
                _saveResult.emit(
                    if (savedCount > 0) {
                        markHandled(ShareLinkHandledAction.Saved)
                        ShareSaveResult("已保存${savedCount}个文件到网盘", shouldExit = true)
                    } else {
                        ShareSaveResult("保存失败，文件已不存在", shouldExit = false)
                    }
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false)
                _saveResult.emit(ShareSaveResult("保存失败，请重试", shouldExit = false))
            }
        }
    }

    fun markOpened() {
        markHandled(ShareLinkHandledAction.Opened)
    }

    private fun markHandled(action: ShareLinkHandledAction) {
        val token = currentToken ?: return
        viewModelScope.launch {
            shareLinkStore.markHandled(token, action)
        }
    }
}
