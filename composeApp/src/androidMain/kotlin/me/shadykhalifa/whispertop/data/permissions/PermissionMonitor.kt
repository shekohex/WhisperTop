package me.shadykhalifa.whispertop.data.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.domain.models.AppPermission
import me.shadykhalifa.whispertop.domain.models.PermissionState
import me.shadykhalifa.whispertop.domain.models.manifestPermission
import me.shadykhalifa.whispertop.domain.models.getPermissionsForApiLevel
import me.shadykhalifa.whispertop.domain.models.getCriticalPermissions
import me.shadykhalifa.whispertop.domain.models.getOptionalPermissions

class PermissionMonitor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    
    private val _permissionStates = MutableStateFlow<Map<AppPermission, PermissionState>>(emptyMap())
    val permissionStates: StateFlow<Map<AppPermission, PermissionState>> = _permissionStates.asStateFlow()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    private val contentObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            scope.launch {
                checkAllPermissions()
            }
        }
    }
    
    fun startMonitoring() {
        if (_isMonitoring.value) return
        
        _isMonitoring.value = true
        
        // Register content observer for system settings changes
        context.contentResolver.registerContentObserver(
            Settings.Secure.CONTENT_URI,
            true,
            contentObserver
        )
        
        // Start periodic checks as fallback
        scope.launch {
            while (isActive && _isMonitoring.value) {
                checkAllPermissions()
                delay(5000) // Check every 5 seconds
            }
        }
        
        // Initial check
        scope.launch {
            checkAllPermissions()
        }
    }
    
    fun stopMonitoring() {
        if (!_isMonitoring.value) return
        
        _isMonitoring.value = false
        context.contentResolver.unregisterContentObserver(contentObserver)
    }
    
    suspend fun checkAllPermissions() {
        val apiLevel = Build.VERSION.SDK_INT
        val applicablePermissions = AppPermission.getPermissionsForApiLevel(apiLevel)
        
        val currentStates = _permissionStates.value.toMutableMap()
        
        applicablePermissions.forEach { permission ->
            val isGranted = checkPermission(permission)
            val currentState = currentStates[permission]
            
            if (currentState?.isGranted != isGranted) {
                currentStates[permission] = PermissionState(
                    permission = permission,
                    isGranted = isGranted,
                    isPermanentlyDenied = currentState?.isPermanentlyDenied ?: false,
                    denialCount = currentState?.denialCount ?: 0,
                    lastDeniedTimestamp = currentState?.lastDeniedTimestamp ?: 0L,
                    canShowRationale = currentState?.canShowRationale ?: true
                )
            }
        }
        
        _permissionStates.value = currentStates
    }
    
    private fun checkPermission(permission: AppPermission): Boolean {
        return when (permission) {
            AppPermission.SYSTEM_ALERT_WINDOW -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else {
                    true // Permission not needed on older versions
                }
            }
            AppPermission.ACCESSIBILITY_SERVICE -> {
                checkAccessibilityServiceEnabled()
            }
            else -> {
                // Standard runtime permission check
                ContextCompat.checkSelfPermission(
                    context,
                    permission.manifestPermission
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    
    private fun checkAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        
        if (accessibilityEnabled != 1) return false
        
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        val packageName = context.packageName
        val serviceName = "$packageName/me.shadykhalifa.whispertop.services.WhisperTopAccessibilityService"
        
        return enabledServices.contains(serviceName)
    }
    
    fun updatePermissionState(permission: AppPermission, newState: PermissionState) {
        val currentStates = _permissionStates.value.toMutableMap()
        currentStates[permission] = newState
        _permissionStates.value = currentStates
    }
    
    fun getPermissionState(permission: AppPermission): PermissionState? {
        return _permissionStates.value[permission]
    }
    
    fun getCriticalPermissionsStatus(): Pair<Int, Int> {
        val criticalPermissions = AppPermission.getCriticalPermissions()
            .filter { AppPermission.getPermissionsForApiLevel(Build.VERSION.SDK_INT).contains(it) }
        val grantedCount = criticalPermissions.count { permission ->
            _permissionStates.value[permission]?.isGranted == true
        }
        return Pair(grantedCount, criticalPermissions.size)
    }
    
    fun getOptionalPermissionsStatus(): Pair<Int, Int> {
        val optionalPermissions = AppPermission.getOptionalPermissions()
            .filter { AppPermission.getPermissionsForApiLevel(Build.VERSION.SDK_INT).contains(it) }
        val grantedCount = optionalPermissions.count { permission ->
            _permissionStates.value[permission]?.isGranted == true
        }
        return Pair(grantedCount, optionalPermissions.size)
    }
    
    fun areAllCriticalPermissionsGranted(): Boolean {
        val (granted, total) = getCriticalPermissionsStatus()
        return granted == total
    }
    
    fun cleanup() {
        stopMonitoring()
        scope.cancel()
    }
}