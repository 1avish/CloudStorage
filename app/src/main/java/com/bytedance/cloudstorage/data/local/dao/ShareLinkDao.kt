package com.bytedance.cloudstorage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.bytedance.cloudstorage.data.local.entity.ShareLinkEntity
import com.bytedance.cloudstorage.data.local.entity.ShareLinkFileEntity

/**
 * 分享链接表的数据库访问对象（DAO）
 *
 * 管理分享链接的创建、查询和处理状态更新。
 */
@Dao
interface ShareLinkDao {
    /** 插入分享链接主记录 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShareLink(link: ShareLinkEntity)

    /** 批量插入分享链接关联的文件记录 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShareLinkFiles(files: List<ShareLinkFileEntity>)

    /** 根据 token 获取关联的文件 ID 列表（按 sortOrder 排序） */
    @Query("""
        SELECT fileId FROM share_link_files
        WHERE token = :token
        ORDER BY sortOrder ASC
    """)
    suspend fun getFileIds(token: String): List<String>

    /** 检查指定 token 是否存在有效的（未删除的）分享记录 */
    @Query("""
        SELECT COUNT(*) FROM share_links
        WHERE token = :token AND isDeleted = 0
    """)
    suspend fun getActiveShareCount(token: String): Int

    /** 检查指定 token 是否可自动弹窗（未删除且未被处理过） */
    @Query("""
        SELECT COUNT(*) FROM share_links
        WHERE token = :token AND isDeleted = 0 AND handledAt IS NULL
    """)
    suspend fun getAutoPromptableShareCount(token: String): Int

    /** 标记分享链接为已处理（仅首次处理生效，handledAt IS NULL 的记录才更新） */
    @Query("""
        UPDATE share_links
        SET handledAt = :handledAt, handledAction = :action
        WHERE token = :token AND handledAt IS NULL
    """)
    suspend fun markHandled(token: String, action: String, handledAt: Long)

    /**
     * 创建分享链接（事务操作）
     *
     * 原子写入主表和文件关联表，保证数据一致性。
     */
    @Transaction
    suspend fun createShare(link: ShareLinkEntity, files: List<ShareLinkFileEntity>) {
        insertShareLink(link)
        insertShareLinkFiles(files)
    }
}
