package me.shadykhalifa.whispertop.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.shadykhalifa.whispertop.presentation.ui.screens.SettingsScreen

object Screen {
    const val Home = "home"
    const val Settings = "settings"
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    requestPermissions: Boolean = false
) {
    NavHost(
        navController = navController,
        startDestination = if (requestPermissions) Screen.Settings else Screen.Home
    ) {
        composable(Screen.Home) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings)
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
    }
}

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "WhisperTop",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onNavigateToSettings
        ) {
            Text("Settings")
        }
    }
}