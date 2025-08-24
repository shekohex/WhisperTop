package me.shadykhalifa.whispertop.presentation.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.MainActivity
import me.shadykhalifa.whispertop.managers.OnboardingPermissionManager
import me.shadykhalifa.whispertop.presentation.ui.screens.OnboardingScreen
import me.shadykhalifa.whispertop.presentation.viewmodels.OnboardingViewModel
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlinx.coroutines.runBlocking

class PermissionOnboardingActivity : ComponentActivity() {
    
    private val onboardingViewModel: OnboardingViewModel by viewModel()
    private val permissionManager: OnboardingPermissionManager by inject()
    private val settingsRepository: SettingsRepository by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Initialize permission manager with this activity
        permissionManager.initialize(this)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeContentPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navigationEvent by onboardingViewModel.navigationEvent.collectAsState(initial = null)
                    
                    OnboardingScreen(
                        viewModel = onboardingViewModel
                    )
                    
                    // Handle navigation events
                    LaunchedEffect(navigationEvent) {
                        navigationEvent?.let { event ->
                            when (event) {
                                is OnboardingViewModel.NavigationEvent.CompleteOnboarding -> {
                                    completeOnboarding()
                                }
                                is OnboardingViewModel.NavigationEvent.ShowError -> {
                                    // Show error (could be implemented with SnackBar or Toast)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Observe navigation events in lifecycle scope
        lifecycleScope.launch {
            onboardingViewModel.navigationEvent.collect { event ->
                when (event) {
                    is OnboardingViewModel.NavigationEvent.CompleteOnboarding -> {
                        completeOnboarding()
                    }
                    is OnboardingViewModel.NavigationEvent.ShowError -> {
                        // Handle error display if needed
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh permissions when returning from settings
        onboardingViewModel.refreshPermissions()
    }
    
    private fun completeOnboarding() {
        // Mark onboarding as complete in preferences
        val sharedPrefs = getSharedPreferences("whispertop_prefs", MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
        
        // Check if WPM onboarding should be shown next
        val shouldShowWmpOnboarding: Boolean = runBlocking {
            !settingsRepository.isWpmOnboardingCompleted()
        }
        
        // Navigate to main activity with appropriate flags
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (shouldShowWmpOnboarding) {
                putExtra("show_wpm_onboarding", true)
            }
        }
        startActivity(intent)
        finish()
    }
    
    companion object {
        fun shouldShowOnboarding(activity: ComponentActivity): Boolean {
            val sharedPrefs = activity.getSharedPreferences("whispertop_prefs", MODE_PRIVATE)
            return !sharedPrefs.getBoolean("onboarding_completed", false)
        }
    }
}