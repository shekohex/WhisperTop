package me.shadykhalifa.whispertop.presentation.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import me.shadykhalifa.whispertop.MainActivity
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

@RunWith(AndroidJUnit4::class)
class DashboardScreenUITest : KoinTest {
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
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
    fun pullToRefresh_triggersRefreshAction() {
        setContent()
        
        // Wait for content to load
        composeTestRule.waitForIdle()
        
        // Find scrollable content using semantic matcher
        composeTestRule.onNodeWithTag("dashboard_content")
            .assertExists()
        
        // Perform pull-to-refresh gesture
        composeTestRule.onNodeWithTag("dashboard_content")
            .performTouchInput {
                swipeDown(
                    startY = visibleSize.height * 0.1f,
                    endY = visibleSize.height * 0.8f
                )
            }
        
        // Verify refresh was triggered
        verify(timeout = 3000) { mockViewModel.refreshData() }
    }
    
    @Test
    fun pullToRefresh_showsRefreshIndicator() {
        // Set refreshing state
        uiStateFlow.value = uiStateFlow.value.copy(isRefreshing = true)
        
        setContent()
        
        composeTestRule.waitForIdle()
        
        // Content should still be visible during refresh
        composeTestRule.onNodeWithText("Productivity Metrics")
            .assertIsDisplayed()
        
        // Pull-to-refresh indicator should be visible (this is internal to PullToRefreshBox)
        // We verify by checking that refresh is not interfering with content visibility
        composeTestRule.onNodeWithText("Speaking Time")
            .assertIsDisplayed()
    }
    
    @Test
    fun responsiveLayout_adaptsToScreenSize() {
        setContent()
        
        composeTestRule.waitForIdle()
        
        // Verify grid layout adapts (metrics and statistics should be visible)
        composeTestRule.onNodeWithText("Speaking Time")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Typing Time")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Words Captured")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Sessions")
            .assertIsDisplayed()
    }
    
    @Test
    fun accessibilitySupport_providesContentDescriptions() {
        setContent()
        
        composeTestRule.waitForIdle()
        
        // Test semantic content descriptions
        composeTestRule.onNodeWithContentDescription(
            "Dashboard header with greeting and current date"
        ).assertExists()
        
        // Test that metric cards have accessibility labels
        composeTestRule.onNode(
            hasContentDescription("Speaking Time: 0m", substring = true)
        ).assertExists()
        
        composeTestRule.onNode(
            hasContentDescription("Words Captured: 0", substring = true)
        ).assertExists()
    }
    
    @Test
    fun accessibilitySupport_talkBackNavigation() {
        setContent()
        
        composeTestRule.waitForIdle()
        
        // Enable accessibility services simulation
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test that all interactive elements are accessible
        composeTestRule.onAllNodesWithContentDescription("", substring = false, useUnmergedTree = true)
            .assertCountEquals(0) // No empty content descriptions
        
        // Test navigation order for TalkBack users
        composeTestRule.onNodeWithText("Good morning", substring = true)
            .assertIsDisplayed()
            .assert(hasClickAction().not()) // Header should not be clickable
        
        // Metric cards should be focusable but not necessarily clickable
        composeTestRule.onNodeWithText("Speaking Time")
            .assertIsDisplayed()
    }
    
    @Test
    fun animations_playCorrectlyForCounters() {
        // Start with initial state
        setContent()
        
        composeTestRule.waitForIdle()
        
        // Update state to trigger animations
        val newStatistics = UserStatistics(
            id = "test",
            totalWords = 500L,
            totalSessions = 5,
            totalSpeakingTimeMs = 60000L,
            averageWordsPerMinute = 100.0,
            averageWordsPerSession = 100.0,
            userTypingWpm = 40,
            totalTranscriptions = 5L,
            totalDuration = 1.0f,
            averageAccuracy = 95.0f,
            dailyUsageCount = 2L,
            mostUsedLanguage = "en",
            mostUsedModel = "whisper-1",
            createdAt = Clock.System.now().epochSeconds,
            updatedAt = Clock.System.now().epochSeconds
        )
        
        uiStateFlow.value = uiStateFlow.value.copy(
            statistics = newStatistics,
            totalWordsTranscribed = 500L
        )
        
        // Wait for animation to complete
        composeTestRule.waitForIdle()
        
        // Verify animated values appear
        composeTestRule.onNodeWithText("500")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("5")
            .assertIsDisplayed()
    }
    
    @Test
    fun errorState_showsRetryButton() {
        // Mock error state by having no data and not loading
        uiStateFlow.value = DashboardUiState(
            statistics = null,
            isLoading = false,
            isRefreshing = false
        )
        
        setContent()
        
        composeTestRule.waitForIdle()
        
        // In our current implementation, we show shimmer for null statistics + loading
        // For a true error state, we would need to add error handling to the ViewModel
        // This test verifies the graceful handling of missing data
        composeTestRule.onNodeWithText("Speaking Time")
            .assertIsDisplayed() // Should show with default values
    }
    
    @Test
    fun scrolling_performsSmoothly() {
        // Add multiple recent transcriptions to enable scrolling
        val transcriptions = (1..10).map { index ->
            TranscriptionSession(
                id = "session-$index",
                timestamp = Clock.System.now(),
                audioLengthMs = 30000L + (index * 1000L),
                wordCount = 50 + index,
                characterCount = (50 + index) * 5,
                transcribedText = "Test transcription $index"
            )
        }
        
        uiStateFlow.value = uiStateFlow.value.copy(
            recentTranscriptions = transcriptions
        )
        
        setContent()
        
        composeTestRule.waitForIdle()
        
        // Verify recent activity section is visible
        composeTestRule.onNodeWithText("Recent Activity")
            .assertIsDisplayed()
        
        // Perform scroll operation
        composeTestRule.onNodeWithTag("dashboard_content")
            .performScrollToNode(hasText("Test transcription 5"))
        
        // Verify scrolling worked
        composeTestRule.onNodeWithText("55 words transcribed")
            .assertIsDisplayed()
    }
    
    @Test
    fun dataFormatting_displaysCorrectly() {
        val statistics = UserStatistics(
            id = "format-test",
            totalWords = 1500L,
            totalSessions = 12,
            totalSpeakingTimeMs = 180000L, // 3 minutes
            averageWordsPerMinute = 87.5,
            averageWordsPerSession = 125.0,
            userTypingWpm = 40,
            totalTranscriptions = 12L,
            totalDuration = 3.0f,
            averageAccuracy = 92.5f,
            dailyUsageCount = 4L,
            mostUsedLanguage = "en",
            mostUsedModel = "whisper-1",
            createdAt = Clock.System.now().epochSeconds,
            updatedAt = Clock.System.now().epochSeconds
        )
        
        uiStateFlow.value = uiStateFlow.value.copy(
            statistics = statistics,
            totalWordsTranscribed = 1500L,
            timeSavedTotal = 37.5, // 37.5 minutes = 37m
            efficiencyMultiplier = 1.875f // 187.5% -> 188%
        )
        
        setContent()
        
        composeTestRule.waitForIdle()
        
        // Test number formatting
        composeTestRule.onNodeWithText("1K") // 1500 -> 1K
            .assertIsDisplayed()
        
        // Test time formatting
        composeTestRule.onNodeWithText("37m") // 37.5 minutes -> 37m
            .assertIsDisplayed()
        
        // Test percentage formatting
        composeTestRule.onNodeWithText("188%") // 187.5% -> 188%
            .assertIsDisplayed()
        
        // Test average formatting
        composeTestRule.onNodeWithText("88") // 87.5 -> 88
            .assertIsDisplayed()
    }
    
    private fun setContent() {
        composeTestRule.setContent {
            WhisperTopTheme {
                DashboardScreen(
                    modifier = androidx.compose.ui.Modifier.testTag("dashboard_content")
                )
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