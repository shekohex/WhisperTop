package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.domain.models.EvictionPolicy
import me.shadykhalifa.whispertop.domain.models.EvictionReason
import me.shadykhalifa.whispertop.domain.models.MemoryInfo

interface CacheEvictionPolicyManager {
    fun startEvictionPolicies()
    fun stopEvictionPolicies()
    fun triggerManualEviction(reason: EvictionReason)
    fun updateMemoryPressureThreshold(threshold: Double)
    fun isMemoryUnderPressure(): Boolean
}

class CacheEvictionPolicyManagerImpl(
    private val performanceCacheManager: PerformanceCacheManager,
    private val statisticsCacheService: StatisticsCacheService,
    private val memoryProfiler: MemoryProfiler,
    private val backgroundThreadManager: BackgroundThreadManager,
    private val coroutineScope: CoroutineScope,
    private val memoryPressureThreshold: Double = 85.0 // 85% memory usage
) : CacheEvictionPolicyManager {
    
    private var evictionJobs = mutableListOf<Job>()
    private var isRunning = false
    private var currentMemoryThreshold = memoryPressureThreshold
    
    override fun startEvictionPolicies() {
        if (isRunning) return
        isRunning = true
        
        // Start time-based eviction policy
        startTimeBased()
        
        // Start size-based eviction policy
        startSizeBased()
        
        // Start memory pressure-based eviction policy
        startMemoryPressureBased()
        
        // Start periodic cleanup
        startPeriodicCleanup()
    }
    
    override fun stopEvictionPolicies() {
        isRunning = false
        evictionJobs.forEach { it.cancel() }
        evictionJobs.clear()
    }
    
    override fun triggerManualEviction(reason: EvictionReason) {
        val job = coroutineScope.launch {
            backgroundThreadManager.executeTask(ThreadType.CACHE_OPERATIONS, "manual_eviction") {
                when (reason) {
                    EvictionReason.MEMORY_PRESSURE -> {
                        // Aggressive eviction
                        performanceCacheManager.evictExpired()
                        performanceCacheManager.evictLRU(10)
                        statisticsCacheService.clearCache()
                        memoryProfiler.performWeakReferenceCleanup()
                        memoryProfiler.suggestGarbageCollection()
                    }
                    EvictionReason.DATA_INVALIDATION -> {
                        // Clear all caches
                        performanceCacheManager.clear()
                        statisticsCacheService.clearCache()
                    }
                    EvictionReason.TTL_EXPIRED -> {
                        // Remove expired entries
                        performanceCacheManager.evictExpired()
                    }
                    EvictionReason.SIZE_LIMIT_EXCEEDED -> {
                        // Evict LRU items
                        performanceCacheManager.evictLRU(5)
                    }
                    EvictionReason.MANUAL_EVICTION -> {
                        // Moderate cleanup
                        performanceCacheManager.evictExpired()
                        memoryProfiler.performWeakReferenceCleanup()
                    }
                }
            }
        }
        evictionJobs.add(job)
    }
    
    override fun updateMemoryPressureThreshold(threshold: Double) {
        currentMemoryThreshold = threshold.coerceIn(50.0, 95.0)
    }
    
    override fun isMemoryUnderPressure(): Boolean {
        val memoryInfo = memoryProfiler.getCurrentMemoryInfo()
        return memoryInfo.usedMemoryPercentage > currentMemoryThreshold
    }
    
    private fun startTimeBased() {
        val job = coroutineScope.launch {
            while (isRunning) {
                delay(5 * 60 * 1000L) // Every 5 minutes
                
                backgroundThreadManager.executeTask(ThreadType.CACHE_OPERATIONS, "time_based_eviction") {
                    // Evict expired entries
                    performanceCacheManager.evictExpired()
                }
            }
        }
        evictionJobs.add(job)
    }
    
    private fun startSizeBased() {
        val job = coroutineScope.launch {
            performanceCacheManager.observeStats()
                .onEach { stats ->
                    if (stats.currentSize > stats.maxSize * 0.9) { // 90% full
                        backgroundThreadManager.executeTask(ThreadType.CACHE_OPERATIONS, "size_based_eviction") {
                            performanceCacheManager.evictLRU(5)
                        }
                    }
                }
                .launchIn(this)
        }
        evictionJobs.add(job)
    }
    
    private fun startMemoryPressureBased() {
        val job = coroutineScope.launch {
            memoryProfiler.observeMemoryInfo()
                .onEach { memoryInfo ->
                    if (memoryInfo.usedMemoryPercentage > currentMemoryThreshold) {
                        handleMemoryPressure(memoryInfo)
                    }
                }
                .launchIn(this)
        }
        evictionJobs.add(job)
    }
    
    private fun startPeriodicCleanup() {
        val job = coroutineScope.launch {
            while (isRunning) {
                delay(10 * 60 * 1000L) // Every 10 minutes
                
                backgroundThreadManager.executeTask(ThreadType.CACHE_OPERATIONS, "periodic_cleanup") {
                    // Perform general cleanup
                    performanceCacheManager.evictExpired()
                    memoryProfiler.performWeakReferenceCleanup()
                    
                    // Check for memory leaks
                    val leaks = memoryProfiler.detectPotentialLeaks()
                    if (leaks.isNotEmpty()) {
                        // Log potential leaks (would integrate with logging service)
                        println("Detected ${leaks.size} potential memory leaks")
                        
                        // Trigger additional cleanup for critical leaks
                        val criticalLeaks = leaks.filter { it.severity == me.shadykhalifa.whispertop.domain.models.LeakSeverity.CRITICAL }
                        if (criticalLeaks.isNotEmpty()) {
                            memoryProfiler.suggestGarbageCollection()
                        }
                    }
                }
            }
        }
        evictionJobs.add(job)
    }
    
    private suspend fun handleMemoryPressure(memoryInfo: MemoryInfo) {
        val pressureLevel = when {
            memoryInfo.usedMemoryPercentage > 95.0 -> "critical"
            memoryInfo.usedMemoryPercentage > 90.0 -> "high"
            memoryInfo.usedMemoryPercentage > currentMemoryThreshold -> "moderate"
            else -> return
        }
        
        backgroundThreadManager.executeTask(ThreadType.CACHE_OPERATIONS, "memory_pressure_$pressureLevel") {
            when (pressureLevel) {
                "critical" -> {
                    // Aggressive cleanup
                    performanceCacheManager.clear()
                    statisticsCacheService.clearCache()
                    memoryProfiler.performWeakReferenceCleanup()
                    memoryProfiler.suggestGarbageCollection()
                }
                "high" -> {
                    // Significant cleanup
                    performanceCacheManager.evictLRU(15)
                    performanceCacheManager.evictExpired()
                    statisticsCacheService.invalidateCache(CacheInvalidationEvent.DataChanged)
                    memoryProfiler.performWeakReferenceCleanup()
                }
                "moderate" -> {
                    // Light cleanup
                    performanceCacheManager.evictExpired()
                    performanceCacheManager.evictLRU(5)
                    memoryProfiler.performWeakReferenceCleanup()
                }
            }
        }
    }
}