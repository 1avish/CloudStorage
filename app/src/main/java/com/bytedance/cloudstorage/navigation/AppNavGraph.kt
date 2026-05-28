package com.bytedance.cloudstorage.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bytedance.cloudstorage.presentation.MainScreen
import com.bytedance.cloudstorage.presentation.txtreader.TxtReaderScreen
import com.bytedance.cloudstorage.presentation.videoplayer.VideoPlayerScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Screen {
    const val Main = "main"
    const val VideoPlayer = "videoplayer/{fileId}/{fileName}/{fileUri}"
    const val TxtReader = "txtreader/{fileId}/{fileName}/{fileUri}"
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

    fun decodeRouteArg(value: String): String {
        val decoded = URLDecoder.decode(value, "UTF-8")
        return if (decoded == EmptyArg) "" else decoded
    }

    private fun encodeRouteArg(value: String): String {
        return URLEncoder.encode(value.ifEmpty { EmptyArg }, "UTF-8")
    }
}

@Composable
fun AppNavGraph(navController: NavHostController) {
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
    }
}
