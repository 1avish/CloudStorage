package com.bytedance.cloudstorage.data.remote.datasource

import android.content.Context
import com.bytedance.cloudstorage.data.remote.dto.FileDto
import com.bytedance.cloudstorage.data.remote.dto.FileListResponseDto
import com.bytedance.cloudstorage.data.remote.dto.StorageInfoDto
import org.json.JSONObject

/**
 * Mock 远程数据源，模拟服务端接口返回。
 *
 * 从 assets/mock_files.json 解析数据，返回 DTO（而非 Entity）。
 * Repository 通过 FileMapper 将 DTO 转为 Entity 后写入 Room。
 *
 * 后续接入真实后端时，只需新建 RetrofitFileRemoteDataSource 实现同一接口，
 * 然后在 AppContainer 里替换一行绑定即可，Repository 和 ViewModel 不需要改。
 */
class MockFileRemoteDataSource(context: Context) : FileRemoteDataSource {

    companion object {
        /** 模拟网络请求延迟（毫秒） */
        const val MOCK_NETWORK_DELAY = 300L
    }

    private val cachedResponse: FileListResponseDto by lazy {
        val json = context.assets.open("mock_files.json")
            .bufferedReader()
            .use { it.readText() }
        parseResponse(json)
    }

    override suspend fun fetchFiles(): List<FileDto> {
        kotlinx.coroutines.delay(MOCK_NETWORK_DELAY)
        return cachedResponse.files
    }

    override suspend fun fetchStorageInfo(): StorageInfoDto {
        kotlinx.coroutines.delay(MOCK_NETWORK_DELAY)
        return cachedResponse.storage
    }

    override suspend fun createFolder(name: String, parentId: String?): String {
        kotlinx.coroutines.delay(MOCK_NETWORK_DELAY)
        val id = java.util.UUID.randomUUID().toString()
        // Mock：模拟服务端生成 ID，实际在 Repository 层写入本地
        return id
    }

    override suspend fun uploadFile(
        fileId: String,
        name: String,
        size: Long,
        uri: String,
        coverUri: String?,
        type: String,
        parentId: String?,
    ) {
        kotlinx.coroutines.delay(MOCK_NETWORK_DELAY)
        // Mock：模拟文件上传到服务端的网络请求
    }

    override suspend fun deleteFiles(ids: List<String>) {
        kotlinx.coroutines.delay(MOCK_NETWORK_DELAY)
        // Mock：模拟批量删除的网络请求
    }

    override suspend fun renameFile(fileId: String, newName: String) {
        kotlinx.coroutines.delay(MOCK_NETWORK_DELAY)
        // Mock：模拟重命名的网络请求
    }

    override suspend fun moveFiles(ids: List<String>, targetParentId: String?) {
        kotlinx.coroutines.delay(MOCK_NETWORK_DELAY)
        // Mock：模拟批量移动的网络请求
    }

    override suspend fun saveFilesToRoot(fileIds: List<String>): Int {
        kotlinx.coroutines.delay(MOCK_NETWORK_DELAY)
        // Mock：模拟批量转存到网盘根目录的网络请求，返回成功数量
        return fileIds.size
    }

    // ────────────────────────────────────────────
    // JSON 解析逻辑
    // ────────────────────────────────────────────

    private fun parseResponse(json: String): FileListResponseDto {
        val root = JSONObject(json)
        val storageObj = root.getJSONObject("storage")
        val storage = StorageInfoDto(
            usedG = storageObj.getDouble("usedG").toFloat(),
            totalG = storageObj.getDouble("totalG").toFloat(),
        )
        val filesArray = root.getJSONArray("files")
        val files = mutableListOf<FileDto>()
        for (i in 0 until filesArray.length()) {
            val obj = filesArray.getJSONObject(i)
            files.add(FileDto(
                fileId       = obj.getString("fileId"),
                name         = obj.getString("name"),
                size         = obj.getLong("size"),
                type         = obj.getString("type"),
                parentId     = if (obj.isNull("parentId")) null else obj.getString("parentId"),
                createdAt    = obj.getLong("createdAt"),
                updatedAt    = obj.getLong("updatedAt"),
                lastOpenedAt = if (obj.isNull("lastOpenedAt")) null else obj.getLong("lastOpenedAt"),
                lastSavedAt  = if (obj.isNull("lastSavedAt")) null else obj.getLong("lastSavedAt"),
                uri          = null,
            ))
        }
        return FileListResponseDto(storage = storage, files = files)
    }

}
