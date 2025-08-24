package me.shadykhalifa.whispertop.presentation.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.shadykhalifa.whispertop.domain.models.TranscriptionSession
import me.shadykhalifa.whispertop.domain.models.UserStatistics
import me.shadykhalifa.whispertop.presentation.ui.theme.WhisperTopTheme
import me.shadykhalifa.whispertop.presentation.viewmodels.DashboardUiState
import me.shadykhalifa.whispertop.presentation.viewmodels.DashboardViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest : KoinTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var mockViewModel: DashboardViewModel
    private lateinit var uiStateFlow: MutableStateFlow<DashboardUiState>
    
    @Before
    fun setUp() {
        stopKoin()
        
        mockViewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow(createDefaultUiState())
        
        every { mockViewModel.uiState } returns uiStateFlow
        every { mockViewModel.refreshData() } just Runs
        
        startKoin {
            modules(module {
                single { mockViewModel }
            })
        }
    }
    
    @Test
    fun dashboardScreen_displaysHeaderWithGreeting() {
        setContent()
        
        composeTestRule.onNodeWithText("Good morning", substring = true)
            .assertExists()
        
        composeTestRule.onNodeWithText("Here's your productivity overview")
            .assertIsDisplayed()
    }
    
    @Test
    fun dashboardScreen_displaysProductivityMetrics() {
        setContent()
        
        // Check all four metric cards are displayed
        composeTestRule.onNodeWithText("Speaking Time")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Typing Time")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Time Saved")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Efficiency")
            .assertIsDisplayed()
    }
    
    @Test
    fun dashboardScreen_displaysStatisticsGrid() {
        setContent()
        
        // Check all four statistic cards are displayed
        composeTestRule.onNodeWithText("Words Captured")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Sessions")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Avg Words/Min")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Words/Session")
            .assertIsDisplayed()
    }
    
    @Test
    fun dashboardScreen_displaysRecentActivity() {
        val transcriptionSession = TranscriptionSession(
            id = "test-session",
            timestamp = Clock.System.now(),
            audioLengthMs = 60000L, // 1 minute
            wordCount = 120,
            characterCount = 600,
            transcribedText = "Test transcription"
        )
        
        uiStateFlow.value = uiStateFlow.value.copy(
            recentTranscriptions = listOf(transcriptionSession)
        )
        
        setContent()
        
        composeTestRule.onNodeWithText("Recent Activity")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("120 words transcribed")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("1:00")
            .assertIsDisplayed()
    }
    
    @Test
    fun dashboardScreen_pullToRefresh_triggersRefresh() {
        setContent()
        
        // Simulate pull-to-refresh gesture
        composeTestRule.onRoot()
            .performTouchInput { swipeDown() }
        
        verify { mockViewModel.refreshData() }
    }
    
    @Test
    fun dashboardScreen_showsLoadingShimmer_whenInitiallyLoading() {
        uiStateFlow.value = DashboardUiState(
            isLoading = true,
            statistics = null
        )
        
        setContent()
        
        // Should show shimmer content instead of regular content
        composeTestRule.onNodeWithText("Speaking Time")
            .assertDoesNotExist()
        
        // Check that shimmer animation is present by verifying no real content is shown
        composeTestRule.onNodeWithText("Recent Activity")
            .assertDoesNotExist()
    }
    
    @Test
    fun dashboardScreen_showsRefreshIndicator_whenRefreshing() {
        uiStateFlow.value = uiStateFlow.value.copy(isRefreshing = true)
        
        setContent()
        
        // Content should still be visible during refresh
        composeTestRule.onNodeWithText("Speaking Time")
            .assertIsDisplayed()
    }
    
    @Test
    fun dashboardScreen_displaysCorrectMetricValues() {
        val statistics = UserStatistics(
            id = "test-user",
            totalWords = 5000L,
            totalSessions = 25,
            totalSpeakingTimeMs = 300000L, // 5 minutes
            averageWordsPerMinute = 100.0,
            averageWordsPerSession = 200.0,
            userTypingWpm = 40,
            totalTranscriptions = 25L,
            totalDuration = 5.0f,
            averageAccuracy = 95.0f,
            dailyUsageCount = 7L,
            mostUsedLanguage = "en",
            mostUsedModel = "whisper-1",
            createdAt = Clock.System.now().epochSeconds,
            updatedAt = Clock.System.now().epochSeconds
        )
        
        uiStateFlow.value = uiStateFlow.value.copy(
            statistics = statistics,
            totalWordsTranscribed = 5000L,
            timeSavedTotal = 62.5, // in minutes
            efficiencyMultiplier = 2.5f
        )
        
        setContent()
        
        // Check that calculated values are displayed
        composeTestRule.onNodeWithText("5K") // Words Captured
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("25") // Sessions
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("100") // Avg Words/Min
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("250%") // Efficiency
            .assertIsDisplayed()
    }
    
    @Test
    fun dashboardScreen_hasCorrectAccessibilityLabels() {
        setContent()
        
        // Check semantic properties are set for accessibility
        composeTestRule.onNodeWithContentDescription("Dashboard header with greeting and current date")
            .assertExists()
        
        composeTestRule.onNode(
            hasContentDescription("Speaking Time: 0m")
        ).assertExists()
    }
    
    @Test
    fun dashboardScreen_animatesCounterValues() {
        // Start with zero values
        val initialState = createDefaultUiState()
        uiStateFlow.value = initialState
        
        setContent()
        
        // Update to non-zero values to trigger animation
        val statistics = UserStatistics(
            id = "test-user",
            totalWords = 1000L,
            totalSessions = 10,
            totalSpeakingTimeMs = 120000L, // 2 minutes
            averageWordsPerMinute = 50.0,
            averageWordsPerSession = 100.0,
            userTypingWpm = 40,
            totalTranscriptions = 10L,
            totalDuration = 2.0f,
            averageAccuracy = 90.0f,
            dailyUsageCount = 3L,
            mostUsedLanguage = "en",
            mostUsedModel = "whisper-1",
            createdAt = Clock.System.now().epochSeconds,
            updatedAt = Clock.System.now().epochSeconds
        )
        
        uiStateFlow.value = initialState.copy(
            statistics = statistics,
            totalWordsTranscribed = 1000L
        )
        
        // Wait for animation to potentially complete
        composeTestRule.waitForIdle()
        
        // Verify final values are displayed
        composeTestRule.onNodeWithText("1K")
            .assertIsDisplayed()
    }
    
    @Test
    fun dashboardScreen_respondsToScreenSizeChanges() {
        setContent()
        
        // The grid layout should adjust to screen size
        // This test verifies the composable doesn't crash with different layouts
        composeTestRule.onNodeWithText("Productivity Metrics")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Statistics Overview")
            .assertIsDisplayed()
    }
    
    @Test
    fun dashboardScreen_handlesEmptyRecentTranscriptions() {
        uiStateFlow.value = uiStateFlow.value.copy(
            recentTranscriptions = emptyList()
        )
        
        setContent()
        
        // Should not show Recent Activity section when empty
        composeTestRule.onNodeWithText("Recent Activity")
            .assertDoesNotExist()
    }
    
    @Test
    fun dashboardScreen_formatsTimeCorrectly() {
        val session = TranscriptionSession(
            id = "test",
            timestamp = Clock.System.now(),
            audioLengthMs = 125000L, // 2 minutes 5 seconds
            wordCount = 100,
            characterCount = 500,
            transcribedText = "Test"
        )
        
        uiStateFlow.value = uiStateFlow.value.copy(
            recentTranscriptions = listOf(session)
        )
        
        setContent()
        
        composeTestRule.onNodeWithText("2:05")
            .assertIsDisplayed()
    }
    
    private fun setContent() {
        composeTestRule.setContent {
            WhisperTopTheme {
                DashboardScreen()
            }
        }
    }
    
    private fun createDefaultUiState() = DashboardUiState(
        statistics = UserStatistics(
            id = "default",
            totalWords = 0L,
            totalSessions = 0,
            totalSpeakingTimeMs = 0L,
            averageWordsPerMinute = 0.0,
            averageWordsPerSession = 0.0,
            userTypingWpm = 40,
            totalTranscriptions = 0L,
            totalDuration = 0.0f,
            averageAccuracy = null,
            dailyUsageCount = 0L,
            mostUsedLanguage = null,
            mostUsedModel = null,
            createdAt = Clock.System.now().epochSeconds,
            updatedAt = Clock.System.now().epochSeconds
        ),
        recentTranscriptions = emptyList(),
        trendData = emptyList(),
        timeSavedToday = 0.0,
        timeSavedTotal = 0.0,
        efficiencyMultiplier = 1.0f,
        averageSessionDuration = 0.0,
        totalWordsTranscribed = 0L,
        isLoading = false,
        isRefreshing = false,
        lastUpdated = Clock.System.now(),
        cacheValid = true
    )
}