package me.shadykhalifa.whispertop.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Entity(tableName = "user_statistics")
@Serializable
data class UserStatisticsEntity(
    @PrimaryKey
    val id: String = "user_stats",
    val totalWords: Long = 0,
    val totalSessions: Int = 0,
    val totalSpeakingTimeMs: Long = 0,
    val averageWordsPerMinute: Double = 0.0,
    val averageWordsPerSession: Double = 0.0,
    val userTypingWpm: Int = 0,
    val totalTranscriptions: Long = 0,
    val totalDuration: Float = 0f,
    val averageAccuracy: Float? = null,
    val dailyUsageCount: Long = 0,
    val mostUsedLanguage: String? = null,
    val mostUsedModel: String? = null,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)