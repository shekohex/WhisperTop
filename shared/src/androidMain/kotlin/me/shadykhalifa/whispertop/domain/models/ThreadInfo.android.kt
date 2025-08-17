package me.shadykhalifa.whispertop.domain.models

/**
 * Android-specific implementation for getting current thread information
 */
actual fun getCurrentThreadName(): String {
    return Thread.currentThread().name
}

actual fun getCurrentThreadId(): Long {
    return Thread.currentThread().id
}

actual fun getCurrentTimeMillis(): Long {
    return System.currentTimeMillis()
}