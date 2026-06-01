package com.bytedance.cloudstorage.presentation.txtreader

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.StringReader
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// ── 主题色 ──
private val ReaderBg = Color(0xFFF6F6F6)
private val ReaderSurface = Color(0xFFF6F6F6)
private val ReaderText = Color(0xFF111827)
private val ReaderSubText = Color(0xFF8B919E)
private val ReaderBlue = Color(0xFF3370FF)

// 每次从文件读取的缓冲区大小（16KB），影响 I/O 次数
private const val READ_BUFFER_SIZE = 16 * 1024

// 每个文本分段的最大字符数（8KB），分段送给 TextMeasurer 测量
// 越大测量越准（减少跨段边界误差），但单次测量越慢
private const val MAX_SEGMENT_CHARS = 8 * 1024

// ── TXT 阅读器入口 ──

@Composable
fun TxtReaderScreen(
    fileId: String,
    fileName: String,
    fileUri: String,
    onBack: () -> Unit,
) {
    // fileKey = fileId + fileUri，用于在文件切换时重置所有状态
    TxtPagerContent(
        fileKey = "$fileId-$fileUri",
        fileName = fileName,
        fileUri = fileUri,
        onBack = onBack
    )
}

// ── 顶部栏：返回按钮 + 文件名 ──

@Composable
private fun TxtReaderTopBar(
    fileName: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReaderSurface)
            .statusBarsPadding()
            .height(56.w.dp)
            .padding(horizontal = 8.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = ReaderSubText
            )
        }
        Text(
            text = fileName,
            modifier = Modifier.weight(1f),
            fontSize = 16.ws.sp,
            fontWeight = FontWeight.Medium,
            color = ReaderSubText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── 加载中 / 错误 状态 ──

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = ReaderBlue)
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.w.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 15.ws.sp,
            color = ReaderSubText
        )
    }
}

// ── 分页阅读主体 ──
// 整个文件永远不会一次性读入内存。
// 流程：文件 → BufferedReader → 按段读取 → TextMeasurer 测量 → 逐页 emit → UI 渐进渲染

@Composable
private fun TxtPagerContent(
    fileKey: String,
    fileName: String,
    fileUri: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val bodyStyle = TextStyle(
        color = ReaderText,
        fontSize = 24.ws.sp,
        lineHeight = 42.ws.sp,
        fontFamily = FontFamily.SansSerif
    )
    val horizontalPadding = 24.w.dp
    val verticalPadding = 36.w.dp

    // pages 是响应式列表：每算出一页就 add 一次，Compose 自动重组 UI
    val pages = remember(fileKey) { mutableStateListOf<String>() }
    var isPaginating by remember(fileKey) { mutableStateOf(true) }
    var errorMessage by remember(fileKey) { mutableStateOf<String?>(null) }
    // pageCount 用 lambda 传入，pages.size 变化时 HorizontalPager 自动扩展
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReaderSurface)
    ) {
        TxtReaderTopBar(fileName = fileName, onBack = onBack)

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ReaderSurface)
        ) {
            // 从 BoxWithConstraints 拿到可用宽高，计算每页能放多少行
            val availableWidth = this.maxWidth
            val availableHeight = this.maxHeight
            val contentWidthPx = with(density) {
                (availableWidth - horizontalPadding * 2).roundToPx()
            }.coerceAtLeast(1)
            val contentHeightPx = with(density) {
                (availableHeight - verticalPadding * 2).roundToPx()
            }.coerceAtLeast(1)
            val lineHeightPx = with(density) { bodyStyle.lineHeight.toPx() }.coerceAtLeast(1f)
            val maxLines = max(1, floor(contentHeightPx / lineHeightPx).toInt())

            // 核心：文件/屏幕变化时重新分页
            // 在 Dispatchers.Default（后台线程）执行流式分页
            // 每算完一页通过 onPage 回调切回主线程 add 到 pages
            LaunchedEffect(fileKey, fileName, fileUri, contentWidthPx, maxLines, bodyStyle) {
                pages.clear()
                isPaginating = true
                errorMessage = null
                if (pagerState.currentPage != 0) {
                    pagerState.scrollToPage(0)
                }

                runCatching {
                    withContext(Dispatchers.Default) {
                        paginateTxtStream(
                            context = context.applicationContext,
                            fileName = fileName,
                            fileUri = fileUri,
                            textMeasurer = textMeasurer,
                            style = bodyStyle,
                            maxWidthPx = contentWidthPx,
                            maxLinesPerPage = maxLines
                        ) { pageText ->
                            // 每算完一页，切回主线程更新 UI
                            withContext(Dispatchers.Main.immediate) {
                                pages.add(pageText)
                            }
                        }
                    }
                }.onFailure { throwable ->
                    errorMessage = throwable.message ?: "文本读取失败"
                }

                // 兜底：文件为空时至少显示一页
                if (pages.isEmpty() && errorMessage == null) {
                    pages.add(" ")
                }
                isPaginating = false
                // 防止当前页码越界
                if (pagerState.currentPage >= pages.size) {
                    pagerState.scrollToPage((pages.lastIndex).coerceAtLeast(0))
                }
            }

            // 三态渲染：有页 → 渲染翻页；有错误 → 错误页；否则 → loading
            when {
                pages.isNotEmpty() -> {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = pages.size > 1,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        Text(
                            text = pages.getOrElse(page) { "" },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                            style = bodyStyle,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
                errorMessage != null -> ErrorContent(errorMessage.orEmpty())
                else -> LoadingContent()
            }
        }

        TxtReaderFooter(
            currentPage = pagerState.currentPage,
            pageCount = pages.size.coerceAtLeast(1),
            isPaginating = isPaginating
        )
    }
}

// ── 底部页码指示器 ──
// 分页未完成时显示 "N/页数计算中"，完成后显示 "N/总数"

@Composable
private fun TxtReaderFooter(
    currentPage: Int,
    pageCount: Int,
    isPaginating: Boolean,
) {
    val lastPageIndex = if (isPaginating) -1 else pageCount - 1
    val pageIndicator = if (isPaginating) {
        "${currentPage + 1}/页数计算中"
    } else {
        "${currentPage + 1}/$pageCount"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReaderSurface)
            .navigationBarsPadding()
            .padding(start = 24.w.dp, end = 24.w.dp, top = 12.w.dp, bottom = 14.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (currentPage == lastPageIndex) "本文阅读完毕" else "",
            modifier = Modifier.weight(1f),
            fontSize = 14.ws.sp,
            color = ReaderSubText,
            maxLines = 1
        )
        Text(
            text = pageIndicator,
            fontSize = 14.ws.sp,
            fontWeight = FontWeight.Medium,
            color = ReaderSubText,
            maxLines = 1
        )
    }
}

// ── 流式分页主入口 ──
// 打开文件 → 按段读取并 normalize → 每段用 TextMeasurer 测量行数 → 累积满一页就 emit
// 全程只在内存中持有「当前段」和「当前页」，不持有全文

private suspend fun paginateTxtStream(
    context: Context,
    fileName: String,
    fileUri: String,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    maxWidthPx: Int,
    maxLinesPerPage: Int,
    onPage: suspend (String) -> Unit,
) {
    openTxtReader(context, fileName, fileUri).use { reader ->
        val pageAccumulator = PageAccumulator(
            maxLinesPerPage = maxLinesPerPage,
            onPage = onPage
        )

        // 流式读取 + normalize 换行符 + 切分为不超过 MAX_SEGMENT_CHARS 的段
        streamNormalizedSegments(reader) { segment ->
            // 对每段文本做排版测量，按视觉行数拆分到页
            paginateSegment(
                segment = segment,
                textMeasurer = textMeasurer,
                style = style,
                maxWidthPx = maxWidthPx,
                pageAccumulator = pageAccumulator
            )
        }
        // 把最后不满一页的剩余文本也 emit 出去
        pageAccumulator.flush()
    }
}

// ── 页累积器 ──
// 负责把「行」攒成「页」。每攒够 maxLinesPerPage 行就 emit 一页。

private class PageAccumulator(
    private val maxLinesPerPage: Int,
    private val onPage: suspend (String) -> Unit,
) {
    private val pageBuilder = StringBuilder()
    private var lineCount = 0
    private var emittedAnyPage = false

    // 当前页还能再塞几行
    fun remainingLines(): Int = (maxLinesPerPage - lineCount).coerceAtLeast(1)

    // 追加一段文本到当前页，并记录新增的行数
    suspend fun append(
        segment: String,
        start: Int,
        end: Int,
        lines: Int,
    ) {
        pageBuilder.append(segment, start, end)
        lineCount += lines

        // 行数满了，立即 emit 当前页，下一行从新页开始
        if (lineCount >= maxLinesPerPage) {
            emit()
        }
    }

    // 分页结束时，把最后不满一页的内容也发出去
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

private suspend fun paginateSegment(
    segment: String,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    maxWidthPx: Int,
    pageAccumulator: PageAccumulator,
) {
    if (segment.isEmpty()) return

    // 测量整段文本的排版结果（行数、每行起止位置等）
    val layout = textMeasurer.measure(
        text = AnnotatedString(segment),
        style = style,
        overflow = TextOverflow.Clip,
        constraints = Constraints(maxWidth = maxWidthPx)
    )
    val lineCount = layout.lineCount.coerceAtLeast(1)
    var lineIndex = 0
    var segmentStart = 0

    // 逐行把文本分配到当前页
    while (lineIndex < lineCount) {
        // 检查协程是否被取消（屏幕旋转等场景），避免白算
        currentCoroutineContext().ensureActive()

        // 当前页还能放几行，就取几行
        val remainingLinesOnPage = pageAccumulator.remainingLines()
        val linesToTake = min(remainingLinesOnPage, lineCount - lineIndex)
        val lastLineIndex = lineIndex + linesToTake - 1

        // 取这一批行在 segment 中的字符范围
        val segmentEnd = layout
            .getLineEnd(lastLineIndex, visibleEnd = false)
            .coerceIn(segmentStart + 1, segment.length)

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

private suspend fun streamNormalizedSegments(
    reader: BufferedReader,
    onSegment: suspend (String) -> Unit,
) {
    val readBuffer = CharArray(READ_BUFFER_SIZE)
    val segmentBuilder = StringBuilder(MAX_SEGMENT_CHARS)
    var skipNextLf = false  // 处理 \r\n：遇到 \r 后标记，下一个 \n 跳过

    suspend fun appendChar(char: Char) {
        segmentBuilder.append(char)
        // 遇到换行或达到分段上限时，产出当前段
        if (char == '\n' || segmentBuilder.length >= MAX_SEGMENT_CHARS) {
            onSegment(segmentBuilder.toString())
            segmentBuilder.clear()
        }
    }

    while (true) {
        currentCoroutineContext().ensureActive()

        val count = reader.read(readBuffer)
        if (count == -1) break  // 文件读完了

        for (index in 0 until count) {
            val char = readBuffer[index]
            if (skipNextLf) {
                skipNextLf = false
                if (char == '\n') continue  // \r\n 中的 \n 已被 \r 处理，跳过
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

    // 最后一段不满 MAX_SEGMENT_CHARS 的也要产出
    if (segmentBuilder.isNotEmpty()) {
        onSegment(segmentBuilder.toString())
    }
}

// ── 打开文本文件的 BufferedReader ──
// fileUri 为空时返回模拟数据的 reader

private fun openTxtReader(
    context: Context,
    fileName: String,
    fileUri: String,
): BufferedReader {
    if (fileUri.isBlank()) {
        return BufferedReader(StringReader(buildMockTxtContent(fileName)))
    }

    val uri = Uri.parse(fileUri)
    val input = context.contentResolver.openInputStream(uri)
        ?: error("无法打开文本文件")
    return input.bufferedReader(Charsets.UTF_8)
}

// ── 模拟数据（无真实文件时使用）──

private fun buildMockTxtContent(fileName: String): String {
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
