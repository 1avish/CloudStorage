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
import com.bytedance.cloudstorage.presentation.home.FileType
import com.bytedance.cloudstorage.presentation.home.HomeScreen
import com.bytedance.cloudstorage.presentation.home.RecentFileItem
import com.bytedance.cloudstorage.ui.theme.CloudStorageTheme
import com.bytedance.cloudstorage.utils.ScreenUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 ScreenUtils，基准设计稿宽度 390px
        ScreenUtils.init(this, designWidth = 390f)

        enableEdgeToEdge()
        setContent {
            CloudStorageTheme {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                // 两个 Tab 对应的页面：0=网盘，1=文件
                val pagerState = rememberPagerState(pageCount = { 2 })

                // ── 测试数据，后续由 ViewModel + Room 提供 ──
                val now = System.currentTimeMillis()
                val mockRecentViews = listOf(
                    RecentFileItem(
                        name = "需求说明.txt",
                        type = FileType.TXT,
                        timestamp = now - 9 * 60 * 1000,   // 9 分钟前
                        location = "项目资料"
                    ),
                    RecentFileItem(
                        name = "产品演示.mp4",
                        type = FileType.VIDEO,
                        timestamp = now - 15 * 60 * 1000,  // 15 分钟前
                        location = "视频"
                    ),
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        MainTopBar(
                            selectedTab = pagerState.currentPage,
                            onTabSelected = { index ->
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            transferTaskCount = 0,
                            onTransferClick = {
                                Toast.makeText(context, "传输页（待实现）", Toast.LENGTH_SHORT).show()
                            },
                            onSearchClick = {
                                Toast.makeText(context, "搜索（待实现）", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) { innerPadding ->
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) { page ->
                        when (page) {
                            0 -> HomeScreen(
                                usedStorageG = 4.9f,
                                totalStorageG = 10f,
                                recentSaves = emptyList(),  // 空状态
                                recentViews = mockRecentViews,
                            )
                            1 -> FileListScreen()
                        }
                    }
                }
            }
        }
    }
}
