package com.bytedance.cloudstorage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.bytedance.cloudstorage.data.local.entity.FileEntity
import kotlinx.coroutines.flow.Flow

/**
 * 文件表的数据库访问对象（DAO）
 *
 * Room 会在编译期根据注解自动生成 SQL 实现代码。
 * 返回 Flow 的查询会在数据变更时自动通知收集者。
 */
@Dao
interface FileDao {

    /**
     * 获取最近浏览的文件（最多 3 条）
     *
     * 返回 Flow，数据库中 lastOpenedAt 变化时自动推送新数据。
     */
    @Query("""
        SELECT * FROM files
        WHERE isDeleted = 0 AND lastOpenedAt IS NOT NULL
        ORDER BY lastOpenedAt DESC
        LIMIT 3
    """)
    fun getRecentOpenedFiles(): Flow<List<FileEntity>>

    /**
     * 获取最近转存的文件（最多 3 条）
     */
    @Query("""
        SELECT * FROM files
        WHERE isDeleted = 0 AND lastSavedAt IS NOT NULL
        ORDER BY lastSavedAt DESC
        LIMIT 3
    """)
    fun getRecentSavedFiles(): Flow<List<FileEntity>>

    /**
     * 获取最近浏览的全部文件（无数量限制）
     */
    @Query("""
        SELECT * FROM files
        WHERE isDeleted = 0 AND lastOpenedAt IS NOT NULL
        ORDER BY lastOpenedAt DESC
    """)
    fun getAllRecentOpenedFiles(): Flow<List<FileEntity>>

    /**
     * 获取最近转存的全部文件（无数量限制）
     */
    @Query("""
        SELECT * FROM files
        WHERE isDeleted = 0 AND lastSavedAt IS NOT NULL
        ORDER BY lastSavedAt DESC
    """)
    fun getAllRecentSavedFiles(): Flow<List<FileEntity>>

    /**
     * 批量插入文件（用于 Mock 数据初始化）
     *
     * OnConflictStrategy.REPLACE：主键冲突时覆盖旧数据。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileEntity>)

    /**
     * 查询文件总数，用于判断是否需要注入 Mock 数据
     */
    @Query("SELECT COUNT(*) FROM files")
    suspend fun getFileCount(): Int

    /** 根据文件夹 ID 查询其名称 */
    @Query("SELECT name FROM files WHERE fileId = :folderId")
    suspend fun getFolderNameById(folderId: String): String?

    /** 根据文件夹 ID 查询其父文件夹 ID */
    @Query("SELECT parentId FROM files WHERE fileId = :folderId")
    suspend fun getParentIdOfFolder(folderId: String): String?

    /**
     * 获取指定文件夹下的所有文件（按类型、更新时间排序）
     *
     * @param parentId 父文件夹 ID，null 表示根目录
     */
    @Query("""
        SELECT * FROM files
        WHERE isDeleted = 0 AND
              CASE WHEN :parentId IS NULL THEN parentId IS NULL ELSE parentId = :parentId END
        ORDER BY
            CASE WHEN type = 'folder' THEN 0 ELSE 1 END,
            updatedAt DESC
    """)
    fun getFilesByParent(parentId: String?): Flow<List<FileEntity>>

    /**
     * 获取指定类型的所有文件（不分文件夹层级）
     *
     * @param type 文件类型字符串（folder / video / txt）
     */
    @Query("""
        SELECT * FROM files
        WHERE isDeleted = 0 AND type = :type
        ORDER BY updatedAt DESC
    """)
    fun getFilesByType(type: String): Flow<List<FileEntity>>

    /**
     * 获取指定文件夹下、指定类型的所有文件（含子文件夹）
     *
     * @param parentId 父文件夹 ID，null 表示根目录
     * @param type 文件类型字符串（folder / video / txt）
     */
    @Query("""
        SELECT * FROM files
        WHERE isDeleted = 0 AND type = :type
              AND CASE WHEN :parentId IS NULL THEN parentId IS NULL ELSE parentId = :parentId END
        ORDER BY updatedAt DESC
    """)
    fun getFilesByParentAndType(parentId: String?, type: String): Flow<List<FileEntity>>

    /** 根据文件 ID 查询单个文件 */
    @Query("""
        SELECT * FROM files
        WHERE isDeleted = 0 AND fileId = :fileId
        LIMIT 1
    """)
    suspend fun getFileById(fileId: String): FileEntity?

    /** 根据文件 ID 列表批量查询，用于分享链接还原文件列表 */
    @Query("""
        SELECT * FROM files
        WHERE isDeleted = 0 AND fileId IN (:fileIds)
    """)
    suspend fun getFilesByIds(fileIds: List<String>): List<FileEntity>

    /** 获取指定目录下的视频文件，供视频播放页选集使用 */
    @Query("""
        SELECT * FROM files
        WHERE isDeleted = 0 AND type = 'video'
              AND CASE WHEN :parentId IS NULL THEN parentId IS NULL ELSE parentId = :parentId END
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun getVideoFilesByParent(parentId: String?): Flow<List<FileEntity>>

    /**
     * 根据文件夹名称和父级 ID 查找文件夹 fileId（供面包屑导航反查使用）
     */
    @Query("""
        SELECT fileId FROM files
        WHERE isDeleted = 0 AND type = 'folder' AND name = :name
              AND CASE WHEN :parentId IS NULL THEN parentId IS NULL ELSE parentId = :parentId END
        LIMIT 1
    """)
    suspend fun findFolderIdByName(name: String, parentId: String?): String?

    /** 批量逻辑删除文件 */
    @Query("UPDATE files SET isDeleted = 1 WHERE fileId IN (:ids)")
    suspend fun deleteFiles(ids: List<String>)

    /**
     * 获取指定文件夹下的直接子文件 ID 列表（含已删除）。
     *
     * 仅用于递归删除场景：需要遍历已删除文件夹的子树，
     * 因此不过滤 isDeleted。
     */
    @Query("SELECT fileId FROM files WHERE parentId = :parentId")
    suspend fun getChildIdsByParent(parentId: String): List<String>

    /**
     * 获取指定文件夹下的所有文件（挂起版本，非 Flow）。
     *
     * 用于事务内读取（如递归复制文件树），避免 Flow 在事务中
     * 使用不同数据库连接导致读不到未提交的写入。
     *
     * @param parentId 父文件夹 ID，null 表示根目录
     */
    @Query("""
        SELECT * FROM files
        WHERE isDeleted = 0 AND
              CASE WHEN :parentId IS NULL THEN parentId IS NULL ELSE parentId = :parentId END
        ORDER BY
            CASE WHEN type = 'folder' THEN 0 ELSE 1 END,
            updatedAt DESC
    """)
    suspend fun getFilesByParentOnce(parentId: String?): List<FileEntity>

    /** 重命名文件 */
    @Query("UPDATE files SET name = :newName, updatedAt = :now WHERE fileId = :fileId")
    suspend fun renameFile(fileId: String, newName: String, now: Long)

    /** 更新文件最后浏览时间 */
    @Query("UPDATE files SET lastOpenedAt = :now WHERE fileId = :fileId")
    suspend fun updateLastOpenedAt(fileId: String, now: Long)
    /** 计算所有未删除文件的总大小（字节），文件夹不计入 */
    @Query("SELECT COALESCE(SUM(size), 0) FROM files WHERE isDeleted = 0 AND type != 'folder'")
    suspend fun getTotalUsedSize(): Long

    /**
     * 批量移动文件（修改 parentId）
     *
     * @param ids 待移动的文件 ID 列表
     * @param newParentId 目标父文件夹 ID，null 表示移动到根目录
     * @param now 更新时间戳
     */
    @Query("UPDATE files SET parentId = :newParentId, updatedAt = :now WHERE fileId IN (:ids)")
    suspend fun moveFiles(ids: List<String>, newParentId: String?, now: Long)
    /**
     * 在事务中执行任意数据库操作。
     *
     * 用于 Repository 中需要原子性的多步读写场景（如递归复制文件树）。
     * Room 的 @Transaction 会在协程上下文中正确使用 withTransaction。
     */
    @Transaction
    suspend fun runInTransaction(block: suspend () -> Unit) {
        block()
    }
}
