package me.shadykhalifa.whispertop.presentation.viewmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.minus
import me.shadykhalifa.whispertop.domain.models.UserStatistics
import me.shadykhalifa.whispertop.domain.models.TranscriptionSession
import me.shadykhalifa.whispertop.domain.models.DailyUsage
import me.shadykhalifa.whispertop.domain.repositories.UserStatisticsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.services.MetricsCollector
import me.shadykhalifa.whispertop.presentation.utils.ViewModelErrorHandler
import me.shadykhalifa.whispertop.utils.Result

data class DashboardUiState(
    val statistics: UserStatistics? = null,
    val recentTranscriptions: List<TranscriptionSession> = emptyList(),
    val trendData: List<DailyUsage> = emptyList(),
    val timeSavedToday: Double = 0.0,
    val timeSavedTotal: Double = 0.0,
    val efficiencyMultiplier: Float = 1.0f,
    val averageSessionDuration: Double = 0.0,
    val totalWordsTranscribed: Long = 0L,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val lastUpdated: Instant? = null,
    val cacheValid: Boolean = true
)

data class EfficiencyMetrics(
    val timeSavedMs: Long,
    val typingTimeMs: Long,
    val speakingTimeMs: Long,
    val efficiencyRatio: Float,
    val productivityGain: Float
)

class DashboardViewModel(
    private val userStatisticsRepository: UserStatisticsRepository,
    private val transcriptionHistoryRepository: TranscriptionHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val metricsCollector: MetricsCollector,
    errorHandler: ViewModelErrorHandler
) : BaseViewModel(errorHandler) {
    
    companion object {
        private const val DEFAULT_TYPING_WPM = 40.0
        private const val AVERAGE_CHARS_PER_WORD = 5.0
        private const val RECENT_TRANSCRIPTIONS_LIMIT = 10
        private const val TREND_DATA_DAYS = 30
        private const val CACHE_VALIDITY_MS = 300_000L // 5 minutes
        private const val DEFAULT_USER_ID = "default_user"
        private const val LOADING_TIMEOUT_MS = 10000L // 10 seconds
    }
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    private val updateMutex = Mutex()
    private val calculationMutex = Mutex()
    
    private var lastCacheUpdate: Instant? = null
    
    init {
        println("🔍 DASHBOARD: 🚀 DashboardViewModel initialized")
        println("🔍 DASHBOARD: Starting loadInitialData...")
        loadInitialData()
        println("🔍 DASHBOARD: Starting real-time updates...")
        startRealTimeUpdates()
        println("🔍 DASHBOARD: Starting loading timeout...")
        startLoadingTimeout()
        println("🔍 DASHBOARD: ✅ DashboardViewModel initialization complete")
    }
    
    private fun loadInitialData() {
        println("🔍 DASHBOARD: Starting loadInitialData()")
        launchSafely {
            updateMutex.withLock {
                println("🔍 DASHBOARD: Acquired updateMutex, setting loading state")
                _uiState.value = _uiState.value.copy(isLoading = true)

                try {
                    println("🔍 DASHBOARD: Calling loadDataWithFallback()")
                    // Load data with comprehensive fallback handling
                    val newState = loadDataWithFallback()
                    println("🔍 DASHBOARD: loadDataWithFallback() completed successfully")
                    _uiState.value = newState
                    lastCacheUpdate = Clock.System.now()
                    println("🔍 DASHBOARD: UI state updated successfully, cache updated")
                } catch (e: Exception) {
                    println("🔍 DASHBOARD: CRITICAL ERROR in loadInitialData(): ${e.message}")
                    e.printStackTrace()
                    // This should never happen since loadDataWithFallback handles all exceptions
                    // But just in case, provide a final fallback
                    handleError(e, "loadInitialData")
                    _uiState.value = DashboardUiState(
                        statistics = createFallbackStatistics(),
                        recentTranscriptions = emptyList(),
                        trendData = emptyList(),
                        timeSavedToday = 0.0,
                        timeSavedTotal = 0.0,
                        efficiencyMultiplier = 1.0f,
                        averageSessionDuration = 0.0,
                        totalWordsTranscribed = 0L,
                        isLoading = false,
                        lastUpdated = Clock.System.now(),
                        cacheValid = true
                    )
                    println("🔍 DASHBOARD: Applied fallback state due to critical error")
                }
            }
        }
    }
    
    private fun startRealTimeUpdates() {
        viewModelScope.launch {
            // Observe user statistics changes
            userStatisticsRepository.getUserStatisticsFlow(DEFAULT_USER_ID).collect { statistics ->
                updateMutex.withLock {
                    if (_uiState.value.statistics != statistics) {
                        refreshStatistics()
                    }
                }
            }
        }

        viewModelScope.launch {
            // Observe settings changes that might affect calculations
            settingsRepository.settings.collect { settings ->
                // Recalculate if typing WPM or other relevant settings changed
                refreshCalculations()
            }
        }
    }

    private fun startLoadingTimeout() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(LOADING_TIMEOUT_MS)
            if (_uiState.value.isLoading) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statistics = createFallbackStatistics(),
                    recentTranscriptions = emptyList(),
                    trendData = emptyList()
                )
                handleError(Exception("Dashboard loading timed out after ${LOADING_TIMEOUT_MS}ms"), "loadingTimeout")
            }
        }
    }



    private suspend fun ensureDatabaseAccessible(): Boolean {
        println("🔍 DASHBOARD: Checking database accessibility...")
        return try {
            // Try to perform a simple database operation to check accessibility
            println("🔍 DASHBOARD: Performing database test operation...")
            val testResult = userStatisticsRepository.getUserStatistics(DEFAULT_USER_ID)
            println("🔍 DASHBOARD: Database test result: ${testResult::class.simpleName}")

            when (testResult) {
                is Result.Success -> {
                    println("🔍 DASHBOARD: ✅ Database is accessible")
                    true
                }
                is Result.Error -> {
                    println("🔍 DASHBOARD: ❌ Database test failed: ${testResult.exception.message}")
                    println("🔍 DASHBOARD: Exception details: ${testResult.exception}")
                    testResult.exception.printStackTrace()
                    handleError(testResult.exception, "databaseAccessibilityCheck")
                    false
                }
                is Result.Loading -> {
                    println("🔍 DASHBOARD: ⏳ Database is still loading, considering accessible")
                    true // Still loading, consider accessible
                }
            }
        } catch (e: Exception) {
            println("🔍 DASHBOARD: 💥 CRITICAL: Exception during database accessibility check: ${e.message}")
            e.printStackTrace()
            handleError(e, "databaseAccessibilityCheck")
            false
        }
    }

    private suspend fun loadDataWithFallback(): DashboardUiState {
        println("🔍 DASHBOARD: Starting loadDataWithFallback()")
        return try {
            // First check if database is accessible
            println("🔍 DASHBOARD: Checking database accessibility...")
            val isDatabaseAccessible = ensureDatabaseAccessible()
            println("🔍 DASHBOARD: Database accessible: $isDatabaseAccessible")

            if (!isDatabaseAccessible) {
                println("🔍 DASHBOARD: ❌ Database not accessible, using fallback statistics")
                // Use fallback statistics if database is not accessible
                return DashboardUiState(
                    statistics = createFallbackStatistics(),
                    recentTranscriptions = emptyList(),
                    trendData = emptyList(),
                    timeSavedToday = 0.0,
                    timeSavedTotal = 0.0,
                    efficiencyMultiplier = 1.0f,
                    averageSessionDuration = 0.0,
                    totalWordsTranscribed = 0L,
                    isLoading = false,
                    lastUpdated = Clock.System.now(),
                    cacheValid = true
                )
            }

            println("🔍 DASHBOARD: Loading user statistics...")
            val statisticsResult = userStatisticsRepository.getUserStatistics(DEFAULT_USER_ID)
            println("🔍 DASHBOARD: Statistics result: ${statisticsResult::class.simpleName}")

            println("🔍 DASHBOARD: Loading recent transcriptions...")
            val recentTranscriptions = loadRecentTranscriptions()
            println("🔍 DASHBOARD: Recent transcriptions loaded: ${recentTranscriptions.size} items")

            println("🔍 DASHBOARD: Loading trend data...")
            val trendData = loadTrendData()
            println("🔍 DASHBOARD: Trend data loaded: ${trendData.size} items")

            var statistics = when (statisticsResult) {
                is Result.Success -> {
                    println("🔍 DASHBOARD: ✅ Statistics loaded successfully: ${statisticsResult.data}")
                    statisticsResult.data
                }
                is Result.Error -> {
                    println("🔍 DASHBOARD: ❌ Statistics loading failed: ${statisticsResult.exception.message}")
                    handleError(statisticsResult.exception, "getUserStatistics")
                    null
                }
                is Result.Loading -> {
                    println("🔍 DASHBOARD: ⏳ Statistics still loading")
                    null
                }
            }

            // Create default user statistics if they don't exist
            if (statistics == null) {
                println("🔍 DASHBOARD: No statistics found, creating default statistics...")
                val createResult = userStatisticsRepository.createUserStatistics(DEFAULT_USER_ID)
                println("🔍 DASHBOARD: Create statistics result: ${createResult::class.simpleName}")

                if (createResult is Result.Success) {
                    println("🔍 DASHBOARD: ✅ Statistics created successfully, fetching new statistics...")
                    // Try to get the newly created statistics
                    val newStatsResult = userStatisticsRepository.getUserStatistics(DEFAULT_USER_ID)
                    statistics = when (newStatsResult) {
                        is Result.Success -> {
                            println("🔍 DASHBOARD: ✅ New statistics fetched: ${newStatsResult.data}")
                            newStatsResult.data
                        }
                        is Result.Error -> {
                            println("🔍 DASHBOARD: ❌ Failed to fetch new statistics: ${newStatsResult.exception.message}")
                            handleError(newStatsResult.exception, "getUserStatistics after creation")
                            createFallbackStatistics()
                        }
                        is Result.Loading -> {
                            println("🔍 DASHBOARD: ⏳ New statistics still loading")
                            createFallbackStatistics()
                        }
                    }
                } else if (createResult is Result.Error) {
                    println("🔍 DASHBOARD: ❌ Failed to create statistics: ${createResult.exception.message}")
                    handleError(createResult.exception, "createUserStatistics")
                    statistics = createFallbackStatistics()
                }
            }

            println("🔍 DASHBOARD: Calculating efficiency metrics...")
            val efficiencyMetrics = calculateEfficiencyMetrics(statistics, recentTranscriptions)
            println("🔍 DASHBOARD: Efficiency metrics calculated: timeSaved=${efficiencyMetrics.timeSavedMs}ms, ratio=${efficiencyMetrics.efficiencyRatio}")

            println("🔍 DASHBOARD: Creating final DashboardUiState...")
            val finalState = DashboardUiState(
                statistics = statistics,
                recentTranscriptions = recentTranscriptions,
                trendData = trendData,
                timeSavedToday = calculateTimeSavedToday(recentTranscriptions),
                timeSavedTotal = efficiencyMetrics.timeSavedMs / 1000.0 / 60.0, // Convert to minutes
                efficiencyMultiplier = efficiencyMetrics.efficiencyRatio,
                averageSessionDuration = calculateAverageSessionDuration(recentTranscriptions),
                totalWordsTranscribed = statistics?.totalWords ?: 0L,
                isLoading = false,
                lastUpdated = Clock.System.now(),
                cacheValid = true
            )

            println("🔍 DASHBOARD: ✅ Final state created successfully")
            println("🔍 DASHBOARD: 📊 Stats: words=${statistics?.totalWords ?: 0}, sessions=${statistics?.totalSessions ?: 0}")
            println("🔍 DASHBOARD: 📈 Recent transcriptions: ${recentTranscriptions.size}")
            println("🔍 DASHBOARD: 📉 Trend data points: ${trendData.size}")

            finalState
        } catch (e: Exception) {
            println("🔍 DASHBOARD: 💥 CRITICAL EXCEPTION in loadDataWithFallback: ${e.message}")
            println("🔍 DASHBOARD: Exception type: ${e::class.simpleName}")
            e.printStackTrace()
            handleError(e, "loadDataWithFallback")

            // Always return a valid state, never let the UI get stuck
            println("🔍 DASHBOARD: Returning fallback state due to exception")
            DashboardUiState(
                statistics = createFallbackStatistics(),
                recentTranscriptions = emptyList(),
                trendData = emptyList(),
                timeSavedToday = 0.0,
                timeSavedTotal = 0.0,
                efficiencyMultiplier = 1.0f,
                averageSessionDuration = 0.0,
                totalWordsTranscribed = 0L,
                isLoading = false,
                lastUpdated = Clock.System.now(),
                cacheValid = true
            )
        }
    }

    private fun createFallbackStatistics(): me.shadykhalifa.whispertop.domain.models.UserStatistics {
        println("🔍 DASHBOARD: ⚠️ Creating fallback statistics (all zeros)")
        return me.shadykhalifa.whispertop.domain.models.UserStatistics(
            id = DEFAULT_USER_ID,
            totalWords = 0L,
            totalSessions = 0,
            totalSpeakingTimeMs = 0L,
            averageWordsPerMinute = 0.0,
            averageWordsPerSession = 0.0,
            userTypingWpm = DEFAULT_TYPING_WPM.toInt(),
            totalTranscriptions = 0L,
            totalDuration = 0f,
            averageAccuracy = null,
            dailyUsageCount = 0L,
            mostUsedLanguage = null,
            mostUsedModel = null,
            createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        )
    }
    
    private suspend fun loadRecentTranscriptions(): List<TranscriptionSession> {
        println("🔍 DASHBOARD: Loading recent transcriptions...")
        return try {
            val result = transcriptionHistoryRepository.getRecentTranscriptionSessions(RECENT_TRANSCRIPTIONS_LIMIT)
            println("🔍 DASHBOARD: Recent transcriptions result: ${result::class.simpleName}")

            when (result) {
                is Result.Success -> {
                    println("🔍 DASHBOARD: ✅ Recent transcriptions loaded: ${result.data.size} items")
                    result.data
                }
                is Result.Error -> {
                    println("🔍 DASHBOARD: ❌ Failed to load recent transcriptions: ${result.exception.message}")
                    handleError(result.exception, "loadRecentTranscriptions")
                    emptyList()
                }
                is Result.Loading -> {
                    println("🔍 DASHBOARD: ⏳ Recent transcriptions still loading")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            println("🔍 DASHBOARD: 💥 Exception loading recent transcriptions: ${e.message}")
            handleError(e, "loadRecentTranscriptions")
            emptyList()
        }
    }

    private suspend fun loadTrendData(): List<DailyUsage> {
        println("🔍 DASHBOARD: Loading trend data...")
        return try {
            val endDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val startDate = endDate.minus(DatePeriod(days = TREND_DATA_DAYS))
            println("🔍 DASHBOARD: Trend data date range: $startDate to $endDate")

            val result = transcriptionHistoryRepository.getDailyUsage(startDate, endDate)
            println("🔍 DASHBOARD: Trend data result: ${result::class.simpleName}")

            when (result) {
                is Result.Success -> {
                    println("🔍 DASHBOARD: ✅ Trend data loaded: ${result.data.size} data points")
                    result.data
                }
                is Result.Error -> {
                    println("🔍 DASHBOARD: ❌ Failed to load trend data: ${result.exception.message}")
                    handleError(result.exception, "loadTrendData")
                    emptyList()
                }
                is Result.Loading -> {
                    println("🔍 DASHBOARD: ⏳ Trend data still loading")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            println("🔍 DASHBOARD: 💥 Exception loading trend data: ${e.message}")
            handleError(e, "loadTrendData")
            emptyList()
        }
    }
    
    fun calculateTimeSaved(
        transcriptions: List<TranscriptionSession>,
        typingWpm: Double = DEFAULT_TYPING_WPM
    ): Double {
        if (transcriptions.isEmpty()) return 0.0
        
        return transcriptions.sumOf { session ->
            val speakingTimeMinutes = session.audioLengthMs / 60000.0
            val wordCount = session.wordCount.toDouble()
            val typingTimeMinutes = wordCount / typingWpm
            
            // Time saved is the difference between typing time and speaking time
            maxOf(0.0, typingTimeMinutes - speakingTimeMinutes)
        }
    }
    
    private fun calculateTimeSavedToday(transcriptions: List<TranscriptionSession>): Double {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        val todayTranscriptions = transcriptions.filter { session ->
            val sessionDate = session.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date
            sessionDate == today
        }
        
        return calculateTimeSaved(todayTranscriptions)
    }
    
    private suspend fun calculateEfficiencyMetrics(
        statistics: UserStatistics?,
        transcriptions: List<TranscriptionSession>
    ): EfficiencyMetrics = calculationMutex.withLock {
        if (statistics == null || transcriptions.isEmpty()) {
            return@withLock EfficiencyMetrics(
                timeSavedMs = 0L,
                typingTimeMs = 0L,
                speakingTimeMs = 0L,
                efficiencyRatio = 1.0f,
                productivityGain = 0.0f
            )
        }
        
        val totalSpeakingTimeMs = statistics.totalSpeakingTimeMs
        val totalWordsTranscribed = statistics.totalWords
        
        // Calculate equivalent typing time
        val typingTimeMs = ((totalWordsTranscribed * AVERAGE_CHARS_PER_WORD) / DEFAULT_TYPING_WPM * 60 * 1000).toLong()
        
        val timeSavedMs = maxOf(0L, typingTimeMs - totalSpeakingTimeMs)
        val efficiencyRatio = if (totalSpeakingTimeMs > 0) {
            (typingTimeMs.toFloat() / totalSpeakingTimeMs.toFloat())
        } else 1.0f
        
        val productivityGain = if (typingTimeMs > 0) {
            ((timeSavedMs.toFloat() / typingTimeMs.toFloat()) * 100)
        } else 0.0f
        
        EfficiencyMetrics(
            timeSavedMs = timeSavedMs,
            typingTimeMs = typingTimeMs,
            speakingTimeMs = totalSpeakingTimeMs,
            efficiencyRatio = efficiencyRatio,
            productivityGain = productivityGain
        )
    }
    
    private fun calculateAverageSessionDuration(transcriptions: List<TranscriptionSession>): Double {
        if (transcriptions.isEmpty()) return 0.0
        
        val totalDuration = transcriptions.sumOf { it.audioLengthMs }
        return totalDuration.toDouble() / transcriptions.size / 1000.0 // Convert to seconds
    }
    
    fun refreshData() {
        if (_uiState.value.isRefreshing) return

        launchSafely {
            _uiState.value = _uiState.value.copy(isRefreshing = true)

            try {
                updateMutex.withLock {
                    val newState = loadDataWithFallback()
                    _uiState.value = newState.copy(isRefreshing = false)
                    lastCacheUpdate = Clock.System.now()
                }
            } catch (e: Exception) {
                handleError(e, "refreshData")
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }
    
    private suspend fun refreshStatistics() {
        if (!isCacheValid()) {
            loadInitialData()
        }
    }
    
    private suspend fun refreshCalculations() {
        calculationMutex.withLock {
            val currentState = _uiState.value
            val efficiencyMetrics = calculateEfficiencyMetrics(
                currentState.statistics,
                currentState.recentTranscriptions
            )
            
            _uiState.value = currentState.copy(
                timeSavedToday = calculateTimeSavedToday(currentState.recentTranscriptions),
                timeSavedTotal = efficiencyMetrics.timeSavedMs / 1000.0 / 60.0,
                efficiencyMultiplier = efficiencyMetrics.efficiencyRatio,
                averageSessionDuration = calculateAverageSessionDuration(currentState.recentTranscriptions),
                lastUpdated = Clock.System.now()
            )
        }
    }
    
    private fun isCacheValid(): Boolean {
        val lastUpdate = lastCacheUpdate ?: return false
        val now = Clock.System.now()
        return (now - lastUpdate).inWholeMilliseconds < CACHE_VALIDITY_MS
    }
    
    fun invalidateCache() {
        lastCacheUpdate = null
        _uiState.value = _uiState.value.copy(cacheValid = false)
    }
    
    fun getEfficiencyInsights(): Map<String, Any> {
        val state = _uiState.value
        return mapOf(
            "totalTimeSavedHours" to (state.timeSavedTotal / 60.0),
            "averageEfficiency" to state.efficiencyMultiplier,
            "productivityGain" to ((state.efficiencyMultiplier - 1.0f) * 100),
            "sessionsToday" to state.recentTranscriptions.count { session ->
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val sessionDate = session.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date
                sessionDate == today
            },
            "averageSessionLength" to state.averageSessionDuration,
            "totalWords" to state.totalWordsTranscribed
        )
    }
    

}