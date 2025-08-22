package me.shadykhalifa.whispertop.presentation.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import me.shadykhalifa.whispertop.domain.models.AppPermission
import me.shadykhalifa.whispertop.domain.models.DailyUsage
import me.shadykhalifa.whispertop.domain.models.TranscriptionSession
import me.shadykhalifa.whispertop.domain.models.UserStatistics

@Parcelize
data class ParcelableTranscriptionSession(
    val id: String,
    val timestampEpochMs: Long,
    val audioLengthMs: Long,
    val wordCount: Int,
    val characterCount: Int,
    val transcribedText: String
) : Parcelable

@Parcelize
data class ParcelableUserStatistics(
    val id: String,
    val totalWords: Long,
    val totalSessions: Int,
    val totalSpeakingTimeMs: Long,
    val averageWordsPerMinute: Double,
    val averageWordsPerSession: Double,
    val userTypingWpm: Int,
    val totalTranscriptions: Long,
    val totalDuration: Float,
    val averageAccuracy: Float?,
    val dailyUsageCount: Long,
    val mostUsedLanguage: String?,
    val mostUsedModel: String?,
    val createdAt: Long,
    val updatedAt: Long
) : Parcelable

@Parcelize
data class ParcelableDailyUsage(
    val dateString: String, // ISO format: YYYY-MM-DD
    val sessionsCount: Int,
    val wordsTranscribed: Long,
    val totalTimeMs: Long
) : Parcelable

@Parcelize
enum class ParcelableAppPermission(
    val displayName: String,
    val description: String
) : Parcelable {
    RECORD_AUDIO(
        displayName = "Record Audio",
        description = "Required to capture voice for transcription"
    ),
    SYSTEM_ALERT_WINDOW(
        displayName = "Display over other apps",
        description = "Required to show the floating microphone button over other apps"
    ),
    ACCESSIBILITY_SERVICE(
        displayName = "Accessibility Service",
        description = "Required to automatically insert transcribed text into input fields"
    )
}

// Extension functions to convert between common and Android models
fun TranscriptionSession.toParcelable(): ParcelableTranscriptionSession {
    return ParcelableTranscriptionSession(
        id = id,
        timestampEpochMs = timestamp.toEpochMilliseconds(),
        audioLengthMs = audioLengthMs,
        wordCount = wordCount,
        characterCount = characterCount,
        transcribedText = transcribedText
    )
}

fun ParcelableTranscriptionSession.toCommon(): TranscriptionSession {
    return TranscriptionSession(
        id = id,
        timestamp = Instant.fromEpochMilliseconds(timestampEpochMs),
        audioLengthMs = audioLengthMs,
        wordCount = wordCount,
        characterCount = characterCount,
        transcribedText = transcribedText
    )
}

fun UserStatistics.toParcelable(): ParcelableUserStatistics {
    return ParcelableUserStatistics(
        id = id,
        totalWords = totalWords,
        totalSessions = totalSessions,
        totalSpeakingTimeMs = totalSpeakingTimeMs,
        averageWordsPerMinute = averageWordsPerMinute,
        averageWordsPerSession = averageWordsPerSession,
        userTypingWpm = userTypingWpm,
        totalTranscriptions = totalTranscriptions,
        totalDuration = totalDuration,
        averageAccuracy = averageAccuracy,
        dailyUsageCount = dailyUsageCount,
        mostUsedLanguage = mostUsedLanguage,
        mostUsedModel = mostUsedModel,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ParcelableUserStatistics.toCommon(): UserStatistics {
    return UserStatistics(
        id = id,
        totalWords = totalWords,
        totalSessions = totalSessions,
        totalSpeakingTimeMs = totalSpeakingTimeMs,
        averageWordsPerMinute = averageWordsPerMinute,
        averageWordsPerSession = averageWordsPerSession,
        userTypingWpm = userTypingWpm,
        totalTranscriptions = totalTranscriptions,
        totalDuration = totalDuration,
        averageAccuracy = averageAccuracy,
        dailyUsageCount = dailyUsageCount,
        mostUsedLanguage = mostUsedLanguage,
        mostUsedModel = mostUsedModel,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun DailyUsage.toParcelable(): ParcelableDailyUsage {
    return ParcelableDailyUsage(
        dateString = date.toString(),
        sessionsCount = sessionsCount,
        wordsTranscribed = wordsTranscribed,
        totalTimeMs = totalTimeMs
    )
}

fun ParcelableDailyUsage.toCommon(): DailyUsage {
    return DailyUsage(
        date = LocalDate.parse(dateString),
        sessionsCount = sessionsCount,
        wordsTranscribed = wordsTranscribed,
        totalTimeMs = totalTimeMs
    )
}

fun AppPermission.toParcelable(): ParcelableAppPermission {
    return when (this) {
        AppPermission.RECORD_AUDIO -> ParcelableAppPermission.RECORD_AUDIO
        AppPermission.SYSTEM_ALERT_WINDOW -> ParcelableAppPermission.SYSTEM_ALERT_WINDOW
        AppPermission.ACCESSIBILITY_SERVICE -> ParcelableAppPermission.ACCESSIBILITY_SERVICE
    }
}

fun ParcelableAppPermission.toCommon(): AppPermission {
    return when (this) {
        ParcelableAppPermission.RECORD_AUDIO -> AppPermission.RECORD_AUDIO
        ParcelableAppPermission.SYSTEM_ALERT_WINDOW -> AppPermission.SYSTEM_ALERT_WINDOW
        ParcelableAppPermission.ACCESSIBILITY_SERVICE -> AppPermission.ACCESSIBILITY_SERVICE
    }
}