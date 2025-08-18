package me.shadykhalifa.whispertop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import me.shadykhalifa.whispertop.presentation.navigation.NavGraph
import me.shadykhalifa.whispertop.presentation.ui.theme.WhisperTopTheme
import me.shadykhalifa.whispertop.presentation.viewmodels.SettingsViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App(requestPermissions: Boolean = false, showSettings: Boolean = false) {
    val settingsViewModel: SettingsViewModel = koinInject()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    
    WhisperTopTheme(
        theme = settingsState.settings.theme,
        dynamicColor = true
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            NavGraph(
                navController = navController,
                requestPermissions = requestPermissions,
                showSettings = showSettings
            )
        }
    }
}