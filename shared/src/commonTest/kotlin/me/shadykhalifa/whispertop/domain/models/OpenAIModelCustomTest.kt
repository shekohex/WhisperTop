package me.shadykhalifa.whispertop.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenAIModelCustomTest {

    @Test
    fun `should create custom model with correct properties`() {
        // Given
        val customModel = OpenAIModel(
            modelId = "claude-3-5-sonnet",
            displayName = "Claude 3.5 Sonnet",
            description = "Custom Anthropic model for transcription",
            capabilities = ModelCapability.BALANCED,
            pricing = PricingInfo(pricePerMinute = 0.0),
            useCase = ModelUseCase.GENERAL_PURPOSE,
            isCustom = true
        )

        // Then
        assertEquals("claude-3-5-sonnet", customModel.modelId)
        assertEquals("Claude 3.5 Sonnet", customModel.displayName)
        assertTrue(customModel.isCustom)
        assertEquals(0.0, customModel.pricing.pricePerMinute)
    }

    @Test
    fun `should not find custom models in predefined list`() {
        // Given
        val customModelIds = listOf(
            "claude-3-5-sonnet",
            "whisper-large-v3",
            "gpt-4-turbo",
            "local-whisper-model"
        )

        // When/Then
        customModelIds.forEach { modelId ->
            assertNull(
                OpenAIModel.getModelById(modelId),
                "Custom model $modelId should not be in predefined list"
            )
        }
    }

    @Test
    fun `should find predefined models correctly`() {
        // Given
        val predefinedModelIds = listOf(
            "whisper-1",
            "whisper-3-turbo",
            "gpt-4o-audio-preview"
        )

        // When/Then
        predefinedModelIds.forEach { modelId ->
            assertNotNull(
                OpenAIModel.getModelById(modelId),
                "Predefined model $modelId should be found"
            )
        }
    }

    @Test
    fun `should have correct predefined model properties`() {
        // Given
        val whisperModel = OpenAIModel.getModelById("whisper-1")

        // Then
        assertNotNull(whisperModel)
        assertEquals("whisper-1", whisperModel!!.modelId)
        assertEquals("Whisper v1", whisperModel.displayName)
        assertFalse(whisperModel.isCustom)
        assertEquals(ModelCapability.BALANCED, whisperModel.capabilities)
        assertEquals(0.006, whisperModel.pricing.pricePerMinute)
    }

    @Test
    fun `should create custom models with default capabilities`() {
        // Given
        val customModelIds = listOf(
            "whisper-large-v3",
            "claude-3-5-sonnet",
            "gpt-4-turbo",
            "local-deployment"
        )

        // When
        customModelIds.forEach { modelId ->
            val customModel = OpenAIModel(
                modelId = modelId,
                displayName = modelId,
                description = "Custom model",
                capabilities = ModelCapability.BALANCED,
                pricing = PricingInfo(pricePerMinute = 0.0),
                useCase = ModelUseCase.GENERAL_PURPOSE,
                isCustom = true
            )

            // Then
            assertTrue(customModel.isCustom)
            assertEquals(ModelCapability.BALANCED, customModel.capabilities)
            assertEquals(0.0, customModel.pricing.pricePerMinute)
        }
    }

    @Test
    fun `should format pricing correctly for custom models`() {
        // Given
        val freeCustomModel = OpenAIModel(
            modelId = "local-model",
            displayName = "Local Model",
            description = "Free local deployment",
            capabilities = ModelCapability.BALANCED,
            pricing = PricingInfo(pricePerMinute = 0.0),
            useCase = ModelUseCase.GENERAL_PURPOSE,
            isCustom = true
        )

        val paidCustomModel = OpenAIModel(
            modelId = "premium-model",
            displayName = "Premium Model",
            description = "Paid custom deployment",
            capabilities = ModelCapability.ACCURACY_OPTIMIZED,
            pricing = PricingInfo(pricePerMinute = 0.015),
            useCase = ModelUseCase.HIGH_ACCURACY,
            isCustom = true
        )

        // Then
        assertEquals("$0.000/USD per minute", freeCustomModel.pricing.formatPrice())
        assertEquals("$0.015/USD per minute", paidCustomModel.pricing.formatPrice())
    }

    @Test
    fun `should handle special characters in custom model IDs`() {
        // Given
        val specialModelIds = listOf(
            "whisper-v1.0",
            "my-model_v2",
            "company/whisper-large",
            "model-with-dashes",
            "model_with_underscores"
        )

        // When/Then
        specialModelIds.forEach { modelId ->
            val customModel = OpenAIModel(
                modelId = modelId,
                displayName = modelId,
                description = "Model with special characters",
                capabilities = ModelCapability.BALANCED,
                pricing = PricingInfo(pricePerMinute = 0.0),
                useCase = ModelUseCase.GENERAL_PURPOSE,
                isCustom = true
            )

            assertEquals(modelId, customModel.modelId)
            assertTrue(customModel.isCustom)
        }
    }

    @Test
    fun `should distinguish between custom and predefined models`() {
        // Given
        val predefinedModel = OpenAIModel.getModelById("whisper-1")!!
        val customModel = OpenAIModel(
            modelId = "custom-whisper",
            displayName = "Custom Whisper",
            description = "Custom deployment",
            capabilities = ModelCapability.BALANCED,
            pricing = PricingInfo(pricePerMinute = 0.0),
            useCase = ModelUseCase.GENERAL_PURPOSE,
            isCustom = true
        )

        // Then
        assertFalse(predefinedModel.isCustom)
        assertTrue(customModel.isCustom)
        assertTrue(predefinedModel.pricing.pricePerMinute > 0.0)
        assertEquals(0.0, customModel.pricing.pricePerMinute)
    }
}