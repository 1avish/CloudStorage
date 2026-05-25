package com.bytedance.cloudstorage.data.remote.datasource

import com.bytedance.cloudstorage.data.remote.dto.FileDto
import com.bytedance.cloudstorage.data.remote.dto.StorageInfoDto

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
        return StorageInfoDto(usedG = 4.9f, totalG = 10.0f)
    }

    private fun parseFiles(): List<FileDto> {
        val now = System.currentTimeMillis()
        val result = mutableListOf<FileDto>()

        // ── 手写种子数据（6 条，保证首页场景覆盖）──
        val seeds = listOf(
            Triple("folder_001", "项目资料", "folder"),
            Triple("folder_002", "视频素材", "folder"),
            Triple("folder_003", "前端文档", "folder"),
            Triple("file_001",    "需求说明.txt", "txt"),
            Triple("file_002",    "产品演示.mp4", "video"),
            Triple("file_003",    "会议记录.txt", "txt"),
        )
        seeds.forEach { (id, name, type) ->
            result.add(FileDto(
                fileId = id, name = name, size = if (type == "folder") 0L else (1024L..52428800L).random(),
                uri = null, type = type, parentId = if (id == "folder_003") "folder_001" else null,
                createdAt = now - 86400_000L * 30, updatedAt = now - 86400_000L * 30,
                lastOpenedAt = if (type != "folder") now - (5..120).random() * 60_000L else null,
                lastSavedAt  = if (id == "file_003") now - 3600_000L * 5 else null
            ))
        }

        // ── 批量生成 120 条，按类型均匀分布 ──
        val types  = listOf("folder", "video", "txt")
        val folderNames = listOf("设计稿", "会议录音", "合同文件", "学习笔记", "产品原型", "周报汇总",
            "客户资料", "技术文档", "运营数据", "财务报表", "培训视频", "项目交付")
        val videoNames  = listOf("团建花絮", "发布会录屏", "用户访谈", "产品演示", "技术分享",
            "新人培训", "季度复盘", "需求评审", "UI 动效参考", "竞品分析录屏")
        val docNames    = listOf("接口文档", "数据库设计", "需求 PRD", "测试用例", "上线 checklist",
            "代码规范", "部署指南", "故障复盘", "架构方案", "性能优化", "埋点方案", "AB 实验报告")

        repeat(120) { i ->
            val type = types[i % 3]
            val suffix = String.format("%03d", i + 10)
            val name = when (type) {
                "folder" -> "${folderNames[i % folderNames.size]}_${i / types.size}"
                "video"  -> "${videoNames[i % videoNames.size]}.mp4"
                else     -> "${docNames[i % docNames.size]}.txt"
            }
            val parentId = if (type != "folder" && i % 4 == 0) "folder_001"
                           else if (type != "folder" && i % 4 == 1) "folder_002"
                           else null
            result.add(FileDto(
                fileId       = "gen_${type}_$suffix",
                name         = name,
                size         = if (type == "folder") 0 else (1024L..104857600L).random(),
                uri          = null,
                type         = type,
                parentId     = parentId,
                createdAt    = now - (i * 3600_000L),
                updatedAt    = now - (i * 1800_000L),
                lastOpenedAt = if (type != "folder") now - (i * 60_000L + (0..300_000L).random()) else null,
                lastSavedAt  = null
            ))
        }

        return result
    }

}
