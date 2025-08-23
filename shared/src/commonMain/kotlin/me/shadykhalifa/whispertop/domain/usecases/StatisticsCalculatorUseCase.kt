package me.shadykhalifa.whispertop.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import me.shadykhalifa.whispertop.domain.repositories.SessionMetricsRepository
import me.shadykhalifa.whispertop.domain.repositories.UserStatisticsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.domain.models.DailyStatistics
import me.shadykhalifa.whispertop.domain.models.ProductivityTrends
import me.shadykhalifa.whispertop.domain.models.UsagePatterns
import me.shadykhalifa.whispertop.domain.models.RetentionPolicyResult
import me.shadykhalifa.whispertop.utils.Logger
import me.shadykhalifa.whispertop.utils.Result

interface StatisticsCalculatorUseCase {
    
    /**
     * Calculate comprehensive daily statistics for a given date
     */
    suspend fun calculateDailyStatistics(date: LocalDate): Result<DailyStatistics>
    
    /**
     * Calculate productivity trends over a date range
     */
    suspend fun calculateProductivityTrends(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<ProductivityTrends>
    
    /**
     * Identify usage patterns and peak usage times
     */
    suspend fun calculateUsagePatterns(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<UsagePatterns>
    
    /**
     * Calculate average session duration over a period
     */
    suspend fun calculateAverageSessionDuration(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<Double>
    
    /**
     * Calculate speaking rate trends
     */
    suspend fun calculateSpeakingRateTrends(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<Pair<LocalDate, Double>>>
    
    /**
     * Enforce data retention policies based on configured settings
     */
    suspend fun enforceDataRetentionPolicies(): Result<RetentionPolicyResult>
    
    /**
     * Get real-time statistics updates
     */
    fun getRealtimeStatisticsFlow(): Flow<DailyStatistics>
    
    /**
     * Calculate daily statistics with pagination for large datasets
     */
    suspend fun calculateDailyStatisticsPaginated(
        date: LocalDate, 
        pageSize: Int = 1000
    ): Result<DailyStatistics>
}

class StatisticsCalculatorUseCaseImpl(
    private val sessionMetricsRepository: SessionMetricsRepository,
    private val userStatisticsRepository: UserStatisticsRepository,
    private val transcriptionHistoryRepository: TranscriptionHistoryRepository
) : StatisticsCalculatorUseCase {
    
    override suspend fun calculateDailyStatistics(date: LocalDate): Result<DailyStatistics> {
        return try {
            val timeZone = TimeZone.currentSystemDefault()
            val startOfDay = date.atStartOfDayIn(timeZone).toEpochMilliseconds()
            val endOfDay = date.plus(kotlinx.datetime.DatePeriod(days = 1)).atStartOfDayIn(timeZone).toEpochMilliseconds()
            
            // Get sessions for the specified date
            val sessionsResult = sessionMetricsRepository.getSessionsByDateRange(startOfDay, endOfDay)
            
            when (sessionsResult) {
                is Result.Success -> {
                    val sessions = sessionsResult.data
                    
                    if (sessions.isEmpty()) {
                        return Result.Success(DailyStatistics.empty(date))
                    }
                    
                    // Calculate various metrics
                    val totalSessions = sessions.size
                    val successfulSessions = sessions.count { it.transcriptionSuccess }
                    val totalWords = sessions.sumOf { it.wordCount.toLong() }
                    val totalCharacters = sessions.sumOf { it.characterCount.toLong() }
                    val totalSpeakingTime = sessions.sumOf { it.audioRecordingDuration }
                    val totalSessionDuration = sessions.mapNotNull { session ->
                        session.sessionEndTime?.let { endTime ->
                            endTime - session.sessionStartTime
                        }
                    }.sum()
                    
                    val averageSessionDuration = if (totalSessions > 0) {
                        totalSessionDuration.toDouble() / totalSessions
                    } else 0.0
                    
                    val averageSpeakingRate = if (sessions.isNotEmpty()) {
                        val validRates = sessions.filter { it.speakingRate > 0 }.map { it.speakingRate }
                        if (validRates.isNotEmpty()) validRates.average() else 0.0
                    } else 0.0
                    
                    val successRate = if (totalSessions > 0) {
                        (successfulSessions.toDouble() / totalSessions) * 100
                    } else 0.0
                    
                    // Calculate peak usage hour
                    val peakUsageHour = calculatePeakUsageHour(sessions)
                    
                    // Calculate most used apps
                    val appUsageMap = sessions.groupBy { it.targetAppPackage ?: "unknown" }
                        .mapValues { it.value.size }
                    val mostUsedApp = appUsageMap.maxByOrNull { it.value }?.key
                    
                    // Calculate error breakdown
                    val errorTypes = sessions.filter { !it.transcriptionSuccess }
                        .groupBy { it.errorType ?: "unknown_error" }
                        .mapValues { it.value.size }
                    
                    val dailyStats = DailyStatistics(
                        date = date,
                        sessionsCount = totalSessions,
                        successfulSessions = successfulSessions,
                        totalWords = totalWords,
                        totalCharacters = totalCharacters,
                        totalSpeakingTimeMs = totalSpeakingTime,
                        totalSessionDurationMs = totalSessionDuration,
                        averageSessionDuration = averageSessionDuration,
                        averageSpeakingRate = averageSpeakingRate,
                        successRate = successRate,
                        peakUsageHour = peakUsageHour,
                        mostUsedApp = mostUsedApp,
                        errorBreakdown = errorTypes,
                        uniqueAppsUsed = appUsageMap.size
                    )
                    
                    Result.Success(dailyStats)
                }
                is Result.Error -> Result.Error(sessionsResult.exception)
                is Result.Loading -> Result.Error(IllegalStateException("Unexpected loading state"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun calculateProductivityTrends(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<ProductivityTrends> {
        return try {
            val timeZone = TimeZone.currentSystemDefault()
            val startTime = startDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
            val endTime = endDate.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toEpochMilliseconds()
            
            val sessionsResult = sessionMetricsRepository.getSessionsByDateRange(startTime, endTime)
            
            when (sessionsResult) {
                is Result.Success -> {
                    val sessions = sessionsResult.data
                    
                    // Group sessions by date and calculate daily metrics
                    val dailyMetrics = sessions.groupBy { session ->
                        val sessionDate = kotlinx.datetime.Instant.fromEpochMilliseconds(session.sessionStartTime)
                            .toLocalDateTime(timeZone).date
                        sessionDate
                    }.mapValues { (_, dailySessions) ->
                        val dailyWords = dailySessions.sumOf { it.wordCount }
                        val dailyDuration = dailySessions.sumOf { it.audioRecordingDuration }
                        val successRate = dailySessions.count { it.transcriptionSuccess }.toDouble() / dailySessions.size
                        
                        Triple(dailyWords, dailyDuration, successRate)
                    }
                    
                    // Calculate trends
                    val wordsTrend = calculateTrend(dailyMetrics.values.map { it.first.toDouble() })
                    val durationTrend = calculateTrend(dailyMetrics.values.map { it.second.toDouble() })
                    val successTrend = calculateTrend(dailyMetrics.values.map { it.third })
                    
                    val trends = ProductivityTrends(
                        dateRange = startDate to endDate,
                        wordsPerDayTrend = wordsTrend,
                        speakingTimePerDayTrend = durationTrend,
                        successRateTrend = successTrend,
                        dailyMetrics = dailyMetrics.mapValues { (_, metrics) ->
                            ProductivityTrends.DailyMetric(
                                words = metrics.first,
                                speakingTimeMs = metrics.second,
                                successRate = metrics.third
                            )
                        }
                    )
                    
                    Result.Success(trends)
                }
                is Result.Error -> Result.Error(sessionsResult.exception)
                is Result.Loading -> Result.Error(IllegalStateException("Unexpected loading state"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun calculateUsagePatterns(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<UsagePatterns> {
        return try {
            val timeZone = TimeZone.currentSystemDefault()
            val startTime = startDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
            val endTime = endDate.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toEpochMilliseconds()
            
            val sessionsResult = sessionMetricsRepository.getSessionsByDateRange(startTime, endTime)
            
            when (sessionsResult) {
                is Result.Success -> {
                    val sessions = sessionsResult.data
                    
                    // Analyze hourly usage patterns
                    val hourlyUsage = IntArray(24)
                    sessions.forEach { session ->
                        val hour = kotlinx.datetime.Instant.fromEpochMilliseconds(session.sessionStartTime)
                            .toLocalDateTime(timeZone).hour
                        hourlyUsage[hour]++
                    }
                    
                    // Find peak hours
                    val peakHours = hourlyUsage.withIndex()
                        .sortedByDescending { it.value }
                        .take(3)
                        .map { it.index }
                    
                    // Analyze daily patterns
                    val dailyUsage = sessions.groupBy { session ->
                        kotlinx.datetime.Instant.fromEpochMilliseconds(session.sessionStartTime)
                            .toLocalDateTime(timeZone).dayOfWeek
                    }.mapValues { it.value.size }
                    
                    // App usage patterns
                    val appUsage = sessions.groupBy { it.targetAppPackage ?: "unknown" }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(10)
                    
                    val patterns = UsagePatterns(
                        dateRange = startDate to endDate,
                        hourlyDistribution = hourlyUsage.toList(),
                        peakHours = peakHours,
                        dailyDistribution = dailyUsage,
                        topApps = appUsage,
                        averageSessionsPerDay = sessions.size.toDouble() / (endDate.toEpochDays() - startDate.toEpochDays() + 1),
                        mostActiveDay = dailyUsage.maxByOrNull { it.value }?.key
                    )
                    
                    Result.Success(patterns)
                }
                is Result.Error -> Result.Error(sessionsResult.exception)
                is Result.Loading -> Result.Error(IllegalStateException("Unexpected loading state"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun calculateAverageSessionDuration(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<Double> {
        return try {
            val timeZone = TimeZone.currentSystemDefault()
            val startTime = startDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
            val endTime = endDate.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toEpochMilliseconds()
            
            val sessionsResult = sessionMetricsRepository.getSessionsByDateRange(startTime, endTime)
            
            when (sessionsResult) {
                is Result.Success -> {
                    val sessions = sessionsResult.data
                    val completedSessions = sessions.filter { it.sessionEndTime != null }
                    
                    val averageDuration = if (completedSessions.isNotEmpty()) {
                        completedSessions.map { session ->
                            session.sessionEndTime!! - session.sessionStartTime
                        }.average()
                    } else 0.0
                    
                    Result.Success(averageDuration)
                }
                is Result.Error -> Result.Error(sessionsResult.exception)
                is Result.Loading -> Result.Error(IllegalStateException("Unexpected loading state"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun calculateSpeakingRateTrends(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<Pair<LocalDate, Double>>> {
        return try {
            val timeZone = TimeZone.currentSystemDefault()
            val startTime = startDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
            val endTime = endDate.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toEpochMilliseconds()
            
            val sessionsResult = sessionMetricsRepository.getSessionsByDateRange(startTime, endTime)
            
            when (sessionsResult) {
                is Result.Success -> {
                    val sessions = sessionsResult.data
                    
                    val dailySpeakingRates = sessions
                        .filter { it.speakingRate > 0 }
                        .groupBy { session ->
                            kotlinx.datetime.Instant.fromEpochMilliseconds(session.sessionStartTime)
                                .toLocalDateTime(timeZone).date
                        }
                        .mapValues { (_, dailySessions) ->
                            dailySessions.map { it.speakingRate }.average()
                        }
                        .toList()
                        .sortedBy { it.first }
                    
                    Result.Success(dailySpeakingRates)
                }
                is Result.Error -> Result.Error(sessionsResult.exception)
                is Result.Loading -> Result.Error(IllegalStateException("Unexpected loading state"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun enforceDataRetentionPolicies(): Result<RetentionPolicyResult> {
        return try {
            val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val currentTime = Clock.System.now().toEpochMilliseconds()
            
            // Get privacy-compliant retention periods
            val sessionRetentionDays = me.shadykhalifa.whispertop.utils.PrivacyComplianceManager.CORE_FUNCTIONALITY.retentionPeriodDays
            val transcriptionRetentionDays = me.shadykhalifa.whispertop.utils.PrivacyComplianceManager.CORE_FUNCTIONALITY.retentionPeriodDays
            val statisticsRetentionDays = me.shadykhalifa.whispertop.utils.PrivacyComplianceManager.PERFORMANCE_ANALYTICS.retentionPeriodDays
            
            val sessionCutoffDate = currentDate.minus(DatePeriod(days = sessionRetentionDays))
            val transcriptionCutoffDate = currentDate.minus(DatePeriod(days = transcriptionRetentionDays))
            
            val timeZone = TimeZone.currentSystemDefault()
            val sessionCutoffTime = sessionCutoffDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
            val transcriptionCutoffTime = transcriptionCutoffDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
            
            var sessionsDeleted = 0
            var transcriptionsDeleted = 0
            var bytesFreed = 0L
            
            // Clean up old session metrics with secure deletion
            try {
                val oldSessionsResult = sessionMetricsRepository.getSessionsOlderThan(sessionCutoffTime)
                when (oldSessionsResult) {
                    is Result.Success -> {
                        val oldSessions = oldSessionsResult.data
                        sessionsDeleted = oldSessions.size
                        
                        // Perform secure deletion of old sessions with audit trail
                        val deletionResult = sessionMetricsRepository.deleteSessionsOlderThan(sessionCutoffTime)
                        when (deletionResult) {
                            is Result.Success -> {
                                Logger.debug("DataRetention", "Successfully deleted $sessionsDeleted old session records")
                                
                                // Create audit record for data deletion
                                me.shadykhalifa.whispertop.utils.PrivacyComplianceManager.createDataProcessingRecord(
                                    operation = "data_retention_deletion",
                                    dataTypes = listOf("session_metrics", "transcription_text"),
                                    purposeId = me.shadykhalifa.whispertop.utils.PrivacyComplianceManager.CORE_FUNCTIONALITY.id
                                )
                            }
                            is Result.Error -> {
                                Logger.error("DataRetention", "Failed to delete old sessions: ${deletionResult.exception.message}")
                                sessionsDeleted = 0 // Reset count on failure
                            }
                            is Result.Loading -> {
                                // Should not happen in this context
                            }
                        }
                    }
                    is Result.Error -> {
                        Logger.error("DataRetention", "Failed to retrieve old sessions for deletion: ${oldSessionsResult.exception.message}")
                    }
                    is Result.Loading -> {
                        // Should not happen in this context
                    }
                }
            } catch (e: Exception) {
                Logger.error("DataRetention", "Error during session cleanup: ${e.message}")
            }
            
            // Clean up old transcription history with secure deletion
            try {
                val oldTranscriptionsResult = transcriptionHistoryRepository.getTranscriptionsOlderThan(transcriptionCutoffTime)
                when (oldTranscriptionsResult) {
                    is Result.Success -> {
                        val oldTranscriptions = oldTranscriptionsResult.data
                        transcriptionsDeleted = oldTranscriptions.size
                        bytesFreed = oldTranscriptions.sumOf { it.text.length * 2L } // Rough estimate
                        
                        // Perform secure deletion of old transcriptions
                        val deletionResult = transcriptionHistoryRepository.deleteTranscriptionsOlderThan(transcriptionCutoffTime)
                        when (deletionResult) {
                            is Result.Success -> {
                                Logger.debug("DataRetention", "Successfully deleted $transcriptionsDeleted old transcription records, freed ${bytesFreed / 1024}KB")
                            }
                            is Result.Error -> {
                                Logger.error("DataRetention", "Failed to delete old transcriptions: ${deletionResult.exception.message}")
                                transcriptionsDeleted = 0
                                bytesFreed = 0L
                            }
                            is Result.Loading -> {
                                // Should not happen in this context
                            }
                        }
                    }
                    is Result.Error -> {
                        Logger.error("DataRetention", "Failed to retrieve old transcriptions for deletion: ${oldTranscriptionsResult.exception.message}")
                    }
                    is Result.Loading -> {
                        // Should not happen in this context
                    }
                }
            } catch (e: Exception) {
                Logger.error("DataRetention", "Error during transcription cleanup: ${e.message}")
            }
            
            val result = RetentionPolicyResult(
                sessionsDeleted = sessionsDeleted,
                transcriptionsDeleted = transcriptionsDeleted,
                bytesFreed = bytesFreed,
                lastCleanupDate = currentDate
            )
            
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override fun getRealtimeStatisticsFlow(): Flow<DailyStatistics> {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val timeZone = TimeZone.currentSystemDefault()
        val startOfDay = currentDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val endOfDay = currentDate.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toEpochMilliseconds()
        
        return sessionMetricsRepository.getSessionsByDateRangeFlow(startOfDay, endOfDay)
            .map { sessions ->
                if (sessions.isEmpty()) {
                    DailyStatistics.empty(currentDate)
                } else {
                    // Calculate real-time statistics from current sessions
                    val totalSessions = sessions.size
                    val successfulSessions = sessions.count { it.transcriptionSuccess }
                    val totalWords = sessions.sumOf { it.wordCount.toLong() }
                    val totalCharacters = sessions.sumOf { it.characterCount.toLong() }
                    val totalSpeakingTime = sessions.sumOf { it.audioRecordingDuration }
                    val totalSessionDuration = sessions.mapNotNull { session ->
                        session.sessionEndTime?.let { endTime ->
                            endTime - session.sessionStartTime
                        }
                    }.sum()
                    
                    val averageSessionDuration = if (totalSessions > 0) {
                        totalSessionDuration.toDouble() / totalSessions
                    } else 0.0
                    
                    val averageSpeakingRate = if (totalSpeakingTime > 0) {
                        (totalWords.toDouble() / totalSpeakingTime) * 60000 // words per minute
                    } else 0.0
                    
                    val successRate = if (totalSessions > 0) {
                        (successfulSessions.toDouble() / totalSessions) * 100
                    } else 0.0
                    
                    val peakUsageHour = calculatePeakUsageHour(sessions)
                    val mostUsedApp = sessions.groupBy { it.targetAppPackage ?: "unknown" }
                        .mapValues { it.value.size }
                        .maxByOrNull { it.value }?.key
                    
                    val errorBreakdown = sessions.filter { !it.transcriptionSuccess }
                        .groupBy { it.errorType ?: "unknown_error" }
                        .mapValues { it.value.size }
                    
                    val uniqueAppsUsed = sessions.mapNotNull { it.targetAppPackage }.toSet().size
                    
                    DailyStatistics(
                        date = currentDate,
                        sessionsCount = totalSessions,
                        successfulSessions = successfulSessions,
                        totalWords = totalWords,
                        totalCharacters = totalCharacters,
                        totalSpeakingTimeMs = totalSpeakingTime,
                        totalSessionDurationMs = totalSessionDuration,
                        averageSessionDuration = averageSessionDuration,
                        averageSpeakingRate = averageSpeakingRate,
                        successRate = successRate,
                        peakUsageHour = peakUsageHour,
                        mostUsedApp = mostUsedApp,
                        errorBreakdown = errorBreakdown,
                        uniqueAppsUsed = uniqueAppsUsed
                    )
                }
            }
            .distinctUntilChanged()
    }
    
    override suspend fun calculateDailyStatisticsPaginated(
        date: LocalDate, 
        pageSize: Int
    ): Result<DailyStatistics> {
        return try {
            val timeZone = TimeZone.currentSystemDefault()
            val startOfDay = date.atStartOfDayIn(timeZone).toEpochMilliseconds()
            val endOfDay = date.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toEpochMilliseconds()
            
            // Get total count first to determine if pagination is needed
            val totalCountResult = sessionMetricsRepository.getSessionCountByDateRange(startOfDay, endOfDay)
            
            when (totalCountResult) {
                is Result.Success -> {
                    val totalSessions = totalCountResult.data
                    
                    if (totalSessions == 0) {
                        return Result.Success(DailyStatistics.empty(date))
                    }
                    
                    // If dataset is small, use regular calculation
                    if (totalSessions <= pageSize) {
                        return calculateDailyStatistics(date)
                    }
                    
                    // Process in chunks for large datasets
                    val allSessions = mutableListOf<me.shadykhalifa.whispertop.domain.models.SessionMetrics>()
                    var offset = 0
                    
                    while (offset < totalSessions) {
                        val pageResult = sessionMetricsRepository.getSessionsByDateRangePaginated(
                            startOfDay, endOfDay, pageSize, offset
                        )
                        
                        when (pageResult) {
                            is Result.Success -> {
                                allSessions.addAll(pageResult.data)
                                offset += pageSize
                            }
                            is Result.Error -> {
                                Logger.warn("StatisticsCalculator", "Failed to load page at offset $offset: ${pageResult.exception.message}")
                                break // Exit on error, process what we have
                            }
                            is Result.Loading -> {
                                // Should not happen in this context
                                break
                            }
                        }
                    }
                    
                    // Calculate statistics from aggregated data
                    val sessions = allSessions.toList()
                    val successfulSessions = sessions.count { it.transcriptionSuccess }
                    val totalWords = sessions.sumOf { it.wordCount.toLong() }
                    val totalCharacters = sessions.sumOf { it.characterCount.toLong() }
                    val totalSpeakingTime = sessions.sumOf { it.audioRecordingDuration }
                    val totalSessionDuration = sessions.mapNotNull { session ->
                        session.sessionEndTime?.let { endTime ->
                            endTime - session.sessionStartTime
                        }
                    }.sum()
                    
                    val averageSessionDuration = if (sessions.isNotEmpty()) {
                        totalSessionDuration.toDouble() / sessions.size
                    } else 0.0
                    
                    val averageSpeakingRate = if (totalSpeakingTime > 0) {
                        (totalWords.toDouble() / totalSpeakingTime) * 60000 // words per minute
                    } else 0.0
                    
                    val successRate = if (sessions.isNotEmpty()) {
                        (successfulSessions.toDouble() / sessions.size) * 100
                    } else 0.0
                    
                    val peakUsageHour = calculatePeakUsageHour(sessions)
                    val appUsageMap = sessions.groupBy { it.targetAppPackage ?: "unknown" }
                        .mapValues { it.value.size }
                    val mostUsedApp = appUsageMap.maxByOrNull { it.value }?.key
                    
                    val errorBreakdown = sessions.filter { !it.transcriptionSuccess }
                        .groupBy { it.errorType ?: "unknown_error" }
                        .mapValues { it.value.size }
                    
                    val dailyStats = DailyStatistics(
                        date = date,
                        sessionsCount = sessions.size,
                        successfulSessions = successfulSessions,
                        totalWords = totalWords,
                        totalCharacters = totalCharacters,
                        totalSpeakingTimeMs = totalSpeakingTime,
                        totalSessionDurationMs = totalSessionDuration,
                        averageSessionDuration = averageSessionDuration,
                        averageSpeakingRate = averageSpeakingRate,
                        successRate = successRate,
                        peakUsageHour = peakUsageHour,
                        mostUsedApp = mostUsedApp,
                        errorBreakdown = errorBreakdown,
                        uniqueAppsUsed = appUsageMap.size
                    )
                    
                    Result.Success(dailyStats)
                }
                is Result.Error -> Result.Error(totalCountResult.exception)
                is Result.Loading -> Result.Error(IllegalStateException("Unexpected loading state"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    private fun calculatePeakUsageHour(sessions: List<me.shadykhalifa.whispertop.domain.models.SessionMetrics>): Int {
        val timeZone = TimeZone.currentSystemDefault()
        val hourlyCount = IntArray(24)
        
        sessions.forEach { session ->
            val hour = kotlinx.datetime.Instant.fromEpochMilliseconds(session.sessionStartTime)
                .toLocalDateTime(timeZone).hour
            hourlyCount[hour]++
        }
        
        return hourlyCount.withIndex().maxByOrNull { it.value }?.index ?: 0
    }
    
    private fun calculateTrend(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        // Simple linear regression slope calculation
        val n = values.size
        val sumX = (0 until n).sum().toDouble()
        val sumY = values.sum()
        val sumXY = values.withIndex().sumOf { (index, value) -> index * value }
        val sumX2 = (0 until n).sumOf { it * it }.toDouble()
        
        val denominator = n * sumX2 - sumX * sumX
        return if (denominator != 0.0) {
            (n * sumXY - sumX * sumY) / denominator
        } else 0.0
    }
}