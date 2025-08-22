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
import me.shadykhalifa.whispertop.utils.EncryptionUtils
import me.shadykhalifa.whispertop.utils.CircuitBreaker
import me.shadykhalifa.whispertop.utils.CircuitBreakerOpenException

class SessionMetricsRepositoryImpl(
    private val sessionMetricsDao: SessionMetricsDao
) : BaseRepository(), SessionMetricsRepository {
    
    // TODO: Inject settings repository to get privacy preferences
    // For now, using conservative privacy defaults
    private val enableTranscriptionStorage = true
    private val hashTranscriptionText = false
    private val enableAppUsageTracking = false
    private val encryptSensitiveData = true // Enable field-level encryption for sensitive data
    
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
        // Decrypt sensitive fields if they were encrypted
        val decryptedTranscriptionText = transcriptionText?.let { text ->
            if (encryptSensitiveData && EncryptionUtils.isObfuscated(text)) {
                EncryptionUtils.deobfuscateText(text)
            } else {
                text
            }
        }
        
        val decryptedTargetAppPackage = targetAppPackage?.let { pkg ->
            if (encryptSensitiveData && EncryptionUtils.isObfuscated(pkg)) {
                EncryptionUtils.deobfuscateText(pkg)
            } else {
                pkg
            }
        }
        
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
        
        // Apply privacy controls to sensitive data
        val privacySafeTranscriptionText = transcriptionText?.let { text ->
            when {
                !enableTranscriptionStorage -> null
                hashTranscriptionText -> PrivacyUtils.anonymizeTranscription(text, preserveMetrics = true)
                PrivacyUtils.containsSensitiveInfo(text) -> {
                    val sanitized = PrivacyUtils.sanitizeText(text)
                    if (encryptSensitiveData) EncryptionUtils.obfuscateText(sanitized) else sanitized
                }
                text.length > SessionMetrics.MAX_TRANSCRIPTION_LENGTH -> {
                    val truncated = PrivacyUtils.truncateForStorage(text, SessionMetrics.MAX_TRANSCRIPTION_LENGTH)
                    if (encryptSensitiveData) EncryptionUtils.obfuscateText(truncated) else truncated
                }
                encryptSensitiveData -> EncryptionUtils.obfuscateText(text)
                else -> text
            }
        }
        
        val privacySafeAppPackage = if (enableAppUsageTracking) {
            if (encryptSensitiveData) EncryptionUtils.obfuscateText(targetAppPackage) else targetAppPackage
        } else null
        
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
}