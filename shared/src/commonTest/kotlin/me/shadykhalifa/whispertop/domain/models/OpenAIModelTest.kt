package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenAIModelTest {

    @Test
    fun `predefined models should contain expected models`() {
        val models = OpenAIModel.PREDEFINED_MODELS
        
        assertTrue(models.isNotEmpty())
        assertTrue(models.any { it.modelId == "whisper-1" })
        assertTrue(models.any { it.modelId == "whisper-3-turbo" })
        assertTrue(models.any { it.modelId == "gpt-4o-audio-preview" })
    }

    @Test
    fun `getModelById should return correct model`() {
        val whisperModel = OpenAIModel.getModelById("whisper-1")
        
        assertNotNull(whisperModel)
        assertEquals("whisper-1", whisperModel.modelId)
        assertEquals("Whisper v1", whisperModel.displayName)
    }

    @Test
    fun `getModelById should return null for non-existent model`() {
        val model = OpenAIModel.getModelById("non-existent-model")
        assertEquals(null, model)
    }

    @Test
    fun `getDefaultModel should return first predefined model`() {
        val defaultModel = OpenAIModel.getDefaultModel()
        
        assertNotNull(defaultModel)
        assertEquals(OpenAIModel.PREDEFINED_MODELS.first().modelId, defaultModel.modelId)
    }

    @Test
    fun `model capability should have valid ratings`() {
        OpenAIModel.PREDEFINED_MODELS.forEach { model ->
            assertTrue(model.capabilities.speedRating in 1..5)
            assertTrue(model.capabilities.accuracyRating in 1..5)
        }
    }

    @Test
    fun `pricing info should format correctly`() {
        val pricing = PricingInfo(pricePerMinute = 0.006)
        assertEquals("$0.006/USD per minute", pricing.formatPrice())
    }

    @Test
    fun `model serialization should work correctly`() {
        val model = OpenAIModel.PREDEFINED_MODELS.first()
        val json = Json.encodeToString(model)
        val deserializedModel = Json.decodeFromString<OpenAIModel>(json)
        
        assertEquals(model.modelId, deserializedModel.modelId)
        assertEquals(model.displayName, deserializedModel.displayName)
        assertEquals(model.description, deserializedModel.description)
    }

    @Test
    fun `model use case should have proper recommendations`() {
        ModelUseCase.entries.forEach { useCase ->
            assertTrue(useCase.recommendedFor.isNotEmpty())
            assertTrue(useCase.displayName.isNotBlank())
            assertTrue(useCase.description.isNotBlank())
        }
    }

    @Test
    fun `custom model should be marked correctly`() {
        val customModel = OpenAIModel(
            modelId = "custom-model",
            displayName = "Custom Model",
            description = "A custom model",
            capabilities = ModelCapability.BALANCED,
            pricing = PricingInfo(pricePerMinute = 0.0),
            useCase = ModelUseCase.GENERAL_PURPOSE,
            isCustom = true
        )
        
        assertTrue(customModel.isCustom)
    }
}