package com.bytedance.cloudstorage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bytedance.cloudstorage.data.local.entity.TransferRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TransferRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<TransferRecordEntity>)

    @Query("""
        SELECT * FROM transfer_records
        WHERE direction = :direction
        ORDER BY createdAt DESC
    """)
    fun observeRecords(direction: String): Flow<List<TransferRecordEntity>>
}
