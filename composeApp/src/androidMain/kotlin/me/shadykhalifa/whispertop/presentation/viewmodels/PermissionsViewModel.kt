package me.shadykhalifa.whispertop.presentation.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.data.permissions.PermissionMonitor
import me.shadykhalifa.whispertop.domain.models.AppPermission
import me.shadykhalifa.whispertop.domain.models.PermissionResult
import me.shadykhalifa.whispertop.domain.models.PermissionState
import me.shadykhalifa.whispertop.domain.models.*
import androidx.lifecycle.ViewModel
import me.shadykhalifa.whispertop.domain.repositories.PermissionRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PermissionsViewModel(
    private val context: Context,
    private val permissionRepository: PermissionRepository,
    private val permissionMonitor: PermissionMonitor
) : ViewModel(), KoinComponent {

    companion object {
        private const val TAG = "PermissionsViewModel"
    }

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    private val _permissionStates = MutableStateFlow<Map<AppPermission, PermissionState>>(emptyMap())
    val permissionStates: StateFlow<Map<AppPermission, PermissionState>> = _permissionStates.asStateFlow()

    private val _criticalPermissionsMissing = MutableStateFlow(true)
    val criticalPermissionsMissing: StateFlow<Boolean> = _criticalPermissionsMissing.asStateFlow()

    private val _optionalPermissionsMissing = MutableStateFlow(false)
    val optionalPermissionsMissing: StateFlow<Boolean> = _optionalPermissionsMissing.asStateFlow()

    private val _lastPermissionResult = MutableStateFlow<PermissionResult?>(null)
    val lastPermissionResult: StateFlow<PermissionResult?> = _lastPermissionResult.asStateFlow()

    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null
    private var requestOverlayPermissionLauncher: ActivityResultLauncher<Intent>? = null
    private var requestAccessibilityPermissionLauncher: ActivityResultLauncher<Intent>? = null
    
    private var pendingPermissionRequest: AppPermission? = null

    init {
        observePermissionStates()
        startMonitoring()
    }

    fun registerActivityResultLaunchers(activity: ComponentActivity) {
        requestPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            handlePermissionResult(isGranted)
        }

        requestOverlayPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            handleOverlayPermissionResult()
        }

        requestAccessibilityPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            handleAccessibilityPermissionResult()
        }
    }

    private fun observePermissionStates() {
        viewModelScope.launch {
            permissionMonitor.permissionStates.collect { states ->
                _permissionStates.value = states
                updatePermissionSummary(states)
            }
        }

        viewModelScope.launch {
            combine(
                _permissionStates,
                _uiState
            ) { states, uiState ->
                val criticalMissing = !permissionMonitor.areAllCriticalPermissionsGranted()
                val (optionalGranted, optionalTotal) = permissionMonitor.getOptionalPermissionsStatus()
                val optionalMissing = optionalGranted < optionalTotal

                _criticalPermissionsMissing.value = criticalMissing
                _optionalPermissionsMissing.value = optionalMissing

                _uiState.value = uiState.copy(
                    allCriticalPermissionsGranted = !criticalMissing,
                    hasOptionalPermissions = optionalTotal > 0,
                    optionalPermissionsGranted = optionalGranted,
                    totalOptionalPermissions = optionalTotal
                )
            }.collect { }
        }
    }

    private fun updatePermissionSummary(states: Map<AppPermission, PermissionState>) {
        val applicablePermissions = AppPermission.getPermissionsForApiLevel(Build.VERSION.SDK_INT)
        val categorized = applicablePermissions.groupBy { it.isCritical }
        
        val criticalPermissions = categorized[true] ?: emptyList()
        val optionalPermissions = categorized[false] ?: emptyList()
        
        val criticalGranted = criticalPermissions.count { permission ->
            states[permission]?.isGranted == true
        }
        
        val optionalGranted = optionalPermissions.count { permission ->
            states[permission]?.isGranted == true
        }

        _uiState.value = _uiState.value.copy(
            criticalPermissionsGranted = criticalGranted,
            totalCriticalPermissions = criticalPermissions.size,
            optionalPermissionsGranted = optionalGranted,
            totalOptionalPermissions = optionalPermissions.size,
            allCriticalPermissionsGranted = criticalGranted == criticalPermissions.size
        )
    }

    fun startMonitoring() {
        permissionMonitor.startMonitoring()
    }

    fun stopMonitoring() {
        permissionMonitor.stopMonitoring()
    }

    fun requestPermission(permission: AppPermission) {
        viewModelScope.launch {
            val result = requestPermissionSuspend(permission)
            _lastPermissionResult.value = result
        }
    }
    
    private suspend fun requestPermissionSuspend(permission: AppPermission): PermissionResult {
        val currentState = _permissionStates.value[permission]
        
        // Check if already granted
        if (currentState?.isGranted == true) {
            return PermissionResult.AlreadyGranted
        }
        
        // Check backoff timing
        if (currentState != null && currentState.denialCount > 0) {
            val currentTime = System.currentTimeMillis()
            if (currentTime < currentState.nextRequestAllowedTime) {
                val retryAfter = currentState.nextRequestAllowedTime - currentTime
                return PermissionResult.TooEarly(permission, retryAfter)
            }
        }
        
        // Check if requires settings
        if (currentState?.requiresSettings == true) {
            return PermissionResult.RequiresSettings(permission)
        }
        
        // Show rationale if needed
        if (currentState?.needsRationale == true) {
            return PermissionResult.ShowRationale(permission)
        }
        
        // Proceed with permission request
        return performPermissionRequest(permission)
    }

    private suspend fun performPermissionRequest(permission: AppPermission): PermissionResult {
        return try {
            pendingPermissionRequest = permission
            _uiState.value = _uiState.value.copy(requestingPermission = permission)
            
            when (permission) {
                AppPermission.SYSTEM_ALERT_WINDOW -> requestOverlayPermission()
                AppPermission.ACCESSIBILITY_SERVICE -> requestAccessibilityPermission()
                else -> requestStandardPermission(permission)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(requestingPermission = null)
            PermissionResult.Error(permission, e.message ?: "Unknown error")
        }
    }

    private fun requestStandardPermission(permission: AppPermission): PermissionResult {
        requestPermissionLauncher?.launch(permission.manifestPermission)
            ?: return PermissionResult.Error(permission, "Permission launcher not registered")
        
        return PermissionResult.Granted // This will be updated in the callback
    }

    private fun requestOverlayPermission(): PermissionResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            requestOverlayPermissionLauncher?.launch(intent)
                ?: return PermissionResult.Error(
                    AppPermission.SYSTEM_ALERT_WINDOW, 
                    "Overlay permission launcher not registered"
                )
        }
        return PermissionResult.Granted // This will be updated in the callback
    }

    private fun requestAccessibilityPermission(): PermissionResult {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        requestAccessibilityPermissionLauncher?.launch(intent)
            ?: return PermissionResult.Error(
                AppPermission.ACCESSIBILITY_SERVICE,
                "Accessibility permission launcher not registered"
            )
        return PermissionResult.Granted // This will be updated in the callback
    }

    private fun handlePermissionResult(isGranted: Boolean) {
        val permission = pendingPermissionRequest ?: return
        pendingPermissionRequest = null
        
        val currentState = _permissionStates.value[permission]
        val newState = if (isGranted) {
            currentState?.copy(
                isGranted = true,
                isPermanentlyDenied = false,
                canShowRationale = true
            ) ?: PermissionState(permission, true)
        } else {
            val denialCount = (currentState?.denialCount ?: 0) + 1
            val isPermanent = denialCount >= 2 // Android typically considers 2+ denials as permanent
            
            currentState?.copy(
                isGranted = false,
                isPermanentlyDenied = isPermanent,
                denialCount = denialCount,
                lastDeniedTimestamp = System.currentTimeMillis(),
                canShowRationale = !isPermanent
            ) ?: PermissionState(
                permission = permission,
                isGranted = false,
                isPermanentlyDenied = isPermanent,
                denialCount = denialCount,
                lastDeniedTimestamp = System.currentTimeMillis(),
                canShowRationale = !isPermanent
            )
        }
        
        permissionMonitor.updatePermissionState(permission, newState)
        
        val result = if (isGranted) {
            PermissionResult.Granted
        } else {
            PermissionResult.Denied(permission, newState.isPermanentlyDenied)
        }
        
        _lastPermissionResult.value = result
        _uiState.value = _uiState.value.copy(requestingPermission = null)
        
        // Trigger a fresh permission check
        viewModelScope.launch {
            permissionMonitor.checkAllPermissions()
        }
    }

    private fun handleOverlayPermissionResult() {
        val permission = AppPermission.SYSTEM_ALERT_WINDOW
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        
        handlePermissionResult(isGranted)
    }

    private fun handleAccessibilityPermissionResult() {
        viewModelScope.launch {
            // Give the system a moment to update settings
            kotlinx.coroutines.delay(1000)
            permissionMonitor.checkAllPermissions()
            
            val permission = AppPermission.ACCESSIBILITY_SERVICE
            val isGranted = _permissionStates.value[permission]?.isGranted == true
            handlePermissionResult(isGranted)
        }
    }

    fun navigateToSettings(permission: AppPermission) {
        try {
        val intent = when (permission.settingsAction) {
            "android.settings.action.MANAGE_OVERLAY_PERMISSION" -> {
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            }
            "android.settings.APP_NOTIFICATION_SETTINGS" -> {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            }
            "android.settings.APPLICATION_DETAILS_SETTINGS" -> {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
            else -> {
                Intent(permission.settingsAction)
            }
        }
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    fun clearLastResult() {
        _lastPermissionResult.value = null
    }
    
    private fun handleError(throwable: Throwable) {
        // Log error or handle as appropriate for the app
        // Could emit to an error state or show a toast
    }

    fun getCriticalPermissions(): List<AppPermission> {
        return AppPermission.getCriticalPermissions()
            .filter { AppPermission.getPermissionsForApiLevel(Build.VERSION.SDK_INT).contains(it) }
    }

    fun getOptionalPermissions(): List<AppPermission> {
        return AppPermission.getOptionalPermissions()
            .filter { AppPermission.getPermissionsForApiLevel(Build.VERSION.SDK_INT).contains(it) }
    }

    fun getPermissionState(permission: AppPermission): PermissionState? {
        return _permissionStates.value[permission]
    }

    fun requestAllCriticalPermissions() {
        viewModelScope.launch {
            val criticalPermissions = getCriticalPermissions()
            for (permission in criticalPermissions) {
                val state = getPermissionState(permission)
                if (state?.isGranted != true) {
                    val result = requestPermissionSuspend(permission)
                    _lastPermissionResult.value = result
                    
                    // If we hit a rationale or settings requirement, stop and let user handle
                    when (result) {
                        is PermissionResult.ShowRationale,
                        is PermissionResult.RequiresSettings -> break
                        else -> continue
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        permissionMonitor.cleanup()
    }
}

data class PermissionsUiState(
    val criticalPermissionsGranted: Int = 0,
    val totalCriticalPermissions: Int = 0,
    val optionalPermissionsGranted: Int = 0,
    val totalOptionalPermissions: Int = 0,
    val allCriticalPermissionsGranted: Boolean = false,
    val hasOptionalPermissions: Boolean = false,
    val requestingPermission: AppPermission? = null,
    val showRationale: Boolean = false,
    val rationalePermission: AppPermission? = null,
    val showSettingsDialog: Boolean = false,
    val settingsPermission: AppPermission? = null
)