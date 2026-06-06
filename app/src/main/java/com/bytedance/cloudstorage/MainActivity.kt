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
