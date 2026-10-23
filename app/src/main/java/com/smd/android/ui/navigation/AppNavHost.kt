package com.smd.android.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.smd.android.ui.screens.*
import com.smd.android.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Upload : Screen("upload")
    object DepotSelect : Screen("depot_select")
    object Download : Screen("download")
    object History : Screen("history")
    object Queue : Screen("queue")
    object Settings : Screen("settings")
    object ServerManager : Screen("server_manager")
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    pendingFileUri: Uri?,
    modifier: Modifier = Modifier
) {
    val viewModel: MainViewModel = viewModel()

    // If app was opened with a file URI, parse it immediately
    if (pendingFileUri != null) {
        viewModel.parseFile(pendingFileUri)
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Upload.route,
        modifier = modifier
    ) {
        composable(Screen.Upload.route) {
            UploadScreen(
                viewModel = viewModel,
                onNavigateToDepotSelect = { navController.navigate(Screen.DepotSelect.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToQueue = { navController.navigate(Screen.Queue.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.DepotSelect.route) {
            DepotSelectScreen(
                viewModel = viewModel,
                onNavigateToDownload = { navController.navigate(Screen.Download.route) },
                onNavigateBack = { navController.popBackStack() },
                onAddToQueue = {
                    viewModel.addToQueue()
                    navController.navigate(Screen.Queue.route) {
                        popUpTo(Screen.Upload.route)
                    }
                }
            )
        }
        composable(Screen.Download.route) {
            DownloadScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onDone = {
                    navController.navigate(Screen.History.route) {
                        popUpTo(Screen.Upload.route)
                    }
                }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Queue.route) {
            QueueScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToServerManager = { navController.navigate(Screen.ServerManager.route) }
            )
        }
        composable(Screen.ServerManager.route) {
            ServerManagerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
