package me.shadykhalifa.whispertop.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class LanguageTest {

    @Test
    fun `fromCode should return correct language for valid codes`() {
        assertEquals(Language.ENGLISH, Language.fromCode("en"))
        assertEquals(Language.SPANISH, Language.fromCode("es"))
        assertEquals(Language.FRENCH, Language.fromCode("fr"))
        assertEquals(Language.GERMAN, Language.fromCode("de"))
        assertEquals(Language.CHINESE, Language.fromCode("zh"))
    }

    @Test
    fun `fromCode should be case insensitive`() {
        assertEquals(Language.ENGLISH, Language.fromCode("EN"))
        assertEquals(Language.SPANISH, Language.fromCode("Es"))
        assertEquals(Language.FRENCH, Language.fromCode("FR"))
    }

    @Test
    fun `fromCode should return AUTO for null or empty code`() {
        assertEquals(Language.AUTO, Language.fromCode(null))
        assertEquals(Language.AUTO, Language.fromCode(""))
        assertEquals(Language.AUTO, Language.fromCode("   "))
    }

    @Test
    fun `fromCode should return null for invalid code`() {
        assertNull(Language.fromCode("xyz"))
        assertNull(Language.fromCode("invalid"))
        assertNull(Language.fromCode("123"))
    }

    @Test
    fun `getApiCode should return null for AUTO language`() {
        assertNull(Language.AUTO.getApiCode())
    }

    @Test
    fun `getApiCode should return correct code for other languages`() {
        assertEquals("en", Language.ENGLISH.getApiCode())
        assertEquals("es", Language.SPANISH.getApiCode())
        assertEquals("fr", Language.FRENCH.getApiCode())
        assertEquals("de", Language.GERMAN.getApiCode())
    }

    @Test
    fun `getFullDisplayName should return display name when same as native name`() {
        assertEquals("English", Language.ENGLISH.getFullDisplayName())
        assertEquals("Auto-detect", Language.AUTO.getFullDisplayName())
    }

    @Test
    fun `getFullDisplayName should return combined name when different`() {
        assertEquals("Chinese (中文)", Language.CHINESE.getFullDisplayName())
        assertEquals("Japanese (日本語)", Language.JAPANESE.getFullDisplayName())
        assertEquals("Russian (Русский)", Language.RUSSIAN.getFullDisplayName())
        assertEquals("Arabic (العربية)", Language.ARABIC.getFullDisplayName())
    }

    @Test
    fun `getPopularLanguages should include expected languages`() {
        val popularLanguages = Language.getPopularLanguages()

        assertTrue(popularLanguages.contains(Language.AUTO))
        assertTrue(popularLanguages.contains(Language.ENGLISH))
        assertTrue(popularLanguages.contains(Language.SPANISH))
        assertTrue(popularLanguages.contains(Language.FRENCH))
        assertTrue(popularLanguages.contains(Language.GERMAN))
        assertTrue(popularLanguages.contains(Language.CHINESE))
        assertTrue(popularLanguages.contains(Language.JAPANESE))
    }

    @Test
    fun `getSupportedLanguages should not include AUTO`() {
        val supportedLanguages = Language.getSupportedLanguages()

        assertFalse(supportedLanguages.contains(Language.AUTO))
        assertTrue(supportedLanguages.contains(Language.ENGLISH))
        assertTrue(supportedLanguages.contains(Language.SPANISH))
        assertTrue(supportedLanguages.size > 20) // Should have many languages
    }

    @Test
    fun `all languages should have valid ISO codes except AUTO`() {
        Language.entries.forEach { language ->
            if (language == Language.AUTO) {
                assertEquals("", language.code)
            } else {
                assertTrue(language.code.isNotBlank())
                assertTrue(language.code.length == 2) // ISO-639-1 codes are 2 characters
            }
        }
    }

    @Test
    fun `all languages should have display names`() {
        Language.entries.forEach { language ->
            assertTrue(language.displayName.isNotBlank())
            assertTrue(language.nativeName.isNotBlank())
        }
    }

    @Test
    fun `language codes should be unique`() {
        val codes = Language.entries.map { it.code }
        val uniqueCodes = codes.toSet()
        assertEquals(codes.size, uniqueCodes.size)
    }
}

class LanguageDetectionResultTest {

    @Test
    fun `autoDetected should create result with correct values`() {
        val language = Language.ENGLISH
        val confidence = 0.95f

        val result = LanguageDetectionResult.autoDetected(language, confidence)

        assertEquals(language, result.detectedLanguage)
        assertEquals(confidence, result.confidence)
        assertFalse(result.isManualOverride)
    }

    @Test
    fun `autoDetected should work without confidence`() {
        val language = Language.SPANISH

        val result = LanguageDetectionResult.autoDetected(language)

        assertEquals(language, result.detectedLanguage)
        assertNull(result.confidence)
        assertFalse(result.isManualOverride)
    }

    @Test
    fun `manualOverride should create result with correct values`() {
        val language = Language.FRENCH

        val result = LanguageDetectionResult.manualOverride(language)

        assertEquals(language, result.detectedLanguage)
        assertNull(result.confidence)
        assertTrue(result.isManualOverride)
    }

    @Test
    fun `getConfidencePercentage should return null when confidence is null`() {
        val result = LanguageDetectionResult.manualOverride(Language.ENGLISH)

        assertNull(result.getConfidencePercentage())
    }

    @Test
    fun `getConfidencePercentage should return formatted percentage`() {
        val result = LanguageDetectionResult.autoDetected(Language.ENGLISH, 0.856f)

        assertEquals("85%", result.getConfidencePercentage())
    }

    @Test
    fun `getConfidencePercentage should handle edge cases`() {
        assertEquals("0%", LanguageDetectionResult.autoDetected(Language.ENGLISH, 0.0f).getConfidencePercentage())
        assertEquals("100%", LanguageDetectionResult.autoDetected(Language.ENGLISH, 1.0f).getConfidencePercentage())
        assertEquals("50%", LanguageDetectionResult.autoDetected(Language.ENGLISH, 0.504f).getConfidencePercentage())
    }
}

class LanguagePreferenceTest {

    @Test
    fun `default constructor should have expected values`() {
        val preference = LanguagePreference()

        assertEquals(Language.AUTO, preference.preferredLanguage)
        assertTrue(preference.autoDetectEnabled)
        assertTrue(preference.showConfidence)
        assertTrue(preference.allowManualOverride)
    }

    @Test
    fun `copy should allow modification of individual fields`() {
        val original = LanguagePreference()

        val modified = original.copy(
            preferredLanguage = Language.ENGLISH,
            autoDetectEnabled = false,
            showConfidence = false,
            allowManualOverride = false
        )

        assertEquals(Language.ENGLISH, modified.preferredLanguage)
        assertFalse(modified.autoDetectEnabled)
        assertFalse(modified.showConfidence)
        assertFalse(modified.allowManualOverride)

        // Original should be unchanged
        assertEquals(Language.AUTO, original.preferredLanguage)
        assertTrue(original.autoDetectEnabled)
        assertTrue(original.showConfidence)
        assertTrue(original.allowManualOverride)
    }
}