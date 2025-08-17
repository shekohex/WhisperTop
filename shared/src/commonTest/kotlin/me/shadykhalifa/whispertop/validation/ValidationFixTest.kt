package me.shadykhalifa.whispertop.validation

import me.shadykhalifa.whispertop.domain.models.AppSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationFixTest {
    
    @Test
    fun `endpoint detection should correctly identify OpenAI vs custom endpoints`() {
        // Test cases for endpoint detection
        val testCases = mapOf(
            "https://api.openai.com/v1/" to true,
            "https://api.groq.com/openai/v1/" to false,  // This should be false now
            "https://azure.openai.com/v1/" to false,     // This should be false now  
            "https://api.openai.com/v1/chat" to true,
            "https://openai.azure.com/v1/" to true,
            "https://custom.example.com/v1/" to false,
            "" to true
        )
        
        testCases.forEach { (endpoint, expected) ->
            val settings = AppSettings(baseUrl = endpoint)
            val result = settings.isOpenAIEndpoint()
            if (expected) {
                assertTrue(result, "Endpoint '$endpoint' should be detected as OpenAI")
            } else {
                assertFalse(result, "Endpoint '$endpoint' should NOT be detected as OpenAI")
            }
        }
    }
    
    @Test 
    fun `groq endpoint should be treated as custom endpoint`() {
        val groqSettings = AppSettings(baseUrl = "https://api.groq.com/openai/v1/")
        assertFalse(groqSettings.isOpenAIEndpoint(), "Groq endpoint should be treated as custom")
    }
    
    @Test
    fun `azure openai with different subdomain should be treated as custom`() {
        val azureSettings = AppSettings(baseUrl = "https://my-resource.azure.openai.com/v1/")
        assertFalse(azureSettings.isOpenAIEndpoint(), "Azure OpenAI with subdomain should be treated as custom")
    }
    
    @Test
    fun `official azure openai endpoints should be detected correctly`() {
        val azureSettings1 = AppSettings(baseUrl = "https://openai.azure.com/v1/")
        assertTrue(azureSettings1.isOpenAIEndpoint(), "Official Azure OpenAI endpoint should be detected")
        
        val azureSettings2 = AppSettings(baseUrl = "https://oai.azure.com/v1/")
        assertTrue(azureSettings2.isOpenAIEndpoint(), "Official OAI Azure endpoint should be detected")
    }
}