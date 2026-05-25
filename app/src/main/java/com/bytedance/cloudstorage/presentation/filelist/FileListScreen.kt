package com.bytedance.cloudstorage.presentation.filelist

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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
private val BgGray           = Color.White
private val PrimaryBlue      = Color(0xFF3370FF)
private val PrimaryBlueBg    = Color(0xFFEBF0FF)
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
    val selectedFileIds by viewModel.selectedFileIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedFiles by viewModel.selectedFiles.collectAsStateWithLifecycle()
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showNewFolderSheet by remember { mutableStateOf(false) }
    var renameTargetFile by remember { mutableStateOf<CloudFile?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val sortDirection by viewModel.sortDirection.collectAsStateWithLifecycle()
    val fileOrderKey = remember(files) {
        files.joinToString(separator = "|") { it.id }
    }
    val sheetState = rememberModalBottomSheetState()

    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        uploadSelectedFile(context, uri, viewModel)
    }

    // ── 系统返回键：选择模式下退出选择，否则返回上一级 ──
    BackHandler(enabled = isSelectionMode) {
        viewModel.exitSelectionMode()
    }
    BackHandler(enabled = !atRoot && !isSelectionMode) {
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
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.w.dp, vertical = 4.w.dp)
                        .clickable { showSortMenu = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val label = when (sortType) {
                        SortType.TIME -> "按修改时间"
                        SortType.NAME -> "按文件名"
                    }
                    val arrow = if (sortDirection == SortDirection.DESC) " ↓" else " ↑"
                    Text(
                        text = label,
                        fontSize = 13.ws.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Text(
                        text = arrow,
                        fontSize = 10.ws.sp,
                        color = TextSecondary
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    containerColor = Color.White
                ) {
                    DropdownMenuItem(
                        text = { Text("按修改时间") },
                        onClick = {
                            viewModel.toggleSort(SortType.TIME)
                            showSortMenu = false
                        },
                        trailingIcon = {
                            if (sortType == SortType.TIME) {
                                Text(if (sortDirection == SortDirection.DESC) "↓" else "↑")
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("按文件名") },
                        onClick = {
                            viewModel.toggleSort(SortType.NAME)
                            showSortMenu = false
                        },
                        trailingIcon = {
                            if (sortType == SortType.NAME) {
                                Text(if (sortDirection == SortDirection.DESC) "↓" else "↑")
                            }
                        }
                    )
                }
            }

            // ── 文件列表 / 空状态 ──
            if (files.isEmpty()) {
                EmptyFileList()
            } else {
                // lastFileId 在 items 块外计算，避免 lambda 捕获 files 列表
                val lastFileId = files.last().id
                key(selectedFilterIndex, sortType, sortDirection, fileOrderKey) {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.w.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            bottom = if (isSelectionMode) 180.w.dp else 96.w.dp
                        )
                    ) {
                        items(files, contentType = { it.type.name }) { file ->
                            FileListItem(
                                file = file,
                                isSelected = file.id in selectedFileIds,
                                isSelectionMode = isSelectionMode,
                                showDivider = file.id != lastFileId,
                                onCircleClick = { viewModel.toggleFileSelection(file.id) },
                                onLongPress = { viewModel.enterSelectionMode(file.id) },
                                onFolderClick = if (file.type == FileType.Folder) { folderId ->
                                    viewModel.navigateIntoFolder(folderId, file.name)
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // ── 悬浮按钮：选择模式下隐藏 ──
        if (!isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-24).w.dp, y = (-48).w.dp)
                    .size(56.w.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = PrimaryBlue.copy(alpha = 0.3f),
                        spotColor = PrimaryBlue.copy(alpha = 0.25f)
                    )
                    .clip(CircleShape)
                    .clickable { showCreateSheet = true },
                contentAlignment = Alignment.Center
            ) {
                // 底层：斜向渐变
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0xFF7AAEFF),
                                    0.33f to Color(0xFF3B82F6),
                                    0.66f to Color(0xFF3B82F6),
                                    1.0f to Color(0xFF7AAEFF)
                                ),
                                start = Offset(0f, Float.POSITIVE_INFINITY),
                                end = Offset(Float.POSITIVE_INFINITY, 0f)
                            )
                        )
                )
                // 光泽层：左上角径向高光
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.Yellow.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = Offset(15f, 15f),
                                radius = 56f
                            )
                        )
                )
                // 图标
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "创建文件夹",
                    tint = Color.White,
                    modifier = Modifier.size(28.w.dp)
                )
            }
        }

        // ── 选择模式底部操作栏（非模态、不遮挡背景）──
        if (isSelectionMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                FileActionBar(
                    selectedFiles = selectedFiles,
                    onDownload = {
                        Toast.makeText(context, "下载功能（待实现）", Toast.LENGTH_SHORT).show()
                    },
                    onShare = {
                        Toast.makeText(context, "分享功能（待实现）", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = {
                        showDeleteConfirm = true
                    },
                    onRename = { file ->
                        viewModel.exitSelectionMode()
                        renameTargetFile = file
                    },
                    onToggleFile = { fileId -> viewModel.toggleFileSelection(fileId) }
                )
            }
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
                },
                onUploadVideo = {
                    showCreateSheet = false
                    filePickerLauncher.launch(arrayOf("video/*"))
                },
                onUploadDoc = {
                    showCreateSheet = false
                    filePickerLauncher.launch(arrayOf("application/pdf", "text/plain"))
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

    // ── 重命名弹窗 ──
    if (renameTargetFile != null) {
        ModalBottomSheet(
            onDismissRequest = { renameTargetFile = null },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = null
        ) {
            RenameBottomSheet(
                file = renameTargetFile!!,
                onDismiss = { renameTargetFile = null },
                onConfirm = { newName ->
                    viewModel.renameFile(renameTargetFile!!.id, newName)
                    renameTargetFile = null
                }
            )
        }
    }

    // ── 删除确认弹窗 ──
    if (showDeleteConfirm) {
        ModalBottomSheet(
            onDismissRequest = { showDeleteConfirm = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = null
        ) {
            DeleteConfirmBottomSheet(
                count = selectedFiles.size,
                onDismiss = { showDeleteConfirm = false },
                onConfirm = {
                    viewModel.deleteSelectedFiles()
                    showDeleteConfirm = false
                    Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// ────────────────────────────────────────────────
// 底部弹窗：离线下载 + 上传文件
// ────────────────────────────────────────────────

@Composable
private fun BottomSheetContent(
    onDismiss: () -> Unit,
    onNewFolder: () -> Unit = {},
    onUploadVideo: () -> Unit = {},
    onUploadDoc: () -> Unit = {},
) {
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
                            when (label) {
                                "新建文件夹" -> onNewFolder()
                                "视频" -> onUploadVideo()
                                "文档" -> onUploadDoc()
                                else -> onDismiss()
                            }
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
// 底部弹窗：文件操作（下载/分享/删除/重命名 + 已选文件横滑列表）
// ────────────────────────────────────────────────

@Composable
private fun FileActionBottomSheet(
    selectedFiles: List<CloudFile>,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRename: (CloudFile) -> Unit,
    onToggleFile: (String) -> Unit,
) {
    val multiSelect = selectedFiles.size > 1
    val primaryBlue = Color(0xFF3370FF)

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

        // 标题
        Text(
            text = "已选择 ${selectedFiles.size} 个文件",
            fontSize = 20.ws.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 4.w.dp, bottom = 16.w.dp)
        )

        // ── 已选文件横滑列表 ──
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.w.dp),
            contentPadding = PaddingValues(horizontal = 2.w.dp)
        ) {
            items(selectedFiles, key = { it.id }) { file ->
                SelectedFileChip(
                    file = file,
                    onRemove = { onToggleFile(file.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.w.dp))
        HorizontalDivider(color = DividerColor, thickness = 1.w.dp)
        Spacer(modifier = Modifier.height(20.w.dp))

        // ── 操作按钮行 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(
                icon = Icons.Default.ArrowDownward,
                label = "下载",
                iconTint = primaryBlue,
                bgColor = Color(0xFFEBF0FF),
                onClick = onDownload
            )
            ActionButton(
                icon = Icons.Default.Share,
                label = "分享",
                iconTint = Color(0xFF34C759),
                bgColor = Color(0xFFE8F8ED),
                onClick = onShare
            )
            ActionButton(
                icon = Icons.Default.Delete,
                label = "删除",
                iconTint = Color(0xFFFF3B30),
                bgColor = Color(0xFFFFEBEE),
                onClick = onDelete
            )
            ActionButton(
                icon = Icons.Default.Edit,
                label = "重命名",
                iconTint = if (multiSelect) EmptyIconTint else Color(0xFF6366F1),
                bgColor = if (multiSelect) Color(0xFFF0F2F5) else Color(0xFFF0F5FF),
                enabled = !multiSelect,
                onClick = {
                    if (!multiSelect && selectedFiles.isNotEmpty()) {
                        onRename(selectedFiles.first())
                    }
                }
            )
        }
    }
}

// ────────────────────────────────────────────────
// 底部操作栏：非模态版本（选择模式下固定在底部，不遮挡背景）
// ────────────────────────────────────────────────

@Composable
private fun FileActionBar(
    selectedFiles: List<CloudFile>,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRename: (CloudFile) -> Unit,
    onToggleFile: (String) -> Unit,
) {
    val multiSelect = selectedFiles.size > 1
    val primaryBlue = Color(0xFF3370FF)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.w.dp)
            .padding(top = 12.w.dp, bottom = 24.w.dp)
    ) {
        // ── 已选文件横滑列表 ──
        if (selectedFiles.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.w.dp),
                contentPadding = PaddingValues(horizontal = 2.w.dp)
            ) {
                items(selectedFiles, key = { it.id }) { file ->
                    SelectedFileChip(
                        file = file,
                        onRemove = { onToggleFile(file.id) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.w.dp))
            HorizontalDivider(color = DividerColor, thickness = 1.w.dp)
            Spacer(modifier = Modifier.height(16.w.dp))
        }

        // ── 操作按钮行 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(
                icon = Icons.Default.ArrowDownward,
                label = "下载",
                iconTint = primaryBlue,
                bgColor = Color(0xFFEBF0FF),
                onClick = onDownload
            )
            ActionButton(
                icon = Icons.Default.Share,
                label = "分享",
                iconTint = Color(0xFF34C759),
                bgColor = Color(0xFFE8F8ED),
                onClick = onShare
            )
            ActionButton(
                icon = Icons.Default.Delete,
                label = "删除",
                iconTint = Color(0xFFFF3B30),
                bgColor = Color(0xFFFFEBEE),
                onClick = onDelete
            )
            ActionButton(
                icon = Icons.Default.Edit,
                label = "重命名",
                iconTint = if (multiSelect) EmptyIconTint else Color(0xFF6366F1),
                bgColor = if (multiSelect) Color(0xFFF0F2F5) else Color(0xFFF0F5FF),
                enabled = !multiSelect,
                onClick = {
                    if (!multiSelect && selectedFiles.isNotEmpty()) {
                        onRename(selectedFiles.first())
                    }
                }
            )
        }
    }
}

// ── 已选文件标签（横滑列表中的每一项） ──

@Composable
private fun SelectedFileChip(
    file: CloudFile,
    onRemove: () -> Unit,
) {
    val (icon, _, iconTint) = remember(file.type) { fileStyle(file.type) }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.w.dp))
            .background(Color(0xFFF5F7FA))
            .padding(horizontal = 10.w.dp, vertical = 8.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.w.dp)
        )
        Spacer(modifier = Modifier.width(6.w.dp))
        Text(
            text = file.name,
            fontSize = 14.ws.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 100.w.dp)
        )
        Spacer(modifier = Modifier.width(6.w.dp))
        Box(
            modifier = Modifier
                .size(18.w.dp)
                .clip(CircleShape)
                .background(EmptyIconTint.copy(alpha = 0.3f))
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "取消选择",
                tint = TextSecondary,
                modifier = Modifier.size(12.w.dp)
            )
        }
    }
}

// ── 操作按钮 ──

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    bgColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.w.dp)
                .clip(RoundedCornerShape(14.w.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.w.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.w.dp))
        Text(
            text = label,
            fontSize = 13.ws.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) TextPrimary else TextSecondary
        )
    }
}

// ────────────────────────────────────────────────
// 底部弹窗：重命名文件
// ────────────────────────────────────────────────

@Composable
private fun RenameBottomSheet(
    file: CloudFile,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var fileName by remember { mutableStateOf(file.name) }
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

        Text(
            text = "重命名",
            fontSize = 20.ws.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.w.dp, bottom = 20.w.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.w.dp)
                .clip(RoundedCornerShape(8.w.dp))
                .background(Color(0xFFF5F7FA))
                .padding(horizontal = 14.w.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (fileName.isEmpty()) {
                Text(
                    text = "请输入新文件名",
                    fontSize = 15.ws.sp,
                    color = TextSecondary
                )
            }
            BasicTextField(
                value = fileName,
                onValueChange = { fileName = it },
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
                    if (fileName.isNotBlank()) onConfirm(fileName.trim())
                })
            )
        }

        Spacer(modifier = Modifier.height(24.w.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.w.dp)
        ) {
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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.w.dp)
                    .clip(RoundedCornerShape(8.w.dp))
                    .background(Color(0xFF3370FF))
                    .clickable(enabled = fileName.isNotBlank()) {
                        onConfirm(fileName.trim())
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "确认",
                    fontSize = 16.ws.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (fileName.isNotBlank()) Color.White
                    else Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ────────────────────────────────────────────────
// 底部弹窗：删除确认
// ────────────────────────────────────────────────

@Composable
private fun DeleteConfirmBottomSheet(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
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

        // 警告图标
        Box(
            modifier = Modifier
                .padding(top = 12.w.dp, bottom = 8.w.dp)
                .size(48.w.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFEBEE))
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = Color(0xFFFF3B30),
                modifier = Modifier.size(26.w.dp)
            )
        }

        // 标题
        Text(
            text = "确认删除",
            fontSize = 20.ws.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.w.dp, bottom = 6.w.dp)
        )

        // 描述
        Text(
            text = "将删除 $count 个文件，删除后无法恢复",
            fontSize = 14.ws.sp,
            color = TextSecondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.w.dp)
        )

        // 按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.w.dp)
        ) {
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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.w.dp)
                    .clip(RoundedCornerShape(8.w.dp))
                    .background(Color(0xFFFF3B30))
                    .clickable { onConfirm() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "删除",
                    fontSize = 16.ws.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}

// ────────────────────────────────────────────────
// 胶囊分段控件（滑动指示器 + 无波纹）
// ────────────────────────────────────────────────

@Composable
private fun CapsuleSegmentedControl(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = FileFilter.entries
    var totalWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .height(40.w.dp)
            .clip(RoundedCornerShape(50))
            .background(CapsuleBg)
            .padding(4.w.dp)
            .onSizeChanged { totalWidth = it.width }
    ) {
        if (totalWidth > 0) {
            val itemWidthDp = with(density) { totalWidth.toDp() } / filters.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidthDp * selectedIndex,
                animationSpec = tween(durationMillis = 300),
                label = "capsuleSlide"
            )

            // 滑动指示器白球
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

        // 文字层
        Row(modifier = Modifier.fillMaxSize().padding(vertical = 4.w.dp)) {
            filters.forEachIndexed { index, filter ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onSelected(index) },
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListItem(
    file: CloudFile,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    showDivider: Boolean = true,
    onCircleClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onFolderClick: ((String) -> Unit)? = null,
) {
    TrackRecompose(file.id)
    val (icon, iconBg, iconTint) = remember(file.type) { fileStyle(file.type) }
    val formattedSize = remember(file.size, file.type) {
        if (file.type != FileType.Folder && file.size > 0) formatFileSize(file.size) else null
    }
    val formattedTime = remember(file.updatedAt) { formatTimestamp(file.updatedAt) }

    // 圆圈选中静态样式
    val circleBg = if (isSelected) Color(0xFF3370FF) else Color.Transparent
    val circleBorder = if (isSelected) Color(0xFF3370FF) else EmptyIconTint
    val rowBg = if (isSelected) Color(0xFFEBF0FF) else Color.Transparent

    Column(modifier = Modifier.height(74.w.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.w.dp)
                .background(rowBg)
                .then(
                    if (isSelectionMode) {
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onCircleClick?.invoke() }
                    } else if (onLongPress != null) {
                        Modifier.combinedClickable(
                            onClick = {
                                if (file.type == FileType.Folder && onFolderClick != null) {
                                    onFolderClick(file.id)
                                }
                            },
                            onLongClick = { onLongPress() }
                        )
                    } else if (file.type == FileType.Folder && onFolderClick != null) {
                        Modifier.clickable { onFolderClick(file.id) }
                    } else Modifier
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
                modifier = Modifier
                    .size(40.w.dp, 72.w.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onCircleClick?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.w.dp)
                        .border(
                            width = if (isSelected) 0.dp else 1.5f.w.dp,
                            color = circleBorder,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .background(circleBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "已选中",
                            tint = Color.White,
                            modifier = Modifier.size(14.w.dp)
                        )
                    }
                }
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

// ────────────────────────────────────────────────
// 文件上传辅助函数
// ────────────────────────────────────────────────

/** MIME 类型 → 文件分类类型键 */
private fun mimeTypeToFileType(mimeType: String?): String = when {
    mimeType == null                -> "other"
    mimeType.startsWith("video/")  -> "video"
    mimeType.startsWith("text/")   -> "txt"
    mimeType == "application/pdf"  -> "txt"
    else                           -> "other"
}

/**
 * 从 Content URI 读取文件元数据并写入本地数据库。
 *
 * 流程：
 * 1. ContentResolver.query() 读取 DISPLAY_NAME 和 SIZE
 * 2. 将数据封装为 FileEntity，通过 ViewModel 存入 Room
 * 3. 数据库变更自动触发 Room Flow → UI 即时刷新
 */
private fun uploadSelectedFile(context: android.content.Context, uri: Uri, viewModel: FileListViewModel) {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri)
    val fileType = mimeTypeToFileType(mimeType)
    var fileName = "未命名文件"
    var fileSize = 0L

    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) fileName = cursor.getString(nameIndex) ?: fileName
            if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
        }
    }

    viewModel.uploadFile(
        name = fileName,
        size = fileSize,
        uri = uri.toString(),
        type = fileType
    )
}
