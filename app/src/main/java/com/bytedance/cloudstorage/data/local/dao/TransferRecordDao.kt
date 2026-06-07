package com.bytedance.cloudstorage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bytedance.cloudstorage.data.local.entity.TransferRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 传输记录表的数据库访问对象（DAO）
 *
 * 记录文件上传和下载的历史，供传输列表页按方向和时间排序查询。
 */
@Dao
interface TransferRecordDao {
    /** 插入单条传输记录 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TransferRecordEntity)

    /** 批量插入传输记录（批量转存/下载完成后统一写入） */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<TransferRecordEntity>)

    /** 按传输方向查询记录（Flow，数据变更时自动推送） */
    @Query("""
        SELECT * FROM transfer_records
        WHERE direction = :direction
        ORDER BY createdAt DESC
    """)
    fun observeRecords(direction: String): Flow<List<TransferRecordEntity>>
}
