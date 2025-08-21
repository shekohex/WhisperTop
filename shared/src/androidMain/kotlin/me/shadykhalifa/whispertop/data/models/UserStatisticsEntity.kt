package me.shadykhalifa.whispertop.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Entity(
    tableName = "user_statistics",
    indices = [
        Index(value = ["updatedAt"], name = "idx_updated_at")
    ]
)
@Serializable
data class UserStatisticsEntity(
    @PrimaryKey
    val id: String,
    val totalTranscriptions: Long,
    val totalDuration: Float,
    val averageAccuracy: Float?,
    val dailyUsageCount: Long,
    val mostUsedLanguage: String?,
    val mostUsedModel: String?,
    val createdAt: Long,
    val updatedAt: Long
)