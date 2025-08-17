package me.shadykhalifa.whispertop.data.services

import platform.Foundation.NSProcessInfo

import platform.Foundation.NSDate

/**
 * iOS-specific implementation for memory usage tracking
 */
actual fun getMemoryUsage(): Double {
    // iOS memory tracking is more limited
    // Return an approximation based on available system info
    val processInfo = NSProcessInfo.processInfo
    val physicalMemory = processInfo.physicalMemory
    
    // This is a simplified approximation
    // In a real implementation, you might want to use more detailed iOS-specific APIs
    return physicalMemory / (1024.0 * 1024.0) * 0.1 // Estimate 10% of physical memory
}

/**
 * iOS-specific implementation for getting current time
 */
actual fun getCurrentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}