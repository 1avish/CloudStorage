package com.bytedance.cloudstorage.data.remote.datasource

import com.bytedance.cloudstorage.data.remote.dto.FileDto
import com.bytedance.cloudstorage.data.remote.dto.StorageInfoDto
import org.json.JSONObject
import java.util.Calendar

/**
 * Mock 远程数据源，模拟服务端接口返回。
 *
 * 从硬编码 JSON 解析数据，返回 DTO（而非 Entity）。
 * Repository 通过 FileMapper 将 DTO 转为 Entity 后写入 Room。
 *
 * 后续接入真实后端时，只需新建 RetrofitFileRemoteDataSource 实现同一接口，
 * 然后在 AppContainer 里替换一行绑定即可，Repository 和 ViewModel 不需要改。
 */
class MockFileRemoteDataSource : FileRemoteDataSource {

    companion object {
        /** 模拟网络请求延迟（毫秒） */
        const val MOCK_NETWORK_DELAY = 300L
    }

    override suspend fun fetchFiles(): List<FileDto> {
        kotlinx.coroutines.delay(MOCK_NETWORK_DELAY)
        return parseFiles()
    }

    override suspend fun fetchStorageInfo(): StorageInfoDto {
        return parseStorageInfo()
    }

    // ────────────────────────────────────────────
    // 以下为 JSON 解析逻辑，从原 MockDataSource 迁移而来
    // ────────────────────────────────────────────

    private fun parseStorageInfo(): StorageInfoDto {
        val json = JSONObject(MOCK_JSON)
        val storage = json.getJSONObject("storage")
        return StorageInfoDto(
            usedG = storage.getDouble("usedG").toFloat(),
            totalG = storage.getDouble("totalG").toFloat(),
        )
    }

    private fun parseFiles(): List<FileDto> {
        val json = JSONObject(MOCK_JSON)
        val filesArray = json.getJSONArray("files")
        val result = mutableListOf<FileDto>()

        // 动态时间戳，保证首页三种时间展示场景都能命中
        val now = System.currentTimeMillis()
        val thisYearTimestamp = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.MARCH)
            set(Calendar.DAY_OF_MONTH, 15)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val lastYearTimestamp = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2025)
            set(Calendar.MONTH, Calendar.MAY)
            set(Calendar.DAY_OF_MONTH, 23)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        for (i in 0 until filesArray.length()) {
            val obj = filesArray.getJSONObject(i)
            val fileId = obj.getString("fileId")

            val lastOpenedAt = when (fileId) {
                "file_001" -> now - 9 * 60 * 1000L   // 9分钟前
                "file_002" -> thisYearTimestamp        // 本年3月15日
                "file_004" -> lastYearTimestamp         // 2025年5月23日
                else       -> if (obj.has("lastOpenedAt")) obj.getLong("lastOpenedAt") else null
            }

            result.add(
                FileDto(
                    fileId       = fileId,
                    name         = obj.getString("name"),
                    size         = obj.getLong("size"),
                    uri          = if (obj.isNull("uri")) null else obj.getString("uri"),
                    type         = obj.getString("type"),
                    parentId     = if (obj.isNull("parentId")) null else obj.getString("parentId"),
                    createdAt    = obj.getLong("createdAt"),
                    updatedAt    = obj.getLong("updatedAt"),
                    lastOpenedAt = lastOpenedAt,
                    lastSavedAt  = if (obj.has("lastSavedAt")) obj.getLong("lastSavedAt") else null,
                )
            )
        }
        return result
    }

    private val MOCK_JSON = """
    {
      "storage": {
        "usedG": 4.9,
        "totalG": 10.0
      },
      "files": [
        {
          "fileId": "folder_001",
          "name": "项目资料",
          "size": 0,
          "uri": null,
          "type": "folder",
          "parentId": null,
          "createdAt": 1748000000000,
          "updatedAt": 1748000000000
        },
        {
          "fileId": "folder_003",
          "name": "前端文档",
          "size": 0,
          "uri": null,
          "type": "folder",
          "parentId": "folder_001",
          "createdAt": 1748000000000,
          "updatedAt": 1748000000000
        },
        {
          "fileId": "folder_002",
          "name": "视频",
          "size": 0,
          "uri": null,
          "type": "folder",
          "parentId": null,
          "createdAt": 1748000000000,
          "updatedAt": 1748000000000
        },
        {
          "fileId": "file_001",
          "name": "需求说明.txt",
          "size": 2048,
          "uri": "content://mock/requirements.txt",
          "type": "txt",
          "parentId": "folder_001",
          "createdAt": 1748000000000,
          "updatedAt": 1748000000000,
          "lastOpenedAt": 1748008100000
        },
        {
          "fileId": "file_002",
          "name": "产品演示.mp4",
          "size": 52428800,
          "uri": "content://mock/demo.mp4",
          "type": "video",
          "parentId": "folder_002",
          "createdAt": 1748000000000,
          "updatedAt": 1748000000000,
          "lastOpenedAt": 1748007900000
        },
        {
          "fileId": "file_003",
          "name": "会议记录.txt",
          "size": 1024,
          "uri": "content://mock/meeting.txt",
          "type": "txt",
          "parentId": "folder_001",
          "createdAt": 1748000000000,
          "updatedAt": 1748000000000,
          "lastSavedAt": 1748005000000
        },
        {
          "fileId": "file_004",
          "name": "组件设计.txt",
          "size": 512,
          "uri": "content://mock/component.txt",
          "type": "txt",
          "parentId": "folder_003",
          "createdAt": 1748000000000,
          "updatedAt": 1748000000000,
          "lastOpenedAt": 1748008200000
        }
      ]
    }
    """
}
