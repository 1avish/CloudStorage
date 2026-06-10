package com.bytedance.cloudstorage.presentation

import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytedance.cloudstorage.data.share.ShareLinkHandledAction
import com.bytedance.cloudstorage.presentation.common.MainTopBar
import com.bytedance.cloudstorage.presentation.common.TextInputBottomSheet
import com.bytedance.cloudstorage.presentation.filelist.BottomSheetContent
import com.bytedance.cloudstorage.presentation.filelist.FileListScreen
import com.bytedance.cloudstorage.presentation.filelist.FileListViewModel
import com.bytedance.cloudstorage.presentation.filelist.SaveLocationPickerSheet
import com.bytedance.cloudstorage.presentation.filelist.generateUniqueName
import com.bytedance.cloudstorage.presentation.filelist.uploadSelectedFile
import com.bytedance.cloudstorage.presentation.home.HomeScreen
import com.bytedance.cloudstorage.utils.w
import kotlinx.coroutines.launch

// ────────────────────────────────────────────────
// 主页面（HorizontalPager + 顶部导航栏）
// ────────────────────────────────────────────────

/**
 * 应用主页面，包含「网盘」和「文件」两个 Tab 的水平滑动切换。
 *
 * 使用 HorizontalPager 实现左右滑动切换，顶部 MainTopBar 提供 Tab 切换入口。
 * 文件 Tab 的 ViewModel 在此处创建并注入，保证文件列表状态在 Tab 切换时保持不变。
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenVideo: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenTxt: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenShareLink: (String) -> Unit = {},
    onOpenTransfer: () -> Unit = {},
    onShowAllViews: () -> Unit = {},
    onShowAllSaves: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fileListViewModel: FileListViewModel = viewModel()
    var showCreateSheet by androidx.compose.runtime.remember { mutableStateOf(false) }
    var showSaveShareSheet by androidx.compose.runtime.remember { mutableStateOf(false) }
    var showNewFolderSheet by androidx.compose.runtime.remember { mutableStateOf(false) }
    var showUploadNewFolderSheet by androidx.compose.runtime.remember { mutableStateOf(false) }
    var pendingUploadUri by androidx.compose.runtime.remember { mutableStateOf<Uri?>(null) }
    val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pagerState = rememberPagerState(pageCount = { 2 })

    val files by fileListViewModel.files.collectAsStateWithLifecycle()
    val showUploadTargetSheet by fileListViewModel.showUploadTargetSheet.collectAsStateWithLifecycle()
    val uploadTargetFolderId by fileListViewModel.uploadTargetFolderId.collectAsStateWithLifecycle()
    val uploadTargetPathStack by fileListViewModel.uploadTargetPathStack.collectAsStateWithLifecycle()
    val uploadTargetFolders by fileListViewModel.uploadTargetFolders.collectAsStateWithLifecycle()
    val uploadTargetFiles by fileListViewModel.uploadTargetFiles.collectAsStateWithLifecycle()
    val hasBack by fileListViewModel.hasBack.collectAsStateWithLifecycle()
    val pathStack by fileListViewModel.pathStack.collectAsStateWithLifecycle()
    val currentFolderName by fileListViewModel.currentFolderName.collectAsStateWithLifecycle()
    val isSelectionMode by fileListViewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedCount by fileListViewModel.selectedCount.collectAsStateWithLifecycle()
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        pendingUploadUri = uri
        fileListViewModel.openUploadTargetSheet()
    }

    LaunchedEffect(fileListViewModel) {
        fileListViewModel.shareTokenLookupResult.collect { token ->
            if (token == null) {
                Toast.makeText(context, "分享链接不存在", Toast.LENGTH_SHORT).show()
            } else {
                fileListViewModel.markShareLinkHandled(token, ShareLinkHandledAction.Opened)
                showSaveShareSheet = false
                onOpenShareLink(token)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            MainTopBar(
                selectedTab = pagerState.currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                transferTaskCount = 0,
                onTransferClick = onOpenTransfer,
                hasBack = hasBack,
                onBackClick = { fileListViewModel.navigateBack() },
                currentFolderName = currentFolderName,
                pathSegments = pathStack,
                onPathClick = { index -> fileListViewModel.navigateToPathIndex(index) },
                isSelectionMode = isSelectionMode,
                selectedCount = selectedCount,
                onCancelSelection = { fileListViewModel.exitSelectionMode() },
                onSelectAll = { fileListViewModel.selectAllFiles() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !hasBack && !isSelectionMode,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> HomeScreen(
                        onOpenVideo = onOpenVideo,
                        onOpenTxt = onOpenTxt,
                        onShowAllViews = onShowAllViews,
                        onShowAllSaves = onShowAllSaves,
                    )
                    1 -> FileListScreen(
                        viewModel = fileListViewModel,
                        onOpenVideo = onOpenVideo,
                        onOpenTxt = onOpenTxt,
                        onOpenShareLink = onOpenShareLink
                    )
                }
            }

            if (!isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-36).w.dp, y = (-62).w.dp)
                        .size(56.w.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.5f),
                            spotColor = Color.Black.copy(alpha = 0.5f)
                        )
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { showCreateSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "创建",
                        tint = Color(0xFF111111),
                        modifier = Modifier.size(34.w.dp)
                    )
                }
            }
        }
    }

    if (showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = createSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = null
        ) {
            BottomSheetContent(
                onDismiss = { showCreateSheet = false },
                onSaveShare = {
                    showCreateSheet = false
                    showSaveShareSheet = true
                },
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
                    filePickerLauncher.launch(arrayOf("text/plain"))
                }
            )
        }
    }

    if (showSaveShareSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSaveShareSheet = false },
            sheetState = createSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = null
        ) {
            TextInputBottomSheet(
                title = "保存分享",
                placeholder = "请输入分享链接",
                onDismiss = { showSaveShareSheet = false },
                onConfirm = { link ->
                    fileListViewModel.findExistingShareToken(link)
                }
            )
        }
    }

    if (showNewFolderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNewFolderSheet = false },
            sheetState = createSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = null
        ) {
            TextInputBottomSheet(
                title = "新建文件夹",
                placeholder = "请输入文件夹名",
                onDismiss = { showNewFolderSheet = false },
                onConfirm = { name ->
                    val uniqueName = generateUniqueName(name, files.map { it.name }.toSet())
                    fileListViewModel.createFolder(uniqueName)
                    showNewFolderSheet = false
                }
            )
        }
    }

    if (showUploadTargetSheet && pendingUploadUri != null) {
        ModalBottomSheet(
            onDismissRequest = {
                fileListViewModel.closeUploadTargetSheet()
                pendingUploadUri = null
            },
            sheetState = createSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = null
        ) {
            SaveLocationPickerSheet(
                pathStack = uploadTargetPathStack,
                folders = uploadTargetFolders,
                onNavigateInto = { id, name -> fileListViewModel.navigateUploadIntoFolder(id, name) },
                onNavigateBack = { fileListViewModel.navigateUploadBack() },
                onPathClick = { index -> fileListViewModel.navigateUploadToPathIndex(index) },
                onConfirmUpload = {
                    pendingUploadUri?.let { uri ->
                        uploadSelectedFile(
                            context = context,
                            uri = uri,
                            viewModel = fileListViewModel,
                            existingFiles = uploadTargetFiles,
                            parentId = uploadTargetFolderId,
                        )
                    }
                    pendingUploadUri = null
                    fileListViewModel.closeUploadTargetSheet()
                },
                onDismiss = {
                    pendingUploadUri = null
                    fileListViewModel.closeUploadTargetSheet()
                },
                onNewFolder = {
                    fileListViewModel.closeUploadTargetSheet()
                    showUploadNewFolderSheet = true
                },
            )
        }
    }

    if (showUploadNewFolderSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showUploadNewFolderSheet = false
                fileListViewModel.reopenUploadTargetSheet()
            },
            sheetState = createSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = null
        ) {
            TextInputBottomSheet(
                title = "新建文件夹",
                placeholder = "请输入文件夹名",
                onDismiss = {
                    showUploadNewFolderSheet = false
                    fileListViewModel.reopenUploadTargetSheet()
                },
                onConfirm = { name ->
                    fileListViewModel.createFolderInUploadTarget(name)
                    showUploadNewFolderSheet = false
                    fileListViewModel.reopenUploadTargetSheet()
                }
            )
        }
    }
}
