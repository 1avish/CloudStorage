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
import com.bytedance.cloudstorage.presentation.videoplayer.VideoPlayerScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Screen {
    const val Main = "main"
    const val VideoPlayer = "videoplayer/{fileId}/{fileName}/{fileUri}"

    fun videoPlayer(fileId: String, fileName: String, fileUri: String): String {
        val id = URLEncoder.encode(fileId, "UTF-8")
        val name = URLEncoder.encode(fileName, "UTF-8")
        val uri = URLEncoder.encode(fileUri, "UTF-8")
        return "videoplayer/$id/$name/$uri"
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
            val id = URLDecoder.decode(backStackEntry.arguments?.getString("fileId") ?: "", "UTF-8")
            val name = URLDecoder.decode(backStackEntry.arguments?.getString("fileName") ?: "", "UTF-8")
            val uri = URLDecoder.decode(backStackEntry.arguments?.getString("fileUri") ?: "", "UTF-8")
            VideoPlayerScreen(
                fileId = id,
                fileName = name,
                fileUri = uri,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
