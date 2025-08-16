package me.shadykhalifa.whispertop

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import me.shadykhalifa.whispertop.presentation.activities.PermissionOnboardingActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Check if onboarding should be shown
        if (PermissionOnboardingActivity.shouldShowOnboarding(this)) {
            val intent = Intent(this, PermissionOnboardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        val requestPermissions = intent?.getBooleanExtra("request_permissions", false) ?: false

        setContent {
            App(requestPermissions = requestPermissions)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}