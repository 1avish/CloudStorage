package com.bytedance.cloudstorage.presentation.filelist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.activity.compose.BackHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytedance.cloudstorage.domain.model.CloudFile
import com.bytedance.cloudstorage.domain.model.FileType
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws

// ────────────────────────────────────────────────
// 颜色常量，严格对齐设计稿 App.tsx
// ────────────────────────────────────────────────
private val BgGray           = Color(0xFFF8F9FA)
private val CapsuleBg        = Color(0xFFEBEDF0)
private val TextPrimary      = Color(0xFF1D2129)
private val TextSecondary    = Color(0xFF8C93A4)
private val DividerColor     = Color(0xFFF0F2F5)
private val IconFolderBg     = Color(0xFFF0F5FF)
private val IconFolderTint   = Color(0xFF6366F1)
private val IconVideoBg      = Color(0xFFFFF0F5)
private val IconVideoTint    = Color(0xFFEB2F96)
private val IconDocBg        = Color(0xFFE6F7FF)
private val IconDocTint      = Color(0xFF1890FF)
private val EmptyIconBg      = Color(0xFFF0F2F5)
private val EmptyIconTint    = Color(0xFFC0C4D0)

// ────────────────────────────────────────────────
// 胶囊筛选项（不含文件夹）
// ────────────────────────────────────────────────

private enum class FileFilter(val label: String, val typeKey: String?) {
    All("全部", null),
    Video("视频", "video"),
    Doc("文档", "txt")
}

// ────────────────────────────────────────────────
// 文件 Tab 主页面
// ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    viewModel: FileListViewModel = viewModel()
) {
    val files by viewModel.files.collectAsStateWithLifecycle()
    val atRoot by viewModel.atRoot.collectAsStateWithLifecycle()
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showNewFolderSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // ── 系统返回键：在子目录时返回上一级，否则交由系统处理 ──
    BackHandler(enabled = !atRoot) {
        viewModel.navigateBack()
    }

    // ── 性能监测（测试完连同 FileListPerfMonitor 一起删除）──
    DisposableEffect(Unit) {
        FileListPerfMonitor.start()
        onDispose { FileListPerfMonitor.stop() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGray)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 胶囊筛选栏 ──
            CapsuleSegmentedControl(
                selectedIndex = selectedFilterIndex,
                onSelected = { index ->
                    selectedFilterIndex = index
                    viewModel.setFilter(FileFilter.entries[index].typeKey)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.w.dp, vertical = 12.w.dp)
            )

            // ── 排序栏 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.w.dp, vertical = 4.w.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "按综合排序",
                    fontSize = 13.ws.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                Text(
                    text = " ↓",
                    fontSize = 10.ws.sp,
                    color = TextSecondary
                )
            }

            // ── 文件列表 / 空状态 ──
            if (files.isEmpty()) {
                EmptyFileList()
            } else {
                // lastFileId 在 items 块外计算，避免 lambda 捕获 files 列表
                val lastFileId = files.last().id
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.w.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        bottom = 96.w.dp
                    )
                ) {
                    items(files, key = { it.id }, contentType = { it.type.name }) { file ->
                        FileListItem(
                            file = file,
                            showDivider = file.id != lastFileId,
                            onFolderClick = if (file.type == FileType.Folder) { folderId ->
                                viewModel.navigateIntoFolder(folderId, file.name)
                            } else null
                        )
                    }
                }
            }
        }

        // ── 悬浮按钮：白色圆底 + 黑色加号 ──
        FloatingActionButton(
            onClick = { showCreateSheet = true },
            shape = CircleShape,
            containerColor = Color.White,
            contentColor = TextPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-24).w.dp, y = (-48).w.dp)
                .size(56.w.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "创建文件夹",
                modifier = Modifier.size(28.w.dp)
            )
        }
    }

    if (showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = null
        ) {
            BottomSheetContent(
                onDismiss = { showCreateSheet = false },
                onNewFolder = {
                    showCreateSheet = false
                    showNewFolderSheet = true
                }
            )
        }
    }

    if (showNewFolderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNewFolderSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = null
        ) {
            NewFolderBottomSheet(
                onDismiss = { showNewFolderSheet = false },
                onConfirm = { name ->
                    viewModel.createFolder(name)
                    showNewFolderSheet = false
                }
            )
        }
    }
}

// ────────────────────────────────────────────────
// 底部弹窗：离线下载 + 上传文件
// ────────────────────────────────────────────────

@Composable
private fun BottomSheetContent(onDismiss: () -> Unit, onNewFolder: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.w.dp)
            .padding(bottom = 32.w.dp)
    ) {
        // 拖拽手柄
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.w.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.w.dp, height = 4.w.dp)
                    .clip(RoundedCornerShape(2.w.dp))
                    .background(DividerColor)
            )
        }

        // ── 离线下载 ──
        Text(
            text = "离线下载",
            fontSize = 20.ws.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 4.w.dp, bottom = 12.w.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.w.dp))
                .background(Color(0xFFF5F7FA))
                .clickable { onDismiss() }
                .padding(horizontal = 16.w.dp, vertical = 16.w.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.w.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = Color(0xFF3370FF),
                    modifier = Modifier.size(22.w.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.w.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "添加磁力链",
                    fontSize = 16.ws.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.w.dp)
            )
        }

        // ── 上传文件 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.w.dp, bottom = 14.w.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "上传文件",
                fontSize = 20.ws.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onDismiss() }
            ) {
                Text(
                    text = "保存到 网盘/文件",
                    fontSize = 13.ws.sp,
                    color = TextSecondary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.w.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.w.dp)
        ) {
            listOf(
                Triple("视频", Icons.Default.OndemandVideo, Color(0xFF3370FF)),
                Triple("文档", Icons.Default.Description, Color(0xFF3370FF)),
                Triple("新建文件夹", Icons.Default.CreateNewFolder, Color(0xFF3370FF))
            ).forEach { (label, icon, iconColor) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.w.dp))
                        .background(Color(0xFFF5F7FA))
                        .clickable {
                            if (label == "新建文件夹") onNewFolder() else onDismiss()
                        }
                        .padding(vertical = 18.w.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.w.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = iconColor,
                            modifier = Modifier.size(26.w.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.w.dp))
                    Text(
                        text = label,
                        fontSize = 14.ws.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────
// 底部弹窗：新建文件夹
// ────────────────────────────────────────────────

@Composable
private fun NewFolderBottomSheet(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var folderName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.w.dp)
            .padding(bottom = 32.w.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusManager.clearFocus() }
    ) {
        // 拖拽手柄
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.w.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.w.dp, height = 4.w.dp)
                    .clip(RoundedCornerShape(2.w.dp))
                    .background(DividerColor)
            )
        }

        // 标题
        Text(
            text = "新建文件夹",
            fontSize = 20.ws.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.w.dp, bottom = 20.w.dp)
        )

        // 输入框
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.w.dp)
                .clip(RoundedCornerShape(8.w.dp))
                .background(Color(0xFFF5F7FA))
                .padding(horizontal = 14.w.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (folderName.isEmpty()) {
                Text(
                    text = "请输入文件夹名",
                    fontSize = 15.ws.sp,
                    color = TextSecondary
                )
            }
            BasicTextField(
                value = folderName,
                onValueChange = { folderName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.ws.sp,
                    color = TextPrimary
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (folderName.isNotBlank()) onConfirm(folderName.trim())
                })
            )
        }

        Spacer(modifier = Modifier.height(24.w.dp))

        // 取消 / 确认 按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.w.dp)
        ) {
            // 取消
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.w.dp)
                    .clip(RoundedCornerShape(8.w.dp))
                    .background(Color(0xFFF5F7FA))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "取消",
                    fontSize = 16.ws.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            // 确认
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.w.dp)
                    .clip(RoundedCornerShape(8.w.dp))
                    .background(Color(0xFF3370FF))
                    .clickable(enabled = folderName.isNotBlank()) {
                        onConfirm(folderName.trim())
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "确认",
                    fontSize = 16.ws.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (folderName.isNotBlank()) Color.White
                    else Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ────────────────────────────────────────────────
// 胶囊分段控件（active = 白底 + 阴影 + 黑色粗体）
// ────────────────────────────────────────────────

@Composable
private fun CapsuleSegmentedControl(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = FileFilter.entries

    Box(
        modifier = modifier
            .height(32.w.dp)
            .clip(RoundedCornerShape(50))
            .background(CapsuleBg)
            .padding(1.w.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            filters.forEachIndexed { index, filter ->
                val isSelected = index == selectedIndex

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "capsuleBg"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(2.w.dp)
                        .shadow(
                            elevation = if (isSelected) 2.dp else 0.dp,
                            shape = RoundedCornerShape(50)
                        )
                        .clip(RoundedCornerShape(50))
                        .background(bgColor)
                        .clickable { onSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label,
                        fontSize = 16.ws.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────
// 文件列表项（严格对齐设计稿）
// ────────────────────────────────────────────────

@Stable
@Composable
private fun FileListItem(file: CloudFile, showDivider: Boolean = true, onFolderClick: ((String) -> Unit)? = null) {
    TrackRecompose(file.id)
    val (icon, iconBg, iconTint) = remember(file.type) { fileStyle(file.type) }
    val formattedSize = remember(file.size, file.type) {
        if (file.type != FileType.Folder && file.size > 0) formatFileSize(file.size) else null
    }
    val formattedTime = remember(file.updatedAt) { formatTimestamp(file.updatedAt) }

    Column(modifier = Modifier.height(74.w.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.w.dp)
                .then(
                    if (file.type == FileType.Folder && onFolderClick != null)
                        Modifier.clickable { onFolderClick(file.id) }
                    else Modifier
                )
                .padding(horizontal = 16.w.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Spacer(modifier = Modifier.width(14.w.dp))

            // 文件名 + 元信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 16.ws.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.w.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (formattedSize != null) {
                        Text(
                            text = formattedSize,
                            fontSize = 12.ws.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = " | ",
                            fontSize = 12.ws.sp,
                            color = DividerColor
                        )
                    }
                    Text(
                        text = formattedTime,
                        fontSize = 12.ws.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }

            // 右侧圆形选择按钮
            Box(
                modifier = Modifier.size(40.w.dp, 72.w.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.w.dp)
                        .border(
                            width = 1.5f.w.dp,
                            color = EmptyIconTint,
                            shape = CircleShape
                        )
                )
            }
        }

        // 底部分割线（仅 n-1 条）
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.w.dp)
                    .height((2f).w.dp)
                    .background(DividerColor)
            )
        }
    }
}

// ────────────────────────────────────────────────
// 空状态（大文件夹图标 + 标题 + 副标题）
// ────────────────────────────────────────────────

@Composable
private fun EmptyFileList() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.w.dp)
                    .clip(CircleShape)
                    .background(EmptyIconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = EmptyIconTint,
                    modifier = Modifier.size(48.w.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.w.dp))
            Text(
                text = "暂无文件",
                fontSize = 16.ws.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.w.dp))
        }
    }
}

// ────────────────────────────────────────────────
// 辅助函数
// ────────────────────────────────────────────────

private fun fileStyle(type: FileType): Triple<ImageVector, Color, Color> = when (type) {
    FileType.Folder -> Triple(Icons.Default.Folder, IconFolderBg, IconFolderTint)
    FileType.Video  -> Triple(Icons.Default.OndemandVideo, IconVideoBg, IconVideoTint)
    FileType.Txt    -> Triple(Icons.Default.Description, IconDocBg, IconDocTint)
    FileType.Other  -> Triple(Icons.Default.Description, IconDocBg, IconDocTint)
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024                   -> "${bytes}B"
    bytes < 1024 * 1024            -> "%.1fKB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024    -> "%.1fMB".format(bytes / (1024.0 * 1024))
    else                           -> "%.1fGB".format(bytes / (1024.0 * 1024 * 1024))
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
