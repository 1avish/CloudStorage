package com.bytedance.cloudstorage.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bytedance.cloudstorage.data.local.database.AppDatabase
import com.bytedance.cloudstorage.data.remote.datasource.MockFileRemoteDataSource
import com.bytedance.cloudstorage.data.repository.FileRepository
import com.bytedance.cloudstorage.data.repository.RecentFileWithFolderInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 网盘首页 ViewModel
 *
 * 职责：
 * - 持有页面状态（StateFlow）
 * - 调用 Repository 初始化数据和观察数据变化
 * - 将 Repository 返回的 Domain Model 直接传递给 UI
 *
 * 设计要点：
 * - ViewModel 不直接引用 FileEntity，只使用 CloudFile（由 Repository 封装在 RecentFileWithFolderInfo 中）
 * - ViewModel 不直接操作 Dao，父文件夹信息查询已在 Repository 内部完成
 * - 当前注入 MockFileRemoteDataSource；后续接入真实后端时，在 AppContainer 中替换绑定即可
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FileRepository

    // ── 页面状态 ──

    private val _usedStorageG = MutableStateFlow(0f)
    val usedStorageG: StateFlow<Float> = _usedStorageG.asStateFlow()

    private val _totalStorageG = MutableStateFlow(10f)
    // 第37行，不用改，保持 10f 即可
    val totalStorageG: StateFlow<Float> = _totalStorageG.asStateFlow()

    private val _recentViews = MutableStateFlow<List<RecentFileWithFolderInfo>>(emptyList())
    val recentViews: StateFlow<List<RecentFileWithFolderInfo>> = _recentViews.asStateFlow()

    private val _recentSaves = MutableStateFlow<List<RecentFileWithFolderInfo>>(emptyList())
    val recentSaves: StateFlow<List<RecentFileWithFolderInfo>> = _recentSaves.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = FileRepository(
            fileDao = db.fileDao(),
            remoteDataSource = MockFileRemoteDataSource(application),
        )
        loadData()
    }

    /**
     * 加载数据流程：
     * 1. 从远程数据源初始化数据到 Room（首次为空时执行）
     * 2. 读取存储空间信息
     * 3. 收集 Repository 返回的 Flow，直接转发给 UI
     */
    private fun loadData() {
        viewModelScope.launch {
            repository.initializeDataIfEmpty()

            val usedBytes = repository.getUsedStorageBytes()
            _usedStorageG.value = String.format(Locale.US, "%.1f", usedBytes / (1024f * 1024f * 1024f)).toFloat()

            launch {
                repository.observeRecentOpenedWithFolderInfo().collect { items ->
                    _recentViews.value = items
                }
            }
            launch {
                repository.observeRecentSavedWithFolderInfo().collect { items ->
                    _recentSaves.value = items
                }
            }

            _isLoading.value = false
        }
    }
}
