package me.shadykhalifa.whispertop.data.database.mappers

import me.shadykhalifa.whispertop.data.database.entities.TranscriptionHistoryEntity
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistory

fun TranscriptionHistoryEntity.toDomain(): TranscriptionHistory {
    return TranscriptionHistory(
        id = id,
        text = text,
        timestamp = timestamp,
        duration = duration,
        audioFilePath = audioFilePath,
        confidence = confidence,
        customPrompt = customPrompt,
        temperature = temperature,
        language = language,
        model = model,
        wordCount = wordCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun TranscriptionHistory.toEntity(): TranscriptionHistoryEntity {
    return TranscriptionHistoryEntity(
        id = id,
        text = text,
        timestamp = timestamp,
        duration = duration,
        audioFilePath = audioFilePath,
        confidence = confidence,
        customPrompt = customPrompt,
        temperature = temperature,
        language = language,
        model = model,
        wordCount = wordCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun List<TranscriptionHistoryEntity>.toDomain(): List<TranscriptionHistory> {
    return map { it.toDomain() }
}

fun List<TranscriptionHistory>.toEntity(): List<TranscriptionHistoryEntity> {
    return map { it.toEntity() }
}