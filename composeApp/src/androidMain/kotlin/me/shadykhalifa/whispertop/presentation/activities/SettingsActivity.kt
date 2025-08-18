package me.shadykhalifa.whispertop.presentation.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.shadykhalifa.whispertop.presentation.ui.screens.SettingsScreen
import me.shadykhalifa.whispertop.presentation.ui.theme.WhisperTopTheme
import me.shadykhalifa.whispertop.presentation.viewmodels.SettingsViewModel
import org.koin.android.ext.android.inject

/**
 * Standalone Settings Activity that can be launched directly
 * Usage: adb shell am start -n me.shadykhalifa.whispertop.sideload.debug/.presentation.activities.SettingsActivity
 */
class SettingsActivity : ComponentActivity() {
    
    private val settingsViewModel: SettingsViewModel by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        setContent {
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            
            WhisperTopTheme(
                theme = settingsState.settings.theme,
                dynamicColor = true
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        onNavigateBack = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}