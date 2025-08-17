package me.shadykhalifa.whispertop.domain.models

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsEndpointDetectionTest {

    @Test
    fun `should detect official OpenAI API endpoints`() {
        val openAIEndpoints = listOf(
            "https://api.openai.com",
            "https://api.openai.com/",
            "https://api.openai.com/v1",
            "https://api.openai.com/v1/"
        )
        
        openAIEndpoints.forEach { endpoint ->
            val settings = AppSettings(baseUrl = endpoint)
            assertTrue(
                settings.isOpenAIEndpoint(),
                "Should detect $endpoint as OpenAI endpoint"
            )
        }
    }

    @Test
    fun `should detect Azure OpenAI endpoints`() {
        val azureEndpoints = listOf(
            "https://openai.azure.com",
            "https://openai.azure.com/",
            "https://my-resource.openai.azure.com",
            "https://my-resource.openai.azure.com/",
            "https://oai.azure.com",
            "https://oai.azure.com/",
            "https://my-resource.oai.azure.com",
            "https://my-resource.oai.azure.com/"
        )
        
        azureEndpoints.forEach { endpoint ->
            val settings = AppSettings(baseUrl = endpoint)
            assertTrue(
                settings.isOpenAIEndpoint(),
                "Should detect $endpoint as Azure OpenAI endpoint"
            )
        }
    }

    @Test
    fun `should not detect custom endpoints as OpenAI`() {
        val customEndpoints = listOf(
            "https://api.groq.com",
            "https://api.groq.com/",
            "https://api.groq.com/openai/v1",
            "https://localhost:8080",
            "https://localhost:8080/v1",
            "https://my-server.com/api/v1",
            "https://huggingface.co/api",
            "https://inference.example.com",
            "https://custom-whisper.herokuapp.com",
            "https://whisper.mycompany.com",
            "http://192.168.1.100:5000",
            "https://api.anthropic.com",
            "https://api.together.xyz",
            "https://api.replicate.com"
        )
        
        customEndpoints.forEach { endpoint ->
            val settings = AppSettings(baseUrl = endpoint)
            assertFalse(
                settings.isOpenAIEndpoint(),
                "Should not detect $endpoint as OpenAI endpoint"
            )
        }
    }

    @Test
    fun `should handle case sensitivity in endpoint detection`() {
        val mixedCaseEndpoints = listOf(
            "https://API.OPENAI.COM",
            "https://Api.OpenAI.Com",
            "https://OPENAI.AZURE.COM",
            "https://OpenAI.Azure.Com",
            "https://OAI.AZURE.COM",
            "https://Oai.Azure.Com"
        )
        
        mixedCaseEndpoints.forEach { endpoint ->
            val settings = AppSettings(baseUrl = endpoint)
            assertTrue(
                settings.isOpenAIEndpoint(),
                "Should detect $endpoint as OpenAI endpoint (case insensitive)"
            )
        }
    }

    @Test
    fun `should handle empty or null endpoint`() {
        val emptySettings = AppSettings(baseUrl = "")
        assertTrue(
            emptySettings.isOpenAIEndpoint(),
            "Empty endpoint should default to OpenAI"
        )
        
        val nullSettings = AppSettings()
        assertTrue(
            nullSettings.isOpenAIEndpoint(),
            "Default endpoint should be OpenAI"
        )
    }

    @Test
    fun `should handle malformed URLs gracefully`() {
        val malformedUrls = listOf(
            "not-a-url",
            "ftp://api.openai.com",
            "mailto:test@openai.com",
            "api.openai.com", // Missing protocol
            "://api.openai.com", // Missing scheme
            "https://", // Empty host
            " https://api.openai.com ", // Whitespace
            "https://api.openai.com\n" // Newline
        )
        
        malformedUrls.forEach { url ->
            val settings = AppSettings(baseUrl = url)
            // Should not throw exception, should have deterministic behavior
            val result = settings.isOpenAIEndpoint()
            // For malformed URLs, we expect false unless it contains openai/azure domains
            if (url.contains("api.openai.com") || url.contains("openai.azure.com") || url.contains("oai.azure.com")) {
                assertTrue(result, "Should still detect OpenAI in malformed URL: $url")
            } else {
                assertFalse(result, "Should not detect OpenAI in malformed URL: $url")
            }
        }
    }

    @Test
    fun `should handle subdomains and paths correctly`() {
        val validOpenAIPaths = listOf(
            "https://api.openai.com/v1/audio/transcriptions",
            "https://api.openai.com/v1/completions",
            "https://my-resource.openai.azure.com/openai/deployments/whisper/audio/transcriptions"
        )
        
        validOpenAIPaths.forEach { endpoint ->
            val settings = AppSettings(baseUrl = endpoint)
            assertTrue(
                settings.isOpenAIEndpoint(),
                "Should detect $endpoint as OpenAI endpoint despite path"
            )
        }
        
        val invalidPaths = listOf(
            "https://api.not-openai.com/v1/audio/transcriptions",
            "https://openai.fake-azure.com/v1/completions",
            "https://groq.com/openai/v1/audio/transcriptions" // Contains "openai" but wrong domain
        )
        
        invalidPaths.forEach { endpoint ->
            val settings = AppSettings(baseUrl = endpoint)
            assertFalse(
                settings.isOpenAIEndpoint(),
                "Should not detect $endpoint as OpenAI endpoint"
            )
        }
    }
}