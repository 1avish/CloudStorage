package com.bytedance.cloudstorage.data.mock

import com.bytedance.cloudstorage.data.local.entity.FileEntity
import org.json.JSONObject
import java.util.Calendar

/**
 * Mock 数据源，模拟网络请求返回的 JSON 数据。
 *
 * 真实项目中，这部分数据来自服务器 API。
 * MVP 阶段用硬编码 JSON 模拟，流程：
 *   JSON 字符串 → 解析为 List<FileEntity> → 写入 Room → UI 展示
 */
object MockDataSource {

    // 模拟网络请求延迟（毫秒），真实场景为网络往返耗时
    const val MOCK_NETWORK_DELAY = 300L

    /**
     * Mock JSON 数据
     *
     * 包含：
     * - storage：使用空间 / 总空间
     * - files：文件列表（含 lastOpenedAt / lastSavedAt 标记最近浏览和转存）
     */
    private const val MOCK_JSON = """
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

    /** 存储信息，解析自 Mock JSON */
    data class StorageInfo(val usedG: Float, val totalG: Float)

    /**
     * 解析 Mock JSON，返回存储信息。
     *
     * 使用 Android 内置 org.json 解析，无需引入第三方 JSON 库。
     */
    fun parseStorageInfo(): StorageInfo {
        val json = JSONObject(MOCK_JSON)
        val storage = json.getJSONObject("storage")
        return StorageInfo(
            usedG = storage.getDouble("usedG").toFloat(),
            totalG = storage.getDouble("totalG").toFloat()
        )
    }

    /**
     * 解析 Mock JSON，返回文件列表。
     *
     * 解析流程：JSON 字符串 → JSONObject → 遍历 files 数组 → 构造 FileEntity 列表。
     */
    fun parseFiles(): List<FileEntity> {
        val json = JSONObject(MOCK_JSON)
        val filesArray = json.getJSONArray("files")
        val result = mutableListOf<FileEntity>()

        // 动态时间戳，保证三种展示场景都能命中
        val now = System.currentTimeMillis()

        // 本年内、超过1小时：今年 3月15日 14:30
        val calThisYear = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.MARCH)
            set(Calendar.DAY_OF_MONTH, 15)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val thisYearTimestamp = calThisYear.timeInMillis

        // 跨年：2025年5月23日 10:00
        val calLastYear = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2025)
            set(Calendar.MONTH, Calendar.MAY)
            set(Calendar.DAY_OF_MONTH, 23)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val lastYearTimestamp = calLastYear.timeInMillis

        for (i in 0 until filesArray.length()) {
            val obj = filesArray.getJSONObject(i)
            val fileId = obj.getString("fileId")

            // 根据文件 ID 分配不同的演示时间戳
            val lastOpenedAt = when (fileId) {
                "file_001" -> now - 9 * 60 * 1000L   // 9分钟前 → "X分钟前"
                "file_002" -> thisYearTimestamp        // 本年3月15日 → "03-15 14:30"
                "file_004" -> lastYearTimestamp         // 2025年5月23日 → "2025-05-23"
                else       -> if (obj.has("lastOpenedAt")) obj.getLong("lastOpenedAt") else null
            }

            result.add(
                FileEntity(
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
}
