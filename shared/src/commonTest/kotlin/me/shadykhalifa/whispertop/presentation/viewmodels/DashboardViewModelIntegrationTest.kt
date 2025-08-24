package me.shadykhalifa.whispertop.presentation.viewmodels

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.minus
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.DailyUsage
import me.shadykhalifa.whispertop.domain.models.TranscriptionSession
import me.shadykhalifa.whispertop.domain.models.UserStatistics
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.domain.repositories.UserStatisticsRepository
import me.shadykhalifa.whispertop.domain.services.MetricsCollector
import me.shadykhalifa.whispertop.presentation.utils.ViewModelErrorHandler
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DashboardViewModelIntegrationTest {
    
    private lateinit var userStatisticsRepository: UserStatisticsRepository
    private lateinit var transcriptionHistoryRepository: TranscriptionHistoryRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var metricsCollector: MetricsCollector
    private lateinit var errorHandler: ViewModelErrorHandler
    
    private lateinit var userStatisticsFlow: MutableStateFlow<UserStatistics?>
    private lateinit var settingsFlow: MutableStateFlow<AppSettings>
    
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeTest
    fun setup() {
        userStatisticsRepository = mockk()
        transcriptionHistoryRepository = mockk()
        settingsRepository = mockk()
        metricsCollector = mockk()
        errorHandler = mockk(relaxed = true)
        
        userStatisticsFlow = MutableStateFlow(createTestUserStatistics())
        settingsFlow = MutableStateFlow(AppSettings())
        
        // Setup flow-based mock responses for real-time updates
        coEvery { userStatisticsRepository.getUserStatisticsFlow(any()) } returns userStatisticsFlow
        coEvery { userStatisticsRepository.getUserStatistics(any()) } returns Result.Success(createTestUserStatistics())
        coEvery { transcriptionHistoryRepository.getRecentTranscriptionSessions(any()) } returns Result.Success(createTestTranscriptionSessions())
        coEvery { transcriptionHistoryRepository.getDailyUsage(any(), any()) } returns Result.Success(createTestDailyUsage())
        coEvery { settingsRepository.settings } returns settingsFlow
    }
    
    private fun createViewModel(): DashboardViewModel {
        return DashboardViewModel(
            userStatisticsRepository = userStatisticsRepository,
            transcriptionHistoryRepository = transcriptionHistoryRepository,
            settingsRepository = settingsRepository,
            metricsCollector = metricsCollector,
            errorHandler = errorHandler
        )
    }
    
    private fun createTestUserStatistics(): UserStatistics {
        return UserStatistics(
            id = "test-user",
            totalWords = 5000L,
            totalSessions = 50,
            totalSpeakingTimeMs = 300000L,
            averageWordsPerMinute = 100.0,
            averageWordsPerSession = 100.0,
            userTypingWpm = 40,
            totalTranscriptions = 50L,
            totalDuration = 300.0f,
            averageAccuracy = 0.95f,
            dailyUsageCount = 10L,
            mostUsedLanguage = "en",
            mostUsedModel = "whisper-1",
            createdAt = Clock.System.now().epochSeconds,
            updatedAt = Clock.System.now().epochSeconds
        )
    }
    
    private fun createTestTranscriptionSessions(): List<TranscriptionSession> {
        val now = Clock.System.now()
        return listOf(
            TranscriptionSession(
                id = "session-1",
                timestamp = now,
                audioLengthMs = 30000L,
                wordCount = 50,
                characterCount = 250,
                transcribedText = "Test transcription session one with fifty words total for accurate testing purposes."
            ),
            TranscriptionSession(
                id = "session-2",
                timestamp = now,
                audioLengthMs = 60000L,
                wordCount = 100,
                characterCount = 500,
                transcribedText = "Test transcription session two with exactly one hundred words for comprehensive testing."
            )
        )
    }
    
    private fun createTestDailyUsage(): List<DailyUsage> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return listOf(
            DailyUsage(
                date = today.minus(DatePeriod(days = 1)),
                sessionsCount = 3,
                wordsTranscribed = 300L,
                totalTimeMs = 180000L
            )
        )
    }
    
    @Test
    fun `ViewModel initializes correctly and loads data`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        
        viewModel.uiState.test {
            // Initial empty state
            val initialState = awaitItem()
            assertEquals(0.0, initialState.timeSavedToday)
            assertTrue(initialState.recentTranscriptions.isEmpty())
            
            // After data loads
            val loadedState = awaitItem()
            assertNotNull(loadedState.statistics)
            assertTrue(loadedState.recentTranscriptions.isNotEmpty())
            assertTrue(loadedState.trendData.isNotEmpty())
            assertNotNull(loadedState.lastUpdated)
            assertTrue(loadedState.cacheValid)
            
            // Verify repository calls
            coVerify { userStatisticsRepository.getUserStatistics(any()) }
            coVerify { transcriptionHistoryRepository.getRecentTranscriptionSessions(any()) }
            coVerify { transcriptionHistoryRepository.getDailyUsage(any(), any()) }
        }
    }
    
    @Test
    fun `real-time statistics updates trigger UI state changes`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        
        viewModel.uiState.test {
            // Skip initial states
            awaitItem() // initial
            val initialLoadedState = awaitItem() // loaded
            
            val initialTotalWords = initialLoadedState.totalWordsTranscribed
            
            // Update statistics with different data
            val updatedStats = createTestUserStatistics().copy(
                totalWords = 10000L, // Double the words
                totalSessions = 100,
                updatedAt = Clock.System.now().epochSeconds
            )
            
            userStatisticsFlow.value = updatedStats
            
            // Should get an updated state
            val updatedState = awaitItem()
            
            // Total words should be updated
            assertNotEquals(initialTotalWords, updatedState.totalWordsTranscribed)
            assertEquals(10000L, updatedState.totalWordsTranscribed)
            
            // Last updated should be refreshed
            assertNotNull(updatedState.lastUpdated)
        }
    }
    
    @Test
    fun `settings changes trigger recalculation`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        
        viewModel.uiState.test {
            awaitItem() // initial
            val initialState = awaitItem() // loaded
            val initialTimeSaved = initialState.timeSavedTotal
            
            // Update settings (this might affect WPM calculations in the future)
            val updatedSettings = AppSettings(
                enableUsageAnalytics = true,
                enableHapticFeedback = true
            )
            
            settingsFlow.value = updatedSettings
            
            // Should trigger recalculation
            val recalculatedState = awaitItem()
            
            // Last updated should be newer
            assertNotNull(recalculatedState.lastUpdated)
            assertTrue(
                recalculatedState.lastUpdated!! >= initialState.lastUpdated!!
            )
        }
    }
    
    @Test
    fun `concurrent updates are handled safely with mutex`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        
        viewModel.uiState.test {
            awaitItem() // initial
            awaitItem() // loaded
            
            // Trigger multiple concurrent updates
            repeat(5) { index ->
                val updatedStats = createTestUserStatistics().copy(
                    totalWords = (1000L * (index + 1)),
                    updatedAt = Clock.System.now().epochSeconds + index
                )
                userStatisticsFlow.value = updatedStats
            }
            
            // Should receive updates, but they should be serialized safely
            var updateCount = 0
            var lastTotalWords = 0L
            
            // Collect multiple state updates
            repeat(5) {
                val state = awaitItem()
                updateCount++
                
                // Total words should be increasing (or at least not corrupted)
                assertTrue(state.totalWordsTranscribed >= lastTotalWords)
                lastTotalWords = state.totalWordsTranscribed
                
                // State should be consistent
                assertNotNull(state.lastUpdated)
                assertTrue(state.cacheValid)
            }
            
            assertTrue(updateCount > 0, "Should have received multiple updates")
        }
    }
    
    @Test
    fun `refresh data correctly updates all state components`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        
        viewModel.uiState.test {
            awaitItem() // initial
            val beforeRefresh = awaitItem() // loaded
            
            // Prepare updated data for refresh
            val newStats = createTestUserStatistics().copy(totalWords = 15000L)
            val newSessions = listOf(
                TranscriptionSession(
                    id = "new-session",
                    timestamp = Clock.System.now(),
                    audioLengthMs = 45000L,
                    wordCount = 75,
                    characterCount = 375,
                    transcribedText = "New transcription session for testing refresh functionality with seventy five words exactly."
                )
            )
            
            coEvery { userStatisticsRepository.getUserStatistics(any()) } returns Result.Success(newStats)
            coEvery { transcriptionHistoryRepository.getRecentTranscriptionSessions(any()) } returns Result.Success(newSessions)
            
            // Trigger manual refresh
            viewModel.refreshData()
            
            // Should get refreshing state
            val refreshingState = awaitItem()
            assertTrue(refreshingState.isRefreshing)
            
            // Then updated state
            val refreshedState = awaitItem()
            assertNotEquals(beforeRefresh.totalWordsTranscribed, refreshedState.totalWordsTranscribed)
            assertNotEquals(beforeRefresh.recentTranscriptions.size, refreshedState.recentTranscriptions.size)
            assertNotNull(refreshedState.lastUpdated)
        }
    }
    
    @Test
    fun `cache invalidation and refresh cycle works correctly`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        
        viewModel.uiState.test {
            awaitItem() // initial
            val initialState = awaitItem() // loaded
            assertTrue(initialState.cacheValid)
            
            // Invalidate cache
            viewModel.invalidateCache()
            
            val invalidatedState = awaitItem()
            assertNotNull(invalidatedState)
            // Note: cache validity might be updated in the next refresh cycle
            
            // Refresh should restore cache validity
            viewModel.refreshData()
            
            val refreshingState = awaitItem()
            assertTrue(refreshingState.isRefreshing)
            
            val restoredState = awaitItem()
            assertTrue(restoredState.cacheValid)
            assertNotNull(restoredState.lastUpdated)
        }
    }
    
    @Test
    fun `error recovery works correctly after repository failures`() = runTest(testDispatcher) {
        // Start with failing repository
        coEvery { userStatisticsRepository.getUserStatistics(any()) } returns Result.Error(Exception("Network error"))
        
        val viewModel = createViewModel()
        
        viewModel.uiState.test {
            awaitItem() // initial
            val errorState = awaitItem() // after error
            
            // Should handle error gracefully
            coVerify { errorHandler.handleError(any<Throwable>(), any()) }
            
            // Fix the repository and refresh
            coEvery { userStatisticsRepository.getUserStatistics(any()) } returns Result.Success(createTestUserStatistics())
            
            viewModel.refreshData()
            
            val refreshingState = awaitItem()
            assertTrue(refreshingState.isRefreshing)
            
            val recoveredState = awaitItem()
            assertNotNull(recoveredState.statistics)
            assertNotNull(recoveredState.lastUpdated)
            assertTrue(recoveredState.cacheValid)
        }
    }
    
    @Test
    fun `efficiency insights calculation remains consistent during updates`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        
        viewModel.uiState.test {
            awaitItem() // initial
            awaitItem() // loaded
            
            val initialInsights = viewModel.getEfficiencyInsights()
            
            // Update statistics
            val updatedStats = createTestUserStatistics().copy(totalWords = 8000L)
            userStatisticsFlow.value = updatedStats
            
            awaitItem() // updated state
            
            val updatedInsights = viewModel.getEfficiencyInsights()
            
            // Structure should remain consistent
            assertEquals(initialInsights.keys, updatedInsights.keys)
            
            // Values should be updated appropriately
            assertTrue(
                (updatedInsights["totalWords"] as Long) > (initialInsights["totalWords"] as Long)
            )
            
            // All numeric values should be reasonable
            assertTrue((updatedInsights["averageEfficiency"] as Float) >= 0.0f)
            assertTrue((updatedInsights["productivityGain"] as Float) >= 0.0f)
            assertTrue((updatedInsights["totalTimeSavedHours"] as Double) >= 0.0)
        }
    }
}