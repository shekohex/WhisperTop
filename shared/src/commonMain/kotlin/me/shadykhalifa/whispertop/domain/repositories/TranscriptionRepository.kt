package me.shadykhalifa.whispertop.domain.repositories

import me.shadykhalifa.whispertop.domain.models.TranscriptionRequest
import me.shadykhalifa.whispertop.domain.models.TranscriptionResponse
import me.shadykhalifa.whispertop.domain.models.Language
import me.shadykhalifa.whispertop.utils.Result

interface TranscriptionRepository {
    suspend fun transcribe(request: TranscriptionRequest): Result<TranscriptionResponse>
    suspend fun transcribeWithLanguageDetection(
        request: TranscriptionRequest,
        userLanguageOverride: Language? = null
    ): Result<TranscriptionResponse>
    suspend fun validateApiKey(apiKey: String): Result<Boolean>
    suspend fun isConfigured(): Boolean
}