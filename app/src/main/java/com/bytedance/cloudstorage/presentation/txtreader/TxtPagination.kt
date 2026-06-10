package com.bytedance.cloudstorage.presentation.txtreader

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.BufferedReader
import java.io.StringReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.math.min

// ── 流式分页主入口 ──
// 打开文件 → 按段读取并 normalize → 每段用 TextMeasurer 测量行数 → 累积满一页就 emit
// 全程只在内存中持有「当前段」和「当前页」，不持有全文

internal suspend fun paginateTxtStream(
    context: Context,
    fileName: String,
    fileUri: String,
    charset: Charset,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    maxWidthPx: Int,
    maxLinesPerPage: Int,
    onPage: suspend (String) -> Unit,
) {
    openTxtReader(context, fileName, fileUri, charset).use { reader ->
        val pageAccumulator = PageAccumulator(
            maxLinesPerPage = maxLinesPerPage,
            onPage = onPage
        )

        streamNormalizedSegments(reader) { segment ->
            paginateSegment(
                segment = segment,
                textMeasurer = textMeasurer,
                style = style,
                maxWidthPx = maxWidthPx,
                pageAccumulator = pageAccumulator
            )
        }
        pageAccumulator.flush()
    }
}

// ── 页累积器 ──
// 负责把「行」攒成「页」。每攒够 maxLinesPerPage 行就 emit 一页。

internal class PageAccumulator(
    private val maxLinesPerPage: Int,
    private val onPage: suspend (String) -> Unit,
) {
    private val pageBuilder = StringBuilder()
    private var lineCount = 0
    private var emittedAnyPage = false

    fun remainingLines(): Int = (maxLinesPerPage - lineCount).coerceAtLeast(1)

    suspend fun append(
        segment: String,
        start: Int,
        end: Int,
        lines: Int,
    ) {
        pageBuilder.append(segment, start, end)
        lineCount += lines

        if (lineCount >= maxLinesPerPage) {
            emit()
        }
    }

    suspend fun flush() {
        if (pageBuilder.isNotEmpty() || !emittedAnyPage) {
            emit()
        }
    }

    private suspend fun emit() {
        val page = pageBuilder.toString().trimEnd('\n').ifEmpty { " " }
        onPage(page)
        pageBuilder.clear()
        lineCount = 0
        emittedAnyPage = true
    }
}

// ── 单段分页：用 TextMeasurer 测量一段文本的排版，按视觉行数拆给 PageAccumulator ──
// TextMeasurer 会处理中英文混排、自动换行、标点禁则等

internal suspend fun paginateSegment(
    segment: String,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    maxWidthPx: Int,
    pageAccumulator: PageAccumulator,
) {
    if (segment.isEmpty()) return

    val layout = textMeasurer.measure(
        text = AnnotatedString(segment),
        style = style,
        overflow = TextOverflow.Clip,
        constraints = Constraints(maxWidth = maxWidthPx)
    )
    val lineCount = layout.lineCount.coerceAtLeast(1)
    var lineIndex = 0
    var segmentStart = 0

    while (lineIndex < lineCount) {
        currentCoroutineContext().ensureActive()

        val remainingLinesOnPage = pageAccumulator.remainingLines()
        val linesToTake = min(remainingLinesOnPage, lineCount - lineIndex)
        val lastLineIndex = lineIndex + linesToTake - 1

        if (segmentStart >= segment.length) break
        val rawSegmentEnd = layout.getLineEnd(lastLineIndex, visibleEnd = false)
        val segmentEnd = when {
            rawSegmentEnd <= segmentStart -> (segmentStart + 1).coerceAtMost(segment.length)
            else -> rawSegmentEnd.coerceAtMost(segment.length)
        }

        pageAccumulator.append(
            segment = segment,
            start = segmentStart,
            end = segmentEnd,
            lines = linesToTake
        )
        lineIndex += linesToTake
        segmentStart = segmentEnd
    }
}

// ── 流式读取 + 换行符 normalize + 分段 ──
// 从 BufferedReader 按 READ_BUFFER_SIZE 读取，逐字符处理：
//   \r\n → \n（Windows 换行统一化）
//   \r   → \n（老 Mac 换行统一化）
// 每积累 MAX_SEGMENT_CHARS 个字符或遇到 \n 就产出一个段

internal suspend fun streamNormalizedSegments(
    reader: BufferedReader,
    onSegment: suspend (String) -> Unit,
) {
    val readBuffer = CharArray(READ_BUFFER_SIZE)
    val segmentBuilder = StringBuilder(MAX_SEGMENT_CHARS)
    var skipNextLf = false

    suspend fun appendChar(char: Char) {
        segmentBuilder.append(char)
        if (char == '\n' || segmentBuilder.length >= MAX_SEGMENT_CHARS) {
            onSegment(segmentBuilder.toString())
            segmentBuilder.clear()
        }
    }

    while (true) {
        currentCoroutineContext().ensureActive()

        val count = reader.read(readBuffer)
        if (count == -1) break

        for (index in 0 until count) {
            val char = readBuffer[index]
            if (skipNextLf) {
                skipNextLf = false
                if (char == '\n') continue
            }

            when (char) {
                '\r' -> {
                    appendChar('\n')
                    skipNextLf = true
                }
                else -> appendChar(char)
            }
        }
    }

    if (segmentBuilder.isNotEmpty()) {
        onSegment(segmentBuilder.toString())
    }
}

// ── 打开文本文件的 BufferedReader ──
// fileUri 为空时返回模拟数据的 reader

internal fun openTxtReader(
    context: Context,
    fileName: String,
    fileUri: String,
    charset: Charset = StandardCharsets.UTF_8,
): BufferedReader {
    if (fileUri.isBlank()) {
        return BufferedReader(StringReader(buildMockTxtContent(fileName)))
    }

    val uri = Uri.parse(fileUri)
    val input = context.contentResolver.openInputStream(uri)
        ?: error("无法打开文本文件")
    return input.bufferedReader(charset)
}

// ── 模拟数据（无真实文件时使用）──

internal fun buildMockTxtContent(fileName: String): String {
    val title = fileName.ifBlank { "未命名文档.txt" }
    val paragraph = """
        $title

        这是一个应用内 TXT 阅读器示例。当前文件来自内置模拟数据，没有真实文件 URI，因此使用这段示例文本来验证分页阅读体验。

        阅读器会根据当前屏幕宽度、高度、字号和行高动态分页。向左滑动进入下一页，向右滑动回到上一页。

        文本内容保持原始换行，并按实际排版后的行数切分页面，避免只按固定字符数切分造成一页过满或过空。
    """.trimIndent()

    return (1..12).joinToString(separator = "\n\n") { index ->
        "第 $index 节\n$paragraph"
    }
}
