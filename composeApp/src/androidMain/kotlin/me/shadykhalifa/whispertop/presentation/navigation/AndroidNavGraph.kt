package me.shadykhalifa.whispertop.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.shadykhalifa.whispertop.presentation.ui.screens.SettingsScreen
import me.shadykhalifa.whispertop.presentation.ui.screens.PermissionsDashboardScreen
import me.shadykhalifa.whispertop.presentation.ui.screens.AndroidHomeScreen

@Composable
fun AndroidNavGraph(
    navController: NavHostController = rememberNavController(),
    requestPermissions: Boolean = false,
    showSettings: Boolean = false
) {
    NavHost(
        navController = navController,
        startDestination = when {
            requestPermissions -> Screen.Permissions
            showSettings -> Screen.Settings
            else -> Screen.Home
        }
    ) {
        composable(Screen.Home) {
            AndroidHomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings)
                },
                onNavigateToPermissions = {
                    navController.navigate(Screen.Permissions)
                }
            )
        }
        
        composable(Screen.Settings) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Permissions) {
            PermissionsDashboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}