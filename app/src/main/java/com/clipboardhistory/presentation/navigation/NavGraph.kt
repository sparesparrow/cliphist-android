package com.clipboardhistory.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clipboardhistory.presentation.ui.screens.ItemDetailScreen
import com.clipboardhistory.presentation.ui.screens.MainScreen
import com.clipboardhistory.presentation.ui.screens.SettingsScreen
import com.clipboardhistory.presentation.ui.screens.SmartActionsScreen
import com.clipboardhistory.presentation.ui.screens.StatisticsScreen
import com.clipboardhistory.presentation.viewmodels.MainViewModel

/**
 * Navigation graph for the ClipHist application.
 *
 * Defines all navigation routes and their destinations.
 */
sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Settings : Screen("settings")
    object Statistics : Screen("statistics")
    object ItemDetail : Screen("item_detail/{itemId}") {
        fun createRoute(itemId: Long) = "item_detail/$itemId"
    }
    object SmartActions : Screen("smart_actions/{itemId}") {
        fun createRoute(itemId: Long) = "smart_actions/$itemId"
    }
}

/**
 * Main navigation host for the application.
 *
 * @param navController The navigation controller
 * @param viewModel The main view model
 * @param onStartServices Callback to start services
 * @param onStopServices Callback to stop services
 * @param modifier Modifier for the navigation host
 */
@Composable
fun ClipHistNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel,
    onStartServices: () -> Unit = {},
    onStopServices: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
        modifier = modifier,
    ) {
        // Main clipboard history screen - simplified navigation for now
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    // Simplified - just stay on main screen for now
                },
                onNavigateToStatistics = {
                    // Simplified - just stay on main screen for now
                },
                onNavigateToItemDetail = { itemId ->
                    // Simplified - just stay on main screen for now
                },
                onNavigateToSmartActions = { itemId ->
                    // Simplified - just stay on main screen for now
                },
                onStartServices = onStartServices,
                onStopServices = onStopServices,
            )
        }
    }
}