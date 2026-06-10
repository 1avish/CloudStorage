package com.bytedance.cloudstorage.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.SwapHorizontalCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets

private val BreadcrumbGray = Color(0xFF8C93A4)
private val BreadcrumbDivider = Color(0xFFD0D3D9)

/**
 * 应用顶部导航栏
 *
 * @param selectedTab 当前选中的 Tab 索引，0=网盘，1=文件
 * @param onTabSelected Tab 点击回调
 * @param transferTaskCount 传输任务数，大于 0 时显示徽标
 * @param onTransferClick 传输按钮点击回调
 * @param hasBack 是否显示返回按钮（在子目录时为 true）
 * @param onBackClick 返回按钮点击回调
 * @param currentFolderName 当前文件夹名称，null 时显示 tabs（根目录）
 * @param pathSegments 面包屑路径分段，空列表 = 根目录（不显示面包屑）
 * @param onPathClick 点击面包屑第 i 段的回调
 * @param isSelectionMode 是否处于文件选择模式
 * @param selectedCount 已选中文件数量
 * @param onCancelSelection 取消选择按钮回调
 * @param onSelectAll 全选按钮回调
 */
@Composable
fun MainTopBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    transferTaskCount: Int = 0,
    onTransferClick: () -> Unit,
    hasBack: Boolean = false,
    onBackClick: () -> Unit = {},
    currentFolderName: String? = null,
    pathSegments: List<String> = emptyList(),
    onPathClick: (index: Int) -> Unit = {},
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    onCancelSelection: () -> Unit = {},
    onSelectAll: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        if (isSelectionMode) {
            // ── 选择模式：关闭 | 管理文件 + 已选择 N 项 | 全选 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCancelSelection,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "取消",
                        tint = Color(0xFF111111),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "管理文件",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111111)
                        )
                        Text(
                            text = "已选择${selectedCount}项",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF8A8A8A)
                        )
                    }
                }
                Text(
                    text = "全选",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111111),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { onSelectAll() }
                )
            }
        } else {
            Column {
            // ── 第一行：返回按钮 + 居中 Tabs + 操作按钮 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：返回按钮（占 48dp，与 CenterAlignedTopAppBar 的 navigationIcon 占位一致）
                if (hasBack) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                } else {
                    // 无返回按钮时，用等宽占位保持 tabs 居中
                    Box(modifier = Modifier.size(48.dp))
                }

                // 中间：Tabs 或当前文件夹名
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentFolderName != null) {
                        Text(
                            text = currentFolderName,
                            fontSize = 22.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    } else {
                        TabBar(
                            selectedTab = selectedTab,
                            onTabSelected = onTabSelected
                        )
                    }
                }

                // 右侧：传输按钮
                TransferButton(
                    taskCount = transferTaskCount,
                    onClick = onTransferClick
                )
            }

            // ── 第二行：面包屑路径（仅在子目录时显示） ──
            if (pathSegments.isNotEmpty()) {
                // 构建完整路径：根目录 + 进入的文件夹
                val fullPath = listOf("我的网盘") + pathSegments
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    fullPath.forEachIndexed { index, name ->
                        if (index > 0) {
                            Text(
                                text = " / ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = BreadcrumbDivider
                            )
                        }
                        val isCurrent = index == fullPath.lastIndex
                        Text(
                            text = name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) Color.Black else BreadcrumbGray,
                            modifier = if (!isCurrent)
                                Modifier.clickable { onPathClick(index) }
                            else Modifier
                        )
                    }
                }
            }
        }
        }
    }
}

/**
 * 居中 Tab 组件：「网盘」和「文件」两个 Tab。
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
                testTag = if (index == 1) "main_tab_files" else "main_tab_home",
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
    testTag: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            letterSpacing = 2.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
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
                    .size(width = 40.dp, height = 2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFE36A))
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
