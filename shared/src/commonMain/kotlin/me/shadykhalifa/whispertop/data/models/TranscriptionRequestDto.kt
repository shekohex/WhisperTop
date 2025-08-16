package me.shadykhalifa.whispertop.data.models

import kotlinx.serialization.Serializable

@Serializable
data class TranscriptionRequestDto(
    val file: String,
    val model: String = "whisper-1",
    val language: String? = null,
    val prompt: String? = null,
    val response_format: String = "json",
    val temperature: Float = 0.0f
)

@Serializable
data class TranscriptionResponseDto(
    val text: String,
    val language: String? = null,
    val duration: Float? = null,
    val words: List<WordDto>? = null
)

@Serializable
data class WordDto(
    val word: String,
    val start: Float,
    val end: Float
)

fun TranscriptionResponseDto.toDomain(): me.shadykhalifa.whispertop.domain.models.TranscriptionResponse {
    return me.shadykhalifa.whispertop.domain.models.TranscriptionResponse(
        text = text,
        language = language,
        duration = duration
    )
}

fun me.shadykhalifa.whispertop.domain.models.TranscriptionRequest.toOpenAIDto(): CreateTranscriptionRequestDto {
    return CreateTranscriptionRequestDto(
        model = model,
        language = language,
        responseFormat = "json"
    )
}

fun CreateTranscriptionResponseDto.toDomain(): me.shadykhalifa.whispertop.domain.models.TranscriptionResponse {
    return me.shadykhalifa.whispertop.domain.models.TranscriptionResponse(
        text = text,
        language = null,
        duration = null
    )
}

fun CreateTranscriptionResponseVerboseDto.toDomain(): me.shadykhalifa.whispertop.domain.models.TranscriptionResponse {
    return me.shadykhalifa.whispertop.domain.models.TranscriptionResponse(
        text = text,
        language = language,
        duration = duration,
        languageDetection = language?.let { langCode ->
            val detectedLanguage = me.shadykhalifa.whispertop.domain.models.Language.fromCode(langCode) 
                ?: me.shadykhalifa.whispertop.domain.models.Language.AUTO
            me.shadykhalifa.whispertop.domain.models.LanguageDetectionResult.autoDetected(detectedLanguage)
        }
    )
}