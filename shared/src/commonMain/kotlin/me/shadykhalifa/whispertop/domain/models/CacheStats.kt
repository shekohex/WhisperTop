package me.shadykhalifa.whispertop.domain.models

data class CacheStats(
    val hitCount: Long = 0,
    val missCount: Long = 0,
    val evictionCount: Long = 0,
    val currentSize: Int = 0,
    val maxSize: Int = 0
) {
    val hitRate: Double
        get() = if (hitCount + missCount == 0L) 0.0 else hitCount.toDouble() / (hitCount + missCount)
    
    val missRate: Double
        get() = 1.0 - hitRate
}