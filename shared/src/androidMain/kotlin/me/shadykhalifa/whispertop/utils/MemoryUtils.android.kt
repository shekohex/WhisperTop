package me.shadykhalifa.whispertop.utils

actual object MemoryUtils {
    actual fun getUsedMemoryBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    actual fun getTotalMemoryBytes(): Long {
        return Runtime.getRuntime().totalMemory()
    }
    
    actual fun getMaxMemoryBytes(): Long {
        return Runtime.getRuntime().maxMemory()
    }
}