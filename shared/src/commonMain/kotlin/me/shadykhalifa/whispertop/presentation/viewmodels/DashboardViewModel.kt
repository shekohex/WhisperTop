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
import me.shadykhalifa.whispertop.utils.Logger

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
     val cacheValid: Boolean = true,
     val isEmptyState: Boolean = false
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
        private const val TAG = "DashboardViewModel"
        private const val DEFAULT_TYPING_WPM = 40.0
        private const val AVERAGE_CHARS_PER_WORD = 5.0
        private const val RECENT_TRANSCRIPTIONS_LIMIT = 10
        private const val TREND_DATA_DAYS = 30
        private const val CACHE_VALIDITY_MS = 300_000L // 5 minutes
        private const val DEFAULT_USER_ID = "default_user"
        private const val LOADING_TIMEOUT_MS = 15000L // 15 seconds (reduced for faster feedback)
    }
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    private val updateMutex = Mutex()
    private val calculationMutex = Mutex()
    private val initializationMutex = Mutex()
    
    private var lastCacheUpdate: Instant? = null
    private var isInitializationComplete = false
    
    init {
        Logger.debug(TAG, "Initializing DashboardViewModel")
        // Start with loading state
        _uiState.value = DashboardUiState(isLoading = true)
        loadInitialData()
        startLoadingTimeout()
    }
    
    private fun loadInitialData() {
        launchSafely {
            initializationMutex.withLock {
                Logger.debug(TAG, "Starting initial data load")
                _uiState.value = _uiState.value.copy(isLoading = true)

                try {
                    // Load data with comprehensive fallback handling
                    val startTime = Clock.System.now()
                    val newState = loadDataWithFallback()
                    val loadTime = Clock.System.now() - startTime
                    
                    _uiState.value = newState.copy(isLoading = false)
                    lastCacheUpdate = Clock.System.now()
                    isInitializationComplete = true
                    
                    Logger.debug(TAG, "Initial data load completed successfully in ${loadTime.inWholeMilliseconds}ms")
                    Logger.debug(TAG, "Statistics loaded: hasStats=${newState.statistics != null}, isEmpty=${newState.isEmptyState}")
                    
                    // Start real-time updates only after initial data is loaded
                    startRealTimeUpdates()
                } catch (e: Exception) {
                    // This should never happen since loadDataWithfallback handles all exceptions
                    // But just in case, provide a final fallback
                    Logger.error(TAG, "Critical error during initial data load: ${e.message}")
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
                        cacheValid = true,
                        isEmptyState = true
                    )
                    isInitializationComplete = true
                    
                    Logger.debug(TAG, "Using fallback statistics after error")
                    // Start real-time updates even if fallback was used
                    startRealTimeUpdates()
                }
            }
        }
    }
    
    private fun startRealTimeUpdates() {
        viewModelScope.launch {
            Logger.debug(TAG, "Starting real-time updates")
            // Wait for initialization to complete before starting real-time updates
            while (!isInitializationComplete) {
                kotlinx.coroutines.delay(100) // Wait 100ms before checking again
            }
            
            Logger.debug(TAG, "Initialization complete - starting statistics flow collection")
            // Observe user statistics changes
            userStatisticsRepository.getUserStatisticsFlow(DEFAULT_USER_ID).collect { statistics ->
                updateMutex.withLock {
                    val currentState = _uiState.value
                    if (currentState.statistics != statistics) {
                        Logger.debug(TAG, "Statistics changed - updating UI state")
                        if (statistics != null) {
                            // Update the UI state with new statistics
                            val efficiencyMetrics = calculateEfficiencyMetrics(statistics, currentState.recentTranscriptions)

                            _uiState.value = currentState.copy(
                                statistics = statistics,
                                timeSavedTotal = efficiencyMetrics.timeSavedMs / 1000.0 / 60.0,
                                efficiencyMultiplier = efficiencyMetrics.efficiencyRatio,
                                totalWordsTranscribed = statistics.totalWords,
                                isEmptyState = isStatisticsEmpty(statistics),
                                isLoading = false,
                                lastUpdated = Clock.System.now()
                            )
                        } else if (currentState.statistics != null && isInitializationComplete) {
                            Logger.warn(TAG, "Statistics were deleted - refreshing data")
                            // Statistics were deleted, refresh everything (only if init is complete)
                            loadInitialData()
                        }
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
            Logger.debug(TAG, "Starting loading timeout watchdog (${LOADING_TIMEOUT_MS}ms)")
            kotlinx.coroutines.delay(LOADING_TIMEOUT_MS)
            if (_uiState.value.isLoading && _uiState.value.statistics == null) {
                // Only timeout if we're still loading and have no data
                Logger.warn(TAG, "Loading timeout triggered - dashboard failed to load within ${LOADING_TIMEOUT_MS}ms")
                updateMutex.withLock {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statistics = createFallbackStatistics(),
                        recentTranscriptions = emptyList(),
                        trendData = emptyList(),
                        isEmptyState = true
                    )
                }
                handleError(Exception("Dashboard loading timed out after ${LOADING_TIMEOUT_MS}ms"), "loadingTimeout")
            } else {
                Logger.debug(TAG, "Loading timeout watchdog completed - data loaded successfully")
            }
        }
    }



    private suspend fun ensureDatabaseAccessible(): Boolean {
        return try {
            Logger.debug(TAG, "Checking database accessibility")
            // Try to perform a simple database operation to check accessibility
            val testResult = userStatisticsRepository.getUserStatistics(DEFAULT_USER_ID)

            when (testResult) {
                is Result.Success -> {
                    Logger.debug(TAG, "Database access test successful")
                    true
                }
                is Result.Error -> {
                    Logger.warn(TAG, "Database access failed, attempting to create default statistics")
                    // Try to create default statistics if they don't exist
                    val createResult = userStatisticsRepository.createUserStatistics(DEFAULT_USER_ID)
                    when (createResult) {
                        is Result.Success -> {
                            Logger.debug(TAG, "Default statistics created successfully")
                            true
                        }
                        is Result.Error -> {
                            Logger.error(TAG, "Failed to create default statistics: ${createResult.exception.message}")
                            handleError(createResult.exception, "createDefaultStatistics")
                            false
                        }
                        is Result.Loading -> true
                    }
                }
                is Result.Loading -> {
                    Logger.debug(TAG, "Database still loading, considering accessible")
                    true // Still loading, consider accessible
                }
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Database accessibility check exception: ${e.message}")
            // Try to create default statistics as a last resort
            try {
                val createResult = userStatisticsRepository.createUserStatistics(DEFAULT_USER_ID)
                createResult is Result.Success
            } catch (createException: Exception) {
                Logger.error(TAG, "Last resort statistics creation failed: ${createException.message}")
                handleError(e, "databaseAccessibilityCheck")
                false
            }
        }
    }

    private suspend fun loadDataWithFallback(): DashboardUiState {
        return try {
            // First check if database is accessible
            val isDatabaseAccessible = ensureDatabaseAccessible()

            if (!isDatabaseAccessible) {
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

            val statisticsResult = userStatisticsRepository.getUserStatistics(DEFAULT_USER_ID)
            val recentTranscriptions = loadRecentTranscriptions()
            val trendData = loadTrendData()

            var statistics = when (statisticsResult) {
                is Result.Success -> statisticsResult.data
                is Result.Error -> {
                    handleError(statisticsResult.exception, "getUserStatistics")
                    null
                }
                is Result.Loading -> null
            }

            // Create default user statistics if they don't exist
            if (statistics == null) {
                val createResult = userStatisticsRepository.createUserStatistics(DEFAULT_USER_ID)

                if (createResult is Result.Success) {
                    // Try to get the newly created statistics
                    val newStatsResult = userStatisticsRepository.getUserStatistics(DEFAULT_USER_ID)
                    statistics = when (newStatsResult) {
                        is Result.Success -> newStatsResult.data
                        is Result.Error -> {
                            handleError(newStatsResult.exception, "getUserStatistics after creation")
                            createFallbackStatistics()
                        }
                        is Result.Loading -> createFallbackStatistics()
                    }
                } else if (createResult is Result.Error) {
                    handleError(createResult.exception, "createUserStatistics")
                    statistics = createFallbackStatistics()
                }
            }

            // Check if we should show sample data for empty state
            val isEmptyState = isStatisticsEmpty(statistics)
            val finalStatistics = if (isEmptyState) {
                createSampleStatistics()
            } else {
                statistics
            }

            val efficiencyMetrics = calculateEfficiencyMetrics(finalStatistics, recentTranscriptions)

            DashboardUiState(
                statistics = finalStatistics,
                recentTranscriptions = recentTranscriptions,
                trendData = trendData,
                timeSavedToday = calculateTimeSavedToday(recentTranscriptions),
                timeSavedTotal = efficiencyMetrics.timeSavedMs / 1000.0 / 60.0, // Convert to minutes
                efficiencyMultiplier = efficiencyMetrics.efficiencyRatio,
                averageSessionDuration = calculateAverageSessionDuration(recentTranscriptions),
                totalWordsTranscribed = finalStatistics?.totalWords ?: 0L,
                isLoading = false,
                lastUpdated = Clock.System.now(),
                cacheValid = true,
                isEmptyState = isEmptyState
            )
        } catch (e: Exception) {
            handleError(e, "loadDataWithFallback")

            // Always return a valid state, never let the UI get stuck
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
                cacheValid = true,
                isEmptyState = true
            )
        }
    }

    private fun createFallbackStatistics(): me.shadykhalifa.whispertop.domain.models.UserStatistics {
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

    private fun createSampleStatistics(): me.shadykhalifa.whispertop.domain.models.UserStatistics {
        return me.shadykhalifa.whispertop.domain.models.UserStatistics(
            id = DEFAULT_USER_ID,
            totalWords = 1250L,
            totalSessions = 8,
            totalSpeakingTimeMs = 180000L, // 3 minutes
            averageWordsPerMinute = 150.0,
            averageWordsPerSession = 156.25,
            userTypingWpm = DEFAULT_TYPING_WPM.toInt(),
            totalTranscriptions = 8L,
            totalDuration = 180f,
            averageAccuracy = 0.92f,
            dailyUsageCount = 8L,
            mostUsedLanguage = "en",
            mostUsedModel = "whisper-1",
            createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - 86400000, // 1 day ago
            updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        )
    }

    private fun isStatisticsEmpty(statistics: UserStatistics?): Boolean {
        return statistics == null ||
               (statistics.totalWords == 0L &&
                statistics.totalSessions == 0 &&
                statistics.totalSpeakingTimeMs == 0L &&
                statistics.totalTranscriptions == 0L)
    }
    
    private suspend fun loadRecentTranscriptions(): List<TranscriptionSession> {
        return try {
            val result = transcriptionHistoryRepository.getRecentTranscriptionSessions(RECENT_TRANSCRIPTIONS_LIMIT)

            when (result) {
                is Result.Success -> result.data
                is Result.Error -> {
                    handleError(result.exception, "loadRecentTranscriptions")
                    emptyList()
                }
                is Result.Loading -> emptyList()
            }
        } catch (e: Exception) {
            handleError(e, "loadRecentTranscriptions")
            emptyList()
        }
    }

    private suspend fun loadTrendData(): List<DailyUsage> {
        return try {
            val endDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val startDate = endDate.minus(DatePeriod(days = TREND_DATA_DAYS))

            val result = transcriptionHistoryRepository.getDailyUsage(startDate, endDate)

            when (result) {
                is Result.Success -> result.data
                is Result.Error -> {
                    handleError(result.exception, "loadTrendData")
                    emptyList()
                }
                is Result.Loading -> emptyList()
            }
        } catch (e: Exception) {
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
                    _uiState.value = newState.copy(isRefreshing = false, isLoading = false)
                    lastCacheUpdate = Clock.System.now()
                }
            } catch (e: Exception) {
                handleError(e, "refreshData")
                _uiState.value = _uiState.value.copy(isRefreshing = false, isLoading = false)
            }
        }
    }
    
    private suspend fun refreshStatistics() {
        if (!isCacheValid()) {
            loadInitialData()
        } else {
            // If cache is valid, just refresh the statistics data
            val statisticsResult = userStatisticsRepository.getUserStatistics(DEFAULT_USER_ID)
            if (statisticsResult is Result.Success && statisticsResult.data != null) {
                updateMutex.withLock {
                    val currentState = _uiState.value
                    val efficiencyMetrics = calculateEfficiencyMetrics(statisticsResult.data, currentState.recentTranscriptions)

                    _uiState.value = currentState.copy(
                        statistics = statisticsResult.data,
                        timeSavedTotal = efficiencyMetrics.timeSavedMs / 1000.0 / 60.0,
                        efficiencyMultiplier = efficiencyMetrics.efficiencyRatio,
                        totalWordsTranscribed = statisticsResult.data.totalWords,
                        isEmptyState = isStatisticsEmpty(statisticsResult.data),
                        lastUpdated = Clock.System.now()
                    )
                }
            }
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