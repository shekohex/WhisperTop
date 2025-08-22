package me.shadykhalifa.whispertop.utils

import platform.Foundation.NSProcessInfo

actual object MemoryUtils {
    actual fun getUsedMemoryBytes(): Long {
        // iOS doesn't provide direct memory usage info in the same way as JVM
        // Return approximate value or implement native code if needed
        return NSProcessInfo.processInfo.physicalMemory.toLong() / 10 // Rough estimate
    }
    
    actual fun getTotalMemoryBytes(): Long {
        return NSProcessInfo.processInfo.physicalMemory.toLong()
    }
    
    actual fun getMaxMemoryBytes(): Long {
        return NSProcessInfo.processInfo.physicalMemory.toLong()
    }
}