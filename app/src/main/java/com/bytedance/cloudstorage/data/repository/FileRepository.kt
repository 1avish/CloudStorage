package com.bytedance.cloudstorage.data.repository

import com.bytedance.cloudstorage.data.local.dao.FileDao
import com.bytedance.cloudstorage.data.local.entity.FileEntity
import com.bytedance.cloudstorage.data.mock.MockDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/** 文件夹父级信息，供首页展示「位置」字段 */
data class FolderInfo(
    val parentName: String,
    val hasGrandParent: Boolean,
)

/**
 * 文件数据仓库（Repository）
 *
 * 职责：
 * - 统一数据访问入口，隔离数据源细节（Room / Mock / 网络）
 * - ViewModel 只调用 Repository，不直接接触 DAO 或网络
 *
 * MVP 阶段数据源为 Room + Mock 数据注入，
 * 后续可替换为真实网络请求，上层代码无需修改。
 */
class FileRepository(private val fileDao: FileDao) {

    /** 最近浏览文件（Flow，数据变更时自动通知） */
    val recentOpenedFiles: Flow<List<FileEntity>> = fileDao.getRecentOpenedFiles()

    /** 最近转存文件 */
    val recentSavedFiles: Flow<List<FileEntity>> = fileDao.getRecentSavedFiles()

    /**
     * 模拟网络请求：从 Mock JSON 注入数据到 Room
     *
     * 流程：模拟网络延迟 → 解析 JSON → 写入数据库
     * 只在数据库为空时执行，避免重复注入。
     */
    suspend fun initializeMockDataIfEmpty() {
        val count = fileDao.getFileCount()
        if (count == 0) {
            // 模拟网络请求延迟
            delay(MockDataSource.MOCK_NETWORK_DELAY)
            // 解析 Mock JSON 并写入 Room
            val files = MockDataSource.parseFiles()
            fileDao.insertFiles(files)
        }
    }

    /**
     * 获取存储信息（来自 Mock JSON）
     *
     * 真实项目中应从服务器 API 获取，这里直接从 Mock JSON 解析。
     */
    fun getStorageInfo(): Pair<Float, Float> {
        val info = MockDataSource.parseStorageInfo()
        return info.usedG to info.totalG
    }

    /** 查询文件夹的父级信息（名称 + 是否有祖父文件夹） */
    suspend fun getFolderInfo(folderId: String): FolderInfo? {
        val name = fileDao.getFolderNameById(folderId) ?: return null
        val grandParentId = fileDao.getParentIdOfFolder(folderId)
        return FolderInfo(
            parentName = name,
            hasGrandParent = grandParentId != null,
        )
    }
}
