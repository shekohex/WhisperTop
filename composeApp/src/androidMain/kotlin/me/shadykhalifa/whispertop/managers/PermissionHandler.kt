package me.shadykhalifa.whispertop.managers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PermissionHandler : KoinComponent {
    
    private val context: Context by inject()
    
    private val _permissionState = MutableStateFlow(PermissionState.UNKNOWN)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    
    private val requiredPermissions = mutableListOf<String>().apply {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        add(Manifest.permission.FOREGROUND_SERVICE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
        }
    }
    
    private val overlayPermissions = listOf(
        Manifest.permission.SYSTEM_ALERT_WINDOW
    )
    
    init {
        updatePermissionState()
    }
    
    fun checkAllPermissions(): PermissionCheckResult {
        val deniedPermissions = mutableListOf<String>()
        val rationalePermissions = mutableListOf<String>()
        
        for (permission in requiredPermissions) {
            when {
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission granted
                }
                else -> {
                    deniedPermissions.add(permission)
                }
            }
        }
        
        // Check overlay permission separately
        val overlayPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        
        if (!overlayPermissionGranted) {
            deniedPermissions.add(Manifest.permission.SYSTEM_ALERT_WINDOW)
        }
        
        return when {
            deniedPermissions.isEmpty() -> PermissionCheckResult.ALL_GRANTED
            else -> PermissionCheckResult.SOME_DENIED(deniedPermissions)
        }
    }
    
    suspend fun requestAllPermissions(): PermissionResult {
        val checkResult = checkAllPermissions()
        
        return when (checkResult) {
            is PermissionCheckResult.ALL_GRANTED -> {
                updatePermissionState()
                PermissionResult.GRANTED
            }
            is PermissionCheckResult.SOME_DENIED -> {
                val regularPermissions = checkResult.deniedPermissions.filter { 
                    it != Manifest.permission.SYSTEM_ALERT_WINDOW 
                }
                val needsOverlayPermission = checkResult.deniedPermissions.contains(Manifest.permission.SYSTEM_ALERT_WINDOW)
                
                // Handle regular permissions
                val rationalePermissions = mutableListOf<String>()
                val currentContext = context
                if (regularPermissions.isNotEmpty() && currentContext is Activity) {
                    for (permission in regularPermissions) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(currentContext, permission)) {
                            rationalePermissions.add(permission)
                        }
                    }
                    
                    if (rationalePermissions.isNotEmpty()) {
                        return PermissionResult.SHOW_RATIONALE(rationalePermissions)
                    }
                }
                
                // If we get here, either no rationale needed or overlay permission needed
                PermissionResult.DENIED(checkResult.deniedPermissions)
            }
        }
    }
    
    fun requestPermissions(permissions: Array<String>, requestCode: Int = PERMISSION_REQUEST_CODE) {
        val currentContext = context
        if (currentContext is Activity) {
            ActivityCompat.requestPermissions(currentContext, permissions, requestCode)
        }
    }
    
    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
    
    fun onPermissionResult(permissions: Array<out String>, grantResults: IntArray): PermissionResult {
        val deniedPermissions = mutableListOf<String>()
        
        for (i in permissions.indices) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i])
            }
        }
        
        updatePermissionState()
        
        return if (deniedPermissions.isEmpty()) {
            PermissionResult.GRANTED
        } else {
            PermissionResult.DENIED(deniedPermissions)
        }
    }
    
    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for older versions
        }
    }
    
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    fun hasForegroundServicePermission(): Boolean {
        val foregroundServiceGranted = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.FOREGROUND_SERVICE
        ) == PackageManager.PERMISSION_GRANTED
        
        val microphoneServiceGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.FOREGROUND_SERVICE_MICROPHONE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        return foregroundServiceGranted && microphoneServiceGranted
    }
    
    private fun updatePermissionState() {
        val checkResult = checkAllPermissions()
        _permissionState.value = when (checkResult) {
            is PermissionCheckResult.ALL_GRANTED -> PermissionState.GRANTED
            is PermissionCheckResult.SOME_DENIED -> PermissionState.DENIED
        }
    }
    
    fun getPermissionStatusSummary(): PermissionStatusSummary {
        return PermissionStatusSummary(
            audioRecording = hasAudioPermission(),
            notifications = hasNotificationPermission(),
            overlay = hasOverlayPermission(),
            foregroundService = hasForegroundServicePermission()
        )
    }
    
    enum class PermissionState {
        UNKNOWN,
        GRANTED,
        DENIED
    }
    
    sealed class PermissionCheckResult {
        object ALL_GRANTED : PermissionCheckResult()
        data class SOME_DENIED(val deniedPermissions: List<String>) : PermissionCheckResult()
    }
    
    sealed class PermissionResult {
        object GRANTED : PermissionResult()
        data class DENIED(val deniedPermissions: List<String>) : PermissionResult()
        data class SHOW_RATIONALE(val permissions: List<String>) : PermissionResult()
    }
    
    data class PermissionStatusSummary(
        val audioRecording: Boolean,
        val notifications: Boolean,
        val overlay: Boolean,
        val foregroundService: Boolean
    ) {
        val allGranted: Boolean get() = audioRecording && notifications && overlay && foregroundService
        val anyDenied: Boolean get() = !allGranted
    }
    
    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
        const val OVERLAY_PERMISSION_REQUEST_CODE = 1002
    }
}