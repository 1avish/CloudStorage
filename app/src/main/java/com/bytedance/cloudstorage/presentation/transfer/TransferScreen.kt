package com.bytedance.cloudstorage.presentation.transfer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytedance.cloudstorage.presentation.filelist.CapsuleBg
import com.bytedance.cloudstorage.presentation.filelist.fileStyle
import com.bytedance.cloudstorage.presentation.filelist.formatFileSize
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PageBg = Color.White
private val TextPrimary = Color(0xFF1D2129)
private val TextSecondary = Color(0xFF8C8C8C)

@Composable
fun TransferScreen(
    onBack: () -> Unit = {},
    viewModel: TransferViewModel = viewModel(),
) {
    BackHandler { onBack() }

    var direction by remember { mutableStateOf(TransferDirectionFilter.Upload) }
    var statusFilter by remember { mutableStateOf(TransferStatusFilter.All) }

    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val state = if (direction == TransferDirectionFilter.Upload) uploadState else downloadState
    val visibleRecords = remember(state.records, statusFilter) {
        statusFilter.value?.let { status -> state.records.filter { it.status == status } }
            ?: state.records
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .navigationBarsPadding()
    ) {
        TransferTopBar(
            direction = direction,
            onBack = onBack,
            onDirectionChange = {
                direction = it
                statusFilter = TransferStatusFilter.All
            }
        )
        StatusFilterBar(
            direction = direction,
            state = state,
            selected = statusFilter,
            onSelected = { statusFilter = it }
        )
        TransferList(
            records = visibleRecords,
            allCount = state.allCount,
            direction = direction,
        )
    }
}

@Composable
private fun TransferTopBar(
    direction: TransferDirectionFilter,
    onBack: () -> Unit,
    onDirectionChange: (TransferDirectionFilter) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "返回",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DirectionTab(
                        title = "上传",
                        selected = direction == TransferDirectionFilter.Upload,
                        onClick = { onDirectionChange(TransferDirectionFilter.Upload) }
                    )
                    DirectionTab(
                        title = "下载",
                        selected = direction == TransferDirectionFilter.Download,
                        onClick = { onDirectionChange(TransferDirectionFilter.Download) }
                    )
                }
            }

            Box(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun DirectionTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = title,
        fontSize = 22.sp,
        letterSpacing = 2.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) TextPrimary else TextSecondary,
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun StatusFilterBar(
    direction: TransferDirectionFilter,
    state: TransferUiState,
    selected: TransferStatusFilter,
    onSelected: (TransferStatusFilter) -> Unit,
) {
    val activeText = if (direction == TransferDirectionFilter.Upload) "上传中" else "下载中"
    val items = listOf(
        TransferStatusFilter.All to "全部",
        TransferStatusFilter.Completed to "已完成 ${state.completedCount}",
        TransferStatusFilter.Transferring to "$activeText ${state.transferringCount}",
        TransferStatusFilter.Failed to "失败 ${state.failedCount}",
    )
    val selectedIndex = items.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    var totalWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.w.dp, vertical = 12.w.dp)
            .height(40.w.dp)
            .clip(RoundedCornerShape(50))
            .background(CapsuleBg)
            .padding(4.w.dp)
            .onSizeChanged { totalWidth = it.width }
    ) {
        if (totalWidth > 0) {
            val itemWidthDp = with(density) { totalWidth.toDp() } / items.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidthDp * selectedIndex,
                animationSpec = tween(durationMillis = 300),
                label = "transferCapsuleSlide"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .size(itemWidthDp, 32.w.dp)
                    .padding(horizontal = 2.w.dp)
                    .shadow(2.dp, RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
            )
        }

        Row(modifier = Modifier.fillMaxSize().padding(vertical = 4.w.dp)) {
            items.forEach { (filter, title) ->
                val isSelected = selected == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onSelected(filter) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 15.ws.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferList(
    records: List<TransferRecordUi>,
    allCount: Int,
    direction: TransferDirectionFilter,
) {
    if (allCount == 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "暂无传输记录",
                    fontSize = 16.ws.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                )
                if (direction == TransferDirectionFilter.Download) {
                    Spacer(modifier = Modifier.height(8.w.dp))
                    Text(
                        text = "下载默认保存到本地 Download 文件夹",
                        fontSize = 13.ws.sp,
                        color = Color(0xFFB0B0B0),
                    )
                }
            }
        }
        return
    }

    val groups = remember(records) {
        records.groupBy { formatDay(it.createdAt) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 6.w.dp,
            bottom = 28.w.dp,
        )
    ) {
        groups.forEach { (day, dayRecords) ->
            item(key = "day-$day") {
                Text(
                    text = day,
                    fontSize = 14.ws.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 16.w.dp, bottom = 12.w.dp)
                )
            }
            items(dayRecords, key = { it.id }) { record ->
                TransferRecordRow(record = record)
            }
        }
        item {
            Text(
                text = "没有更多内容了",
                fontSize = 14.ws.sp,
                color = Color(0xFFB0B0B0),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.w.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TransferRecordRow(record: TransferRecordUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.w.dp)
            .padding(horizontal = 16.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransferFileIcon(record = record)
        Spacer(modifier = Modifier.width(14.w.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.name,
                fontSize = 16.ws.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.w.dp))
            Text(
                text = "${formatFileSize(record.size)}  ${statusText(record.status)}",
                fontSize = 12.ws.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TransferFileIcon(record: TransferRecordUi) {
    val (icon, iconBg, iconTint) = remember(record.type) { fileStyle(record.type) }
    Box(
        modifier = Modifier
            .size(46.w.dp)
            .clip(RoundedCornerShape(14.w.dp))
            .background(iconBg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.w.dp)
        )
    }
}

private fun statusText(status: String): String = when (status) {
    TransferStatusFilter.Completed.value -> "已完成"
    TransferStatusFilter.Transferring.value -> "上传中"
    TransferStatusFilter.Failed.value -> "失败"
    else -> "已完成"
}

private fun formatDay(timestamp: Long): String {
    if (timestamp <= 0L) return "--"
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
}
