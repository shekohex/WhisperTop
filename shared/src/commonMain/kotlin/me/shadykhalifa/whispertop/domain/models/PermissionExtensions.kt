package me.shadykhalifa.whispertop.domain.models

val AppPermission.manifestPermission: String
    get() = when (this) {
        AppPermission.RECORD_AUDIO -> "android.permission.RECORD_AUDIO"
        AppPermission.SYSTEM_ALERT_WINDOW -> "android.permission.SYSTEM_ALERT_WINDOW"
        AppPermission.ACCESSIBILITY_SERVICE -> "android.permission.BIND_ACCESSIBILITY_SERVICE"
    }

val AppPermission.isCritical: Boolean
    get() = when (this) {
        AppPermission.RECORD_AUDIO -> true
        AppPermission.SYSTEM_ALERT_WINDOW -> true  
        AppPermission.ACCESSIBILITY_SERVICE -> true
    }

val AppPermission.minSdkVersion: Int
    get() = when (this) {
        AppPermission.RECORD_AUDIO -> 26
        AppPermission.SYSTEM_ALERT_WINDOW -> 26
        AppPermission.ACCESSIBILITY_SERVICE -> 26
    }

val AppPermission.settingsAction: String
    get() = when (this) {
        AppPermission.RECORD_AUDIO -> "android.settings.APPLICATION_DETAILS_SETTINGS"
        AppPermission.SYSTEM_ALERT_WINDOW -> "android.settings.action.MANAGE_OVERLAY_PERMISSION"
        AppPermission.ACCESSIBILITY_SERVICE -> "android.settings.ACCESSIBILITY_SETTINGS"
    }

fun AppPermission.Companion.getCriticalPermissions(): List<AppPermission> = 
    AppPermission.entries.filter { it.isCritical }

fun AppPermission.Companion.getOptionalPermissions(): List<AppPermission> = 
    AppPermission.entries.filter { !it.isCritical }

fun AppPermission.Companion.getPermissionsForApiLevel(apiLevel: Int): List<AppPermission> = 
    AppPermission.entries.filter { it.minSdkVersion <= apiLevel }

fun AppPermission.Companion.fromManifestPermission(manifestPermission: String): AppPermission? = 
    AppPermission.entries.find { it.manifestPermission == manifestPermission }

@kotlinx.serialization.Serializable
data class PermissionState(
    val permission: AppPermission,
    val isGranted: Boolean,
    val isPermanentlyDenied: Boolean = false,
    val denialCount: Int = 0,
    val lastDeniedTimestamp: Long = 0L,
    val canShowRationale: Boolean = true
) {
    val needsRationale: Boolean
        get() = !isGranted && canShowRationale && !isPermanentlyDenied
    
    val requiresSettings: Boolean
        get() = !isGranted && (isPermanentlyDenied || !canShowRationale)
    
    val nextRequestAllowedTime: Long
        get() = if (denialCount > 0) {
            lastDeniedTimestamp + getBackoffDelay(denialCount)
        } else {
            0L
        }
    
    companion object {
        private fun getBackoffDelay(denialCount: Int): Long {
            return when {
                denialCount <= 1 -> 0L
                denialCount == 2 -> 30_000L // 30 seconds
                denialCount == 3 -> 300_000L // 5 minutes  
                denialCount == 4 -> 1_800_000L // 30 minutes
                else -> 3_600_000L // 1 hour
            }
        }
    }
}

sealed class PermissionResult {
    object Granted : PermissionResult()
    object AlreadyGranted : PermissionResult()
    data class Denied(val permission: AppPermission, val isPermanent: Boolean) : PermissionResult()
    data class ShowRationale(val permission: AppPermission) : PermissionResult()
    data class RequiresSettings(val permission: AppPermission) : PermissionResult()
    data class TooEarly(val permission: AppPermission, val retryAfterMs: Long) : PermissionResult()
    data class Error(val permission: AppPermission, val message: String) : PermissionResult()
}