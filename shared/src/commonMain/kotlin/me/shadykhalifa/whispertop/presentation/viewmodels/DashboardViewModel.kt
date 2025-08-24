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
    }
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    private val updateMutex = Mutex()
    private val calculationMutex = Mutex()
    
    private var lastCacheUpdate: Instant? = null
    
    init {
        loadInitialData()
        startRealTimeUpdates()
    }
    
    private fun loadInitialData() {
        launchSafely {
            updateMutex.withLock {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                try {
                    val statisticsResult = userStatisticsRepository.getUserStatistics(DEFAULT_USER_ID)
                    val recentTranscriptions = loadRecentTranscriptions()
                    val trendData = loadTrendData()
                    
                    val statistics = when (statisticsResult) {
                        is Result.Success -> statisticsResult.data
                        is Result.Error -> {
                            handleError(statisticsResult.exception)
                            null
                        }
                        is Result.Loading -> null
                    }
                    
                    val efficiencyMetrics = calculateEfficiencyMetrics(statistics, recentTranscriptions)
                    
                    _uiState.value = _uiState.value.copy(
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
                    
                    lastCacheUpdate = Clock.System.now()
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    handleError(e, "loadInitialData")
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
    
    private suspend fun loadRecentTranscriptions(): List<TranscriptionSession> {
        return try {
            when (val result = transcriptionHistoryRepository.getRecentTranscriptionSessions(RECENT_TRANSCRIPTIONS_LIMIT)) {
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
            
            when (val result = transcriptionHistoryRepository.getDailyUsage(startDate, endDate)) {
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
                loadInitialData()
            } finally {
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