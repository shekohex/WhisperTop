package me.shadykhalifa.whispertop.domain.models

/**
 * Domain model representing the status of permission requests.
 * This replaces infrastructure types from PermissionHandler.PermissionResult.
 */
sealed class PermissionStatus {
    /**
     * All required permissions have been granted.
     */
    object Granted : PermissionStatus()
    
    /**
     * Some or all permissions were denied.
     */
    data class Denied(val deniedPermissions: List<String>) : PermissionStatus()
    
    /**
     * Permission rationale should be shown to the user before requesting again.
     */
    data class RequiresRationale(val permissions: List<String>) : PermissionStatus()
}