package me.shadykhalifa.whispertop.managers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.shadykhalifa.whispertop.presentation.models.IndividualPermissionState
import me.shadykhalifa.whispertop.presentation.models.OnboardingPermissionState
import me.shadykhalifa.whispertop.presentation.models.PermissionState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OnboardingPermissionManager : KoinComponent {
    
    private val context: Context by inject()
    private val existingPermissionHandler: PermissionHandler by inject()
    private val systemSettingsProvider: SystemSettingsProvider by inject()
    
    private val _onboardingPermissionState = MutableStateFlow(OnboardingPermissionState())
    val onboardingPermissionState: StateFlow<OnboardingPermissionState> = _onboardingPermissionState.asStateFlow()
    
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var overlayPermissionLauncher: ActivityResultLauncher<Intent>? = null
    private var accessibilityPermissionLauncher: ActivityResultLauncher<Intent>? = null
    
    fun initialize(activity: Activity) {
        permissionLauncher = (activity as androidx.activity.ComponentActivity).registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResults(permissions)
        }
        
        overlayPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            updateOverlayPermissionState()
        }
        
        accessibilityPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            updateAccessibilityPermissionState()
        }
        
        refreshAllPermissionStates()
    }
    
    fun refreshAllPermissionStates() {
        val currentState = _onboardingPermissionState.value
        
        _onboardingPermissionState.value = currentState.copy(
            audioRecording = checkIndividualPermission(Manifest.permission.RECORD_AUDIO),
            notifications = checkIndividualPermission(
                Manifest.permission.POST_NOTIFICATIONS,
                isRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ),
            foregroundService = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                checkIndividualPermission(Manifest.permission.FOREGROUND_SERVICE)
            } else IndividualPermissionState(
                Manifest.permission.FOREGROUND_SERVICE,
                PermissionState.Granted,
                isRequired = false
            ),
            foregroundServiceMicrophone = checkIndividualPermission(
                Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
                isRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ),
            overlay = checkOverlayPermission(),
            accessibility = checkAccessibilityPermission()
        )
    }
    
    private fun checkIndividualPermission(permission: String, isRequired: Boolean = true): IndividualPermissionState {
        val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        val currentContext = context
        val isPermanentlyDenied = if (currentContext is Activity) {
            !isGranted && !ActivityCompat.shouldShowRequestPermissionRationale(currentContext, permission)
        } else {
            false
        }
        
        val state = when {
            isGranted -> PermissionState.Granted
            isPermanentlyDenied && context is Activity -> PermissionState.PermanentlyDenied
            !isGranted -> PermissionState.Denied
            else -> PermissionState.NotRequested
        }
        
        return IndividualPermissionState(permission, state, isRequired)
    }
    
    private fun checkOverlayPermission(): IndividualPermissionState {
        val isGranted = systemSettingsProvider.canDrawOverlays()
        val state = if (isGranted) PermissionState.Granted else PermissionState.NotRequested
        return IndividualPermissionState(Manifest.permission.SYSTEM_ALERT_WINDOW, state)
    }
    
    private fun checkAccessibilityPermission(): IndividualPermissionState {
        val isEnabled = isAccessibilityServiceEnabled()
        val state = if (isEnabled) PermissionState.Granted else PermissionState.NotRequested
        return IndividualPermissionState("accessibility_service", state)
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = systemSettingsProvider.isAccessibilityEnabled()
        
        if (!accessibilityEnabled) return false
        
        val enabledServices = systemSettingsProvider.getEnabledAccessibilityServices() ?: return false
        
        val packageName = context.packageName
        val serviceName = "me.shadykhalifa.whispertop.service.WhisperTopAccessibilityService"
        val serviceNameShort = "$packageName/$serviceName"
        val serviceNameFull = "$packageName/.service.WhisperTopAccessibilityService"
        
        return enabledServices.contains(serviceNameShort) || enabledServices.contains(serviceNameFull)
    }
    
    fun requestAudioPermission() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
        }
        
        permissionLauncher?.launch(permissions.toTypedArray())
    }
    
    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            overlayPermissionLauncher?.launch(intent)
        }
    }
    
    fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        accessibilityPermissionLauncher?.launch(intent)
    }
    
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        refreshAllPermissionStates()
    }
    
    private fun updateOverlayPermissionState() {
        val currentState = _onboardingPermissionState.value
        _onboardingPermissionState.value = currentState.copy(
            overlay = checkOverlayPermission()
        )
    }
    
    private fun updateAccessibilityPermissionState() {
        val currentState = _onboardingPermissionState.value
        _onboardingPermissionState.value = currentState.copy(
            accessibility = checkAccessibilityPermission()
        )
    }
    
    fun shouldShowRationale(permission: String): Boolean {
        val currentContext = context
        return if (currentContext is Activity) {
            ActivityCompat.shouldShowRequestPermissionRationale(currentContext, permission)
        } else {
            false
        }
    }
    
    fun getPermissionDenialReason(permission: String): PermissionDenialReason {
        val currentState = _onboardingPermissionState.value
        
        val permissionState = when (permission) {
            Manifest.permission.RECORD_AUDIO -> currentState.audioRecording
            Manifest.permission.SYSTEM_ALERT_WINDOW -> currentState.overlay
            Manifest.permission.POST_NOTIFICATIONS -> currentState.notifications
            Manifest.permission.FOREGROUND_SERVICE -> currentState.foregroundService
            Manifest.permission.FOREGROUND_SERVICE_MICROPHONE -> currentState.foregroundServiceMicrophone
            "accessibility_service" -> currentState.accessibility
            else -> return PermissionDenialReason.NotRequested
        }
        
        return when (permissionState.state) {
            PermissionState.Granted -> PermissionDenialReason.Granted
            PermissionState.PermanentlyDenied -> PermissionDenialReason.PermanentlyDenied
            PermissionState.Denied -> if (shouldShowRationale(permission)) {
                PermissionDenialReason.NeedsRationale
            } else {
                PermissionDenialReason.Denied
            }
            PermissionState.NotRequested -> PermissionDenialReason.NotRequested
        }
    }
    
    enum class PermissionDenialReason {
        Granted,
        NotRequested,
        Denied,
        PermanentlyDenied,
        NeedsRationale
    }
}