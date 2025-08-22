package me.shadykhalifa.whispertop.domain.models

sealed class PermissionStatus {
    data object AllGranted : PermissionStatus()
    data class SomeDenied(val deniedPermissions: List<String>) : PermissionStatus()
    data class ShowRationale(val permissions: List<String>) : PermissionStatus()
}