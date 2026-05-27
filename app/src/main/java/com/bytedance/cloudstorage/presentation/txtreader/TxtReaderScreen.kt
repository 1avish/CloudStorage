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
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.max

private val ReaderBg = Color(0xFFF6F6F6)
private val ReaderSurface = Color(0xFFF6F6F6)
private val ReaderText = Color(0xFF111827)
private val ReaderSubText = Color(0xFF8B919E)
private val ReaderBlue = Color(0xFF3370FF)

// ────────────────────────────────────────────────
// TXT 加载状态封装
// ────────────────────────────────────────────────

private sealed interface TxtLoadState {
    data object Loading : TxtLoadState
    data class Loaded(val text: String) : TxtLoadState
    data class Error(val message: String) : TxtLoadState
}

// ────────────────────────────────────────────────
// TXT 阅读器主页面
// ────────────────────────────────────────────────

@Composable
fun TxtReaderScreen(
    fileId: String,
    fileName: String,
    fileUri: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var loadState by remember(fileId) { mutableStateOf<TxtLoadState>(TxtLoadState.Loading) }

    LaunchedEffect(fileId, fileName, fileUri) {
        loadState = TxtLoadState.Loading
        loadState = runCatching {
            TxtLoadState.Loaded(readTxtContent(context, fileName, fileUri))
        }.getOrElse { throwable ->
            TxtLoadState.Error(throwable.message ?: "文本读取失败")
        }
    }

    when (val state = loadState) {
        is TxtLoadState.Loaded -> TxtPagerContent(
            text = state.text,
            fileName = fileName,
            onBack = onBack
        )
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ReaderBg)
            ) {
                TxtReaderTopBar(
                    fileName = fileName,
                    onBack = onBack
                )

                when (state) {
                    TxtLoadState.Loading -> LoadingContent()
                    is TxtLoadState.Error -> ErrorContent(state.message)
                    is TxtLoadState.Loaded -> Unit
                }
            }
        }
    }
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

// ── 加载中状态 ──
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = ReaderBlue)
    }
}

// ── 错误状态 ──
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

// ── 分页阅读主体：根据屏幕尺寸动态分页，支持左右滑动翻页 ──
@Composable
private fun TxtPagerContent(
    text: String,
    fileName: String,
    onBack: () -> Unit,
) {
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
    var pages by remember(text) { mutableStateOf(listOf(text.ifBlank { " " })) }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReaderSurface)
    ) {
        TxtReaderTopBar(
            fileName = fileName,
            onBack = onBack
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ReaderSurface)
        ) {
            // ── 计算可视区域能容纳的行数 ──
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

            LaunchedEffect(text, contentWidthPx, maxLines, bodyStyle) {
                pages = paginateTxt(
                    text = text,
                    textMeasurer = textMeasurer,
                    style = bodyStyle,
                    maxWidthPx = contentWidthPx,
                    maxLinesPerPage = maxLines
                )
                if (pagerState.currentPage >= pages.size) {
                    pagerState.scrollToPage((pages.lastIndex).coerceAtLeast(0))
                }
            }

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

        TxtReaderFooter(
            currentPage = pagerState.currentPage,
            pageCount = pages.size
        )
    }
}

// ── 底部页码指示器 ──
@Composable
private fun TxtReaderFooter(
    currentPage: Int,
    pageCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReaderSurface)
            .navigationBarsPadding()
            .padding(start = 24.w.dp, end = 24.w.dp, top = 12.w.dp, bottom = 14.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (currentPage == pageCount - 1) "本文阅读完毕" else "",
            modifier = Modifier.weight(1f),
            fontSize = 14.ws.sp,
            color = ReaderSubText,
            maxLines = 1
        )
        Text(
            text = "${currentPage + 1}/$pageCount",
            fontSize = 14.ws.sp,
            fontWeight = FontWeight.Medium,
            color = ReaderSubText,
            maxLines = 1
        )
    }
}

// ── 文本分页算法：按实际排版行数切分，避免固定字符数切分造成页面过满或过空 ──
private fun paginateTxt(
    text: String,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    maxWidthPx: Int,
    maxLinesPerPage: Int,
): List<String> {
    val normalizedText = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .ifBlank { " " }
    val pages = mutableListOf<String>()
    var start = 0

    while (start < normalizedText.length) {
        val layout = textMeasurer.measure(
            text = AnnotatedString(normalizedText.substring(start)),
            style = style,
            overflow = TextOverflow.Clip,
            maxLines = maxLinesPerPage,
            constraints = Constraints(maxWidth = maxWidthPx)
        )
        val visibleLineCount = layout.lineCount.coerceAtLeast(1)
        val endInRemaining = layout
            .getLineEnd(visibleLineCount - 1, visibleEnd = true)
            .coerceAtLeast(1)
        val end = (start + endInRemaining).coerceAtMost(normalizedText.length)
        pages += normalizedText.substring(start, end).trimEnd('\n')
        start = end
    }

    return pages.ifEmpty { listOf(" ") }
}

// ── 读取文本文件：优先从 URI 读取，URI 为空时使用模拟数据 ──
private suspend fun readTxtContent(
    context: Context,
    fileName: String,
    fileUri: String,
): String = withContext(Dispatchers.IO) {
    if (fileUri.isBlank()) {
        return@withContext buildMockTxtContent(fileName)
    }

    // ── 通过 ContentResolver 读取文件内容 ──

    context.contentResolver.openInputStream(Uri.parse(fileUri))?.use { input ->
        input.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readText()
        }
    } ?: error("无法打开文本文件")
}

// ── 构建模拟文本内容（无真实文件时使用）──
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
