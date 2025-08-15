package me.shadykhalifa.whispertop.domain.repositories

import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurePreferencesRepositoryTest {

    private val mockRepository = MockSecurePreferencesRepository()

    @Test
    fun `validateApiKey should accept valid OpenAI API keys`() {
        val validKeys = listOf(
            "sk-1234567890abcdef1234567890abcdef1234567890abcdef123",
            "sk-proj-1234567890abcdef1234567890abcdef1234567890abcdef123",
            "sk-1234567890ABCDEF1234567890abcdef1234567890abcdef123",
            "sk-1234567890abcdef1234567890abcdef1234567890abcdef-123"
        )

        validKeys.forEach { key ->
            assertTrue(
                mockRepository.validateApiKey(key),
                "Expected '$key' to be valid"
            )
        }
    }

    @Test
    fun `validateApiKey should reject invalid API keys`() {
        val invalidKeys = listOf(
            "",
            "invalid-key",
            "sk-",
            "sk-too-short",
            "pk-1234567890abcdef1234567890abcdef1234567890abcdef123", // wrong prefix
            "sk-1234567890abcdef1234567890abcdef1234567890abcdef123 ", // trailing space
            " sk-1234567890abcdef1234567890abcdef1234567890abcdef123", // leading space
            "sk-1234567890abcdef1234567890abcdef1234567890abcdef123@#$" // invalid chars
        )

        invalidKeys.forEach { key ->
            assertFalse(
                mockRepository.validateApiKey(key),
                "Expected '$key' to be invalid"
            )
        }
    }

    @Test
    fun `saveApiKey should reject invalid keys`() = runTest {
        val result = mockRepository.saveApiKey("invalid-key")
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is IllegalArgumentException)
    }

    @Test
    fun `saveApiKey should accept valid keys`() = runTest {
        val validKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef123"
        val result = mockRepository.saveApiKey(validKey)
        
        assertTrue(result is Result.Success)
    }

    @Test
    fun `API endpoint should be normalized with trailing slash`() = runTest {
        val endpointWithoutSlash = "https://api.example.com"
        
        mockRepository.saveApiEndpoint(endpointWithoutSlash)
        val result = mockRepository.getApiEndpoint()
        
        assertTrue(result is Result.Success)
        assertTrue(result.data.endsWith("/"))
    }

    @Test
    fun `should return default endpoint when none saved`() = runTest {
        val result = mockRepository.getApiEndpoint()
        
        assertTrue(result is Result.Success)
        assertTrue(result.data == "https://api.openai.com/v1/")
    }
}

// Mock implementation for testing business logic
private class MockSecurePreferencesRepository : SecurePreferencesRepository {
    private var storedApiKey: String? = null
    private var storedEndpoint: String? = null
    
    private companion object {
        const val DEFAULT_API_ENDPOINT = "https://api.openai.com/v1/"
        const val API_KEY_PREFIX = "sk-"
        const val API_KEY_MIN_LENGTH = 51
    }

    override suspend fun saveApiKey(apiKey: String): Result<Unit> {
        return if (!validateApiKey(apiKey)) {
            Result.Error(IllegalArgumentException("Invalid API key format"))
        } else {
            storedApiKey = apiKey.trim()
            Result.Success(Unit)
        }
    }

    override suspend fun getApiKey(): Result<String?> {
        return Result.Success(storedApiKey)
    }

    override suspend fun clearApiKey(): Result<Unit> {
        storedApiKey = null
        return Result.Success(Unit)
    }

    override suspend fun hasApiKey(): Result<Boolean> {
        return Result.Success(!storedApiKey.isNullOrBlank())
    }

    override suspend fun saveApiEndpoint(endpoint: String): Result<Unit> {
        val cleanEndpoint = endpoint.trim().let { 
            if (it.endsWith("/")) it else "$it/"
        }
        storedEndpoint = cleanEndpoint
        return Result.Success(Unit)
    }

    override suspend fun getApiEndpoint(): Result<String> {
        return Result.Success(storedEndpoint ?: DEFAULT_API_ENDPOINT)
    }

    override fun validateApiKey(apiKey: String): Boolean {
        // Don't allow leading/trailing whitespace
        if (apiKey != apiKey.trim()) {
            return false
        }
        
        return apiKey.startsWith(API_KEY_PREFIX) && 
               apiKey.length >= API_KEY_MIN_LENGTH &&
               apiKey.matches(Regex("^sk-[A-Za-z0-9\\-_]+$"))
    }
}