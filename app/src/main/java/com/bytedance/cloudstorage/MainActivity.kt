package com.bytedance.cloudstorage

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bytedance.cloudstorage.presentation.common.MainTopBar
import com.bytedance.cloudstorage.presentation.filelist.FileListScreen
import com.bytedance.cloudstorage.presentation.home.HomeScreen
import com.bytedance.cloudstorage.ui.theme.CloudStorageTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CloudStorageTheme {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                // 两个 Tab 对应的页面：0=网盘，1=文件
                val pagerState = rememberPagerState(pageCount = { 2 })

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        MainTopBar(
                            selectedTab = pagerState.currentPage,
                            onTabSelected = { index ->
                                // 点击 Tab → 平滑滚动到对应页面
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            transferTaskCount = 0, // MVP 阶段无真实任务，后续从 ViewModel 获取
                            onTransferClick = {
                                // MVP 阶段仅 Toast，后续跳转到 TransferScreen
                                Toast.makeText(context, "传输页（待实现）", Toast.LENGTH_SHORT).show()
                            },
                            onSearchClick = {
                                // MVP 阶段仅 Toast，后续跳转到搜索页
                                Toast.makeText(context, "搜索（待实现）", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) { innerPadding ->
                    // HorizontalPager 托管网盘和文件两个页面
                    // 滑动页面时 Tab 高亮通过 pagerState.currentPage 自动同步
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) { page ->
                        when (page) {
                            0 -> HomeScreen()
                            1 -> FileListScreen()
                        }
                    }
                }
            }
        }
    }
}
