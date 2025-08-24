package me.shadykhalifa.whispertop.domain.models

sealed class EvictionPolicy {
    object SizeBased : EvictionPolicy()
    data class TimeBased(val ttlMs: Long) : EvictionPolicy()
    object EventBased : EvictionPolicy()
    data class Combined(val policies: List<EvictionPolicy>) : EvictionPolicy()
}

enum class EvictionReason {
    SIZE_LIMIT_EXCEEDED,
    TTL_EXPIRED,
    MANUAL_EVICTION,
    DATA_INVALIDATION,
    MEMORY_PRESSURE
}