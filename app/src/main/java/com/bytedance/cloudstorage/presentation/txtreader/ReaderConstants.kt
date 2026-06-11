package com.bytedance.cloudstorage.presentation.txtreader

import androidx.compose.ui.graphics.Color

// ── 主题色 ──
internal val ReaderSurface = Color(0xFFF6F6F6)
internal val ReaderText = Color(0xFF111827)
internal val ReaderSubText = Color(0xFF8B919E)
internal val ReaderBlue = Color(0xFFFFE36A)
internal val ReaderSkyBlue = Color(0xFFFFF4BA)
internal val ReaderPanelSurface = Color.White
internal val ReaderControlBg = Color(0xFFF3F5F8)
internal val ReaderDivider = Color(0xFFE8ECF2)

// 阅读器底部设置面板的 4 个 Tab 页
enum class ReaderSettingPanel {
    Brightness,   // 亮度
    Background,   // 背景色
    Font,         // 字号（A-/A+）
    LineSpacing   // 行间距（小/较小/适中/大）
}

// 背景色选项：浅灰（默认）、米黄、浅绿、粉色、纯黑（暗黑模式）
internal val ReaderBackgroundOptions = listOf(
    Color(0xFFF6F6F6),
    Color(0xFFEFE0BF),
    Color(0xFFE1ECD6),
    Color(0xFFF3DDDD),
    Color(0xFF000000)
)

// 字号候选值（单位 sp，通过 .ws 缩放），默认 index=2 → 24sp
internal val ReaderFontSizes = listOf(20, 22, 24, 26, 28, 30)
// 行高候选值（与字号一一对应，保持约 1.75 行距比），默认 index=2 → 42sp
internal val ReaderLineHeights = listOf(34, 38, 42, 48)
// 行间距档位标签，与 ReaderLineHeights 一一对应
internal val ReaderLineSpacingLabels = listOf("小", "较小", "适中", "大")

// 每次从文件读取的缓冲区大小（16KB），影响 I/O 次数
// 调试日志开关：设为 true 打开分页性能日志，false 关闭
internal const val READER_SETTING_ROW_HEIGHT = 56
internal const val READER_BOTTOM_BAR_ITEM_HEIGHT = 44
internal const val DEBUG_READER_LOG = false

internal const val READ_BUFFER_SIZE = 16 * 1024

// 每个文本分段的最大字符数（8KB），分段送给 TextMeasurer 测量
// 越大测量越准（减少跨段边界误差），但单次测量越慢
internal val ReaderLineHeightMultipliers = listOf(1.40f, 1.55f, 1.70f, 1.90f)

internal fun readerLineHeight(fontSize: Int, lineSpacingIndex: Int): Float {
    val multiplier = ReaderLineHeightMultipliers[lineSpacingIndex.coerceIn(ReaderLineHeightMultipliers.indices)]
    return fontSize * multiplier
}

internal const val MAX_SEGMENT_CHARS = 8 * 1024
internal const val READER_METRICS_TAG = "TxtReaderMetrics"
internal const val LOG_PROGRESS_PAGE_INTERVAL = 50
