package com.bytedance.cloudstorage.presentation.txtreader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Debug
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max

// ── TXT 阅读器入口 ──

/**
 * TXT 文本阅读器主页面。
 *
 * 打开后自动检测文件编码，流式分页渲染文本内容，
 * 支持翻页阅读、亮度调节、背景切换、字号/行间距设置。
 *
 * @param fileId   文件 ID（用于标记已打开）
 * @param fileName 文件显示名称
 * @param fileUri  文件 content:// URI
 * @param onBack   返回上一页回调
 */
@Composable
fun TxtReaderScreen(
    fileId: String,
    fileName: String,
    fileUri: String,
    onBack: () -> Unit,
) {
    TxtPagerContent(
        fileKey = "$fileId-$fileUri",
        fileId = fileId,
        fileName = fileName,
        fileUri = fileUri,
        onBack = onBack
    )
}

// ── 顶部栏：返回按钮 + 文件名 ──

/**
 * 阅读器顶部导航栏，显示返回按钮和当前文件名。
 *
 * @param fileName     文件显示名称
 * @param onBack       返回回调
 * @param surfaceColor 顶栏背景色（跟随阅读器主题）
 * @param contentColor 顶栏文字/图标色
 */
@Composable
private fun TxtReaderTopBar(
    fileName: String,
    onBack: () -> Unit,
    surfaceColor: Color,
    contentColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .statusBarsPadding()
            .height(56.w.dp)
            .padding(horizontal = 8.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = contentColor
            )
        }
        Text(
            text = fileName,
            modifier = Modifier.weight(1f),
            fontSize = 16.ws.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor,
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

@Composable
private fun TxtPagerContent(
    fileKey: String,
    fileName: String,
    fileUri: String,
    onBack: () -> Unit,
    fileId: String,
    viewModel: TxtReaderViewModel = viewModel(),
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val uiState by viewModel.uiState.collectAsState()

    val readerSurface = uiState.readerSurface
    val bodyStyle = TextStyle(
        color = uiState.readerText,
        fontSize = uiState.currentFontSize.ws.sp,
        lineHeight = uiState.currentLineHeight.ws.sp,
        fontFamily = FontFamily.SansSerif,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        )
    )
    val horizontalPadding = 24.w.dp
    val verticalPadding = 36.w.dp

    // ── 亮度管理：通过 Window.screenBrightness 控制屏幕亮度 ──
    val activity = remember(context) { context.findActivity() }
    val originalBrightness = remember(activity) {
        activity?.window?.attributes?.screenBrightness
            ?: android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }

    LaunchedEffect(activity, uiState.brightness, uiState.useSystemBrightness) {
        activity?.window?.let { window ->
            val attributes = window.attributes
            attributes.screenBrightness = if (uiState.useSystemBrightness) {
                android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            } else {
                uiState.brightness.coerceIn(0.05f, 1f)
            }
            window.attributes = attributes
        }
    }

    DisposableEffect(activity) {
        onDispose {
            activity?.window?.let { window ->
                val attributes = window.attributes
                attributes.screenBrightness = originalBrightness
                window.attributes = attributes
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { uiState.pages.size })

    // 分页完成时防止页码越界
    LaunchedEffect(uiState.pages.size, uiState.isPaginating) {
        val pageCount = uiState.pages.size
        if (pageCount == 0) return@LaunchedEffect

        if (!uiState.isPaginating && pagerState.currentPage >= pageCount) {
            pagerState.scrollToPage(uiState.pages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(readerSurface)
        ) {
            TxtReaderTopBar(
                fileName = fileName,
                onBack = onBack,
                surfaceColor = readerSurface,
                contentColor = uiState.readerSubText
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(readerSurface)
            ) {
                val availableWidth = this.maxWidth
                val availableHeight = this.maxHeight
                val contentWidthPx = with(density) {
                    (availableWidth - horizontalPadding * 2).roundToPx()
                }.coerceAtLeast(1)
                val contentHeightPx = with(density) {
                    (availableHeight - verticalPadding * 2).roundToPx()
                }.coerceAtLeast(1)
                val measuredLineHeightPx = remember(
                    textMeasurer,
                    bodyStyle,
                    contentWidthPx
                ) {
                    textMeasurer.measure(
                        text = AnnotatedString("国\n国"),
                        style = bodyStyle,
                        constraints = Constraints(maxWidth = contentWidthPx)
                    ).size.height / 2f
                }.coerceAtLeast(with(density) { bodyStyle.lineHeight.toPx() }.coerceAtLeast(1f))
                val maxLines = max(1, floor(contentHeightPx / measuredLineHeightPx).toInt())

                // 触发分页
                LaunchedEffect(fileKey, fileName, fileUri, contentWidthPx, maxLines, uiState.fontSizeIndex, uiState.lineSpacingIndex) {
                    viewModel.markFileOpened(fileId)
                    viewModel.startPagination()
                    if (pagerState.currentPage != 0) {
                        pagerState.scrollToPage(0)
                    }

                    val paginationStartMs: Long
                    var firstPageLogged = false
                    if (DEBUG_READER_LOG) {
                        paginationStartMs = SystemClock.elapsedRealtime()
                        logReaderMetric("start", fileName, 0, paginationStartMs, "width_px=$contentWidthPx max_lines_per_page=$maxLines")
                    } else {
                        paginationStartMs = 0L
                    }

                    runCatching {
                        val charset = withContext(Dispatchers.IO) {
                            detectCharset(context.applicationContext, fileUri)
                        }
                        withContext(Dispatchers.Default) {
                            paginateTxtStream(
                                context = context.applicationContext,
                                fileName = fileName,
                                fileUri = fileUri,
                                charset = charset,
                                textMeasurer = textMeasurer,
                                style = bodyStyle,
                                maxWidthPx = contentWidthPx,
                                maxLinesPerPage = maxLines
                            ) { pageText ->
                                withContext(Dispatchers.Main.immediate) {
                                    viewModel.addPage(pageText)
                                    val pageSize = uiState.pages.size + 1
                                    if (DEBUG_READER_LOG && !firstPageLogged) {
                                        firstPageLogged = true
                                        logReaderMetric("first_page", fileName, pageSize, paginationStartMs)
                                    } else if (DEBUG_READER_LOG && pageSize % LOG_PROGRESS_PAGE_INTERVAL == 0) {
                                        logReaderMetric("progress", fileName, pageSize, paginationStartMs)
                                    }
                                }
                            }
                        }
                    }.onFailure { throwable ->
                        viewModel.setError(throwable.message ?: "文本读取失败")
                        if (DEBUG_READER_LOG) {
                            logReaderMetric("error", fileName, uiState.pages.size, paginationStartMs, "message=${throwable.message.orEmpty()}")
                        }
                    }

                    viewModel.finishPagination()
                    if (uiState.errorMessage == null && DEBUG_READER_LOG) {
                        logReaderMetric("complete", fileName, uiState.pages.size, paginationStartMs)
                    }
                }

                when {
                    uiState.pages.isNotEmpty() -> {
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = uiState.pages.size > 1,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        if (uiState.isReaderMenuVisible) {
                                            viewModel.setReaderMenuVisible(false)
                                            viewModel.setActiveSettingPanel(null)
                                        } else {
                                            viewModel.setReaderMenuVisible(true)
                                        }
                                    }
                                }
                        ) { page ->
                            Text(
                                text = uiState.pages.getOrElse(page) { "" },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                                style = bodyStyle,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                    uiState.errorMessage != null -> ErrorContent(uiState.errorMessage.orEmpty())
                    else -> LoadingContent()
                }
            }

            TxtReaderFooter(
                currentPage = pagerState.currentPage,
                pageCount = uiState.pages.size.coerceAtLeast(1),
                isPaginating = uiState.isPaginating,
                surfaceColor = readerSurface,
                contentColor = uiState.readerSubText
            )
        }

        if (uiState.isReaderMenuVisible) {
            ReaderSettingsOverlay(
                activePanel = uiState.activeSettingPanel,
                onActivePanelChange = { viewModel.setActiveSettingPanel(it) },
                brightness = uiState.brightness,
                onBrightnessChange = { viewModel.setBrightness(it) },
                useSystemBrightness = uiState.useSystemBrightness,
                onUseSystemBrightnessChange = { viewModel.setUseSystemBrightness(it) },
                backgroundIndex = uiState.backgroundIndex,
                onBackgroundIndexChange = { viewModel.setBackgroundIndex(it) },
                fontSizeIndex = uiState.fontSizeIndex,
                onDecreaseFontSize = { viewModel.onFontSizeChanged((uiState.fontSizeIndex - 1).coerceAtLeast(0)) },
                onIncreaseFontSize = { viewModel.onFontSizeChanged((uiState.fontSizeIndex + 1).coerceAtMost(ReaderFontSizes.lastIndex)) },
                lineSpacingIndex = uiState.lineSpacingIndex,
                onLineSpacingIndexChange = { viewModel.onLineSpacingChanged(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ── 底部页码指示器 ──

@Composable
private fun TxtReaderFooter(
    currentPage: Int,
    pageCount: Int,
    isPaginating: Boolean,
    surfaceColor: Color,
    contentColor: Color,
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
            .background(surfaceColor)
            .navigationBarsPadding()
            .padding(start = 24.w.dp, end = 24.w.dp, top = 12.w.dp, bottom = 14.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (currentPage == lastPageIndex) "本文阅读完毕" else "",
            modifier = Modifier.weight(1f),
            fontSize = 14.ws.sp,
            color = contentColor,
            maxLines = 1
        )
        Text(
            text = pageIndicator,
            fontSize = 14.ws.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 1
        )
    }
}

// ── Context → Activity ──

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// ── 日志工具 ──

private fun logReaderMetric(
    event: String,
    fileName: String,
    pages: Int,
    startMs: Long,
    extra: String = "",
) {
    val elapsedMs = (SystemClock.elapsedRealtime() - startMs).coerceAtLeast(0L)
    val elapsedSeconds = elapsedMs / 1000.0
    val pagesPerSecond = if (elapsedSeconds > 0.0) pages / elapsedSeconds else 0.0

    val runtime = Runtime.getRuntime()
    val usedHeapMb = bytesToMb(runtime.totalMemory() - runtime.freeMemory())
    val maxHeapMb = bytesToMb(runtime.maxMemory())
    val memoryInfo = Debug.MemoryInfo()
    Debug.getMemoryInfo(memoryInfo)
    val pssMb = memoryInfo.totalPss / 1024.0

    Log.d(
        READER_METRICS_TAG,
        buildString {
            append("event=").append(event)
            append(" file=\"").append(fileName).append("\"")
            append(" elapsed_ms=").append(elapsedMs)
            append(" pages=").append(pages)
            append(" pages_per_second=").append(formatDouble(pagesPerSecond))
            append(" java_heap_mb=").append(formatDouble(usedHeapMb))
            append(" max_heap_mb=").append(formatDouble(maxHeapMb))
            append(" pss_mb=").append(formatDouble(pssMb))
            if (extra.isNotBlank()) {
                append(' ').append(extra)
            }
        }
    )
}

private fun bytesToMb(bytes: Long): Double = bytes / 1024.0 / 1024.0

private fun formatDouble(value: Double): String = String.format(Locale.US, "%.2f", value)
