package me.shadykhalifa.whispertop.domain.services

/**
 * Android-specific implementation for time functions
 */
actual fun getCurrentTimeMillis(): Long {
    return System.currentTimeMillis()
}