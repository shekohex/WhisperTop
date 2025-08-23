package me.shadykhalifa.whispertop.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import me.shadykhalifa.whispertop.domain.models.DailyStatistics
import me.shadykhalifa.whispertop.domain.models.RetentionPolicyResult
import me.shadykhalifa.whispertop.domain.usecases.StatisticsCalculatorUseCase
import me.shadykhalifa.whispertop.domain.repositories.SessionMetricsRepository
import me.shadykhalifa.whispertop.domain.repositories.UserStatisticsRepository
import me.shadykhalifa.whispertop.utils.Result
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DailyStatsAggregatorWorkerTest {

    private lateinit var context: Context
    private val statisticsCalculator = mockk<StatisticsCalculatorUseCase>()
    private val sessionMetricsRepository = mockk<SessionMetricsRepository>()
    private val userStatisticsRepository = mockk<UserStatisticsRepository>()

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        
        // Setup Koin for dependency injection in tests
        startKoin {
            modules(module {
                single { statisticsCalculator }
                single { sessionMetricsRepository }
                single { userStatisticsRepository }
            })
        }
    }

    @Test
    fun `doWork returns success when daily statistics calculation succeeds`() = runTest {
        // Given
        val testDate = LocalDate(2023, 12, 1)
        val mockDailyStats = DailyStatistics(
            date = testDate,
            sessionsCount = 5,
            successfulSessions = 4,
            totalWords = 1000L,
            totalCharacters = 5000L,
            totalSpeakingTimeMs = 300000L,
            totalSessionDurationMs = 600000L,
            averageSessionDuration = 120000.0,
            averageSpeakingRate = 200.0,
            successRate = 80.0,
            peakUsageHour = 14,
            mostUsedApp = "com.example.app",
            errorBreakdown = mapOf("network_error" to 1),
            uniqueAppsUsed = 3
        )
        
        val mockRetentionResult = RetentionPolicyResult(
            sessionsDeleted = 0,
            transcriptionsDeleted = 0,
            bytesFreed = 0L,
            lastCleanupDate = testDate
        )

        coEvery { 
            statisticsCalculator.calculateDailyStatistics(any()) 
        } returns Result.Success(mockDailyStats)
        
        coEvery { 
            userStatisticsRepository.updateDailyAggregatedStats(
                date = any(),
                totalSessions = any(),
                totalWords = any(),
                totalSpeakingTime = any(),
                averageSessionDuration = any(),
                peakUsageHour = any()
            ) 
        } returns Result.Success(Unit)
        
        coEvery { 
            statisticsCalculator.enforceDataRetentionPolicies() 
        } returns Result.Success(mockRetentionResult)

        // When
        val worker = TestListenableWorkerBuilder<DailyStatsAggregatorWorker>(context).build()
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Success)
        
        val outputData = result.outputData
        assertEquals(5, outputData.getInt(DailyStatsAggregatorWorker.KEY_SESSIONS_PROCESSED, 0))
        assertEquals(1, outputData.getInt(DailyStatsAggregatorWorker.KEY_STATS_UPDATED, 0))
        assertEquals(0, outputData.getInt(DailyStatsAggregatorWorker.KEY_ERRORS_COUNT, 0))
        
        // Verify interactions
        coVerify { statisticsCalculator.calculateDailyStatistics(any()) }
        coVerify { 
            userStatisticsRepository.updateDailyAggregatedStats(
                date = any(),
                totalSessions = 5,
                totalWords = 1000L,
                totalSpeakingTime = 300000L,
                averageSessionDuration = 120000.0,
                peakUsageHour = 14
            ) 
        }
        coVerify { statisticsCalculator.enforceDataRetentionPolicies() }
    }

    @Test
    fun `doWork returns success with errors when statistics calculation fails`() = runTest {
        // Given
        val exception = RuntimeException("Statistics calculation failed")
        
        coEvery { 
            statisticsCalculator.calculateDailyStatistics(any()) 
        } returns Result.Error(exception)
        
        coEvery { 
            statisticsCalculator.enforceDataRetentionPolicies() 
        } returns Result.Success(RetentionPolicyResult(0, 0, 0L, LocalDate(2023, 12, 1)))

        // When
        val worker = TestListenableWorkerBuilder<DailyStatsAggregatorWorker>(context).build()
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Success)
        
        val outputData = result.outputData
        assertEquals(0, outputData.getInt(DailyStatsAggregatorWorker.KEY_SESSIONS_PROCESSED, 0))
        assertEquals(0, outputData.getInt(DailyStatsAggregatorWorker.KEY_STATS_UPDATED, 0))
        assertEquals(1, outputData.getInt(DailyStatsAggregatorWorker.KEY_ERRORS_COUNT, 0))
    }

    @Test
    fun `doWork returns retry when critical failure occurs`() = runTest {
        // Given - both statistics calculation and retention policies fail
        val exception = RuntimeException("Critical database failure")
        
        coEvery { 
            statisticsCalculator.calculateDailyStatistics(any()) 
        } returns Result.Error(exception)
        
        coEvery { 
            statisticsCalculator.enforceDataRetentionPolicies() 
        } returns Result.Error(exception)

        // When
        val worker = TestListenableWorkerBuilder<DailyStatsAggregatorWorker>(context).build()
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Retry)
    }

    @Test
    fun `doWork handles user statistics update failure gracefully`() = runTest {
        // Given
        val testDate = LocalDate(2023, 12, 1)
        val mockDailyStats = DailyStatistics(
            date = testDate,
            sessionsCount = 3,
            successfulSessions = 3,
            totalWords = 500L,
            totalCharacters = 2500L,
            totalSpeakingTimeMs = 180000L,
            totalSessionDurationMs = 360000L,
            averageSessionDuration = 120000.0,
            averageSpeakingRate = 166.7,
            successRate = 100.0,
            peakUsageHour = 10,
            mostUsedApp = "com.example.notes",
            errorBreakdown = emptyMap(),
            uniqueAppsUsed = 1
        )
        
        val updateException = RuntimeException("User statistics update failed")
        
        coEvery { 
            statisticsCalculator.calculateDailyStatistics(any()) 
        } returns Result.Success(mockDailyStats)
        
        coEvery { 
            userStatisticsRepository.updateDailyAggregatedStats(any(), any(), any(), any(), any(), any()) 
        } returns Result.Error(updateException)
        
        coEvery { 
            statisticsCalculator.enforceDataRetentionPolicies() 
        } returns Result.Success(RetentionPolicyResult(0, 0, 0L, testDate))

        // When
        val worker = TestListenableWorkerBuilder<DailyStatsAggregatorWorker>(context).build()
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Success)
        
        val outputData = result.outputData
        assertEquals(3, outputData.getInt(DailyStatsAggregatorWorker.KEY_SESSIONS_PROCESSED, 0))
        assertEquals(0, outputData.getInt(DailyStatsAggregatorWorker.KEY_STATS_UPDATED, 0))
        assertEquals(1, outputData.getInt(DailyStatsAggregatorWorker.KEY_ERRORS_COUNT, 0))
    }

    @Test
    fun `doWork processes empty statistics correctly`() = runTest {
        // Given
        val testDate = LocalDate(2023, 12, 1)
        val emptyStats = DailyStatistics.empty(testDate)
        
        coEvery { 
            statisticsCalculator.calculateDailyStatistics(any()) 
        } returns Result.Success(emptyStats)
        
        coEvery { 
            userStatisticsRepository.updateDailyAggregatedStats(any(), any(), any(), any(), any(), any()) 
        } returns Result.Success(Unit)
        
        coEvery { 
            statisticsCalculator.enforceDataRetentionPolicies() 
        } returns Result.Success(RetentionPolicyResult(0, 0, 0L, testDate))

        // When
        val worker = TestListenableWorkerBuilder<DailyStatsAggregatorWorker>(context).build()
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Success)
        
        val outputData = result.outputData
        assertEquals(0, outputData.getInt(DailyStatsAggregatorWorker.KEY_SESSIONS_PROCESSED, 0))
        assertEquals(1, outputData.getInt(DailyStatsAggregatorWorker.KEY_STATS_UPDATED, 0))
        assertEquals(0, outputData.getInt(DailyStatsAggregatorWorker.KEY_ERRORS_COUNT, 0))
        
        // Verify that update was called even with empty stats
        coVerify { 
            userStatisticsRepository.updateDailyAggregatedStats(
                date = testDate,
                totalSessions = 0,
                totalWords = 0L,
                totalSpeakingTime = 0L,
                averageSessionDuration = 0.0,
                peakUsageHour = 0
            ) 
        }
    }

    @Test
    fun `schedulePeriodicAggregation configures work correctly`() {
        // This test verifies that the scheduling method exists and doesn't throw
        // In a real implementation, you might want to verify WorkManager scheduling
        DailyStatsAggregatorWorker.schedulePeriodicAggregation(context)
        
        // No exception should be thrown
        assertTrue(true)
    }

    @Test
    fun `cancelScheduledAggregation executes without error`() {
        // This test verifies that the cancellation method exists and doesn't throw
        DailyStatsAggregatorWorker.cancelScheduledAggregation(context)
        
        // No exception should be thrown
        assertTrue(true)
    }

    @After
    fun tearDown() {
        stopKoin()
    }
}