package me.shadykhalifa.whispertop.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Entity(
    tableName = "transcription_history",
    indices = [
        Index(value = ["timestamp"], name = "idx_timestamp"),
        Index(value = ["text"], name = "idx_text_search"),
        Index(value = ["retentionPolicyId"], name = "idx_retention_policy"),
        Index(value = ["isProtected"], name = "idx_protected"),
        Index(value = ["lastExported"], name = "idx_last_exported")
    ]
)
@Serializable
data class TranscriptionHistoryEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val timestamp: Long,
    val duration: Float? = null,
    val audioFilePath: String? = null,
    val confidence: Float? = null,
    val customPrompt: String? = null,
    val temperature: Float? = null,
    val language: String? = null,
    val model: String? = null,
    val wordCount: Int = 0,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    // Retention and export tracking fields
    val retentionPolicyId: String? = null,
    val isProtected: Boolean = false,
    val exportCount: Int = 0,
    val lastExported: Long? = null
)