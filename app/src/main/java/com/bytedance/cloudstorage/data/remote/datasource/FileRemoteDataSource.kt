package com.bytedance.cloudstorage.data.remote.datasource

import com.bytedance.cloudstorage.data.remote.dto.FileDto
import com.bytedance.cloudstorage.data.remote.dto.StorageInfoDto

/**
 * 远程数据源抽象接口
 *
 * 职责：定义所有远程文件操作的能力，不关心数据来自 Mock 还是真实后端。
 *
 * 当前实现：MockFileRemoteDataSource（从 JSON 解析）
 * 后续实现：RetrofitFileRemoteDataSource（从真实 HTTP 接口获取）
 *
 * 优势：替换实现时，Repository、ViewModel、UI 均无需修改。
 */
interface FileRemoteDataSource {

    /** 获取全部文件列表（DTO 格式） */
    suspend fun fetchFiles(): List<FileDto>

    /** 获取存储空间信息 */
    suspend fun fetchStorageInfo(): StorageInfoDto
}
