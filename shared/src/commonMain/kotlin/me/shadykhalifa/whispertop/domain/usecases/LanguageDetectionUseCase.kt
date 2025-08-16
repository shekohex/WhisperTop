package me.shadykhalifa.whispertop.domain.usecases

import me.shadykhalifa.whispertop.domain.models.Language
import me.shadykhalifa.whispertop.domain.models.LanguageDetectionResult
import me.shadykhalifa.whispertop.domain.models.LanguagePreference

/**
 * Use case for handling language detection and selection logic
 */
class LanguageDetectionUseCase {

    /**
     * Determines the language to use for transcription based on user preferences
     */
    fun determineTranscriptionLanguage(
        preference: LanguagePreference,
        userOverride: Language? = null
    ): String? {
        return when {
            // User manually selected a language for this transcription
            userOverride != null && userOverride != Language.AUTO -> userOverride.getApiCode()
            
            // User has auto-detect disabled and a preferred language set
            !preference.autoDetectEnabled && preference.preferredLanguage != Language.AUTO -> 
                preference.preferredLanguage.getApiCode()
            
            // Use auto-detection (send null to OpenAI API)
            else -> null
        }
    }

    /**
     * Creates language detection result from API response
     */
    fun createDetectionResult(
        detectedLanguageCode: String?,
        userOverride: Language? = null,
        confidence: Float? = null
    ): LanguageDetectionResult? {
        return when {
            // User manually overrode the language
            userOverride != null -> LanguageDetectionResult.manualOverride(userOverride)
            
            // API detected a language
            detectedLanguageCode != null -> {
                val detectedLanguage = Language.fromCode(detectedLanguageCode) ?: Language.AUTO
                LanguageDetectionResult.autoDetected(detectedLanguage, confidence)
            }
            
            // No detection available
            else -> null
        }
    }

    /**
     * Determines if language detection should be shown to the user
     */
    fun shouldShowLanguageDetection(
        preference: LanguagePreference,
        detectionResult: LanguageDetectionResult?
    ): Boolean {
        return preference.showConfidence && 
               detectionResult != null && 
               detectionResult.detectedLanguage != Language.AUTO
    }

    /**
     * Determines if manual override option should be shown
     */
    fun shouldShowManualOverride(preference: LanguagePreference): Boolean {
        return preference.allowManualOverride
    }

    /**
     * Gets the appropriate model for language detection
     * Newer models generally have better language detection capabilities
     */
    fun getRecommendedModelForLanguageDetection(): String {
        return "gpt-4o-transcribe" // Best performing model for language detection
    }

    /**
     * Gets the appropriate response format for language detection
     * Verbose JSON provides language detection information
     */
    fun getRecommendedResponseFormat(): String {
        return "verbose_json"
    }

    /**
     * Validates if a language is supported
     */
    fun isLanguageSupported(languageCode: String?): Boolean {
        if (languageCode.isNullOrBlank()) return true // Auto-detect is always supported
        return Language.fromCode(languageCode) != null
    }

    /**
     * Gets popular languages for UI selection
     */
    fun getPopularLanguages(): List<Language> {
        return Language.getPopularLanguages()
    }

    /**
     * Gets all supported languages for UI selection
     */
    fun getAllSupportedLanguages(): List<Language> {
        return Language.getSupportedLanguages()
    }
}