package me.shadykhalifa.whispertop.domain.models

import kotlinx.datetime.Clock

data class CacheEntry<T>(
    val data: T,
    val timestamp: Long,
    val ttlMs: Long = 24 * 60 * 60 * 1000L // 24 hours default TTL
) {
    fun isExpired(): Boolean {
        return Clock.System.now().toEpochMilliseconds() - timestamp > ttlMs
    }
    
    fun remainingTtl(): Long {
        val elapsed = Clock.System.now().toEpochMilliseconds() - timestamp
        return maxOf(0L, ttlMs - elapsed)
    }
}