package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import me.shadykhalifa.whispertop.domain.models.CacheEntry
import me.shadykhalifa.whispertop.domain.models.CacheStats
import me.shadykhalifa.whispertop.domain.models.EvictionReason
import me.shadykhalifa.whispertop.domain.models.TranscriptionSession

interface PerformanceCacheManager {
    suspend fun put(key: String, session: TranscriptionSession)
    suspend fun get(key: String): TranscriptionSession?
    suspend fun remove(key: String): TranscriptionSession?
    suspend fun clear()
    suspend fun evictExpired(): Int
    suspend fun getStats(): CacheStats
    fun observeStats(): Flow<CacheStats>
    suspend fun evictLRU(count: Int = 1): List<String>
}

class PerformanceCacheManagerImpl(
    private val maxSize: Int = 50,
    private val defaultTtlMs: Long = 24 * 60 * 60 * 1000L // 24 hours
) : PerformanceCacheManager {
    
    private val cache = LinkedHashMap<String, CacheEntry<TranscriptionSession>>(maxSize, 0.75f, true)
    private val mutex = Mutex()
    
    private var hitCount = 0L
    private var missCount = 0L
    private var evictionCount = 0L
    
    private val _stats = MutableStateFlow(createCacheStats())
    
    override suspend fun put(key: String, session: TranscriptionSession) = mutex.withLock {
        val entry = CacheEntry(
            data = session,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            ttlMs = defaultTtlMs
        )
        
        // Remove expired entries first
        evictExpiredInternal()
        
        // Check if we need to evict LRU entries
        if (cache.size >= maxSize && !cache.containsKey(key)) {
            evictLRUInternal(1)
        }
        
        cache[key] = entry
        updateStats()
    }
    
    override suspend fun get(key: String): TranscriptionSession? = mutex.withLock {
        val entry = cache[key]
        
        if (entry == null) {
            missCount++
            updateStats()
            return null
        }
        
        if (entry.isExpired()) {
            cache.remove(key)
            evictionCount++
            missCount++
            updateStats()
            return null
        }
        
        hitCount++
        updateStats()
        return entry.data
    }
    
    override suspend fun remove(key: String): TranscriptionSession? = mutex.withLock {
        val entry = cache.remove(key)
        if (entry != null) {
            evictionCount++
            updateStats()
        }
        entry?.data
    }
    
    override suspend fun clear() = mutex.withLock {
        val removedCount = cache.size
        cache.clear()
        evictionCount += removedCount
        updateStats()
    }
    
    override suspend fun evictExpired(): Int = mutex.withLock {
        evictExpiredInternal()
    }
    
    private fun evictExpiredInternal(): Int {
        val iterator = cache.iterator()
        var evictedCount = 0
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.isExpired()) {
                iterator.remove()
                evictedCount++
                evictionCount++
            }
        }
        
        if (evictedCount > 0) {
            updateStats()
        }
        
        return evictedCount
    }
    
    override suspend fun getStats(): CacheStats = mutex.withLock {
        createCacheStats()
    }
    
    override fun observeStats(): Flow<CacheStats> = _stats.asStateFlow()
    
    override suspend fun evictLRU(count: Int): List<String> = mutex.withLock {
        evictLRUInternal(count)
    }
    
    private fun evictLRUInternal(count: Int): List<String> {
        val evictedKeys = mutableListOf<String>()
        val iterator = cache.iterator()
        var evicted = 0
        
        while (iterator.hasNext() && evicted < count) {
            val entry = iterator.next()
            evictedKeys.add(entry.key)
            iterator.remove()
            evicted++
            evictionCount++
        }
        
        if (evictedKeys.isNotEmpty()) {
            updateStats()
        }
        
        return evictedKeys
    }
    
    private fun createCacheStats(): CacheStats {
        return CacheStats(
            hitCount = hitCount,
            missCount = missCount,
            evictionCount = evictionCount,
            currentSize = cache.size,
            maxSize = maxSize
        )
    }
    
    private fun updateStats() {
        _stats.value = createCacheStats()
    }
}