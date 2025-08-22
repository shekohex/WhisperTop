package me.shadykhalifa.whispertop.domain.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.shadykhalifa.whispertop.data.serializers.InstantSerializer

@Serializable
data class TranscriptionSession(
    val id: String,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant,
    val audioLengthMs: Long,
    val wordCount: Int,
    val characterCount: Int,
    val transcribedText: String
)