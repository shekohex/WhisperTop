package me.shadykhalifa.whispertop.presentation.viewmodels

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class DashboardViewModelTest {
    
    private lateinit var userStatisticsRepository: UserStatisticsRepository
    private lateinit var transcriptionHistoryRepository: TranscriptionHistoryRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var metricsCollector: MetricsCollector
    private lateinit var errorHandler: ViewModelErrorHandler
    private lateinit var viewModel: DashboardViewModel
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @BeforeTest
    fun setup() {
        userStatisticsRepository = mockk()
        transcriptionHistoryRepository = mockk()
        settingsRepository = mockk()
        metricsCollector = mockk()
        errorHandler = mockk(relaxed = true)
        
        // Setup default mock responses
        coEvery { userStatisticsRepository.getUserStatisticsFlow(any()) } returns flowOf(createTestUserStatistics())
        coEvery { userStatisticsRepository.getUserStatistics(any()) } returns Result.Success(createTestUserStatistics())
        coEvery { transcriptionHistoryRepository.getRecentTranscriptionSessions(any()) } returns Result.Success(createTestTranscriptionSessions())
        coEvery { transcriptionHistoryRepository.getDailyUsage(any(), any()) } returns Result.Success(createTestDailyUsage())
        coEvery { settingsRepository.settings } returns flowOf(AppSettings())
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
            totalSpeakingTimeMs = 300000L, // 5 minutes
            averageWordsPerMinute = 100.0,
            averageWordsPerSession = 100.0,
            userTypingWpm = 40,
            totalTranscriptions = 50L,
            totalDuration = 300.0f, // 5 minutes
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
                audioLengthMs = 30000L, // 30 seconds
                wordCount = 50,
                characterCount = 250,
                transcribedText = "This is a test transcription with fifty words to test our calculation methods and ensure accuracy in our time saved computations throughout the system."
            ),
            TranscriptionSession(
                id = "session-2",
                timestamp = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() - 60000L),
                audioLengthMs = 60000L, // 1 minute
                wordCount = 100,
                characterCount = 500,
                transcribedText = "Another test transcription that is exactly one hundred words long for testing purposes and ensuring our calculations are working properly in all scenarios."
            )
        )
    }
    
    private fun createTestDailyUsage(): List<DailyUsage> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return listOf(
            DailyUsage(
                date = today,
                sessionsCount = 5,
                wordsTranscribed = 500L,
                totalTimeMs = 300000L
            ),
            DailyUsage(
                date = today.minus(kotlinx.datetime.DatePeriod(days = 1)),
                sessionsCount = 3,
                wordsTranscribed = 300L,
                totalTimeMs = 180000L
            )
        )
    }
    
    @Test
    fun `initial state is correct`() = runTest {
        viewModel = createViewModel()
        
        val initialState = viewModel.uiState.value
        
        assertFalse(initialState.isLoading)
        assertEquals(0.0, initialState.timeSavedToday)
        assertEquals(0.0, initialState.timeSavedTotal)
        assertEquals(1.0f, initialState.efficiencyMultiplier)
        assertEquals(0.0, initialState.averageSessionDuration)
        assertEquals(0L, initialState.totalWordsTranscribed)
        assertTrue(initialState.recentTranscriptions.isEmpty())
        assertTrue(initialState.trendData.isEmpty())
    }
    
    @Test
    fun `loadInitialData sets loading state correctly`() = runTest {
        viewModel = createViewModel()
        
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertFalse(initialState.isLoading)
            
            // The ViewModel loads data on init, so we should get a loaded state
            val loadedState = awaitItem()
            assertNotNull(loadedState.statistics)
            assertNotNull(loadedState.lastUpdated)
            assertTrue(loadedState.cacheValid)
        }
    }
    
    @Test
    fun `calculateTimeSaved works correctly with multiple sessions`() = runTest {
        viewModel = createViewModel()
        
        val sessions = createTestTranscriptionSessions()
        val timeSaved = viewModel.calculateTimeSaved(sessions, typingWpm = 40.0)
        
        // For 150 total words (50 + 100):
        // Typing time = 150 words / 40 WPM = 3.75 minutes = 225 seconds
        // Speaking time = 30 seconds + 60 seconds = 90 seconds = 1.5 minutes  
        // Time saved = 225 - 90 = 135 seconds = 2.25 minutes
        val expectedTimeSaved = (150.0 / 40.0) - (90.0 / 60.0) // in minutes
        
        assertEquals(expectedTimeSaved, timeSaved, 0.01)
        assertTrue(timeSaved > 0) // Should save time since typing is slower than speaking
    }
    
    @Test
    fun `calculateTimeSaved handles empty sessions list`() = runTest {
        viewModel = createViewModel()
        
        val timeSaved = viewModel.calculateTimeSaved(emptyList())
        
        assertEquals(0.0, timeSaved)
    }
    
    @Test
    fun `calculateTimeSaved never returns negative time`() = runTest {
        viewModel = createViewModel()
        
        // Create a session where speaking takes longer than typing would
        val slowSpeakingSession = TranscriptionSession(
            id = "slow-session",
            timestamp = Clock.System.now(),
            audioLengthMs = 300000L, // 5 minutes of speaking
            wordCount = 10, // Only 10 words
            characterCount = 50,
            transcribedText = "Only ten words here total exactly"
        )
        
        val timeSaved = viewModel.calculateTimeSaved(listOf(slowSpeakingSession))
        
        assertTrue(timeSaved >= 0.0, "Time saved should never be negative")
    }
    
    @Test
    fun `refreshData triggers data reload`() = runTest {
        viewModel = createViewModel()
        
        viewModel.refreshData()
        
        coVerify(atLeast = 1) { 
            userStatisticsRepository.getUserStatistics(any()) 
            transcriptionHistoryRepository.getRecentTranscriptionSessions(any())
            transcriptionHistoryRepository.getDailyUsage(any(), any())
        }
    }
    
    @Test
    fun `getEfficiencyInsights returns correct insights`() = runTest {
        viewModel = createViewModel()
        
        // Wait for initial load to complete
        viewModel.uiState.test {
            awaitItem() // initial state
            awaitItem() // loaded state
            
            val insights = viewModel.getEfficiencyInsights()
            
            assertTrue(insights.containsKey("totalTimeSavedHours"))
            assertTrue(insights.containsKey("averageEfficiency"))
            assertTrue(insights.containsKey("productivityGain"))
            assertTrue(insights.containsKey("sessionsToday"))
            assertTrue(insights.containsKey("averageSessionLength"))
            assertTrue(insights.containsKey("totalWords"))
            
            // Check that values are reasonable
            val totalWords = insights["totalWords"] as Long
            assertTrue(totalWords >= 0)
            
            val averageEfficiency = insights["averageEfficiency"] as Float
            assertTrue(averageEfficiency >= 0.0f)
        }
    }
    
    @Test
    fun `error handling works correctly for repository failures`() = runTest {
        coEvery { userStatisticsRepository.getUserStatistics(any()) } returns Result.Error(Exception("Test error"))
        
        viewModel = createViewModel()
        
        viewModel.uiState.test {
            awaitItem() // initial state
            awaitItem() // state after error handling
            
            coVerify { errorHandler.handleError(any<Throwable>(), any()) }
        }
    }
    
    @Test
    fun `real-time updates work when statistics change`() = runTest {
        val updatedStatistics = createTestUserStatistics().copy(totalWords = 10000L)
        
        // Create initial flow, then updated flow
        val statsFlow = kotlinx.coroutines.flow.MutableStateFlow(createTestUserStatistics())
        coEvery { userStatisticsRepository.getUserStatisticsFlow(any()) } returns statsFlow
        
        viewModel = createViewModel()
        
        viewModel.uiState.test {
            val initialState = awaitItem()
            val loadedState = awaitItem()
            
            // Update the statistics
            statsFlow.value = updatedStatistics
            
            val updatedState = awaitItem()
            
            // Should trigger refresh of calculations
            assertNotNull(updatedState.lastUpdated)
        }
    }
    
    @Test
    fun `cache invalidation works correctly`() = runTest {
        viewModel = createViewModel()
        
        viewModel.uiState.test {
            awaitItem() // initial
            val loadedState = awaitItem() // loaded
            assertTrue(loadedState.cacheValid)
            
            viewModel.invalidateCache()
            
            val invalidatedState = awaitItem()
            assertFalse(invalidatedState.cacheValid)
        }
    }
    
    @Test
    fun `efficiency multiplier calculation is correct`() = runTest {
        viewModel = createViewModel()
        
        viewModel.uiState.test {
            awaitItem() // initial
            val loadedState = awaitItem() // loaded with data
            
            // With our test data:
            // Total words: 5000, Total speaking time: 300000ms (5 minutes)
            // Typing time for 5000 words at 40 WPM = 5000/40 = 125 minutes
            // Efficiency = 125 minutes / 5 minutes = 25.0
            assertTrue(loadedState.efficiencyMultiplier > 1.0f, 
                "Efficiency multiplier should be greater than 1 for time savings")
        }
    }
    
    @Test
    fun `average session duration is calculated correctly`() = runTest {
        viewModel = createViewModel()
        
        viewModel.uiState.test {
            awaitItem() // initial  
            val loadedState = awaitItem() // loaded
            
            // Test sessions: 30s + 60s = 90s total, 2 sessions = 45s average
            assertEquals(45.0, loadedState.averageSessionDuration, 1.0)
        }
    }
}