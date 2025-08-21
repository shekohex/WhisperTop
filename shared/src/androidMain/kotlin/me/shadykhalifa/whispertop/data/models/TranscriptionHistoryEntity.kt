package me.shadykhalifa.whispertop.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Entity(
    tableName = "transcription_history",
    indices = [
        Index(value = ["timestamp"], name = "idx_timestamp"),
        Index(value = ["text"], name = "idx_text_search")
    ]
)
@Serializable
data class TranscriptionHistoryEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val timestamp: Long,
    val duration: Float?,
    val audioFilePath: String?,
    val confidence: Float?,
    val customPrompt: String?,
    val temperature: Float?,
    val language: String?,
    val model: String?
)