package com.bytedance.cloudstorage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "share_links")
data class ShareLinkEntity(
    @PrimaryKey
    val token: String,
    val createdAt: Long,
    val handledAt: Long? = null,
    val handledAction: String? = null,
    val isDeleted: Boolean = false,
)

@Entity(
    tableName = "share_link_files",
    primaryKeys = ["token", "fileId"],
    foreignKeys = [
        ForeignKey(
            entity = ShareLinkEntity::class,
            parentColumns = ["token"],
            childColumns = ["token"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["token"]),
        Index(value = ["fileId"]),
    ],
)
data class ShareLinkFileEntity(
    val token: String,
    val fileId: String,
    val sortOrder: Int,
)
