package me.shadykhalifa.whispertop.domain.services

import platform.Foundation.NSDate

/**
 * iOS-specific implementation for time functions
 */
actual fun getCurrentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}