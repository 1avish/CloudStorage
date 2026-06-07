package com.bytedance.cloudstorage.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room 实体：传输记录表
 *
 * 记录用户上传和下载的文件历史，供传输列表页展示。
 * 字段说明：
 * - recordId: UUID 主键
 * - fileId:   关联的文件 ID（下载场景可为 null）
 * - direction: "upload" 或 "download"
 * - source:    来源类型，如 manual_upload / share_saved / file_download
 * - status:    "completed" / "transferring" / "failed"
 * - savedPath: 存储路径
 */
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
