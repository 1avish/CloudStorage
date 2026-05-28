package com.bytedance.cloudstorage.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room 实体：文件表
 *
 * 对应 SQLite 中的 files 表，存储网盘中所有文件和文件夹的元数据。
 * 字段说明见 docs/数据库设计.md。
 */
@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey
    val fileId: String,            // UUID，主键
    val name: String,              // 文件名
    val size: Long,                // 文件大小（字节），文件夹为 0
    val uri: String?,              // content:// 或 file:// URI
    val coverUri: String? = null,  // 视频封面本地 URI
    val type: String,              // folder / video / txt / other
    val parentId: String?,         // 父文件夹 ID，null 表示根目录
    val isDeleted: Boolean = false,// 逻辑删除标记
    val createdAt: Long,           // 创建时间戳（毫秒）
    val updatedAt: Long,           // 更新时间戳（毫秒）
    val lastOpenedAt: Long? = null,// 最后浏览时间，用于「最近浏览」排序
    val lastSavedAt: Long? = null, // 最后转存时间，用于「最近转存」排序
)
