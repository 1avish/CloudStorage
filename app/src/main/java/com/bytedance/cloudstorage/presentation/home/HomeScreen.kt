package com.bytedance.cloudstorage.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytedance.cloudstorage.data.repository.RecentFileWithFolderInfo
import com.bytedance.cloudstorage.domain.model.FileType
import com.bytedance.cloudstorage.presentation.common.VideoCoverThumbnail
import com.bytedance.cloudstorage.presentation.filelist.fileStyle
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ────────────────────────────────────────────────
// 颜色常量，与设计稿对齐
// ────────────────────────────────────────────────
private val BgGray         = Color(0xFFF5F6F8)
private val ProgressBlue   = Color(0xFFFFE36A)
private val ProgressYellow = Color(0xFFFAAD14)
private val ProgressRed    = Color(0xFFFF4D4F)
private val TextPrimary    = Color(0xFF1D2129)
private val TextSecondary  = Color(0xFF8C93A4)
private val TextGray       = Color(0xFFB0B4C1)
private val DividerColor   = Color(0xFFF5F6F8)
private val ChevronGray    = Color(0xFFC0C4D0)

// ────────────────────────────────────────────────
// 网盘首页主组件（接入 ViewModel）
// ────────────────────────────────────────────────

/**
 * 网盘 Tab 首页
 *
 * 数据来源：HomeViewModel → Mock JSON → Room 数据库 → Flow → UI
 * 无需外部传参，内部通过 viewModel() 创建并收集状态。
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onOpenVideo: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenTxt: (String, String, String) -> Unit = { _, _, _ -> },
    onShowAllViews: () -> Unit = {},
    onShowAllSaves: () -> Unit = {},
) {
    // 收集 ViewModel 中的 StateFlow，生命周期感知
    val usedStorageG  by viewModel.usedStorageG.collectAsStateWithLifecycle()
    val totalStorageG by viewModel.totalStorageG.collectAsStateWithLifecycle()
    val recentViews   by viewModel.recentViews.collectAsStateWithLifecycle()
    val recentSaves   by viewModel.recentSaves.collectAsStateWithLifecycle()
    val isLoading     by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage  by viewModel.errorMessage.collectAsStateWithLifecycle()

    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgGray),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ProgressBlue)
            }
        }
        errorMessage != null -> {
            ErrorRetryContent(
                message = errorMessage!!,
                onRetry = { viewModel.loadData() }
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgGray)
                    .padding(horizontal = 16.w.dp),
                verticalArrangement = Arrangement.spacedBy(16.w.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 16.w.dp,
                    bottom = 32.w.dp
                )
            ) {
                item {
                    PersonalSpaceCard(
                        usedStorageG = usedStorageG,
                        totalStorageG = totalStorageG,
                    )
                }
                item {
                    RecentFileCard(
                        title = "最近转存",
                        emptyHint = "暂无转存记录",
                        items = recentSaves,
                        onOpenVideo = onOpenVideo,
                        onOpenTxt = onOpenTxt,
                        onShowAll = onShowAllSaves,
                    )
                }
                item {
                    RecentFileCard(
                        title = "最近浏览",
                        emptyHint = "暂无浏览记录",
                        items = recentViews,
                        onOpenVideo = onOpenVideo,
                        onOpenTxt = onOpenTxt,
                        onShowAll = onShowAllViews,
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────
// 加载失败重试组件
// ────────────────────────────────────────────────

/**
 * 加载失败占位组件，展示错误图标、错误信息和重试按钮。
 *
 * @param message 错误提示文字
 * @param onRetry 点击重试回调
 */
@Composable
private fun ErrorRetryContent(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGray),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = TextGray.copy(alpha = 0.6f),
                modifier = Modifier.size(48.w.dp)
            )
            Spacer(modifier = Modifier.height(12.w.dp))
            Text(
                text = message,
                fontSize = 15.ws.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray,
            )
            Spacer(modifier = Modifier.height(16.w.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.w.dp))
                    .background(ProgressBlue)
                    .clickable { onRetry() }
                    .padding(horizontal = 24.w.dp, vertical = 10.w.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "重试",
                    fontSize = 15.ws.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                )
            }
        }
    }
}

/**
 * 个人信息卡片，展示头像、存储使用量和进度条。
 *
 * @param usedStorageG  已用存储（GB）
 * @param totalStorageG 总存储（GB），用于计算进度条百分比
 */
@Composable
private fun PersonalSpaceCard(
    usedStorageG: Float,
    totalStorageG: Float,
) {
    // 进度条百分比，允许超过 100%（溢出显示满条红色）
    val percent = (usedStorageG / totalStorageG).coerceIn(0f, 1f)
    val progressColor = when {
        percent < 0.6f   -> ProgressBlue
        percent <= 0.85f -> ProgressYellow
        else             -> ProgressRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.w.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.w.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 渐变圆形头像
            Box(
                modifier = Modifier
                    .size(46.w.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                                colors = listOf(Color(0xFFFFE36A), Color(0xFFFFB85C))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.w.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.w.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 文字行：我的空间：XG / XG
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "我的空间：",
                        fontSize = 15.ws.sp,
                        color = Color(0xFF4E5969),
                    )
                    Text(
                        text = "${usedStorageG}G",
                        fontSize = 16.ws.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressColor,
                    )
                    Text(
                        text = " / ${totalStorageG}G",
                        fontSize = 14.ws.sp,
                        color = TextGray,
                    )
                }

                Spacer(modifier = Modifier.height(10.w.dp))

                // 进度条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.w.dp)
                        .clip(RoundedCornerShape(3.w.dp))
                        .background(Color(0xFFF5F5F5))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = percent)
                            .height(6.w.dp)
                            .clip(RoundedCornerShape(3.w.dp))
                            .background(progressColor)
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────
// 最近转存 / 最近浏览 通用卡片
// ────────────────────────────────────────────────

/**
 * 通用文件列表卡片，同时用于「最近转存」和「最近浏览」。
 *
 * @param title    卡片标题
 * @param emptyHint 无数据时的提示文字
 * @param items     文件列表（由 DAO 查询限制最多 3 条）
 */
@Composable
private fun RecentFileCard(
    title: String,
    emptyHint: String,
    items: List<RecentFileWithFolderInfo>,
    onOpenVideo: (String, String, String) -> Unit,
    onOpenTxt: (String, String, String) -> Unit,
    onShowAll: () -> Unit,
) {
    // 折叠状态，默认展开
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.w.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.w.dp)) {
            // 标题行：标题 | 折叠按钮(紧邻标题) | spacer | 全部>
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 17.ws.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                // 折叠/展开按钮，放在标题右侧
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(30.w.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (expanded) "折叠" else "展开",
                        tint = Color(0xFF737373),
                        modifier = Modifier.size(18.w.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // 全部 > 按钮
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.w.dp))
                        .testTag(if (title == "最近浏览") "recent_views_all" else "recent_saves_all")
                        .clickable { onShowAll() }
                        .padding(horizontal = 10.w.dp, vertical = 6.w.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "全部",
                        fontSize = 14.ws.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF696969),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "查看全部",
                        tint = Color(0xFF696969),
                        modifier = Modifier.size(16.w.dp)
                    )
                }
            }

            // 列表内容（带动画展开/折叠）
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                if (items.isEmpty()) {
                    EmptyState(hint = emptyHint, cardHeight = 100.w.dp)
                } else {
                    Column {
                        items.forEachIndexed { index, item ->
                            RecentFileItemRow(
                                item = item,
                                onOpenVideo = onOpenVideo,
                                onOpenTxt = onOpenTxt
                            )
                            // 非最后一项显示分割线
                            if (index < items.lastIndex) {
                                Spacer(modifier = Modifier.height(8.w.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.w.dp)
                                        .height(2.dp)
                                        .background(DividerColor)
                                )
                                Spacer(modifier = Modifier.height(8.w.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────
// 全部记录页面（点击「全部>」后展示）
// ────────────────────────────────────────────────

/**
 * 全部最近记录页面。
 *
 * 样式与首页 RecentFileCard 内的记录保持一致：
 * 相同的图标大小、文字规格、分割线样式。
 *
 * @param title    页面标题，如「全部最近浏览」
 * @param items    全部记录列表
 * @param onBack   返回按钮回调
 */
@Composable
internal fun AllRecentRecordsScreen(
    title: String,
    items: List<RecentFileWithFolderInfo>,
    onBack: () -> Unit,
    onOpenVideo: (String, String, String) -> Unit,
    onOpenTxt: (String, String, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 顶部栏：返回箭头 + 居中标题，替换 tab 区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            }
            // 右侧占位，保持标题居中
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(12.w.dp))

        if (items.isEmpty()) {
            EmptyState(hint = "暂无记录", cardHeight = 200.w.dp)
        } else {
            val lastFileId = items.last().file.id
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("recent_record_list")
                    .padding(horizontal = 16.w.dp),
                contentPadding = PaddingValues(
                    top = 6.w.dp,
                    bottom = 24.w.dp
                )
            ) {
                items(
                    items = items,
                    key = { item -> item.file.id },
                    contentType = { item -> item.file.type.name }
                ) { item ->
                    RecentFileItemRow(
                        item = item,
                        onOpenVideo = onOpenVideo,
                        onOpenTxt = onOpenTxt,
                    )
                    if (item.file.id != lastFileId) {
                        Spacer(modifier = Modifier.height(8.w.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.w.dp)
                                .height(2.dp)
                                .background(DividerColor)
                        )
                        Spacer(modifier = Modifier.height(8.w.dp))
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────
// 空状态组件
// ────────────────────────────────────────────────

/**
 * 空状态占位，显示 CloudOff 图标和提示文字。
 *
 * @param hint      提示文字，如「暂无转存记录」
 * @param cardHeight 卡片高度
 */
@Composable
internal fun EmptyState(hint: String, cardHeight: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = TextGray.copy(alpha = 0.6f),
                modifier = Modifier.size(32.w.dp)
            )
            Spacer(modifier = Modifier.height(10.w.dp))
            Text(
                text = hint,
                fontSize = 14.ws.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

// ────────────────────────────────────────────────
// 单条文件记录
// ────────────────────────────────────────────────

/**
 * 单条文件记录行，用于最近浏览和最近转存列表。
 *
 * @param item 带父文件夹信息的文件项
 */
@Composable
internal fun RecentFileItemRow(
    item: RecentFileWithFolderInfo,
    onOpenVideo: (String, String, String) -> Unit,
    onOpenTxt: (String, String, String) -> Unit,
) {
    val (icon, iconBg, iconTint) = remember(item.file.type) { fileStyle(item.file.type) }
    val timestamp = item.file.lastOpenedAt ?: item.file.lastSavedAt ?: item.file.updatedAt
    val formattedTime = remember(timestamp) { formatTimestamp(timestamp) }
    val clickAction = when (item.file.type) {
        FileType.Video -> {
            { onOpenVideo(item.file.id, item.file.name, item.file.uri ?: "") }
        }
        FileType.Txt -> {
            { onOpenTxt(item.file.id, item.file.name, item.file.uri ?: "") }
        }
        else -> null
    }
    val typeLabel = remember(item.file.type) {
        when (item.file.type) {
            FileType.Video -> "视频"
            FileType.Txt -> "文档"
            FileType.Folder -> "文件夹"
            FileType.Other -> "其他"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = clickAction != null) { clickAction?.invoke() }
            .padding(vertical = 2.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标容器
        if (item.file.type == FileType.Video && !item.file.coverUri.isNullOrEmpty()) {
            VideoCoverThumbnail(
                coverUri = item.file.coverUri,
                modifier = Modifier.size(44.w.dp),
                cornerRadiusDp = 12,
                showPlayIcon = false,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.w.dp)
                    .clip(RoundedCornerShape(12.w.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.w.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.w.dp))

        Column(modifier = Modifier.weight(1f)) {

            Spacer(modifier = Modifier.height(6.w.dp))

            // 第二行：文件名
            Text(
                text = item.file.name,
                fontSize = 16.ws.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(6.w.dp))

            // 第三行：位置信息 + 箭头
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedTime,
                    fontSize = 12.ws.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = " · ",
                    fontSize = 12.ws.sp,
                    color = Color(0xFFD1D5DF)
                )
                Text(
                    text = typeLabel,
                    fontSize = 12.ws.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = " | ",
                    fontSize = 12.ws.sp,
                    color = Color(0xFFD1D5DF)
                )
                Text(
                    text = if (item.hasGrandParent) "位置：...${item.parentName}" else "位置：${item.parentName}",
                    fontSize = 12.ws.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ChevronGray,
                    modifier = Modifier.size(14.w.dp)
                )
            }
        }
    }
}

// ────────────────────────────────────────────────
// 时间格式化工具函数
// ────────────────────────────────────────────────

/**
 * 时间戳 → 可读时间字符串
 *
 * - 一小时内：「X分钟前」
 * - 超过一小时且本年内：「MM-dd HH:mm」如 04-11 11:36
 * - 跨年：「yyyy-MM-dd HH:mm」
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    // 一小时内
    if (diff < TimeUnit.HOURS.toMillis(1)) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff).toInt().coerceAtLeast(1)
        return "${minutes}分钟前"
    }

    // 判断是否本年
    val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(now))
    val itemYear    = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(timestamp))

    return if (currentYear == itemYear) {
        // 本年内：MM-dd HH:mm
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    } else {
        // 跨年：yyyy-MM-dd（不显示具体时间）
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }
}
