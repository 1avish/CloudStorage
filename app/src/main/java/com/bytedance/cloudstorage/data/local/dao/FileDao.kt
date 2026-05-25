package com.bytedance.cloudstorage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    /** 重命名文件 */
    @Query("UPDATE files SET name = :newName, updatedAt = :now WHERE fileId = :fileId")
    suspend fun renameFile(fileId: String, newName: String, now: Long)
}
