package me.shadykhalifa.whispertop.data.services

/**
 * Android-specific implementation for memory usage tracking
 */
actual fun getMemoryUsage(): Double {
    val runtime = Runtime.getRuntime()
    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
    return usedMemory / (1024.0 * 1024.0) // Convert to MB
}

/**
 * Android-specific implementation for getting current time
 */
actual fun getCurrentTimeMillis(): Long {
    return System.currentTimeMillis()
}