package me.shadykhalifa.whispertop.data.database.entities

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
    val duration: Float? = null,
    val audioFilePath: String? = null,
    val confidence: Float? = null,
    val customPrompt: String? = null,
    val temperature: Float? = null,
    val language: String? = null,
    val model: String? = null,
    val wordCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)