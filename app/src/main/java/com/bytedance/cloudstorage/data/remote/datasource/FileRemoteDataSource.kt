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

    /**
     * 创建文件夹
     *
     * @param name 文件夹名称
     * @param parentId 父文件夹 ID，null 表示根目录
     * @return 新建文件夹的服务端 ID
     */
    suspend fun createFolder(name: String, parentId: String?): String

    /**
     * 上传文件元数据（模拟文件上传到服务端）
     *
     * @param fileId  客户端生成的文件 ID
     * @param name    文件名
     * @param size    文件大小（字节）
     * @param uri     文件 URI 字符串
     * @param coverUri 封面 URI 字符串，可为 null
     * @param type    文件类型键："video" / "txt" / "other"
     * @param parentId 目标父文件夹 ID，null 表示根目录
     */
    suspend fun uploadFile(
        fileId: String,
        name: String,
        size: Long,
        uri: String,
        coverUri: String?,
        type: String,
        parentId: String?,
    )

    /**
     * 批量删除文件（标记为已删除）
     *
     * @param ids 待删除的文件 ID 列表
     */
    suspend fun deleteFiles(ids: List<String>)

    /**
     * 重命名文件
     *
     * @param fileId  文件 ID
     * @param newName 新名称
     */
    suspend fun renameFile(fileId: String, newName: String)

    /**
     * 批量移动文件
     *
     * @param ids            待移动的文件 ID 列表
     * @param targetParentId 目标父文件夹 ID，null 表示移动到根目录
     */
    suspend fun moveFiles(ids: List<String>, targetParentId: String?)

    /**
     * 将指定文件保存（复制）到用户网盘根目录
     *
     * @param fileIds 源文件 ID 列表
     * @return 成功保存的文件数量
     */
    suspend fun saveFilesToRoot(fileIds: List<String>): Int
}
