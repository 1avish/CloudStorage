package com.bytedance.cloudstorage.domain.model

/**
 * 云端文件业务模型（Domain Model）
 *
 * 用途：Repository 对外返回、ViewModel 使用、UI 展示。
 * 不依赖 Room 注解，不依赖接口字段格式。
 *
 * 与 FileEntity 的区别：
 * - FileEntity 只关心数据库 Schema（字段名、类型要与 SQLite 表对应）
 * - CloudFile 只关心业务展示（字段名更语义化，type 使用枚举而非 String）
 *
 * Flutter 类比：类似 Dart 里的纯 data class / Freezed model。
 */
data class CloudFile(
    val id: String,
    val name: String,
    val type: FileType,
    val size: Long,
    val uri: String?,
    val parentId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long?,
    val lastSavedAt: Long?,
)
