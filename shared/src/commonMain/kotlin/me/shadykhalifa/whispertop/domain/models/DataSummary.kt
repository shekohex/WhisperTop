package me.shadykhalifa.whispertop.domain.models

import java.time.LocalDate

data class DataSummary(
    val totalTranscriptions: Int,
    val totalSizeBytes: Long,
    val oldestTranscription: LocalDate?,
    val newestTranscription: LocalDate?,
    val protectedItems: Int,
    val itemsByRetentionPolicy: Map<RetentionPolicy, Int>
)