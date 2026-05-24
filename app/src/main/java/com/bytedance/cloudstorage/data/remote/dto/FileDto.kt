package com.bytedance.cloudstorage.data.remote.dto

/**
 * 文件数据传输对象（DTO）
 *
 * 职责：对应服务端 / Mock JSON 中单个文件的数据格式。
 * 字段命名和结构尽量贴近真实接口返回，与 Room Entity 解耦。
 *
 * 转换流程：JSON → FileDto → FileMapper.toEntity() → FileEntity → Room
 */
data class FileDto(
    val fileId: String,
    val name: String,
    val size: Long,
    val uri: String?,
    val type: String,       // "folder" / "video" / "txt" / "other"
    val parentId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long? = null,
    val lastSavedAt: Long? = null,
)
