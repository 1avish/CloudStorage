package com.bytedance.cloudstorage.data.repository

import com.bytedance.cloudstorage.data.local.dao.FileDao
import com.bytedance.cloudstorage.data.mapper.FileMapper
import com.bytedance.cloudstorage.data.remote.datasource.FileRemoteDataSource
import com.bytedance.cloudstorage.domain.model.CloudFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 文件夹父级信息，供首页「位置」字段展示 */
data class FolderInfo(
    val parentName: String,
    val hasGrandParent: Boolean,
)

/**
 * 带父文件夹信息的文件项，供首页最近浏览/转存列表使用。
 *
 * 为什么在 Repository 层组装而不是 ViewModel 层：
 * - 查父文件夹名是数据库查询（suspend fun），放在 ViewModel 的 Flow collect 里会阻塞主线程
 * - Repository 天然持有 Dao，可以在 map 里直接查，逻辑更内聚
 */
data class RecentFileWithFolderInfo(
    val file: CloudFile,
    val parentName: String,
    val hasGrandParent: Boolean,
)

/**
 * 文件数据仓库（Repository）
 *
 * 职责：
 * - 统一数据访问入口，协调 FileRemoteDataSource（远程）与 FileDao（本地）
 * - 对外返回 Domain Model（CloudFile），而非 Entity
 * - 内部完成 DTO → Entity → Domain Model 的全部转换
 *
 * 重要约束：
 * - 依赖 FileRemoteDataSource 接口，不依赖具体 Mock 实现
 * - ViewModel 不应直接访问 Dao，只通过本类获取数据
 */
class FileRepository(
    private val fileDao: FileDao,
    private val remoteDataSource: FileRemoteDataSource,
) {

    /**
     * 从远程数据源初始化数据（首次启动时写入 Room）
     *
     * 流程：remoteDataSource.fetchFiles() → DTO → FileMapper.toEntityList() → insertRoom
     */
    suspend fun initializeDataIfEmpty() {
        val count = fileDao.getFileCount()
        if (count == 0) {
            val dtos = remoteDataSource.fetchFiles()
            val entities = FileMapper.toEntityList(dtos)
            fileDao.insertFiles(entities)
        }
    }

    /** 获取存储空间信息（来自远程数据源） */
    suspend fun getStorageInfo(): Pair<Float, Float> {
        val info = remoteDataSource.fetchStorageInfo()
        return info.usedG to info.totalG
    }

    /**
     * 最近浏览文件（带父文件夹信息）
     *
     * Room Flow → map 为 CloudFile → 组装 FolderInfo → 推送给 ViewModel。
     * ViewModel 直接 collect 此 Flow，无需额外数据库查询。
     */
    fun observeRecentOpenedWithFolderInfo(): Flow<List<RecentFileWithFolderInfo>> =
        fileDao.getRecentOpenedFiles().map { entities ->
            entities.map { entity ->
                val cloudFile = FileMapper.toDomain(entity)
                val folderInfo = entity.parentId?.let { getFolderInfo(it) }
                RecentFileWithFolderInfo(
                    file = cloudFile,
                    parentName = folderInfo?.parentName ?: "根目录",
                    hasGrandParent = folderInfo?.hasGrandParent ?: false,
                )
            }
        }

    /**
     * 最近转存文件（带父文件夹信息）
     */
    fun observeRecentSavedWithFolderInfo(): Flow<List<RecentFileWithFolderInfo>> =
        fileDao.getRecentSavedFiles().map { entities ->
            entities.map { entity ->
                val cloudFile = FileMapper.toDomain(entity)
                val folderInfo = entity.parentId?.let { getFolderInfo(it) }
                RecentFileWithFolderInfo(
                    file = cloudFile,
                    parentName = folderInfo?.parentName ?: "根目录",
                    hasGrandParent = folderInfo?.hasGrandParent ?: false,
                )
            }
        }

    /**
     * 查询文件夹的父级信息
     *
     * 仅在 Repository 内部使用，用于组装 RecentFileWithFolderInfo。
     */
    private suspend fun getFolderInfo(folderId: String): FolderInfo? {
        val name = fileDao.getFolderNameById(folderId) ?: return null
        val grandParentId = fileDao.getParentIdOfFolder(folderId)
        return FolderInfo(
            parentName = name,
            hasGrandParent = grandParentId != null,
        )
    }
}
