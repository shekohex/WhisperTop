package me.shadykhalifa.whispertop.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Entity(
    tableName = "session_metrics",
    indices = [
        Index(value = ["sessionStartTime"], name = "idx_session_start_time"),
        Index(value = ["targetAppPackage"], name = "idx_target_app"),
        Index(value = ["transcriptionSuccess"], name = "idx_transcription_success"),
        Index(value = ["errorType"], name = "idx_error_type"),
        // Composite indexes for performance optimization
        Index(value = ["sessionStartTime", "transcriptionSuccess"], name = "idx_session_time_success"),
        Index(value = ["sessionStartTime", "targetAppPackage"], name = "idx_session_time_app"),
        Index(value = ["sessionStartTime", "errorType"], name = "idx_session_time_error"),
        Index(value = ["transcriptionSuccess", "targetAppPackage"], name = "idx_success_app"),
        // Covering index for daily statistics queries
        Index(value = ["sessionStartTime", "transcriptionSuccess", "wordCount", "audioRecordingDuration"], name = "idx_daily_stats_covering")
    ]
)
@Serializable
data class SessionMetricsEntity(
    @PrimaryKey
    val sessionId: String,
    val sessionStartTime: Long,
    val sessionEndTime: Long? = null,
    val audioRecordingDuration: Long = 0,
    val audioFileSize: Long = 0,
    val audioQuality: String? = null,
    val wordCount: Int = 0,
    val characterCount: Int = 0,
    val speakingRate: Double = 0.0,
    val transcriptionText: String? = null,
    val transcriptionSuccess: Boolean = false,
    val textInsertionSuccess: Boolean = false,
    val targetAppPackage: String? = null,
    val errorType: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)