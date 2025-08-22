package me.shadykhalifa.whispertop.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import me.shadykhalifa.whispertop.utils.TestConstants

class AppSettingsTranscriptionCustomizationTest {

    @Test
    fun `should have default values for transcription customization fields`() {
        val settings = AppSettings()
        
        assertNull(settings.customPrompt, "Custom prompt should default to null")
        assertEquals(0.0f, settings.temperature, "Temperature should default to 0.0f")
    }

    @Test
    fun `should accept custom prompt values`() {
        val customPrompt = "The following audio contains technical programming terms and company names."
        val settings = AppSettings(customPrompt = customPrompt)
        
        assertEquals(customPrompt, settings.customPrompt, "Custom prompt should be set correctly")
    }

    @Test
    fun `should accept temperature values in valid range`() {
        val temperatures = listOf(0.0f, 0.5f, 1.0f, 1.5f, 2.0f)
        
        temperatures.forEach { temp ->
            val settings = AppSettings(temperature = temp)
            assertEquals(temp, settings.temperature, "Temperature $temp should be set correctly")
        }
    }

    @Test
    fun `should handle edge case temperature values`() {
        // Test boundary values
        val edgeCases = mapOf(
            0.0f to "minimum temperature",
            2.0f to "maximum temperature", 
            0.1f to "minimal non-zero temperature",
            1.9f to "near maximum temperature"
        )
        
        edgeCases.forEach { (temp, description) ->
            val settings = AppSettings(temperature = temp)
            assertEquals(temp, settings.temperature, "Should handle $description correctly")
        }
    }

    @Test
    fun `should handle long custom prompts`() {
        val shortPrompt = "Short prompt"
        val longPrompt = "A".repeat(800) // Near the 896 character limit
        val veryLongPrompt = "A".repeat(1000) // Over the limit
        
        listOf(shortPrompt, longPrompt, veryLongPrompt).forEach { prompt ->
            val settings = AppSettings(customPrompt = prompt)
            assertEquals(prompt, settings.customPrompt, "Should accept prompt of length ${prompt.length}")
        }
    }

    @Test
    fun `should handle empty and whitespace prompts`() {
        val prompts = listOf(
            "" to "empty string",
            "   " to "whitespace only",
            "\n\t " to "various whitespace characters"
        )
        
        prompts.forEach { (prompt, description) ->
            val settings = AppSettings(customPrompt = prompt)
            assertEquals(prompt, settings.customPrompt, "Should handle $description")
        }
    }

    @Test
    fun `should work with all parameters together`() {
        val settings = AppSettings(
            apiKey = TestConstants.MOCK_API_KEY,
            selectedModel = "whisper-1",
            customPrompt = "This audio contains medical terminology",
            temperature = 0.3f,
            language = "en",
            autoDetectLanguage = false
        )
        
        assertEquals(TestConstants.MOCK_API_KEY, settings.apiKey)
        assertEquals("whisper-1", settings.selectedModel)
        assertEquals("This audio contains medical terminology", settings.customPrompt)
        assertEquals(0.3f, settings.temperature)
        assertEquals("en", settings.language)
        assertEquals(false, settings.autoDetectLanguage)
    }

    @Test
    fun `should maintain backward compatibility with existing settings`() {
        // Create settings without new fields (simulating old data)
        val oldStyleSettings = AppSettings(
            apiKey = TestConstants.MOCK_API_KEY,
            selectedModel = "whisper-1",
            language = "fr"
        )
        
        // New fields should have defaults
        assertNull(oldStyleSettings.customPrompt)
        assertEquals(0.0f, oldStyleSettings.temperature)
        
        // Old fields should still work
        assertEquals(TestConstants.MOCK_API_KEY, oldStyleSettings.apiKey)
        assertEquals("whisper-1", oldStyleSettings.selectedModel)
        assertEquals("fr", oldStyleSettings.language)
    }

    @Test
    fun `should handle special characters in custom prompt`() {
        val specialPrompts = listOf(
            "Contains émoji 🎤 and accénts",
            "Has \"quotes\" and 'apostrophes'",
            "Includes newlines\nand tabs\there",
            "Mixed: 中文, العربية, русский",
            "Technical: <xml>tags</xml> & symbols @#$%"
        )
        
        specialPrompts.forEach { prompt ->
            val settings = AppSettings(customPrompt = prompt)
            assertEquals(prompt, settings.customPrompt, "Should handle special characters in prompt")
        }
    }
}