package com.bytedance.cloudstorage.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytedance.cloudstorage.presentation.common.MainTopBar
import com.bytedance.cloudstorage.presentation.filelist.FileListScreen
import com.bytedance.cloudstorage.presentation.filelist.FileListViewModel
import com.bytedance.cloudstorage.presentation.home.HomeScreen
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
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    onOpenVideo: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenTxt: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenShareLink: (String) -> Unit = {},
    onOpenTransfer: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fileListViewModel: FileListViewModel = viewModel()

    val pagerState = rememberPagerState(pageCount = { 2 })

    val hasBack by fileListViewModel.hasBack.collectAsStateWithLifecycle()
    val pathStack by fileListViewModel.pathStack.collectAsStateWithLifecycle()
    val currentFolderName by fileListViewModel.currentFolderName.collectAsStateWithLifecycle()
    val isSelectionMode by fileListViewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedCount by fileListViewModel.selectedCount.collectAsStateWithLifecycle()

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
                onSearchClick = {
                    Toast.makeText(context, "搜索（待实现）", Toast.LENGTH_SHORT).show()
                },
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
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !hasBack && !isSelectionMode,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> HomeScreen(
                    onOpenVideo = onOpenVideo,
                    onOpenTxt = onOpenTxt
                )
                1 -> FileListScreen(
                    viewModel = fileListViewModel,
                    onOpenVideo = onOpenVideo,
                    onOpenTxt = onOpenTxt,
                    onOpenShareLink = onOpenShareLink
                )
            }
        }
    }
}
