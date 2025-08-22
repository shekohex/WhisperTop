package me.shadykhalifa.whispertop.domain.models

sealed class ServiceConnectionStatus {
    data object Connected : ServiceConnectionStatus()
    data object AlreadyBound : ServiceConnectionStatus()
    data class Failed(val reason: String) : ServiceConnectionStatus()
    data class Error(val exception: Throwable) : ServiceConnectionStatus()
}