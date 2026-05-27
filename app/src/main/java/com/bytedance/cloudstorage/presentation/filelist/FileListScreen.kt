package com.bytedance.cloudstorage.presentation.filelist

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytedance.cloudstorage.domain.model.CloudFile
import com.bytedance.cloudstorage.domain.model.FileType
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws

// ────────────────────────────────────────────────
// 文件 Tab 主页面
// ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    viewModel: FileListViewModel = viewModel(),
    onOpenVideo: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenTxt: (String, String, String) -> Unit = { _, _, _ -> }
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
    val sheetState = rememberModalBottomSheetState()

    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        uploadSelectedFile(context, uri, viewModel, files)
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
                key(selectedFilterIndex, sortType, sortDirection) {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.w.dp),
                        contentPadding = PaddingValues(
                            bottom = if (isSelectionMode) 180.w.dp else 96.w.dp
                        )
                    ) {
                        items(
                            items = files,
                            key = { it.id },
                            contentType = { it.type.name }
                        ) { file ->
                            FileListItem(
                                file = file,
                                isSelected = file.id in selectedFileIds,
                                isSelectionMode = isSelectionMode,
                                showDivider = file.id != lastFileId,
                                onCircleClick = { viewModel.toggleFileSelection(file.id) },
                                onLongPress = { viewModel.enterSelectionMode(file.id) },
                                onFolderClick = if (file.type == FileType.Folder) { folderId ->
                                    viewModel.navigateIntoFolder(folderId, file.name)
                                } else null,
                                onFileClick = when (file.type) {
                                    FileType.Video -> {
                                        { onOpenVideo(file.id, file.name, file.uri ?: "") }
                                    }
                                    FileType.Txt -> {
                                        { onOpenTxt(file.id, file.name, file.uri ?: "") }
                                    }
                                    else -> null
                                }
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
                    val uniqueName = generateUniqueName(name, files.map { it.name }.toSet())
                    viewModel.createFolder(uniqueName)
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
                    val existingNames = files.filter { it.id != renameTargetFile!!.id }.map { it.name }.toSet()
                    val uniqueName = generateUniqueName(newName, existingNames)
                    viewModel.renameFile(renameTargetFile!!.id, uniqueName)
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
