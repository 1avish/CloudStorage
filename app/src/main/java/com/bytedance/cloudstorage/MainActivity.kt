package com.bytedance.cloudstorage

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.bytedance.cloudstorage.navigation.AppNavGraph
import com.bytedance.cloudstorage.ui.theme.CloudStorageTheme
import com.bytedance.cloudstorage.utils.ScreenUtils

/**
 * 应用入口 Activity
 *
 * 职责：锁定竖屏方向、初始化屏幕适配工具、渲染顶层 Compose 内容。
 * 所有页面导航由 AppNavGraph 管理。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // 初始化 ScreenUtils，基准设计稿宽度 390px
        ScreenUtils.init(this, designWidth = 390f)

        enableEdgeToEdge()
        setContent {
            CloudStorageTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}
