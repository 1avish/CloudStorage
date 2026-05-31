package com.bytedance.cloudstorage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.bytedance.cloudstorage.data.local.entity.ShareLinkEntity
import com.bytedance.cloudstorage.data.local.entity.ShareLinkFileEntity

@Dao
interface ShareLinkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShareLink(link: ShareLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShareLinkFiles(files: List<ShareLinkFileEntity>)

    @Query("""
        SELECT fileId FROM share_link_files
        WHERE token = :token
        ORDER BY sortOrder ASC
    """)
    suspend fun getFileIds(token: String): List<String>

    @Query("""
        SELECT COUNT(*) FROM share_links
        WHERE token = :token AND isDeleted = 0
    """)
    suspend fun getActiveShareCount(token: String): Int

    @Query("""
        SELECT COUNT(*) FROM share_links
        WHERE token = :token AND isDeleted = 0 AND handledAt IS NULL
    """)
    suspend fun getAutoPromptableShareCount(token: String): Int

    @Query("""
        UPDATE share_links
        SET handledAt = :handledAt, handledAction = :action
        WHERE token = :token AND handledAt IS NULL
    """)
    suspend fun markHandled(token: String, action: String, handledAt: Long)

    @Transaction
    suspend fun createShare(link: ShareLinkEntity, files: List<ShareLinkFileEntity>) {
        insertShareLink(link)
        insertShareLinkFiles(files)
    }
}
