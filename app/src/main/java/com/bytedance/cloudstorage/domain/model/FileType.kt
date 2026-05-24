package com.bytedance.cloudstorage.domain.model

/**
 * 文件类型枚举
 *
 * 与 Room Entity 中的 type: String 字段对应，
 * 但使用枚举替代裸字符串，避免全项目散落 "txt" / "video" 等魔法值。
 *
 * Flutter 类比：类似 Dart 的 enum FileType { folder, video, txt, other }
 */
enum class FileType {
    Folder,
    Video,
    Txt,
    Other;

    companion object {
        /**
         * 从 Entity / DTO 的字符串字段转换而来。
         * 未知类型归为 [Other]，避免解析异常。
         */
        fun fromString(value: String): FileType = when (value) {
            "folder" -> Folder
            "video"  -> Video
            "txt"    -> Txt
            else     -> Other
        }
    }
}
