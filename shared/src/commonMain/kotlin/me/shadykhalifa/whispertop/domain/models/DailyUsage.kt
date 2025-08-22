package me.shadykhalifa.whispertop.domain.models

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import me.shadykhalifa.whispertop.data.serializers.LocalDateSerializer

@Serializable
data class DailyUsage(
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val sessionsCount: Int,
    val wordsTranscribed: Long,
    val totalTimeMs: Long
)