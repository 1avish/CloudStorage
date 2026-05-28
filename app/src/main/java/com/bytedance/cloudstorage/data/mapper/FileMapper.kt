package com.bytedance.cloudstorage.data.mapper

import com.bytedance.cloudstorage.data.local.entity.FileEntity
import com.bytedance.cloudstorage.data.remote.dto.FileDto
import com.bytedance.cloudstorage.domain.model.CloudFile
import com.bytedance.cloudstorage.domain.model.FileType

/**
 * FileDto ↔ FileEntity 转换器
 *
 * Mapper 的核心价值：
 * - DTO 字段格式可能随服务端变化（例如 snake_case → camelCase、时间格式改变）
 * - Entity 结构只与数据库 Schema 对齐，不跟随接口变动
 * - Mapper 隔离两者，任何一端变化只改 Mapper，不影响 Repository 和 ViewModel
 *
 * Flutter 类比：类似 Model.fromJson() / model.toEntity() 的转换逻辑。
 */
object FileMapper {

    /**
     * DTO → Entity
     *
     * `isDeleted` 字段默认为 false，因为服务端返回的文件都是未删除的。
     */
    fun toEntity(dto: FileDto): FileEntity = FileEntity(
        fileId       = dto.fileId,
        name         = dto.name,
        size         = dto.size,
        uri          = dto.uri,
        coverUri     = null,
        type         = dto.type,
        parentId     = dto.parentId,
        isDeleted    = false,
        createdAt    = dto.createdAt,
        updatedAt    = dto.updatedAt,
        lastOpenedAt = dto.lastOpenedAt,
        lastSavedAt  = dto.lastSavedAt,
    )

    /** 批量转换 */
    fun toEntityList(dtos: List<FileDto>): List<FileEntity> = dtos.map(::toEntity)

    /**
     * Entity → Domain Model
     *
     * Repository 对外返回 CloudFile，ViewModel 只与 Domain Model 打交道，
     * 不直接接触 Room Entity，实现数据层与展示层的解耦。
     */
    fun toDomain(entity: FileEntity): CloudFile = CloudFile(
        id           = entity.fileId,
        name         = entity.name,
        type         = FileType.fromString(entity.type),
        size         = entity.size,
        uri          = entity.uri,
        coverUri     = entity.coverUri,
        parentId     = entity.parentId,
        createdAt    = entity.createdAt,
        updatedAt    = entity.updatedAt,
        lastOpenedAt = entity.lastOpenedAt,
        lastSavedAt  = entity.lastSavedAt,
    )

    /** 批量转换 */
    fun toDomainList(entities: List<FileEntity>): List<CloudFile> = entities.map(::toDomain)
}
