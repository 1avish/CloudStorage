package com.bytedance.cloudstorage.data.repository

import com.bytedance.cloudstorage.data.local.dao.FileDao
import com.bytedance.cloudstorage.data.local.entity.FileEntity
import com.bytedance.cloudstorage.data.mapper.FileMapper
import com.bytedance.cloudstorage.data.remote.datasource.FileRemoteDataSource
import com.bytedance.cloudstorage.domain.model.CloudFile
import com.bytedance.cloudstorage.domain.model.FileType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

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

    /**
     * 获取指定文件夹下的文件列表（Domain Model）
     *
     * @param parentId 父文件夹 ID，null 表示根目录
     */
    fun observeFilesByParent(parentId: String?): Flow<List<CloudFile>> =
        fileDao.getFilesByParent(parentId).map { entities ->
            FileMapper.toDomainList(entities)
        }

    /**
     * 获取指定类型的所有文件（Domain Model）
     *
     * @param type 文件类型字符串（folder / video / txt）
     */
    fun observeFilesByType(type: String): Flow<List<CloudFile>> =
        fileDao.getFilesByType(type).map { entities ->
            FileMapper.toDomainList(entities)
        }

    /**
     * 获取指定文件夹下、指定类型的所有文件（Domain Model）
     *
     * @param parentId 父文件夹 ID，null 表示根目录
     * @param type 文件类型字符串（folder / video / txt）
     */
    fun observeFilesByParentAndType(parentId: String?, type: String): Flow<List<CloudFile>> =
        fileDao.getFilesByParentAndType(parentId, type).map { entities ->
            FileMapper.toDomainList(entities)
        }

    /**
     * 根据文件夹名称和父级 ID 查找文件夹 ID（供面包屑导航反查使用）
     */
    suspend fun findFolderIdByName(name: String, parentId: String?): String? =
        fileDao.findFolderIdByName(name, parentId)

    /**
     * 根据文件 ID 列表批量查询文件，保持传入顺序返回。
     * ID 不存在或已删除的文件会被过滤掉。
     */
    suspend fun getFilesByIds(fileIds: List<String>): List<CloudFile> {
        if (fileIds.isEmpty()) return emptyList()
        val entitiesById = fileDao.getFilesByIds(fileIds).associateBy { it.fileId }
        return fileIds.mapNotNull { id -> entitiesById[id]?.let(FileMapper::toDomain) }
    }

    /**
     * 一次性获取指定父目录下的文件列表（挂起版本），用于复制文件树等场景。
     */
    suspend fun getFilesByParent(parentId: String?): List<CloudFile> =
        fileDao.getFilesByParentOnce(parentId).map(FileMapper::toDomain)

    /**
     * 将指定 ID 的文件复制到当前用户网盘根目录。
     *
     * 文件夹会递归复制整棵子树，重名文件自动追加编号后缀（如 "视频(1).mp4"）。
     *
     * @param fileIds 源文件 ID 列表（来自分享链接）
     * @return 成功复制的根级文件数量
     */
    suspend fun saveFilesToRoot(fileIds: List<String>): Int {
        if (fileIds.isEmpty()) return 0
        val sources = getFilesByIds(fileIds)
        if (sources.isEmpty()) return 0

        val savedCount = remoteDataSource.saveFilesToRoot(fileIds)
        if (savedCount == 0) return 0

        val now = System.currentTimeMillis()
        val rootNames = getFilesByParent(null).map { it.name }.toMutableSet()
        fileDao.runInTransaction {
            sources.forEach { source ->
                val savedName = generateUniqueName(source.name, rootNames)
                rootNames += savedName
                copyFileTree(
                    source = source,
                    targetName = savedName,
                    targetParentId = null,
                    savedAt = now,
                )
            }
        }
        return sources.size
    }

   /** 记录文件被浏览（更新 lastOpenedAt） */
    suspend fun markFileOpened(fileId: String){
        fileDao.updateLastOpenedAt(fileId, System.currentTimeMillis())
    }

    /** 获取真实已用存储（字节） */
    suspend fun getUsedStorageBytes(): Long = fileDao.getTotalUsedSize()

    /**
     * 递归复制单个文件/文件夹到目标父目录，返回新创建的文件 ID。
     *
     * 对于文件夹类型，会遍历其所有子文件逐一递归复制。
     */
    private suspend fun copyFileTree(
        source: CloudFile,
        targetName: String,
        targetParentId: String?,
        savedAt: Long,
    ): String {
        val newId = UUID.randomUUID().toString()
        val entity = FileEntity(
            fileId = newId,
            name = targetName,
            size = source.size,
            uri = source.uri,
            coverUri = source.coverUri,
            type = source.type.toEntityType(),
            parentId = targetParentId,
            createdAt = savedAt,
            updatedAt = savedAt,
            lastSavedAt = savedAt,
        )
        fileDao.insertFiles(listOf(entity))

        if (source.type == FileType.Folder) {
            getFilesByParent(source.id).forEach { child ->
                copyFileTree(
                    source = child,
                    targetName = child.name,
                    targetParentId = newId,
                    savedAt = savedAt,
                )
            }
        }
        return newId
    }

    /**
     * 生成唯一文件名：重名时追加 "(1)"、"(2)" 等编号后缀，保留原始扩展名。
     */
    private fun generateUniqueName(desiredName: String, existingNames: Set<String>): String {
        if (desiredName !in existingNames) return desiredName

        val dotIndex = desiredName.lastIndexOf('.')
        val baseName: String
        val extension: String
        if (dotIndex > 0) {
            baseName = desiredName.substring(0, dotIndex)
            extension = desiredName.substring(dotIndex)
        } else {
            baseName = desiredName
            extension = ""
        }

        var counter = 1
        var candidate: String
        do {
            candidate = "${baseName}(${counter})$extension"
            counter++
        } while (candidate in existingNames)

        return candidate
    }

    /** [FileType] → 数据库 type 字段字符串映射 */
    private fun FileType.toEntityType(): String = when (this) {
        FileType.Folder -> "folder"
        FileType.Video -> "video"
        FileType.Txt -> "txt"
        FileType.Other -> "other"
    }

    /**
     * 创建新文件夹
     *
     * @param name 文件夹名称
     * @param parentId 父文件夹 ID，null 表示根目录
     * @return 新建文件夹的 ID
     */
    suspend fun createFolder(name: String, parentId: String?): String {
        val id = remoteDataSource.createFolder(name, parentId)
        val now = System.currentTimeMillis()
        val entity = FileEntity(
            fileId = id,
            name = name,
            size = 0L,
            uri = null,
            coverUri = null,
            type = "folder",
            parentId = parentId,
            createdAt = now,
            updatedAt = now,
        )
        fileDao.insertFiles(listOf(entity))
        return id
    }

    /**
     * 上传文件：将系统文件选择器返回的文件元数据写入本地数据库。
     *
     * @param name 文件名（由 ContentResolver 查询得到）
     * @param size 文件大小（字节）
     * @param uri  content:// URI 字符串
     * @param type 文件类型键："video" / "txt" / "other"
     * @param parentId 存储位置的父文件夹 ID，null 表示根目录
     */
    suspend fun uploadFile(
        name: String,
        size: Long,
        uri: String,
        coverUri: String?,
        type: String,
        parentId: String?,
    ) {
        val id = java.util.UUID.randomUUID().toString()
        remoteDataSource.uploadFile(id, name, size, uri, coverUri, type, parentId)
        val now = System.currentTimeMillis()
        val entity = FileEntity(
            fileId = id,
            name = name,
            size = size,
            uri = uri,
            coverUri = coverUri,
            type = type,
            parentId = parentId,
            createdAt = now,
            updatedAt = now,
        )
        fileDao.insertFiles(listOf(entity))
    }

    /** 批量逻辑删除文件 */
    suspend fun deleteFiles(ids: List<String>) {
        remoteDataSource.deleteFiles(ids)
        fileDao.deleteFiles(ids)
    }

    /** 重命名文件 */
    suspend fun renameFile(fileId: String, newName: String) {
        remoteDataSource.renameFile(fileId, newName)
        fileDao.renameFile(fileId, newName, System.currentTimeMillis())
    }

    /**
     * 批量移动文件到目标文件夹
     *
     * @param ids 待移动的文件 ID 列表
     * @param targetParentId 目标父文件夹 ID，null 表示移动到根目录
     */
    suspend fun moveFiles(ids: List<String>, targetParentId: String?) {
        remoteDataSource.moveFiles(ids, targetParentId)
        fileDao.moveFiles(ids, targetParentId, System.currentTimeMillis())
    }
}
