package me.shadykhalifa.whispertop.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateTranscriptionRequestDto(
    val model: String,
    val language: String? = null,
    val prompt: String? = null,
    @SerialName("response_format") 
    val responseFormat: String = "json",
    val temperature: Float = 0.0f
)

@Serializable
data class CreateTranscriptionResponseDto(
    val text: String
)

@Serializable
data class CreateTranscriptionResponseVerboseDto(
    val language: String,
    val duration: Float,
    val text: String,
    val words: List<TranscriptionWordDto>? = null,
    val segments: List<TranscriptionSegmentDto>? = null
)

@Serializable
data class TranscriptionWordDto(
    val word: String,
    val start: Float,
    val end: Float
)

@Serializable
data class TranscriptionSegmentDto(
    val id: Int,
    val seek: Int,
    val start: Float,
    val end: Float,
    val text: String,
    val tokens: List<Int>,
    val temperature: Float,
    @SerialName("avg_logprob")
    val avgLogprob: Float,
    @SerialName("compression_ratio")
    val compressionRatio: Float,
    @SerialName("no_speech_prob")
    val noSpeechProb: Float
)

data class MultipartTranscriptionRequest(
    val audioData: ByteArray,
    val fileName: String,
    val contentType: String,
    val request: CreateTranscriptionRequestDto
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MultipartTranscriptionRequest

        if (!audioData.contentEquals(other.audioData)) return false
        if (fileName != other.fileName) return false
        if (contentType != other.contentType) return false
        if (request != other.request) return false

        return true
    }

    override fun hashCode(): Int {
        var result = audioData.contentHashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + request.hashCode()
        return result
    }
}