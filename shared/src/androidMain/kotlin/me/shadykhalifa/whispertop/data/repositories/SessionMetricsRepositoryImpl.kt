package me.shadykhalifa.whispertop.data.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.shadykhalifa.whispertop.data.database.dao.SessionMetricsDao
import me.shadykhalifa.whispertop.data.database.entities.SessionMetricsEntity
import me.shadykhalifa.whispertop.domain.models.SessionMetrics
import me.shadykhalifa.whispertop.domain.repositories.SessionMetricsRepository
import me.shadykhalifa.whispertop.domain.repositories.SessionStatistics
import me.shadykhalifa.whispertop.utils.Result
import me.shadykhalifa.whispertop.utils.PrivacyUtils

import me.shadykhalifa.whispertop.utils.CircuitBreaker
import me.shadykhalifa.whispertop.utils.CircuitBreakerOpenException
import me.shadykhalifa.whispertop.utils.PrivacyComplianceManager
import me.shadykhalifa.whispertop.utils.PrivacySettings

class SessionMetricsRepositoryImpl(
    private val sessionMetricsDao: SessionMetricsDao
) : BaseRepository(), SessionMetricsRepository {
    
    // Privacy settings - in production these would come from user preferences
    private val privacySettings = PrivacySettings(
        enableDataCollection = true,
        enableAnalytics = false,
        enableTranscriptionStorage = true,
        enableAppUsageTracking = false,
        dataRetentionDays = 90,
        anonymizeAfterDays = 30
    )
    
    // Circuit breaker for database operations
    private val circuitBreaker = CircuitBreaker(
        failureThreshold = 3,
        recoveryTimeoutMs = 30000, // 30 seconds
        successThreshold = 2
    )

    override suspend fun createSessionMetrics(sessionMetrics: SessionMetrics): Result<Unit> = execute {
        val validationResult = sessionMetrics.validate()
        if (validationResult is me.shadykhalifa.whispertop.domain.models.ValidationResult.Invalid) {
            throw IllegalArgumentException("Invalid session metrics: ${validationResult.errors.joinToString()}")
        }
        
        try {
            circuitBreaker.execute {
                sessionMetricsDao.insert(sessionMetrics.sanitized().toEntity())
            }
        } catch (e: CircuitBreakerOpenException) {
            // Graceful degradation - log the error but don't fail the operation
            android.util.Log.w("SessionMetricsRepo", "Database circuit breaker open, skipping metrics insert")
            throw e
        }
    }

    override suspend fun getSessionMetrics(sessionId: String): Result<SessionMetrics?> = execute {
        sessionMetricsDao.getById(sessionId)?.toDomainModel()
    }

    override fun getSessionMetricsFlow(sessionId: String): Flow<SessionMetrics?> {
        return sessionMetricsDao.getByIdFlow(sessionId).map { it?.toDomainModel() }
    }

    override suspend fun updateSessionMetrics(sessionMetrics: SessionMetrics): Result<Unit> = execute {
        val validationResult = sessionMetrics.validate()
        if (validationResult is me.shadykhalifa.whispertop.domain.models.ValidationResult.Invalid) {
            throw IllegalArgumentException("Invalid session metrics: ${validationResult.errors.joinToString()}")
        }
        sessionMetricsDao.insertOrUpdate(sessionMetrics.sanitized().toEntity())
    }

    override suspend fun getRecentSessions(limit: Int): Result<List<SessionMetrics>> = execute {
        sessionMetricsDao.getRecent(limit).map { it.toDomainModel() }
    }

    override suspend fun getSessionsByDateRange(startTime: Long, endTime: Long): Result<List<SessionMetrics>> = execute {
        sessionMetricsDao.getByDateRange(startTime, endTime).map { it.toDomainModel() }
    }

    override suspend fun getSessionsByApp(packageName: String): Result<List<SessionMetrics>> = execute {
        sessionMetricsDao.getByTargetApp(packageName).map { it.toDomainModel() }
    }

    override suspend fun getSessionStatistics(): Result<SessionStatistics> = execute {
        val totalSessions = sessionMetricsDao.getTotalSessionCount()
        val successfulSessions = sessionMetricsDao.getSuccessfulSessionCount()
        val averageWordCount = sessionMetricsDao.getAverageWordCount() ?: 0.0
        val averageRecordingDuration = sessionMetricsDao.getAverageRecordingDuration() ?: 0L
        val averageSpeakingRate = sessionMetricsDao.getAverageSpeakingRate() ?: 0.0
        val mostUsedApps = sessionMetricsDao.getMostUsedApps()
        val errorStatistics = sessionMetricsDao.getErrorStatistics()

        SessionStatistics(
            totalSessions = totalSessions,
            successfulSessions = successfulSessions,
            averageWordCount = averageWordCount,
            averageRecordingDuration = averageRecordingDuration,
            averageSpeakingRate = averageSpeakingRate,
            mostUsedApps = mostUsedApps,
            errorStatistics = errorStatistics
        )
    }

    override suspend fun deleteOldSessions(olderThan: Long): Result<Int> = execute {
        sessionMetricsDao.deleteOlderThan(olderThan)
    }

    override suspend fun deleteAllSessions(): Result<Unit> = execute {
        sessionMetricsDao.deleteAll()
    }

    override suspend fun updateSessionWithTextInsertionData(
        sessionId: String,
        wordCount: Int,
        characterCount: Int,
        speakingRate: Double,
        transcriptionText: String?,
        transcriptionSuccess: Boolean,
        textInsertionSuccess: Boolean,
        targetAppPackage: String?
    ): Result<Unit> = execute {
        sessionMetricsDao.updateWithTranscriptionData(
            sessionId = sessionId,
            wordCount = wordCount,
            characterCount = characterCount,
            speakingRate = speakingRate,
            transcriptionText = transcriptionText,
            transcriptionSuccess = transcriptionSuccess,
            textInsertionSuccess = textInsertionSuccess,
            targetAppPackage = targetAppPackage
        )
    }

    private fun SessionMetricsEntity.toDomainModel(): SessionMetrics {
        // No need for manual decryption - SQLCipher handles database encryption transparently
        val decryptedTranscriptionText = transcriptionText
        val decryptedTargetAppPackage = targetAppPackage
        
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
            transcriptionText = decryptedTranscriptionText,
            transcriptionSuccess = transcriptionSuccess,
            textInsertionSuccess = textInsertionSuccess,
            targetAppPackage = decryptedTargetAppPackage,
            errorType = errorType,
            errorMessage = errorMessage
        )
    }

    private fun SessionMetrics.toEntity(): SessionMetricsEntity {
        val currentTime = System.currentTimeMillis()
        
        // Apply privacy compliance transformations
        val privacySafeTranscriptionText = transcriptionText?.let { text ->
            when {
                !privacySettings.enableTranscriptionStorage -> {
                    // Create audit record for data processing decision
                    PrivacyComplianceManager.createDataProcessingRecord(
                        operation = "transcription_storage_denied",
                        dataTypes = listOf("transcription_text"),
                        purposeId = PrivacyComplianceManager.CORE_FUNCTIONALITY.id
                    )
                    null
                }
                else -> {
                    // Apply privacy transformations based on data age and settings
                    val dataAge = sessionStartTime
                    val transformedText = PrivacyComplianceManager.applyPrivacyTransformations(
                        data = text,
                        settings = privacySettings,
                        dataAge = dataAge
                    )
                    
                    // Additional truncation if needed
                    if (transformedText.length > SessionMetrics.MAX_TRANSCRIPTION_LENGTH) {
                        transformedText.take(SessionMetrics.MAX_TRANSCRIPTION_LENGTH) + "..."
                    } else {
                        transformedText
                    }
                }
            }
        }
        
        val privacySafeAppPackage = if (privacySettings.enableAppUsageTracking) {
            targetAppPackage?.let { pkg ->
                PrivacyComplianceManager.applyPrivacyTransformations(
                    data = pkg,
                    settings = privacySettings,
                    dataAge = sessionStartTime
                )
            }
        } else {
            // Create audit record for app tracking opt-out
            PrivacyComplianceManager.createDataProcessingRecord(
                operation = "app_tracking_denied",
                dataTypes = listOf("app_package"),
                purposeId = PrivacyComplianceManager.PERSONALIZATION.id
            )
            null
        }
        
        return SessionMetricsEntity(
            sessionId = sessionId,
            sessionStartTime = sessionStartTime,
            sessionEndTime = sessionEndTime,
            audioRecordingDuration = audioRecordingDuration,
            audioFileSize = audioFileSize,
            audioQuality = audioQuality,
            wordCount = wordCount,
            characterCount = characterCount,
            speakingRate = speakingRate,
            transcriptionText = privacySafeTranscriptionText,
            transcriptionSuccess = transcriptionSuccess,
            textInsertionSuccess = textInsertionSuccess,
            targetAppPackage = privacySafeAppPackage,
            errorType = errorType,
            errorMessage = errorMessage,
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }
    
    override suspend fun getSessionsOlderThan(cutoffTime: Long): Result<List<SessionMetrics>> = execute {
        sessionMetricsDao.getSessionsOlderThan(cutoffTime).map { it.toDomainModel() }
    }
    
    override suspend fun deleteSessionsOlderThan(cutoffTime: Long): Result<Int> = execute {
        sessionMetricsDao.deleteSessionsOlderThan(cutoffTime)
    }
    
    override fun getAllSessionsFlow(): Flow<List<SessionMetrics>> {
        return sessionMetricsDao.getAllFlow().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getSessionsByDateRangeFlow(startTime: Long, endTime: Long): Flow<List<SessionMetrics>> {
        // Since Room doesn't support parameterized flows easily, we'll use a simple approach
        return kotlinx.coroutines.flow.flow {
            while (true) {
                try {
                    val sessions = sessionMetricsDao.getByDateRange(startTime, endTime)
                    emit(sessions.map { it.toDomainModel() })
                } catch (e: Exception) {
                    emit(emptyList())
                }
                kotlinx.coroutines.delay(5000) // Update every 5 seconds
            }
        }
    }
    
    override suspend fun getSessionsByDateRangePaginated(
        startTime: Long, 
        endTime: Long, 
        limit: Int, 
        offset: Int
    ): Result<List<SessionMetrics>> = execute {
        sessionMetricsDao.getByDateRangePaginated(startTime, endTime, limit, offset)
            .map { it.toDomainModel() }
    }
    
    override suspend fun getSessionCountByDateRange(startTime: Long, endTime: Long): Result<Int> = execute {
        sessionMetricsDao.getByDateRange(startTime, endTime).size
    }
}