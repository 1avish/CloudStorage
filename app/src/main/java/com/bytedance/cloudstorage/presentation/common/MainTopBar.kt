package com.bytedance.cloudstorage.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SwapHorizontalCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 应用顶部导航栏
 *
 * @param selectedTab 当前选中的 Tab 索引，0=网盘，1=文件
 * @param onTabSelected Tab 点击回调
 * @param transferTaskCount 传输任务数，大于 0 时显示徽标
 * @param onTransferClick 传输按钮点击回调
 * @param onSearchClick 搜索按钮点击回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    transferTaskCount: Int = 0,
    onTransferClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            // 居中 Tab 区域
            TabBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        },
        actions = {
            // 传输按钮（带任务数徽标）
            TransferButton(
                taskCount = transferTaskCount,
                onClick = onTransferClick
            )
            // 搜索按钮
//            IconButton(onClick = onSearchClick) {
//                Icon(
//                    imageVector = Icons.Default.Search,
//                    contentDescription = "搜索"
//                )
//            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.White,
            scrolledContainerColor = Color.White
        )
    )
}

/**
 * 居中 Tab 组件：「网盘」和「文件」两个 Tab。
 *
 * 使用自定义 Row 实现而非 Material TabRow，因为：
 * - TabRow 默认占满整行宽度，这里只需要在 title 区域显示。
 * - 自定义实现更轻量，便于控制样式。
 */
@Composable
private fun TabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("网盘", "文件")

    Row(
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, title ->
            TabItem(
                title = title,
                isSelected = selectedTab == index,
                onClick = { onTabSelected(index) }
            )
        }
    }
}

/**
 * 单个 Tab 项。
 * 选中时文字加粗、颜色更深，底部显示指示条。
 */
@Composable
private fun TabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        // 选中指示条
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
                    .size(width = 20.dp, height = 2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * 传输按钮，带任务数徽标。
 * taskCount > 0 时在按钮右上角显示红色数字徽标。
 */
@Composable
private fun TransferButton(
    taskCount: Int,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (taskCount > 0) {
                    Badge(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ) {
                        // 超过 99 显示 99+
                        Text(text = if (taskCount > 99) "99+" else "$taskCount")
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.SwapHorizontalCircle,
                contentDescription = "传输",
                modifier = Modifier.graphicsLayer { rotationZ = 90f }
            )
        }
    }
}
