package me.shadykhalifa.whispertop.domain.usecases

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import me.shadykhalifa.whispertop.domain.models.Language
import me.shadykhalifa.whispertop.domain.models.LanguageDetectionResult
import me.shadykhalifa.whispertop.domain.models.LanguagePreference

class LanguageDetectionUseCaseTest {

    private val useCase = LanguageDetectionUseCase()

    @Test
    fun `determineTranscriptionLanguage should return null for auto-detect when preference is auto`() {
        val preference = LanguagePreference(
            preferredLanguage = Language.AUTO,
            autoDetectEnabled = true
        )

        val result = useCase.determineTranscriptionLanguage(preference)

        assertNull(result)
    }

    @Test
    fun `determineTranscriptionLanguage should return user override when provided`() {
        val preference = LanguagePreference(
            preferredLanguage = Language.AUTO,
            autoDetectEnabled = true
        )
        val userOverride = Language.ENGLISH

        val result = useCase.determineTranscriptionLanguage(preference, userOverride)

        assertEquals("en", result)
    }

    @Test
    fun `determineTranscriptionLanguage should return preferred language when auto-detect disabled`() {
        val preference = LanguagePreference(
            preferredLanguage = Language.SPANISH,
            autoDetectEnabled = false
        )

        val result = useCase.determineTranscriptionLanguage(preference)

        assertEquals("es", result)
    }

    @Test
    fun `determineTranscriptionLanguage should prioritize user override over preferred language`() {
        val preference = LanguagePreference(
            preferredLanguage = Language.SPANISH,
            autoDetectEnabled = false
        )
        val userOverride = Language.FRENCH

        val result = useCase.determineTranscriptionLanguage(preference, userOverride)

        assertEquals("fr", result)
    }

    @Test
    fun `createDetectionResult should return manual override when user override provided`() {
        val userOverride = Language.GERMAN

        val result = useCase.createDetectionResult(
            detectedLanguageCode = "en",
            userOverride = userOverride
        )

        assertTrue(result!!.isManualOverride)
        assertEquals(Language.GERMAN, result.detectedLanguage)
        assertNull(result.confidence)
    }

    @Test
    fun `createDetectionResult should return auto-detected with confidence when API detects language`() {
        val confidence = 0.95f

        val result = useCase.createDetectionResult(
            detectedLanguageCode = "es",
            confidence = confidence
        )

        assertFalse(result!!.isManualOverride)
        assertEquals(Language.SPANISH, result.detectedLanguage)
        assertEquals(confidence, result.confidence)
    }

    @Test
    fun `createDetectionResult should return AUTO language for unrecognized code`() {
        val result = useCase.createDetectionResult(
            detectedLanguageCode = "xyz", // Invalid language code
            confidence = 0.8f
        )

        assertFalse(result!!.isManualOverride)
        assertEquals(Language.AUTO, result.detectedLanguage)
        assertEquals(0.8f, result.confidence)
    }

    @Test
    fun `createDetectionResult should return null when no detection available`() {
        val result = useCase.createDetectionResult(
            detectedLanguageCode = null,
            userOverride = null
        )

        assertNull(result)
    }

    @Test
    fun `shouldShowLanguageDetection should return true when conditions met`() {
        val preference = LanguagePreference(showConfidence = true)
        val detectionResult = LanguageDetectionResult.autoDetected(Language.ENGLISH, 0.9f)

        val result = useCase.shouldShowLanguageDetection(preference, detectionResult)

        assertTrue(result)
    }

    @Test
    fun `shouldShowLanguageDetection should return false when showConfidence disabled`() {
        val preference = LanguagePreference(showConfidence = false)
        val detectionResult = LanguageDetectionResult.autoDetected(Language.ENGLISH, 0.9f)

        val result = useCase.shouldShowLanguageDetection(preference, detectionResult)

        assertFalse(result)
    }

    @Test
    fun `shouldShowLanguageDetection should return false when detection result is null`() {
        val preference = LanguagePreference(showConfidence = true)

        val result = useCase.shouldShowLanguageDetection(preference, null)

        assertFalse(result)
    }

    @Test
    fun `shouldShowLanguageDetection should return false when detected language is AUTO`() {
        val preference = LanguagePreference(showConfidence = true)
        val detectionResult = LanguageDetectionResult.autoDetected(Language.AUTO, 0.9f)

        val result = useCase.shouldShowLanguageDetection(preference, detectionResult)

        assertFalse(result)
    }

    @Test
    fun `shouldShowManualOverride should return preference value`() {
        val preferenceTrue = LanguagePreference(allowManualOverride = true)
        val preferenceFalse = LanguagePreference(allowManualOverride = false)

        assertTrue(useCase.shouldShowManualOverride(preferenceTrue))
        assertFalse(useCase.shouldShowManualOverride(preferenceFalse))
    }

    @Test
    fun `getRecommendedModelForLanguageDetection should return gpt-4o-transcribe`() {
        val model = useCase.getRecommendedModelForLanguageDetection()

        assertEquals("gpt-4o-transcribe", model)
    }

    @Test
    fun `getRecommendedResponseFormat should return verbose_json`() {
        val format = useCase.getRecommendedResponseFormat()

        assertEquals("verbose_json", format)
    }

    @Test
    fun `isLanguageSupported should return true for null or empty code`() {
        assertTrue(useCase.isLanguageSupported(null))
        assertTrue(useCase.isLanguageSupported(""))
        assertTrue(useCase.isLanguageSupported("   "))
    }

    @Test
    fun `isLanguageSupported should return true for valid language codes`() {
        assertTrue(useCase.isLanguageSupported("en"))
        assertTrue(useCase.isLanguageSupported("es"))
        assertTrue(useCase.isLanguageSupported("fr"))
        assertTrue(useCase.isLanguageSupported("EN")) // Case insensitive
    }

    @Test
    fun `isLanguageSupported should return false for invalid language codes`() {
        assertFalse(useCase.isLanguageSupported("xyz"))
        assertFalse(useCase.isLanguageSupported("invalid"))
        assertFalse(useCase.isLanguageSupported("123"))
    }

    @Test
    fun `getPopularLanguages should return expected popular languages`() {
        val popularLanguages = useCase.getPopularLanguages()

        assertTrue(popularLanguages.contains(Language.AUTO))
        assertTrue(popularLanguages.contains(Language.ENGLISH))
        assertTrue(popularLanguages.contains(Language.SPANISH))
        assertTrue(popularLanguages.contains(Language.FRENCH))
        assertTrue(popularLanguages.contains(Language.GERMAN))
        assertTrue(popularLanguages.size >= 5)
    }

    @Test
    fun `getAllSupportedLanguages should not include AUTO language`() {
        val supportedLanguages = useCase.getAllSupportedLanguages()

        assertFalse(supportedLanguages.contains(Language.AUTO))
        assertTrue(supportedLanguages.contains(Language.ENGLISH))
        assertTrue(supportedLanguages.contains(Language.SPANISH))
        assertTrue(supportedLanguages.size > 10)
    }
}