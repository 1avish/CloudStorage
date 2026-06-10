package com.bytedance.cloudstorage.presentation.share

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytedance.cloudstorage.domain.model.CloudFile
import com.bytedance.cloudstorage.domain.model.FileType
import com.bytedance.cloudstorage.presentation.common.VideoCoverThumbnail
import com.bytedance.cloudstorage.presentation.filelist.DividerColor
import com.bytedance.cloudstorage.presentation.filelist.EmptyIconTint
import com.bytedance.cloudstorage.presentation.filelist.PrimaryBlue
import com.bytedance.cloudstorage.presentation.filelist.TextPrimary
import com.bytedance.cloudstorage.presentation.filelist.TextSecondary
import com.bytedance.cloudstorage.presentation.filelist.fileStyle
import com.bytedance.cloudstorage.presentation.filelist.formatTimestamp
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws

// ────────────────────────────────────────────────
// 分享文件列表页（通过 deeplink 进入）
// ────────────────────────────────────────────────

/**
 * 分享文件列表页主组件。
 *
 * 用户通过 deeplink（cloudstorage://share/<token>）进入此页面，
 * 可浏览分享的文件列表、多选文件并保存到自己的网盘。
 *
 * 页面结构：
 * - [ShareTopBar] — 顶部返回栏，支持文件夹内逐级返回
 * - [ShareSelectionBar] — 全选/取消全选操作栏
 * - 文件列表 — 点击文件夹进入子目录，点击视频/文本跳转播放/阅读
 * - [ShareSaveButton] — 底部保存按钮，将选中文件存入网盘
 *
 * @param token       分享链接中的 token，用于加载文件列表
 * @param onBack      返回上一页（云盘首页或关闭页面）
 * @param onOpenVideo 跳转视频播放器（fileId, fileName, fileUri）
 * @param onOpenTxt   跳转文本阅读器（fileId, fileName, fileUri）
 */
@Composable
fun ShareListScreen(
    token: String,
    onBack: () -> Unit,
    onOpenVideo: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenTxt: (String, String, String) -> Unit = { _, _, _ -> },
    viewModel: ShareListViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val selectedCount = state.selectedIds.size

    // 加载分享数据，token 不变时跳过重复加载
    LaunchedEffect(token) {
        viewModel.load(token)
    }
    // 收集一次性保存结果事件，展示 Toast 并在成功后退出
    LaunchedEffect(viewModel) {
        viewModel.saveResult.collect { result ->
            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            if (result.shouldExit) {
                onBack()
            }
        }
    }

    // 返回逻辑：文件夹内先返回上一级，根级则退出页面
    val navigateBackOrExit = {
        if (!viewModel.navigateBack()) {
            onBack()
        }
    }

    // 系统返回键拦截
    BackHandler { navigateBackOrExit() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ShareTopBar(onBack = navigateBackOrExit)
            ShareSelectionBar(
                selectedCount = selectedCount,
                allSelected = state.files.isNotEmpty() && selectedCount == state.files.size,
                onToggleAll = {
                    if (state.files.isNotEmpty() && selectedCount == state.files.size) {
                        viewModel.clearSelection()
                    } else {
                        viewModel.selectAll()
                    }
                }
            )

            when {
                state.isLoading -> ShareMessage("正在加载分享内容")
                state.invalid -> ShareMessage("分享链接无效或文件已不存在")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // 底部留白给保存按钮
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 116.w.dp)
                ) {
                    items(state.files, key = { it.id }) { file ->
                        SharedFileRow(
                            file = file,
                            selected = file.id in state.selectedIds,
                            onOpen = {
                                when (file.type) {
                                    FileType.Folder -> {
                                        viewModel.markOpened()
                                        viewModel.navigateIntoFolder(file.id)
                                    }
                                    FileType.Video -> {
                                        viewModel.markOpened()
                                        onOpenVideo(file.id, file.name, file.uri.orEmpty())
                                    }
                                    FileType.Txt -> {
                                        viewModel.markOpened()
                                        onOpenTxt(file.id, file.name, file.uri.orEmpty())
                                    }
                                    FileType.Other -> Toast.makeText(context, "暂不支持打开此文件", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onToggleSelection = { viewModel.toggleSelection(file.id) }
                        )
                    }
                }
            }
        }

        // 底部保存按钮，固定在屏幕底部
        ShareSaveButton(
            enabled = selectedCount > 0 && !state.invalid && !state.isSaving,
            isSaving = state.isSaving,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.w.dp),
            onClick = {
                viewModel.saveSelectedFiles()
            }
        )
    }
}

// ────────────────────────────────────────────────
// 顶部导航栏
// ────────────────────────────────────────────────

/**
 * 分享页顶部栏，包含返回箭头和「资源分享」标题。
 */
@Composable
private fun ShareTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.w.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBackIosNew,
            contentDescription = "返回",
            tint = TextPrimary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.w.dp)
                .size(22.w.dp)
                .clickable { onBack() }
        )
        Text(
            text = "资源分享",
            fontSize = 22.ws.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

// ────────────────────────────────────────────────
// 分享文件列表行
// ────────────────────────────────────────────────

/**
 * 已选择数量和全选/取消全选操作栏。
 */
@Composable
private fun ShareSelectionBar(
    selectedCount: Int,
    allSelected: Boolean,
    onToggleAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.w.dp, vertical = 14.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "已选择${selectedCount}个文件",
            fontSize = 16.ws.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(18.w.dp))
        Text(
            text = if (allSelected) "取消全选" else "全选",
            fontSize = 16.ws.sp,
            fontWeight = FontWeight.Medium,
            color = PrimaryBlue,
            modifier = Modifier.clickable { onToggleAll() }
        )
    }
}

// ────────────────────────────────────────────────
// 分享文件列表行
// ────────────────────────────────────────────────

/**
 * 单条分享文件行。
 *
 * 点击打开文件（文件夹进入子目录、视频跳播放器、文本跳阅读器），
 * 长按切换选中状态。
 *
 * @param file             文件数据
 * @param selected         当前是否选中
 * @param onOpen           点击打开回调
 * @param onToggleSelection 切换选中回调
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SharedFileRow(
    file: CloudFile,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit,
) {
    val (icon, iconBg, iconTint) = remember(file.type) { fileStyle(file.type) }
    // 选中行背景色高亮
    val rowBg = if (selected) Color(0xFFFFF8D8) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.w.dp)
            .background(rowBg)
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onToggleSelection,
            )
            .padding(horizontal = 20.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 文件图标：视频优先展示封面缩略图
        if (file.type == FileType.Video && !file.coverUri.isNullOrEmpty()) {
            VideoCoverThumbnail(
                coverUri = file.coverUri,
                modifier = Modifier.size(46.w.dp),
                cornerRadiusDp = 12,
                showPlayIcon = false,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(46.w.dp)
                    .clip(RoundedCornerShape(10.w.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(25.w.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.w.dp))
        Column(modifier = Modifier.weight(1f)) {
            // 文件名：文件夹展示「来自：xxx」，普通文件直接展示名称
            Text(
                text = if (file.type == FileType.Folder) "来自：${file.name}" else file.name,
                fontSize = 17.ws.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.w.dp))
            Text(
                text = formatTimestamp(file.updatedAt),
                fontSize = 14.ws.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 选中圆圈
        SelectionCircle(
            selected = selected,
            onClick = onToggleSelection
        )
    }
}

// ────────────────────────────────────────────────
// 选中状态圆圈
// ────────────────────────────────────────────────

/**
 * 圆形选中指示器。
 *
 * 未选中：灰色空心圆边框；选中：黄色实心圆 + 深色勾号。
 */
@Composable
private fun SelectionCircle(
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(28.w.dp)
            .border(
                width = if (selected) 0.dp else 1.5f.w.dp,
                color = if (selected) PrimaryBlue else EmptyIconTint,
                shape = CircleShape
            )
            .clip(CircleShape)
            .background(if (selected) PrimaryBlue else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选择",
                tint = TextPrimary,
                modifier = Modifier.size(17.w.dp)
            )
        }
    }
}

// ────────────────────────────────────────────────
// 底部保存按钮
// ────────────────────────────────────────────────

/**
 * 底部保存按钮。
 *
 * 未选中文件或加载中时置灰不可点击，保存中显示「保存中」文字。
 */
@Composable
private fun ShareSaveButton(
    enabled: Boolean,
    isSaving: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(enabled = enabled) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Save,
            contentDescription = "保存",
            tint = if (enabled) TextPrimary else DividerColor,
            modifier = Modifier.size(36.w.dp)
        )
        Text(
            text = if (isSaving) "保存中" else "保存",
            fontSize = 14.ws.sp,
            color = if (enabled) TextPrimary else DividerColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// ────────────────────────────────────────────────
// 状态消息占位
// ────────────────────────────────────────────────

/**
 * 居中文字提示，用于加载中、链接无效等状态展示。
 */
@Composable
private fun ShareMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.ws.sp,
            color = TextSecondary
        )
    }
}
