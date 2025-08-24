package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.shadykhalifa.whispertop.domain.models.CacheEntry
import me.shadykhalifa.whispertop.domain.models.DailyUsage
import me.shadykhalifa.whispertop.domain.models.UserStatistics
import me.shadykhalifa.whispertop.domain.repositories.StatisticsRepository
import me.shadykhalifa.whispertop.utils.getOrNull

sealed class CacheInvalidationEvent {
    object DataChanged : CacheInvalidationEvent()
    object UserSettingsChanged : CacheInvalidationEvent()
    object SessionCompleted : CacheInvalidationEvent()
    data class TimeRangeChanged(val startTime: Long, val endTime: Long) : CacheInvalidationEvent()
}

interface StatisticsCacheService {
    suspend fun getUserStatistics(forceRefresh: Boolean = false): UserStatistics?
    suspend fun getDailyStatistics(date: String, forceRefresh: Boolean = false): DailyUsage?
    suspend fun invalidateCache(event: CacheInvalidationEvent)
    suspend fun clearCache()
    fun observeUserStatistics(): Flow<UserStatistics?>
    fun observeDailyStatistics(): Flow<Map<String, DailyUsage>>
    fun observeCacheInvalidations(): Flow<CacheInvalidationEvent>
}

class StatisticsCacheServiceImpl(
    private val statisticsRepository: StatisticsRepository,
    private val cacheTtlMs: Long = 5 * 60 * 1000L // 5 minutes for statistics
) : StatisticsCacheService {
    
    private val mutex = Mutex()
    
    // Cache storage
    private var userStatisticsCache: CacheEntry<UserStatistics>? = null
    private val dailyStatisticsCache = mutableMapOf<String, CacheEntry<DailyUsage>>()
    
    // Reactive state flows
    private val _userStatistics = MutableStateFlow<UserStatistics?>(null)
    private val _dailyStatistics = MutableStateFlow<Map<String, DailyUsage>>(emptyMap())
    private val _invalidationEvents = MutableSharedFlow<CacheInvalidationEvent>(replay = 0)
    
    init {
        // Set up reactive cache invalidation
        setupReactiveCacheInvalidation()
    }
    
    override suspend fun getUserStatistics(forceRefresh: Boolean): UserStatistics? = mutex.withLock {
        val cachedEntry = userStatisticsCache
        if (!forceRefresh && cachedEntry != null && !cachedEntry.isExpired()) {
            return cachedEntry.data
        }
        
        // Fetch fresh data - using a default userId for now
        val freshStatsResult = statisticsRepository.getUserStatistics("default_user")
        val freshStats: UserStatistics? = freshStatsResult.getOrNull()
        if (freshStats != null) {
            userStatisticsCache = CacheEntry(
                data = freshStats,
                timestamp = System.currentTimeMillis(),
                ttlMs = cacheTtlMs
            )
            _userStatistics.value = freshStats
        }
        
        return freshStats
    }
    
    override suspend fun getDailyStatistics(date: String, forceRefresh: Boolean): DailyUsage? = mutex.withLock {
        val cachedEntry = dailyStatisticsCache[date]
        
        if (!forceRefresh && cachedEntry != null && !cachedEntry.isExpired()) {
            return cachedEntry.data
        }
        
        // Fetch fresh data
        val freshDataResult = statisticsRepository.getDailyStatistics("default_user", kotlinx.datetime.LocalDate.parse(date))
        val freshData: DailyUsage? = freshDataResult.getOrNull()
        
        if (freshData != null) {
            dailyStatisticsCache[date] = CacheEntry(
                data = freshData,
                timestamp = System.currentTimeMillis(),
                ttlMs = cacheTtlMs
            )
        }
        
        updateDailyStatisticsFlow()
        return freshData
    }
    
    override suspend fun invalidateCache(event: CacheInvalidationEvent) {
        mutex.withLock {
            when (event) {
                is CacheInvalidationEvent.DataChanged -> {
                    // Invalidate all caches
                    userStatisticsCache = null
                    dailyStatisticsCache.clear()
                    _userStatistics.value = null
                    _dailyStatistics.value = emptyMap()
                }
                is CacheInvalidationEvent.UserSettingsChanged -> {
                    // Invalidate user statistics cache
                    userStatisticsCache = null
                    _userStatistics.value = null
                }
                is CacheInvalidationEvent.SessionCompleted -> {
                    // Invalidate relevant daily statistics
                    val today = getCurrentDateString()
                    dailyStatisticsCache.remove(today)
                    userStatisticsCache = null
                    updateDailyStatisticsFlow()
                    _userStatistics.value = null
                }
                is CacheInvalidationEvent.TimeRangeChanged -> {
                    // Invalidate statistics within the time range
                    val affectedDates = getDateStringsInRange(event.startTime, event.endTime)
                    affectedDates.forEach { date ->
                        dailyStatisticsCache.remove(date)
                    }
                    updateDailyStatisticsFlow()
                }
            }
        }
        
        // Emit invalidation event
        _invalidationEvents.emit(event)
    }
    
    override suspend fun clearCache() {
        mutex.withLock {
            userStatisticsCache = null
            dailyStatisticsCache.clear()
            _userStatistics.value = null
            _dailyStatistics.value = emptyMap()
        }
    }
    
    override fun observeUserStatistics(): Flow<UserStatistics?> = _userStatistics.asStateFlow()
    
    override fun observeDailyStatistics(): Flow<Map<String, DailyUsage>> = _dailyStatistics.asStateFlow()
    
    override fun observeCacheInvalidations(): Flow<CacheInvalidationEvent> = _invalidationEvents
    
    private fun setupReactiveCacheInvalidation() {
        // This would be connected to repository change events in a real implementation
        // For now, we'll set up the flow structure
    }
    
    private fun updateDailyStatisticsFlow() {
        val currentMap = dailyStatisticsCache.mapNotNull { (key, entry) ->
            if (!entry.isExpired()) key to entry.data else null
        }.toMap()
        _dailyStatistics.value = currentMap
    }
    
    private fun getCurrentDateString(): String {
        // This would use proper date formatting
        return System.currentTimeMillis().toString()
    }
    
    private fun getDateStringsInRange(startTime: Long, endTime: Long): List<String> {
        // This would generate actual date strings for the range
        return listOf(startTime.toString(), endTime.toString())
    }
}

// Extension functions for reactive cache management
fun StatisticsCacheService.observeUserStatisticsWithAutoRefresh(): Flow<UserStatistics?> {
    return observeUserStatistics()
        .combine(observeCacheInvalidations()) { stats, _ -> stats }
        .distinctUntilChanged()
        .onEach { 
            if (it == null) {
                getUserStatistics(forceRefresh = true)
            }
        }
}

fun StatisticsCacheService.observeDailyStatisticsForDate(date: String): Flow<DailyUsage?> {
    return observeDailyStatistics()
        .map { it[date] }
        .distinctUntilChanged()
        .onEach { stats ->
            if (stats == null) {
                getDailyStatistics(date, forceRefresh = true)
            }
        }
}