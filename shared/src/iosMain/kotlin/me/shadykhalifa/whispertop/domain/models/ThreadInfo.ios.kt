package me.shadykhalifa.whispertop.domain.models

import platform.Foundation.NSThread
import platform.Foundation.NSDate

/**
 * iOS-specific implementation for getting current thread information
 */
actual fun getCurrentThreadName(): String {
    return NSThread.currentThread.name ?: "Unknown"
}

actual fun getCurrentThreadId(): Long {
    // iOS doesn't have a direct thread ID concept like Java/Android
    // We'll use the thread object's hash as an identifier
    return NSThread.currentThread.hash().toLong()
}

actual fun getCurrentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}