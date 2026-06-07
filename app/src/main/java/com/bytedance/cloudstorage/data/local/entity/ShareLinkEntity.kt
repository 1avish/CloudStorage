package com.bytedance.cloudstorage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room 实体：分享链接主表
 *
 * 以 token 为主键，记录分享创建时间和处理状态。
 * - handledAt / handledAction: 记录用户是否已查看/保存/关闭该链接
 * - isDeleted: 逻辑删除标记
 */
@Entity(tableName = "share_links")
data class ShareLinkEntity(
    @PrimaryKey
    val token: String,
    val createdAt: Long,
    val handledAt: Long? = null,
    val handledAction: String? = null,
    val isDeleted: Boolean = false,
)

/**
 * Room 实体：分享链接关联的文件表
 *
 * 一条分享链接可以关联多个文件（一对多）。
 * 外键关联 share_links 表，主键删除时级联删除关联记录。
 */
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
