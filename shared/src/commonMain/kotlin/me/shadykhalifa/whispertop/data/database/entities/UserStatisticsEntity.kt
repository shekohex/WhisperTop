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
    val totalTranscriptions: Long = 0,
    val totalDurationMinutes: Float = 0f,
    val totalWordsTranscribed: Long = 0,
    val averageConfidence: Float = 0f,
    val dailyUsageCount: Int = 0,
    val lastUsedDate: String = "",
    val mostUsedLanguage: String = "",
    val mostUsedModel: String = "",
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)