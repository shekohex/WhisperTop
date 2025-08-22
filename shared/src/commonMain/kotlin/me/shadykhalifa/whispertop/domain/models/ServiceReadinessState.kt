package me.shadykhalifa.whispertop.domain.models

data class ServiceReadinessState(
    val serviceConnected: Boolean,
    val permissionsGranted: Boolean,
    val errorMessage: String? = null
) {
    val isReady: Boolean = serviceConnected && permissionsGranted && errorMessage == null
}