package me.shadykhalifa.whispertop.domain.usecases

import me.shadykhalifa.whispertop.domain.models.Language
import me.shadykhalifa.whispertop.domain.models.TranscriptionRequest
import me.shadykhalifa.whispertop.domain.models.TranscriptionResponse
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.utils.Result

/**
 * Use case that handles transcription with automatic language detection and manual override
 */
class TranscribeWithLanguageDetectionUseCase(
    private val transcriptionRepository: TranscriptionRepository,
    private val settingsRepository: SettingsRepository,
    private val languageDetectionUseCase: LanguageDetectionUseCase = LanguageDetectionUseCase()
) {
    
    /**
     * Transcribe audio with automatic language detection
     */
    suspend fun execute(
        request: TranscriptionRequest,
        userLanguageOverride: Language? = null
    ): Result<TranscriptionResponse> {
        val settings = settingsRepository.getSettings()
        
        // Check if language detection is enabled and supported
        return if (settings.languagePreference.autoDetectEnabled || userLanguageOverride != null) {
            // Use language detection capable transcription
            transcriptionRepository.transcribeWithLanguageDetection(request, userLanguageOverride)
        } else {
            // Use regular transcription without language detection
            transcriptionRepository.transcribe(request)
        }
    }

    /**
     * Get the recommended model for language detection
     */
    fun getRecommendedModel(): String {
        return languageDetectionUseCase.getRecommendedModelForLanguageDetection()
    }

    /**
     * Check if the current settings support language detection
     */
    suspend fun isLanguageDetectionSupported(): Boolean {
        return try {
            val settings = settingsRepository.getSettings()
            settings.apiKey.isNotBlank() && 
            settings.languagePreference.autoDetectEnabled
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get popular languages for UI selection
     */
    fun getPopularLanguages(): List<Language> {
        return languageDetectionUseCase.getPopularLanguages()
    }

    /**
     * Get all supported languages for UI selection
     */
    fun getAllSupportedLanguages(): List<Language> {
        return languageDetectionUseCase.getAllSupportedLanguages()
    }

    /**
     * Check if manual override should be shown to user
     */
    suspend fun shouldShowManualOverride(): Boolean {
        return try {
            val settings = settingsRepository.getSettings()
            languageDetectionUseCase.shouldShowManualOverride(settings.languagePreference)
        } catch (e: Exception) {
            false
        }
    }
}