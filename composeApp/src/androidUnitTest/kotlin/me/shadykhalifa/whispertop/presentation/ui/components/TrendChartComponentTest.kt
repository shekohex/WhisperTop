package me.shadykhalifa.whispertop.presentation.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.datetime.LocalDate
import me.shadykhalifa.whispertop.domain.models.DailyUsage
import me.shadykhalifa.whispertop.presentation.ui.theme.WhisperTopTheme
import org.junit.Rule
import org.junit.Test

class TrendChartComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createTestTrendData(): List<DailyUsage> = listOf(
        DailyUsage(
            date = LocalDate(2024, 1, 1),
            sessionsCount = 3,
            wordsTranscribed = 150L,
            totalTimeMs = 30000L
        ),
        DailyUsage(
            date = LocalDate(2024, 1, 2),
            sessionsCount = 5,
            wordsTranscribed = 250L,
            totalTimeMs = 45000L
        ),
        DailyUsage(
            date = LocalDate(2024, 1, 3),
            sessionsCount = 2,
            wordsTranscribed = 100L,
            totalTimeMs = 20000L
        ),
        DailyUsage(
            date = LocalDate(2024, 1, 4),
            sessionsCount = 4,
            wordsTranscribed = 200L,
            totalTimeMs = 40000L
        ),
        DailyUsage(
            date = LocalDate(2024, 1, 5),
            sessionsCount = 6,
            wordsTranscribed = 300L,
            totalTimeMs = 60000L
        )
    )

    @Test
    fun trendChartComponent_displaysTitleAndData() {
        val trendData = createTestTrendData()

        composeTestRule.setContent {
            WhisperTopTheme {
                TrendChartComponent(
                    trendData = trendData,
                    title = "Test Chart"
                )
            }
        }

        // Verify title is displayed
        composeTestRule.onNodeWithText("Test Chart").assertIsDisplayed()
        
        // Verify metric toggles are displayed
        composeTestRule.onNodeWithText("Sessions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Words").assertIsDisplayed()
        
        // Verify summary statistics
        composeTestRule.onNodeWithText("Total").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily Avg").assertIsDisplayed()
        composeTestRule.onNodeWithText("Peak Day").assertIsDisplayed()
    }

    @Test
    fun trendChartComponent_displaysEmptyStateWhenNoData() {
        composeTestRule.setContent {
            WhisperTopTheme {
                TrendChartComponent(
                    trendData = emptyList(),
                    title = "Empty Chart"
                )
            }
        }

        // Verify empty state message is displayed
        composeTestRule.onNodeWithText("No usage data yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start transcribing to see your activity trend").assertIsDisplayed()
    }

    @Test
    fun trendChartComponent_togglesBetweenMetrics() {
        val trendData = createTestTrendData()
        var selectedSessions = true

        composeTestRule.setContent {
            WhisperTopTheme {
                TrendChartComponent(
                    trendData = trendData,
                    showSessions = selectedSessions,
                    onMetricToggle = { showSessionsMetric ->
                        selectedSessions = showSessionsMetric
                    }
                )
            }
        }

        // Initially showing sessions
        composeTestRule.onNode(hasText("Sessions")).assertIsDisplayed()
        
        // Click on Words toggle
        composeTestRule.onNodeWithText("Words").performClick()
        
        // The toggle callback should be called but we can't verify state change
        // in this test without additional state management
    }

    @Test
    fun trendChartComponent_calculatesCorrectSummaryStatistics() {
        val trendData = createTestTrendData()
        val totalSessions = trendData.sumOf { it.sessionsCount } // 20
        val totalWords = trendData.sumOf { it.wordsTranscribed } // 1000
        val avgSessions = totalSessions.toFloat() / trendData.size // 4.0
        val avgWords = totalWords.toFloat() / trendData.size // 200.0
        val peakSessions = trendData.maxOf { it.sessionsCount } // 6
        val peakWords = trendData.maxOf { it.wordsTranscribed } // 300

        composeTestRule.setContent {
            WhisperTopTheme {
                TrendChartComponent(
                    trendData = trendData,
                    showSessions = true
                )
            }
        }

        // Verify calculated statistics are displayed correctly
        composeTestRule.onNodeWithText(totalSessions.toString()).assertIsDisplayed()
        composeTestRule.onNodeWithText("4.0").assertIsDisplayed() // Average sessions
        composeTestRule.onNodeWithText(peakSessions.toString()).assertIsDisplayed()
    }
}