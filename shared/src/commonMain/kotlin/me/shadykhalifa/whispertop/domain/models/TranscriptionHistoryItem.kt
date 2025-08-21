package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class TranscriptionHistoryItem(
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