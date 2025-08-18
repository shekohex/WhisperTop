package me.shadykhalifa.whispertop

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.di.androidAppModule
import me.shadykhalifa.whispertop.di.initKoin
import me.shadykhalifa.whispertop.di.providePlatformModule
import me.shadykhalifa.whispertop.managers.OverlayInitializationManager
import me.shadykhalifa.whispertop.presentation.activities.PermissionOnboardingActivity
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class MainActivity : ComponentActivity() {
    
    private val overlayInitManager: OverlayInitializationManager by inject()
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val requestPermissions = intent?.getBooleanExtra("request_permissions", false) ?: false
        val showSettings = intent?.getBooleanExtra("show_settings", false) ?: false

        // Check if onboarding should be shown (skip if showing settings directly)
        if (!showSettings && PermissionOnboardingActivity.shouldShowOnboarding(this)) {
            val intent = Intent(this, PermissionOnboardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContent {
            App(requestPermissions = requestPermissions, showSettings = showSettings)
        }
        
        // Initialize overlay after onboarding is complete
        initializeOverlay()
    }
    
    override fun onResume() {
        super.onResume()
        // Check and re-initialize overlay if needed when returning from settings
        lifecycleScope.launch {
            overlayInitManager.checkAndReinitializeIfNeeded()
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle configuration changes (rotation, screen size, etc.)
        overlayInitManager.onConfigurationChanged(newConfig)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up overlay when activity is destroyed
        overlayInitManager.cleanup()
    }
    
    private fun initializeOverlay() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting overlay initialization")
                val success = overlayInitManager.initializeOverlay()
                if (success) {
                    Log.i(TAG, "Overlay initialized successfully")
                } else {
                    Log.w(TAG, "Failed to initialize overlay")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing overlay", e)
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val context = LocalContext.current
    initKoin(listOf(providePlatformModule(context), androidAppModule)) {
        androidLogger()
        androidContext(context)
    }
    App()

}