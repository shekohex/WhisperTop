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
    val model: String?,
    val wordCount: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    // Retention and export tracking fields
    val retentionPolicyId: String? = null,
    val isProtected: Boolean = false,
    val exportCount: Int = 0,
    val lastExported: Long? = null
)