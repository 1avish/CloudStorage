package com.bytedance.cloudstorage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.bytedance.cloudstorage.navigation.AppNavGraph
import com.bytedance.cloudstorage.ui.theme.CloudStorageTheme
import com.bytedance.cloudstorage.utils.ScreenUtils

class MainActivity : ComponentActivity() {
    /** Deeplink URI，通过 Compose state 驱动导航 */
    private var deepLinkUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 冷启动时获取 deeplink
        deepLinkUri = intent?.data

        // 初始化 ScreenUtils，基准设计稿宽度 390px
        ScreenUtils.init(this, designWidth = 390f)

        enableEdgeToEdge()
        setContent {
            CloudStorageTheme {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    deepLinkUri = deepLinkUri,
                    onDeepLinkConsumed = { deepLinkUri = null }
                )
            }
        }
    }

    /** 热启动时通过 deeplink 再次打开应用 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkUri = intent.data
    }
}
