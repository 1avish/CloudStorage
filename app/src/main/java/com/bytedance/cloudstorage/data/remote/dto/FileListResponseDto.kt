package com.bytedance.cloudstorage.data.remote.dto

/**
 * 服务端文件列表响应 DTO
 *
 * 对应 Mock JSON 的顶层结构：
 * ```json
 * {
 *   "storage": { "usedG": 4.9, "totalG": 10.0 },
 *   "files": [ ... ]
 * }
 * ```
 *
 * 真实后端通常外层还会包裹 code / message 等状态字段，
 * MVP 阶段简化为只包含 data 部分。
 */
data class FileListResponseDto(
    val storage: StorageInfoDto,
    val files: List<FileDto>,
)

/**
 * 存储信息 DTO
 */
data class StorageInfoDto(
    val usedG: Float,
    val totalG: Float,
)
