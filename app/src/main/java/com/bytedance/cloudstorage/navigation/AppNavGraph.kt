package com.bytedance.cloudstorage.navigation

import androidx.annotation.OptIn
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bytedance.cloudstorage.data.share.ShareLinkHandledAction
import com.bytedance.cloudstorage.data.share.ShareLinkStore
import com.bytedance.cloudstorage.presentation.MainScreen
import com.bytedance.cloudstorage.presentation.share.ShareLinkPromptDialog
import com.bytedance.cloudstorage.presentation.share.ShareListScreen
import com.bytedance.cloudstorage.presentation.txtreader.TxtReaderScreen
import com.bytedance.cloudstorage.presentation.videoplayer.VideoPlayerScreen
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

object Screen {
    const val Main = "main"
    const val VideoPlayer = "videoplayer/{fileId}/{fileName}/{fileUri}"
    const val TxtReader = "txtreader/{fileId}/{fileName}/{fileUri}"
    /** 分享文件列表页，token 从 deeplink 或剪贴板解析得到 */
    const val ShareList = "share/{token}"
    private const val EmptyArg = "__empty__"

    fun videoPlayer(fileId: String, fileName: String, fileUri: String): String {
        val id = encodeRouteArg(fileId)
        val name = encodeRouteArg(fileName)
        val uri = encodeRouteArg(fileUri)
        return "videoplayer/$id/$name/$uri"
    }

    fun txtReader(fileId: String, fileName: String, fileUri: String): String {
        val id = encodeRouteArg(fileId)
        val name = encodeRouteArg(fileName)
        val uri = encodeRouteArg(fileUri)
        return "txtreader/$id/$name/$uri"
    }

    fun shareList(token: String): String = "share/${encodeRouteArg(token)}"

    fun decodeRouteArg(value: String): String {
        val decoded = URLDecoder.decode(value, "UTF-8")
        return if (decoded == EmptyArg) "" else decoded
    }

    private fun encodeRouteArg(value: String): String {
        return URLEncoder.encode(value.ifEmpty { EmptyArg }, "UTF-8")
    }
}

// ────────────────────────────────────────────────
// 应用导航图（含剪贴板分享链接检测）
// ────────────────────────────────────────────────

/**
 * 应用顶层导航图。
 *
 * 通过 [CopiedShareLinkObserver] 在应用回到前台时扫描剪贴板，
 * 检测到分享链接后弹窗引导用户进入分享文件列表页。
 *
 * @param navController  导航控制器
 */
@OptIn(UnstableApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
) {
    val context = LocalContext.current

    // ── 剪贴板分享链接检测 ──
    CopiedShareLinkObserver(navController = navController)

    NavHost(
        navController = navController,
        startDestination = Screen.Main
    ) {
        composable(
            route = Screen.Main,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            MainScreen(
                onOpenVideo = { fileId, fileName, fileUri ->
                    navController.navigate(Screen.videoPlayer(fileId, fileName, fileUri))
                },
                onOpenTxt = { fileId, fileName, fileUri ->
                    navController.navigate(Screen.txtReader(fileId, fileName, fileUri))
                },
                onOpenShareLink = { token ->
                    navController.navigate(Screen.shareList(token))
                }
            )
        }
        composable(
            route = Screen.VideoPlayer,
            arguments = listOf(
                navArgument("fileId") { type = NavType.StringType },
                navArgument("fileName") { type = NavType.StringType },
                navArgument("fileUri") { type = NavType.StringType }
            ),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) { backStackEntry ->
            val id = Screen.decodeRouteArg(backStackEntry.arguments?.getString("fileId") ?: "")
            val name = Screen.decodeRouteArg(backStackEntry.arguments?.getString("fileName") ?: "")
            val uri = Screen.decodeRouteArg(backStackEntry.arguments?.getString("fileUri") ?: "")
            VideoPlayerScreen(
                fileId = id,
                fileName = name,
                fileUri = uri,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.TxtReader,
            arguments = listOf(
                navArgument("fileId") { type = NavType.StringType },
                navArgument("fileName") { type = NavType.StringType },
                navArgument("fileUri") { type = NavType.StringType }
            ),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) { backStackEntry ->
            val id = Screen.decodeRouteArg(backStackEntry.arguments?.getString("fileId") ?: "")
            val name = Screen.decodeRouteArg(backStackEntry.arguments?.getString("fileName") ?: "")
            val uri = Screen.decodeRouteArg(backStackEntry.arguments?.getString("fileUri") ?: "")
            TxtReaderScreen(
                fileId = id,
                fileName = name,
                fileUri = uri,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ShareList,
            arguments = listOf(
                navArgument("token") { type = NavType.StringType }
            ),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) { backStackEntry ->
            val token = Screen.decodeRouteArg(backStackEntry.arguments?.getString("token") ?: "")
            ShareListScreen(
                token = token,
                onBack = { navController.popBackStack() },
                onOpenVideo = { fileId, fileName, fileUri ->
                    navController.navigate(Screen.videoPlayer(fileId, fileName, fileUri))
                },
                onOpenTxt = { fileId, fileName, fileUri ->
                    navController.navigate(Screen.txtReader(fileId, fileName, fileUri))
                }
            )
        }
    }
}

// ────────────────────────────────────────────────
// 剪贴板分享链接检测
// ────────────────────────────────────────────────

/**
 * 监听应用回到前台事件，自动扫描系统剪贴板中是否有分享链接。
 *
 * 发现有效链接时弹出 [ShareLinkPromptDialog] 提示用户查看。
 * 已处理过的 token 在 ON_STOP 之前不会重复弹出。
 */
@Composable
private fun CopiedShareLinkObserver(navController: NavHostController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val shareLinkStore = remember(context) { ShareLinkStore(context) }
    val coroutineScope = rememberCoroutineScope()
    var pendingToken by remember { mutableStateOf<String?>(null) }
    var consumedToken by remember { mutableStateOf<String?>(null) }

    fun detectCopiedShareLink() {
        coroutineScope.launch {
            val token = shareLinkStore.readTokenFromClipboard()
            // 仅当 token 有效、未被消费过、且本地有对应分享记录时才弹出
            if (token != null && token != consumedToken && shareLinkStore.shouldAutoPrompt(token)) {
                pendingToken = token
            }
        }
    }

    // 首次进入时检测一次
    LaunchedEffect(shareLinkStore) {
        detectCopiedShareLink()
    }

    // 监听生命周期：回到前台时重新检测，离开时重置消费标记
    DisposableEffect(lifecycleOwner, shareLinkStore) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> detectCopiedShareLink()
                Lifecycle.Event.ON_STOP -> consumedToken = null
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    pendingToken?.let { token ->
        ShareLinkPromptDialog(
            onDismiss = {
                consumedToken = token
                pendingToken = null
                coroutineScope.launch {
                    shareLinkStore.markHandled(token, ShareLinkHandledAction.Dismissed)
                }
            },
            onViewNow = {
                consumedToken = token
                pendingToken = null
                coroutineScope.launch {
                    shareLinkStore.markHandled(token, ShareLinkHandledAction.Opened)
                }
                navController.navigate(Screen.shareList(token))
            }
        )
    }
}
