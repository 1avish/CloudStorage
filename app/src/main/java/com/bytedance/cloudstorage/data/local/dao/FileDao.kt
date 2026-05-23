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
}
