package com.bytedance.cloudstorage.presentation.txtreader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Debug
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytedance.cloudstorage.data.local.database.AppDatabase
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.StringReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// ── 主题色 ──
private val ReaderSurface = Color(0xFFF6F6F6)
private val ReaderText = Color(0xFF111827)
private val ReaderSubText = Color(0xFF8B919E)
private val ReaderBlue = Color(0xFFFFE36A)
private val ReaderSkyBlue = Color(0xFFFFF4BA)
private val ReaderPanelSurface = Color.White
private val ReaderControlBg = Color(0xFFF3F5F8)
private val ReaderDivider = Color(0xFFE8ECF2)

// 阅读器底部设置面板的 4 个 Tab 页
private enum class ReaderSettingPanel {
    Brightness,   // 亮度
    Background,   // 背景色
    Font,         // 字号（A-/A+）
    LineSpacing   // 行间距（小/较小/适中/大）
}

// 背景色选项：浅灰（默认）、米黄、浅绿、粉色、纯黑（暗黑模式）
private val ReaderBackgroundOptions = listOf(
    Color(0xFFF6F6F6),
    Color(0xFFEFE0BF),
    Color(0xFFE1ECD6),
    Color(0xFFF3DDDD),
    Color(0xFF000000)
)

// 字号候选值（单位 sp，通过 .ws 缩放），默认 index=2 → 24sp
private val ReaderFontSizes = listOf(20, 22, 24, 26, 28, 30)
// 行高候选值（与字号一一对应，保持约 1.75 行距比），默认 index=2 → 42sp
private val ReaderLineHeights = listOf(34, 38, 42, 48)
// 行间距档位标签，与 ReaderLineHeights 一一对应
private val ReaderLineSpacingLabels = listOf("小", "较小", "适中", "大")

// 每次从文件读取的缓冲区大小（16KB），影响 I/O 次数
// 调试日志开关：设为 true 打开分页性能日志，false 关闭
private const val READER_SETTING_ROW_HEIGHT = 56
private const val READER_BOTTOM_BAR_ITEM_HEIGHT = 44
private const val DEBUG_READER_LOG = false

private const val READ_BUFFER_SIZE = 16 * 1024

// 每个文本分段的最大字符数（8KB），分段送给 TextMeasurer 测量
// 越大测量越准（减少跨段边界误差），但单次测量越慢
private const val MAX_SEGMENT_CHARS = 8 * 1024
private const val READER_METRICS_TAG = "TxtReaderMetrics"
private const val LOG_PROGRESS_PAGE_INTERVAL = 50

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
    // fileKey = fileId + fileUri，用于在文件切换时重置所有状态
    TxtPagerContent(
        fileKey = "$fileId-$fileUri",
        fileId = fileId,    // ← 新增
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
// 整个文件永远不会一次性读入内存。
// 流程：文件 → BufferedReader → 按段读取 → TextMeasurer 测量 → 逐页 emit → UI 渐进渲染

/**
 * 分页阅读主体组件，实现流式分页和翻页渲染。
 *
 * 整个文件不会一次性读入内存，而是通过 BufferedReader 按段读取，
 * TextMeasurer 逐段测量排版行数，每攒满一页立即 emit 给 UI 渲染，
 * 实现低内存占用的渐进式阅读体验。
 *
 * @param fileKey  文件唯一标识（fileId + fileUri），用于切换文件时重置所有状态
 * @param fileId   文件 ID
 * @param fileName 文件显示名称
 * @param fileUri  文件 content:// URI
 * @param onBack   返回上一页回调
 */
@Composable
private fun TxtPagerContent(
    fileKey: String,
    fileName: String,
    fileUri: String,
    onBack: () -> Unit,
    fileId: String,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    // ── 阅读器设置面板状态（切换文件时重置）──
    var isReaderMenuVisible by remember(fileKey) { mutableStateOf(false) }
    var activeSettingPanel by remember(fileKey) { mutableStateOf<ReaderSettingPanel?>(null) }
    var brightness by remember(fileKey) { mutableFloatStateOf(0.65f) }        // 手动亮度 0.05~1.0
    var useSystemBrightness by remember(fileKey) { mutableStateOf(true) }     // 是否跟随系统亮度
    var backgroundIndex by remember(fileKey) { mutableIntStateOf(0) }         // ReaderBackgroundOptions 下标
    var fontSizeIndex by remember(fileKey) { mutableIntStateOf(2) }           // ReaderFontSizes 下标，默认 24sp
    var lineSpacingIndex by remember(fileKey) { mutableIntStateOf(2) }        // ReaderLineHeights 下标，默认 42sp
    val readerSurface = ReaderBackgroundOptions[backgroundIndex]
    val isDarkReader = readerSurface == Color.Black  // 最后一个选项为纯黑 → 暗黑模式
    // 暗黑模式下切换文字颜色，保证对比度
    val readerText = if (isDarkReader) Color(0xFFEDEFF3) else ReaderText
    val readerSubText = if (isDarkReader) Color(0xFF9CA3AF) else ReaderSubText
    val bodyStyle = TextStyle(
        color = readerText,
        fontSize = ReaderFontSizes[fontSizeIndex].ws.sp,
        lineHeight = ReaderLineHeights[lineSpacingIndex].ws.sp,
        fontFamily = FontFamily.SansSerif
    )
    val horizontalPadding = 24.w.dp
    val verticalPadding = 36.w.dp
    // ── 亮度管理：通过 Window.screenBrightness 控制屏幕亮度 ──
    // 保存用户进入阅读器前的原始亮度，退出时恢复
    val activity = remember(context) { context.findActivity() }
    val originalBrightness = remember(activity) {
        activity?.window?.attributes?.screenBrightness
            ?: android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }

    // 跟随系统 / 手动调节实时生效
    LaunchedEffect(activity, brightness, useSystemBrightness) {
        activity?.window?.let { window ->
            val attributes = window.attributes
            attributes.screenBrightness = if (useSystemBrightness) {
                android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            } else {
                brightness.coerceIn(0.05f, 1f)
            }
            window.attributes = attributes
        }
    }

    // 退出阅读器时，将亮度恢复为用户进入前的原始值
    DisposableEffect(activity) {
        onDispose {
            activity?.window?.let { window ->
                val attributes = window.attributes
                attributes.screenBrightness = originalBrightness
                window.attributes = attributes
            }
        }
    }

    // pages 是响应式列表：每算出一页就 add 一次，Compose 自动重组 UI
    val pages = remember(fileKey) { mutableStateListOf<String>() }
    var isPaginating by remember(fileKey) { mutableStateOf(true) }
    var errorMessage by remember(fileKey) { mutableStateOf<String?>(null) }
    // pageCount 用 lambda 传入，pages.size 变化时 HorizontalPager 自动扩展
    val pagerState = rememberPagerState(pageCount = { pages.size })

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
                contentColor = readerSubText
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(readerSurface)
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
                LaunchedEffect(fileKey, fileName, fileUri, contentWidthPx, maxLines, fontSizeIndex, lineSpacingIndex) {
                    val db = AppDatabase.getInstance(context)
                    withContext(Dispatchers.IO) {
                        db.fileDao().updateLastOpenedAt(fileId, System.currentTimeMillis())
                    }
                    pages.clear()
                    isPaginating = true
                    errorMessage = null
                    if (pagerState.currentPage != 0) {
                        pagerState.scrollToPage(0)
                    }

                    val paginationStartMs: Long
                    var firstPageLogged = false
                    if (DEBUG_READER_LOG) {
                        paginationStartMs = SystemClock.elapsedRealtime()
                        logReaderMetric(
                            event = "start",
                            fileName = fileName,
                            pages = 0,
                            startMs = paginationStartMs,
                            extra = "width_px=$contentWidthPx max_lines_per_page=$maxLines"
                        )
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
                                // 每算完一页，切回主线程更新 UI
                                withContext(Dispatchers.Main.immediate) {
                                    pages.add(pageText)
                                    if (DEBUG_READER_LOG && !firstPageLogged) {
                                        firstPageLogged = true
                                        logReaderMetric(
                                            event = "first_page",
                                            fileName = fileName,
                                            pages = pages.size,
                                            startMs = paginationStartMs
                                        )
                                    } else if (DEBUG_READER_LOG && pages.size % LOG_PROGRESS_PAGE_INTERVAL == 0) {
                                        logReaderMetric(
                                            event = "progress",
                                            fileName = fileName,
                                            pages = pages.size,
                                            startMs = paginationStartMs
                                        )
                                    }
                                }
                            }
                        }
                    }.onFailure { throwable ->
                        errorMessage = throwable.message ?: "文本读取失败"
                        if (DEBUG_READER_LOG) {
                            logReaderMetric(
                                event = "error",
                                fileName = fileName,
                                pages = pages.size,
                                startMs = paginationStartMs,
                                extra = "message=${throwable.message.orEmpty()}"
                            )
                        }
                    }

                    // 兜底：文件为空时至少显示一页
                    if (pages.isEmpty() && errorMessage == null) {
                        pages.add(" ")
                    }
                    isPaginating = false
                    if (errorMessage == null && DEBUG_READER_LOG) {
                        logReaderMetric(
                            event = "complete",
                            fileName = fileName,
                            pages = pages.size,
                            startMs = paginationStartMs
                        )
                    }
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
                            modifier = Modifier
                                .fillMaxSize()
                                // 点击正文区域：菜单可见时收起，不可见时呼出
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        if (isReaderMenuVisible) {
                                            isReaderMenuVisible = false
                                            activeSettingPanel = null
                                        } else {
                                            isReaderMenuVisible = true
                                        }
                                    }
                                }
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
                isPaginating = isPaginating,
                surfaceColor = readerSurface,
                contentColor = readerSubText
            )
        }

        // 菜单可见时，在 Box 底部叠加渲染设置面板（浮于正文之上）
        if (isReaderMenuVisible) {
            ReaderSettingsOverlay(
                activePanel = activeSettingPanel,
                onActivePanelChange = { activeSettingPanel = it },
                brightness = brightness,
                onBrightnessChange = {
                    brightness = it
                    useSystemBrightness = false
                },
                useSystemBrightness = useSystemBrightness,
                onUseSystemBrightnessChange = { useSystemBrightness = it },
                backgroundIndex = backgroundIndex,
                onBackgroundIndexChange = { backgroundIndex = it },
                fontSizeIndex = fontSizeIndex,
                onDecreaseFontSize = { fontSizeIndex = (fontSizeIndex - 1).coerceAtLeast(0) },
                onIncreaseFontSize = { fontSizeIndex = (fontSizeIndex + 1).coerceAtMost(ReaderFontSizes.lastIndex) },
                lineSpacingIndex = lineSpacingIndex,
                onLineSpacingIndexChange = { lineSpacingIndex = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ── 底部页码指示器 ──
// 分页未完成时显示 "N/页数计算中"，完成后显示 "N/总数"

// ── 阅读器设置面板：底部 Tab 栏 + 可展开的设置内容 ──
// 结构：[设置内容行]（可选，点击 Tab 时展开/收起）
//       [底部 Tab 栏]  亮度 | 背景 | 字体 | 行间距

/**
 * 阅读器设置叠加层，包含底部 Tab 栏和可展开的设置面板内容。
 *
 * @param activePanel            当前展开的设置面板，null 表示全部收起
 * @param onActivePanelChange    切换展开面板回调
 * @param brightness             当前亮度值（0.05–1.0）
 * @param onBrightnessChange     亮度调节回调
 * @param useSystemBrightness    是否跟随系统亮度
 * @param onUseSystemBrightnessChange 切换系统亮度回调
 * @param backgroundIndex        当前背景色下标
 * @param onBackgroundIndexChange 切换背景色回调
 * @param fontSizeIndex          当前字号下标
 * @param onDecreaseFontSize     缩小字号回调
 * @param onIncreaseFontSize     放大字号回调
 * @param lineSpacingIndex       当前行间距下标
 * @param onLineSpacingIndexChange 切换行间距回调
 */
@Composable
private fun ReaderSettingsOverlay(
    activePanel: ReaderSettingPanel?,
    onActivePanelChange: (ReaderSettingPanel) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    useSystemBrightness: Boolean,
    onUseSystemBrightnessChange: (Boolean) -> Unit,
    backgroundIndex: Int,
    onBackgroundIndexChange: (Int) -> Unit,
    fontSizeIndex: Int,
    onDecreaseFontSize: () -> Unit,
    onIncreaseFontSize: () -> Unit,
    lineSpacingIndex: Int,
    onLineSpacingIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        activePanel?.let { panel ->
            ReaderSettingPanelContent(
                panel = panel,
                brightness = brightness,
                onBrightnessChange = onBrightnessChange,
                useSystemBrightness = useSystemBrightness,
                onUseSystemBrightnessChange = onUseSystemBrightnessChange,
                backgroundIndex = backgroundIndex,
                onBackgroundIndexChange = onBackgroundIndexChange,
                fontSizeIndex = fontSizeIndex,
                onDecreaseFontSize = onDecreaseFontSize,
                onIncreaseFontSize = onIncreaseFontSize,
                lineSpacingIndex = lineSpacingIndex,
                onLineSpacingIndexChange = onLineSpacingIndexChange
            )
        }
        ReaderSettingsBottomBar(
            activePanel = activePanel,
            onActivePanelChange = onActivePanelChange
        )
    }
}

// 根据当前选中的 Tab 页，渲染对应的设置内容行
@Composable
private fun ReaderSettingPanelContent(
    panel: ReaderSettingPanel,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    useSystemBrightness: Boolean,
    onUseSystemBrightnessChange: (Boolean) -> Unit,
    backgroundIndex: Int,
    onBackgroundIndexChange: (Int) -> Unit,
    fontSizeIndex: Int,
    onDecreaseFontSize: () -> Unit,
    onIncreaseFontSize: () -> Unit,
    lineSpacingIndex: Int,
    onLineSpacingIndexChange: (Int) -> Unit,
) {
    when (panel) {
        ReaderSettingPanel.Brightness -> BrightnessSettingRow(
            brightness = brightness,
            onBrightnessChange = onBrightnessChange,
            useSystemBrightness = useSystemBrightness,
            onUseSystemBrightnessChange = onUseSystemBrightnessChange
        )
        ReaderSettingPanel.Background -> BackgroundSettingRow(
            selectedIndex = backgroundIndex,
            onSelected = onBackgroundIndexChange
        )
        ReaderSettingPanel.Font -> FontSettingRow(
            fontSizeIndex = fontSizeIndex,
            onDecrease = onDecreaseFontSize,
            onIncrease = onIncreaseFontSize
        )
        ReaderSettingPanel.LineSpacing -> LineSpacingSettingRow(
            selectedIndex = lineSpacingIndex,
            onSelected = onLineSpacingIndexChange
        )
    }
}

// 亮度面板：[小太阳] ──[Slider]── [大太阳]  [○ 跟随系统]
@Composable
private fun BrightnessSettingRow(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    useSystemBrightness: Boolean,
    onUseSystemBrightnessChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(READER_SETTING_ROW_HEIGHT.w.dp)
            .background(ReaderPanelSurface)
            .padding(horizontal = 18.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Brightness6,
            contentDescription = null,
            tint = ReaderSkyBlue,
            modifier = Modifier.size(22.w.dp)
        )
        Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            valueRange = 0.05f..1f,
            colors = SliderDefaults.colors(
                thumbColor = ReaderSkyBlue,
                activeTrackColor = ReaderSkyBlue,
                inactiveTrackColor = ReaderSkyBlue.copy(alpha = 0.18f)
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.w.dp)
        )
        Icon(
            imageVector = Icons.Default.Brightness6,
            contentDescription = null,
            tint = ReaderSkyBlue,
            modifier = Modifier.size(28.w.dp)
        )
        Spacer(modifier = Modifier.width(8.w.dp))
        Row(
            modifier = Modifier.clickable { onUseSystemBrightnessChange(!useSystemBrightness) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = useSystemBrightness,
                onClick = { onUseSystemBrightnessChange(!useSystemBrightness) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = ReaderSkyBlue,
                    unselectedColor = ReaderSkyBlue
                )
            )
            Text(
                text = "系统亮度",
                fontSize = 15.ws.sp,
                color = ReaderSkyBlue,
                maxLines = 1
            )
        }
    }
}

// 背景色面板：5 个圆形色块，选中项显示蓝色描边
@Composable
private fun BackgroundSettingRow(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(READER_SETTING_ROW_HEIGHT.w.dp)
            .background(ReaderPanelSurface)
            .padding(horizontal = 18.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderBackgroundOptions.forEachIndexed { index, color ->
                val selected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .size(34.w.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selected) 2.w.dp else 1.w.dp,
                            color = if (selected) ReaderBlue else ReaderDivider,
                            shape = CircleShape
                        )
                        .clickable { onSelected(index) }
                )
            }
        }
    }
}

// 字号面板：[A-]  3/6  [A+]，数字表示当前档位（1~6）
@Composable
private fun FontSettingRow(
    fontSizeIndex: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(READER_SETTING_ROW_HEIGHT.w.dp)
            .background(ReaderPanelSurface)
            .padding(horizontal = 36.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReaderPillButton(
            text = "A-",
            enabled = fontSizeIndex > 0,
            onClick = onDecrease,
            modifier = Modifier.width(88.w.dp)
        )
        Text(
            text = "${fontSizeIndex + 1}",
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.w.dp),
            fontSize = 18.ws.sp,
            fontWeight = FontWeight.Medium,
            color = ReaderText,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        ReaderPillButton(
            text = "A+",
            enabled = fontSizeIndex < ReaderFontSizes.lastIndex,
            onClick = onIncrease,
            modifier = Modifier.width(88.w.dp)
        )
    }
}

// 行间距面板：胶囊 segmented control，4 档可选
@Composable
private fun LineSpacingSettingRow(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(READER_SETTING_ROW_HEIGHT.w.dp)
            .background(ReaderPanelSurface)
            .padding(horizontal = 18.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(40.w.dp)
                .clip(RoundedCornerShape(22.w.dp))
                .background(ReaderControlBg)
                .padding(3.w.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderLineSpacingLabels.forEachIndexed { index, label ->
                val selected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.w.dp))
                        .background(if (selected) Color.White else Color.Transparent)
                        .border(
                            width = if (selected) 1.w.dp else 0.w.dp,
                            color = if (selected) ReaderDivider else Color.Transparent,
                            shape = RoundedCornerShape(20.w.dp)
                        )
                        .clickable { onSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 15.ws.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        color = if (selected) ReaderText else ReaderSubText,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// 胶囊按钮（A-/A+），到达边界时半透明 + 禁用点击
@Composable
private fun ReaderPillButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(40.w.dp)
            .clip(RoundedCornerShape(22.w.dp))
            .background(ReaderControlBg)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 17.ws.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) ReaderSubText else ReaderSubText.copy(alpha = 0.4f),
            maxLines = 1
        )
    }
}

// 底部 Tab 栏：固定在页面最下方，支持导航栏安全区
@Composable
private fun ReaderSettingsBottomBar(
    activePanel: ReaderSettingPanel?,
    onActivePanelChange: (ReaderSettingPanel) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReaderPanelSurface)
            .border(width = 1.w.dp, color = ReaderDivider)
            .navigationBarsPadding()
            .padding(horizontal = 14.w.dp, vertical = 4.w.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderBottomBarItem(
                label = "亮度",
                icon = Icons.Default.Brightness6,
                selected = activePanel == ReaderSettingPanel.Brightness,
                accentColor = ReaderSkyBlue,
                onClick = { onActivePanelChange(ReaderSettingPanel.Brightness) },
                modifier = Modifier.weight(1f)
            )
            ReaderBottomBarItem(
                label = "背景",
                icon = Icons.Default.Palette,
                selected = activePanel == ReaderSettingPanel.Background,
                accentColor = ReaderBlue,
                onClick = { onActivePanelChange(ReaderSettingPanel.Background) },
                modifier = Modifier.weight(1f)
            )
            ReaderBottomBarItem(
                label = "字体",
                icon = Icons.Default.FormatSize,
                selected = activePanel == ReaderSettingPanel.Font,
                accentColor = ReaderBlue,
                onClick = { onActivePanelChange(ReaderSettingPanel.Font) },
                modifier = Modifier.weight(1f)
            )
            ReaderBottomBarItem(
                label = "行间距",
                icon = Icons.Default.FormatLineSpacing,
                selected = activePanel == ReaderSettingPanel.LineSpacing,
                accentColor = ReaderBlue,
                onClick = { onActivePanelChange(ReaderSettingPanel.LineSpacing) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// 单个 Tab 项：图标 + 标签，selectedColor 由调用方指定（亮度用 SkyBlue，其他用 Blue）
@Composable
private fun ReaderBottomBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,  // 选中态高亮色，不再用字符串判断
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(READER_BOTTOM_BAR_ITEM_HEIGHT.w.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) accentColor else ReaderText,
            modifier = Modifier.size(22.w.dp)
        )
        Text(
            text = label,
            fontSize = 12.ws.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) accentColor else ReaderText,
            maxLines = 1
        )
    }
}

// 从 Compose 的 Context 中递归解开 ContextWrapper 链，取出宿主 Activity
// 用于访问 window.attributes.screenBrightness 控制屏幕亮度
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

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

// ── 流式分页主入口 ──
// 打开文件 → 按段读取并 normalize → 每段用 TextMeasurer 测量行数 → 累积满一页就 emit
// 全程只在内存中持有「当前段」和「当前页」，不持有全文

private suspend fun paginateTxtStream(
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

// ── 编码检测 ──
// 读取文件前 8KB，按 BOM 标记 → UTF-8 合法性检查 → 回退 GB18030 的顺序判断

private const val CHARSET_SNIFF_SIZE = 8 * 1024

/**
 * 嗅探文本文件编码。
 *
 * 优先级：
 * 1. BOM 标记（UTF-8 / UTF-16LE / UTF-16BE）
 * 2. 样本是否为合法 UTF-8 多字节序列
 * 3. 回退 GB18030（兼容 GBK、GB2312）
 */
private fun detectCharset(context: Context, fileUri: String): Charset {
    if (fileUri.isBlank()) return StandardCharsets.UTF_8

    val uri = Uri.parse(fileUri)
    val sample = context.contentResolver.openInputStream(uri)?.use { stream ->
        val buf = ByteArray(CHARSET_SNIFF_SIZE)
        val read = stream.read(buf)
        if (read <= 0) return StandardCharsets.UTF_8
        buf.copyOf(read)
    } ?: return StandardCharsets.UTF_8

    // BOM 检测
    if (sample.size >= 3 &&
        sample[0] == 0xEF.toByte() &&
        sample[1] == 0xBB.toByte() &&
        sample[2] == 0xBF.toByte()
    ) return StandardCharsets.UTF_8

    if (sample.size >= 2) {
        if (sample[0] == 0xFF.toByte() && sample[1] == 0xFE.toByte())
            return StandardCharsets.UTF_16LE
        if (sample[0] == 0xFE.toByte() && sample[1] == 0xFF.toByte())
            return StandardCharsets.UTF_16BE
    }

    // 启发式：样本中是否存在合法的 UTF-8 多字节序列
    if (isValidUtf8Sample(sample)) return StandardCharsets.UTF_8

    // 回退：GB18030 完整覆盖 GBK / GB2312
    return Charset.forName("GB18030")
}

/**
 * 简易 UTF-8 合法性检查。
 *
 * 遍历样本字节，统计合法的多字节序列数量。
 * 若存在至少一个合法的 2/3/4 字节序列，且无非法起始字节，判定为 UTF-8。
 */
private fun isValidUtf8Sample(sample: ByteArray): Boolean {
    var multiByteSequences = 0
    var i = 0
    while (i < sample.size) {
        val b = sample[i].toInt() and 0xFF
        when {
            b <= 0x7F -> i++ // ASCII
            b in 0xC2..0xDF && i + 1 < sample.size &&
                (sample[i + 1].toInt() and 0xC0) == 0x80 -> {
                multiByteSequences++; i += 2
            }
            b in 0xE0..0xEF && i + 2 < sample.size &&
                (sample[i + 1].toInt() and 0xC0) == 0x80 &&
                (sample[i + 2].toInt() and 0xC0) == 0x80 -> {
                multiByteSequences++; i += 3
            }
            b in 0xF0..0xF4 && i + 3 < sample.size &&
                (sample[i + 1].toInt() and 0xC0) == 0x80 &&
                (sample[i + 2].toInt() and 0xC0) == 0x80 &&
                (sample[i + 3].toInt() and 0xC0) == 0x80 -> {
                multiByteSequences++; i += 4
            }
            else -> return false // 非法 UTF-8 字节
        }
    }
    return multiByteSequences > 0 || sample.all { (it.toInt() and 0xFF) <= 0x7F }
}

// ── 打开文本文件的 BufferedReader ──
// fileUri 为空时返回模拟数据的 reader

private fun openTxtReader(
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
