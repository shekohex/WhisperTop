package me.shadykhalifa.whispertop.domain.usecases

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import me.shadykhalifa.whispertop.domain.models.DailyStatistics
import me.shadykhalifa.whispertop.domain.models.SessionMetrics
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.domain.repositories.SessionMetricsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.domain.repositories.UserStatisticsRepository
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatisticsCalculatorUseCaseTest {
    
    private val sessionMetricsRepository = mockk<SessionMetricsRepository>()
    private val userStatisticsRepository = mockk<UserStatisticsRepository>()
    private val transcriptionHistoryRepository = mockk<TranscriptionHistoryRepository>()
    
    private val useCase = StatisticsCalculatorUseCaseImpl(
        sessionMetricsRepository = sessionMetricsRepository,
        userStatisticsRepository = userStatisticsRepository,
        transcriptionHistoryRepository = transcriptionHistoryRepository
    )
    
    @Test
    fun `calculateDailyStatistics returns empty stats when no sessions found`() = runTest {
        // Given
        val date = LocalDate(2023, 12, 1)
        val timeZone = TimeZone.currentSystemDefault()
        val startOfDay = date.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val endOfDay = date.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toEpochMilliseconds()
        
        coEvery { 
            sessionMetricsRepository.getSessionsByDateRange(startOfDay, endOfDay) 
        } returns Result.Success(emptyList())
        
        // When
        val result = useCase.calculateDailyStatistics(date)
        
        // Then
        assertTrue(result is Result.Success)
        val stats = result.data
        assertEquals(date, stats.date)
        assertEquals(0, stats.sessionsCount)
        assertEquals(0, stats.successfulSessions)
        assertEquals(0L, stats.totalWords)
        assertEquals(0.0, stats.successRate)
    }
    
    @Test
    fun `calculateDailyStatistics calculates correct metrics for successful sessions`() = runTest {
        // Given
        val date = LocalDate(2023, 12, 1)
        val timeZone = TimeZone.currentSystemDefault()
        val startOfDay = date.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val endOfDay = date.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toEpochMilliseconds()
        
        val sessions = listOf(
            createTestSessionMetrics(
                sessionId = "1",
                sessionStartTime = startOfDay + 3600000, // 1 hour after start
                sessionEndTime = startOfDay + 3660000, // 61 minutes after start
                wordCount = 100,
                characterCount = 500,
                audioRecordingDuration = 60000, // 1 minute
                transcriptionSuccess = true,
                speakingRate = 100.0,
                targetAppPackage = "com.example.app1"
            ),
            createTestSessionMetrics(
                sessionId = "2",
                sessionStartTime = startOfDay + 7200000, // 2 hours after start
                sessionEndTime = startOfDay + 7320000, // 122 minutes after start
                wordCount = 200,
                characterCount = 1000,
                audioRecordingDuration = 120000, // 2 minutes
                transcriptionSuccess = true,
                speakingRate = 100.0,
                targetAppPackage = "com.example.app2"
            ),
            createTestSessionMetrics(
                sessionId = "3",
                sessionStartTime = startOfDay + 10800000, // 3 hours after start
                sessionEndTime = null, // Failed session
                wordCount = 0,
                characterCount = 0,
                audioRecordingDuration = 30000, // 30 seconds
                transcriptionSuccess = false,
                speakingRate = 0.0,
                targetAppPackage = "com.example.app1",
                errorType = "network_error"
            )
        )
        
        coEvery { 
            sessionMetricsRepository.getSessionsByDateRange(startOfDay, endOfDay) 
        } returns Result.Success(sessions)
        
        // When
        val result = useCase.calculateDailyStatistics(date)
        
        // Then
        assertTrue(result is Result.Success)
        val stats = result.data
        
        assertEquals(date, stats.date)
        assertEquals(3, stats.sessionsCount)
        assertEquals(2, stats.successfulSessions)
        assertEquals(300L, stats.totalWords) // 100 + 200 + 0
        assertEquals(1500L, stats.totalCharacters) // 500 + 1000 + 0
        assertEquals(210000L, stats.totalSpeakingTimeMs) // 60000 + 120000 + 30000
        assertEquals(100.0, stats.averageSpeakingRate) // (100 + 100) / 2
        assertEquals(66.67, stats.successRate, 0.01) // 2/3 * 100
        assertEquals(2, stats.uniqueAppsUsed) // app1 and app2
        assertEquals("com.example.app1", stats.mostUsedApp) // app1 appears in 2 sessions
        assertEquals(1, stats.errorBreakdown["network_error"])
    }
    
    @Test
    fun `calculateDailyStatistics handles repository error`() = runTest {
        // Given
        val date = LocalDate(2023, 12, 1)
        val timeZone = TimeZone.currentSystemDefault()
        val startOfDay = date.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val endOfDay = date.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toEpochMilliseconds()
        
        val exception = RuntimeException("Database error")
        coEvery { 
            sessionMetricsRepository.getSessionsByDateRange(startOfDay, endOfDay) 
        } returns Result.Error(exception)
        
        // When
        val result = useCase.calculateDailyStatistics(date)
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, result.exception)
    }
    
    @Test
    fun `calculateAverageSessionDuration returns correct average`() = runTest {
        // Given
        val startDate = LocalDate(2023, 12, 1)
        val endDate = LocalDate(2023, 12, 1)
        val timeZone = TimeZone.currentSystemDefault()
        val startTime = startDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val endTime = endDate.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toEpochMilliseconds()
        
        val sessions = listOf(
            createTestSessionMetrics(
                sessionId = "1",
                sessionStartTime = startTime,
                sessionEndTime = startTime + 60000 // 1 minute duration
            ),
            createTestSessionMetrics(
                sessionId = "2", 
                sessionStartTime = startTime + 3600000,
                sessionEndTime = startTime + 3600000 + 120000 // 2 minute duration
            ),
            createTestSessionMetrics(
                sessionId = "3",
                sessionStartTime = startTime + 7200000,
                sessionEndTime = null // Incomplete session - should be excluded
            )
        )
        
        coEvery { 
            sessionMetricsRepository.getSessionsByDateRange(startTime, endTime) 
        } returns Result.Success(sessions)
        
        // When
        val result = useCase.calculateAverageSessionDuration(startDate, endDate)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(90000.0, result.data) // (60000 + 120000) / 2
    }
    
    @Test
    fun `calculateAverageSessionDuration returns zero when no completed sessions`() = runTest {
        // Given
        val startDate = LocalDate(2023, 12, 1)
        val endDate = LocalDate(2023, 12, 1)
        val timeZone = TimeZone.currentSystemDefault()
        val startTime = startDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val endTime = endDate.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toEpochMilliseconds()
        
        coEvery { 
            sessionMetricsRepository.getSessionsByDateRange(startTime, endTime) 
        } returns Result.Success(emptyList())
        
        // When
        val result = useCase.calculateAverageSessionDuration(startDate, endDate)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(0.0, result.data)
    }
    
    private fun createTestSessionMetrics(
        sessionId: String,
        sessionStartTime: Long,
        sessionEndTime: Long? = null,
        audioRecordingDuration: Long = 0,
        audioFileSize: Long = 0,
        audioQuality: String? = null,
        wordCount: Int = 0,
        characterCount: Int = 0,
        speakingRate: Double = 0.0,
        transcriptionText: String? = null,
        transcriptionSuccess: Boolean = false,
        textInsertionSuccess: Boolean = false,
        targetAppPackage: String? = null,
        errorType: String? = null,
        errorMessage: String? = null
    ): SessionMetrics {
        return SessionMetrics(
            sessionId = sessionId,
            sessionStartTime = sessionStartTime,
            sessionEndTime = sessionEndTime,
            audioRecordingDuration = audioRecordingDuration,
            audioFileSize = audioFileSize,
            audioQuality = audioQuality,
            wordCount = wordCount,
            characterCount = characterCount,
            speakingRate = speakingRate,
            transcriptionText = transcriptionText,
            transcriptionSuccess = transcriptionSuccess,
            textInsertionSuccess = textInsertionSuccess,
            targetAppPackage = targetAppPackage,
            errorType = errorType,
            errorMessage = errorMessage
        )
    }
}