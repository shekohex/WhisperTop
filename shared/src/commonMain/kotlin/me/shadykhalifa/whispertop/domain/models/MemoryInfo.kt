package me.shadykhalifa.whispertop.domain.models

import kotlinx.datetime.Clock

data class MemoryInfo(
    val totalMemory: Long,
    val freeMemory: Long,
    val usedMemory: Long,
    val maxMemory: Long,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    val usedMemoryPercentage: Double
        get() = if (maxMemory > 0) (usedMemory.toDouble() / maxMemory) * 100 else 0.0
    
    val availableMemory: Long
        get() = maxMemory - usedMemory
    
    val isMemoryPressure: Boolean
        get() = usedMemoryPercentage > 80.0
}

data class LeakSuspicion(
    val objectType: String,
    val referenceCount: Int,
    val threshold: Int,
    val detectedAt: Long,
    val severity: LeakSeverity
) {
    val isLeaking: Boolean
        get() = referenceCount > threshold
}

enum class LeakSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class WeakReferenceInfo(
    val id: String,
    val className: String,
    val createdAt: Long,
    val isCleared: Boolean = false,
    val clearedAt: Long? = null
)