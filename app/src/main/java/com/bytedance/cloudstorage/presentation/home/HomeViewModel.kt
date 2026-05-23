package com.bytedance.cloudstorage.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bytedance.cloudstorage.data.local.database.AppDatabase
import com.bytedance.cloudstorage.data.local.entity.FileEntity
import com.bytedance.cloudstorage.data.repository.FileRepository
import com.bytedance.cloudstorage.data.repository.FolderInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 带父文件夹信息的文件项，供首页 UI 直接使用 */
data class RecentFileWithParent(
    val file: FileEntity,
    val parentName: String,      // 父文件夹名称，null 时显示"根目录"
    val hasGrandParent: Boolean, // 父文件夹是否还有父文件夹
)

/**
 * 网盘首页 ViewModel
 *
 * 职责：
 * - 持有页面状态（StateFlow）
 * - 在 viewModelScope 中启动协程，调用 Repository 注入 Mock 数据
 * - 收集 Room Flow 并转换为 UI 状态
 *
 * 使用 AndroidViewModel（而非普通 ViewModel）是因为需要 Application Context
 * 来初始化 Room 数据库实例。
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FileRepository

    // ── 页面状态 ──

    private val _usedStorageG = MutableStateFlow(0f)
    val usedStorageG: StateFlow<Float> = _usedStorageG.asStateFlow()

    private val _totalStorageG = MutableStateFlow(10f)
    val totalStorageG: StateFlow<Float> = _totalStorageG.asStateFlow()

    private val _recentViews = MutableStateFlow<List<RecentFileWithParent>>(emptyList())
    val recentViews: StateFlow<List<RecentFileWithParent>> = _recentViews.asStateFlow()

    private val _recentSaves = MutableStateFlow<List<RecentFileWithParent>>(emptyList())
    val recentSaves: StateFlow<List<RecentFileWithParent>> = _recentSaves.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // 初始化数据库和 Repository
        val db = AppDatabase.getInstance(application)
        repository = FileRepository(db.fileDao())

        // 启动数据加载
        loadData()
    }

    /**
     * 加载数据流程：
     *
     * 1. 注入 Mock 数据（模拟网络请求，首次为空时执行）
     * 2. 读取存储信息
     * 3. 收集 Room Flow，持续监听数据库变化
     *
     * Flow 收集在 viewModelScope 中进行，
     * ViewModel 销毁时自动取消，避免内存泄漏。
     */
    private fun loadData() {
        viewModelScope.launch {
            // 步骤 1：模拟网络请求，注入 Mock 数据到 Room
            repository.initializeMockDataIfEmpty()

            // 步骤 2：获取存储信息
            val (used, total) = repository.getStorageInfo()
            _usedStorageG.value = used
            _totalStorageG.value = total

            // 步骤 3：收集 Room Flow，附加父文件夹信息
            launch {
                repository.recentOpenedFiles.collect { files ->
                    _recentViews.value = files.map { it.toRecentFileWithParent() }
                }
            }
            launch {
                repository.recentSavedFiles.collect { files ->
                    _recentSaves.value = files.map { it.toRecentFileWithParent() }
                }
            }

            _isLoading.value = false
        }
    }

    private suspend fun FileEntity.toRecentFileWithParent(): RecentFileWithParent {
        val info = parentId?.let { repository.getFolderInfo(it) }
        return RecentFileWithParent(
            file = this,
            parentName = info?.parentName ?: "根目录",
            hasGrandParent = info?.hasGrandParent ?: false,
        )
    }
}
