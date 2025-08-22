package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class TranscriptionHistory(
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
    val createdAt: Long,
    val updatedAt: Long
)

interface TranscriptionHistoryData {
    val id: String
    val text: String
    val timestamp: Long
    val duration: Float?
    val audioFilePath: String?
    val confidence: Float?
    val customPrompt: String?
    val temperature: Float?
    val language: String?
    val model: String?
    val wordCount: Int
    val createdAt: Long
    val updatedAt: Long
}