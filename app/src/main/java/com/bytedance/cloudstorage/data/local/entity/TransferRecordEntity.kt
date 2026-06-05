package com.bytedance.cloudstorage.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_records")
data class TransferRecordEntity(
    @PrimaryKey
    val recordId: String,
    val fileId: String?,
    val name: String,
    val size: Long,
    val type: String,
    val direction: String,
    val source: String,
    val status: String,
    val savedPath: String,
    val createdAt: Long,
)
