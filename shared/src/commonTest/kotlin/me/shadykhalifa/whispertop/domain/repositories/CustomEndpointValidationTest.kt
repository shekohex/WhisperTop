package me.shadykhalifa.whispertop.domain.repositories

import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.data.security.SecurePreferencesRepositoryImpl
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.*
import me.shadykhalifa.whispertop.utils.TestConstants

class CustomEndpointValidationTest {

    private class TestSecurePreferencesRepository : SecurePreferencesRepository {
        private var apiKey: String = ""
        private var apiEndpoint: String = "https://api.openai.com/v1/"

        override suspend fun saveApiKey(apiKey: String): Result<Unit> {
            val isOpenAI = isOpenAIEndpoint(apiEndpoint)
            if (!validateApiKey(apiKey, isOpenAI)) {
                val errorMsg = if (isOpenAI) {
                    "Invalid OpenAI API key format. Must start with 'sk-' and be at least 51 characters."
                } else {
                    "Invalid API key format."
                }
                return Result.Error(IllegalArgumentException(errorMsg))
            }
            this.apiKey = apiKey.trim()
            return Result.Success(Unit)
        }

        override suspend fun getApiKey(): Result<String?> = Result.Success(apiKey.takeIf { it.isNotBlank() })

        override suspend fun clearApiKey(): Result<Unit> {
            apiKey = ""
            return Result.Success(Unit)
        }

        override suspend fun hasApiKey(): Result<Boolean> = Result.Success(apiKey.isNotBlank())

        override suspend fun saveApiEndpoint(endpoint: String): Result<Unit> {
            apiEndpoint = endpoint.trim().let { if (it.endsWith("/")) it else "$it/" }
            return Result.Success(Unit)
        }

        override suspend fun getApiEndpoint(): Result<String> = Result.Success(apiEndpoint)

        override fun validateApiKey(apiKey: String, isOpenAIEndpoint: Boolean): Boolean {
            if (apiKey != apiKey.trim()) return false
            
            if (!isOpenAIEndpoint) {
                return apiKey.isBlank() || apiKey.length >= 3
            }
            
            // Accept our test keys or proper sk- format
            return when {
                apiKey == TestConstants.MOCK_API_KEY -> true
                apiKey == TestConstants.MOCK_OPENAI_API_KEY -> true
                apiKey.startsWith("MOCK-sk-") && apiKey.length >= 51 -> true
                apiKey.startsWith("TEST-sk-") && apiKey.length >= 51 -> true
                apiKey.startsWith("FAKE-sk-") && apiKey.length >= 51 -> true
                apiKey.startsWith("sk-") && apiKey.length >= 51 && apiKey.matches(Regex("^sk-[A-Za-z0-9\\-_]+$")) -> true
                else -> false
            }
        }

        private fun isOpenAIEndpoint(endpoint: String): Boolean {
            return endpoint.isBlank() ||
                   endpoint.equals("https://api.openai.com/v1", ignoreCase = true) ||
                   endpoint.contains("api.openai.com", ignoreCase = true) || 
                   endpoint.contains("openai.azure.com", ignoreCase = true) ||
                   endpoint.contains("oai.azure.com", ignoreCase = true)
        }

        private var wpm: Int = 60
        private var wpmOnboardingCompleted: Boolean = false

        override suspend fun saveWpm(wpm: Int): Result<Unit> {
            this.wpm = wpm
            return Result.Success(Unit)
        }

        override suspend fun getWpm(): Result<Int> {
            return Result.Success(wpm)
        }

        override suspend fun saveWpmOnboardingCompleted(completed: Boolean): Result<Unit> {
            this.wpmOnboardingCompleted = completed
            return Result.Success(Unit)
        }

        override suspend fun isWpmOnboardingCompleted(): Result<Boolean> {
            return Result.Success(wpmOnboardingCompleted)
        }

        override fun validateWpm(wpm: Int): Boolean {
            return wpm in 1..300
        }
    }

    @Test
    fun `OpenAI endpoint should require valid API key format`() = runTest {
        val repository = TestSecurePreferencesRepository()
        
        // Set OpenAI endpoint
        repository.saveApiEndpoint("https://api.openai.com/v1/")
        
        // Valid OpenAI key should work
        val validKey = TestConstants.MOCK_API_KEY // Already 60 chars, meets minimum requirement
        val result1 = repository.saveApiKey(validKey)
        assertTrue(result1 is Result.Success)
        
        // Invalid OpenAI key should fail
        val invalidKey = "invalid-key"
        val result2 = repository.saveApiKey(invalidKey)
        assertTrue(result2 is Result.Error)
        
        // Empty key should fail for OpenAI
        val result3 = repository.saveApiKey("")
        assertTrue(result3 is Result.Error)
    }

    @Test
    fun `Custom endpoint should allow flexible API key validation`() = runTest {
        val repository = TestSecurePreferencesRepository()
        
        // Set custom endpoint
        repository.saveApiEndpoint("https://my-custom-whisper.example.com/v1/")
        
        // Empty API key should be allowed for custom endpoints
        val result1 = repository.saveApiKey("")
        assertTrue(result1 is Result.Success)
        
        // Short custom key should be allowed
        val result2 = repository.saveApiKey("abc")
        assertTrue(result2 is Result.Success)
        
        // Any reasonable key should work
        val result3 = repository.saveApiKey("my-custom-token-12345")
        assertTrue(result3 is Result.Success)
        
        // Bearer token format should work
        val result4 = repository.saveApiKey("Bearer my-token")
        assertTrue(result4 is Result.Success)
        
        // Only very short keys should fail
        val result5 = repository.saveApiKey("ab")
        assertTrue(result5 is Result.Error)
    }

    @Test
    fun `Azure OpenAI endpoint should require OpenAI-style validation`() = runTest {
        val repository = TestSecurePreferencesRepository()
        
        // Set Azure OpenAI endpoint
        repository.saveApiEndpoint("https://my-resource.openai.azure.com/")
        
        // OpenAI-style key should work
        val validKey = TestConstants.MOCK_API_KEY // Already meets minimum requirement
        val result1 = repository.saveApiKey(validKey)
        assertTrue(result1 is Result.Success)
        
        // Non-OpenAI key should fail
        val result2 = repository.saveApiKey("azure-key-12345")
        assertTrue(result2 is Result.Error)
    }

    @Test
    fun `Groq endpoint should allow custom validation`() = runTest {
        val repository = TestSecurePreferencesRepository()
        
        // Set Groq endpoint
        repository.saveApiEndpoint("https://api.groq.com/openai/v1/")
        
        // Groq-style keys should work
        val result1 = repository.saveApiKey("gsk_1234567890abcdef")
        assertTrue(result1 is Result.Success)
        
        // Empty key should work (for testing)
        val result2 = repository.saveApiKey("")
        assertTrue(result2 is Result.Success)
    }

    @Test
    fun `Local endpoint should allow any key format`() = runTest {
        val repository = TestSecurePreferencesRepository()
        
        // Set local endpoint
        repository.saveApiEndpoint("http://localhost:8000/v1/")
        
        // No key needed for local
        val result1 = repository.saveApiKey("")
        assertTrue(result1 is Result.Success)
        
        // Any key should work
        val result2 = repository.saveApiKey("local-key")
        assertTrue(result2 is Result.Success)
        
        val result3 = repository.saveApiKey("test")
        assertTrue(result3 is Result.Success)
    }

    @Test
    fun `validateApiKey should work correctly for different endpoint types`() {
        val repository = TestSecurePreferencesRepository()
        
        // OpenAI endpoint validation
        assertTrue(repository.validateApiKey(TestConstants.MOCK_API_KEY, isOpenAIEndpoint = true))
        assertFalse(repository.validateApiKey("invalid", isOpenAIEndpoint = true))
        assertFalse(repository.validateApiKey("", isOpenAIEndpoint = true))
        
        // Custom endpoint validation
        assertTrue(repository.validateApiKey("", isOpenAIEndpoint = false))
        assertTrue(repository.validateApiKey("abc", isOpenAIEndpoint = false))
        assertTrue(repository.validateApiKey("custom-token", isOpenAIEndpoint = false))
        assertFalse(repository.validateApiKey("ab", isOpenAIEndpoint = false)) // Too short
        
        // Leading/trailing whitespace should fail for both
        assertFalse(repository.validateApiKey(" sk-" + "a".repeat(48), isOpenAIEndpoint = true))
        assertFalse(repository.validateApiKey(" custom-key ", isOpenAIEndpoint = false))
    }

    @Test
    fun `endpoint switching should update validation behavior`() = runTest {
        val repository = TestSecurePreferencesRepository()
        
        // Start with OpenAI endpoint
        repository.saveApiEndpoint("https://api.openai.com/v1/")
        
        // OpenAI key should work
        val openaiKey = TestConstants.MOCK_API_KEY
        val result1 = repository.saveApiKey(openaiKey)
        assertTrue(result1 is Result.Success)
        
        // Switch to custom endpoint
        repository.saveApiEndpoint("https://custom-ai.example.com/v1/")
        
        // Now simple key should work
        val result2 = repository.saveApiKey("simple-key")
        assertTrue(result2 is Result.Success)
        
        // Switch back to OpenAI
        repository.saveApiEndpoint("https://api.openai.com/v1/")
        
        // Simple key should now fail
        val result3 = repository.saveApiKey("simple-key")
        assertTrue(result3 is Result.Error)
    }
}