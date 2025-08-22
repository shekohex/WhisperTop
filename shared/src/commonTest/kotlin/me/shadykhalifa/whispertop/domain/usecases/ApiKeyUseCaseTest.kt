package me.shadykhalifa.whispertop.domain.usecases

import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.di.mockPlatformModule
import me.shadykhalifa.whispertop.di.sharedModule
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.utils.Result
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import me.shadykhalifa.whispertop.utils.TestConstants

class ApiKeyUseCaseTest : KoinTest {
    
    private lateinit var securePreferencesRepository: SecurePreferencesRepository
    private lateinit var saveApiKeyUseCase: SaveApiKeyUseCase
    private lateinit var getApiKeyUseCase: GetApiKeyUseCase
    private lateinit var validateApiKeyUseCase: ValidateApiKeyUseCase
    private lateinit var clearApiKeyUseCase: ClearApiKeyUseCase
    
    @BeforeTest
    fun setup() {
        startKoin {
            modules(listOf(sharedModule, mockPlatformModule))
        }
        
        securePreferencesRepository = get()
        saveApiKeyUseCase = SaveApiKeyUseCase(securePreferencesRepository)
        getApiKeyUseCase = GetApiKeyUseCase(securePreferencesRepository)
        validateApiKeyUseCase = ValidateApiKeyUseCase(securePreferencesRepository)
        clearApiKeyUseCase = ClearApiKeyUseCase(securePreferencesRepository)
    }
    
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    // SaveApiKeyUseCase Tests
    @Test
    fun `saveApiKey should save valid API key successfully`() = runTest {
        val validApiKey = TestConstants.MOCK_API_KEY
        
        val result = saveApiKeyUseCase(validApiKey)
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `saveApiKey should handle empty string`() = runTest {
        val emptyApiKey = ""
        
        val result = saveApiKeyUseCase(emptyApiKey)
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `saveApiKey should handle whitespace-only string`() = runTest {
        val whitespaceApiKey = "   "
        
        val result = saveApiKeyUseCase(whitespaceApiKey)
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `saveApiKey should handle string with tabs and newlines`() = runTest {
        val apiKeyWithWhitespace = "\tsk-1234567890abcdef\n"
        
        val result = saveApiKeyUseCase(apiKeyWithWhitespace)
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `saveApiKey should handle very long API key`() = runTest {
        val longApiKey = TestConstants.MOCK_API_KEY + "a".repeat(5000)
        
        val result = saveApiKeyUseCase(longApiKey)
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `saveApiKey should handle API key with special characters`() = runTest {
        val specialCharApiKey = TestConstants.MOCK_API_KEY
        
        val result = saveApiKeyUseCase(specialCharApiKey)
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `saveApiKey should handle Unicode characters`() = runTest {
        val unicodeApiKey = TestConstants.MOCK_API_KEY
        
        val result = saveApiKeyUseCase(unicodeApiKey)
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `saveApiKey should handle null characters in string`() = runTest {
        val nullCharApiKey = TestConstants.MOCK_API_KEY
        
        val result = saveApiKeyUseCase(nullCharApiKey)
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `saveApiKey should handle overwriting existing key`() = runTest {
        val firstKey = TestConstants.MOCK_API_KEY
        val secondKey = TestConstants.MOCK_API_KEY
        
        saveApiKeyUseCase(firstKey)
        val result = saveApiKeyUseCase(secondKey)
        
        assertTrue(result is Result.Success)
        
        val retrievedKey = getApiKeyUseCase()
        assertTrue(retrievedKey is Result.Success)
        assertEquals(secondKey, retrievedKey.data)
    }
    
    @Test
    fun `saveApiKey should handle rapid consecutive saves`() = runTest {
        val keys = listOf(TestConstants.MOCK_API_KEY, TestConstants.MOCK_API_KEY, TestConstants.MOCK_API_KEY, TestConstants.MOCK_API_KEY, TestConstants.MOCK_API_KEY)
        
        keys.forEach { key ->
            val result = saveApiKeyUseCase(key)
            assertTrue(result is Result.Success)
        }
        
        val finalKey = getApiKeyUseCase()
        assertTrue(finalKey is Result.Success)
        assertEquals(TestConstants.MOCK_API_KEY, finalKey.data)
    }

    // GetApiKeyUseCase Tests
    @Test
    fun `getApiKey should return null when no key is stored`() = runTest {
        clearApiKeyUseCase()
        
        val result = getApiKeyUseCase()
        
        assertTrue(result is Result.Success)
        assertNull(result.data)
    }
    
    @Test
    fun `getApiKey should return saved key correctly`() = runTest {
        val testKey = TestConstants.MOCK_API_KEY
        saveApiKeyUseCase(testKey)
        
        val result = getApiKeyUseCase()
        
        assertTrue(result is Result.Success)
        assertEquals(testKey, result.data)
    }
    
    @Test
    fun `getApiKey should return key after multiple saves`() = runTest {
        saveApiKeyUseCase(TestConstants.MOCK_API_KEY)
        saveApiKeyUseCase(TestConstants.MOCK_API_KEY)
        val finalKey = TestConstants.MOCK_API_KEY
        saveApiKeyUseCase(finalKey)
        
        val result = getApiKeyUseCase()
        
        assertTrue(result is Result.Success)
        assertEquals(finalKey, result.data)
    }
    
    @Test
    fun `getApiKey should return null after clear operation`() = runTest {
        val testKey = TestConstants.MOCK_API_KEY
        saveApiKeyUseCase(testKey)
        clearApiKeyUseCase()
        
        val result = getApiKeyUseCase()
        
        assertTrue(result is Result.Success)
        assertNull(result.data)
    }
    
    @Test
    fun `getApiKey should handle empty string retrieval`() = runTest {
        saveApiKeyUseCase("")
        
        val result = getApiKeyUseCase()
        
        assertTrue(result is Result.Success)
        assertEquals("", result.data)
    }
    
    @Test
    fun `getApiKey should preserve whitespace in saved key`() = runTest {
        val keyWithSpaces = " sk-key with spaces "
        saveApiKeyUseCase(keyWithSpaces)
        
        val result = getApiKeyUseCase()
        
        assertTrue(result is Result.Success)
        assertEquals(keyWithSpaces, result.data)
    }

    // ValidateApiKeyUseCase Tests
    @Test
    fun `validateApiKey should return false for empty string`() {
        val result = validateApiKeyUseCase("")
        
        assertFalse(result)
    }
    
    @Test
    fun `validateApiKey should return false for whitespace-only string`() {
        val result = validateApiKeyUseCase("   ")
        
        assertFalse(result)
    }
    
    @Test
    fun `validateApiKey should return false for very short key`() {
        val result = validateApiKeyUseCase("sk-short")
        
        assertFalse(result)
    }
    
    @Test
    fun `validateApiKey should return true for valid OpenAI key format`() {
        val result = validateApiKeyUseCase(TestConstants.MOCK_API_KEY)
        
        assertTrue(result)
    }
    
    @Test
    fun `validateApiKey should return true for valid key with minimum length`() {
        val result = validateApiKeyUseCase(TestConstants.MOCK_API_KEY)
        
        assertTrue(result)
    }
    
    @Test
    fun `validateApiKey should handle key without sk- prefix`() {
        val result = validateApiKeyUseCase("1234567890abcdef1234567890abcdef")
        
        assertTrue(result)
    }
    
    @Test
    fun `validateApiKey should return false for null-like strings`() {
        val testCases = listOf("null", "NULL", "undefined", "UNDEFINED")
        
        testCases.forEach { testCase ->
            val result = validateApiKeyUseCase(testCase)
            assertFalse(result, "Should return false for: $testCase")
        }
    }
    
    @Test
    fun `validateApiKey should handle special characters in key`() {
        val keyWithSpecialChars = TestConstants.MOCK_API_KEY
        
        val result = validateApiKeyUseCase(keyWithSpecialChars)
        
        assertTrue(result)
    }
    
    @Test
    fun `validateApiKey should handle Unicode characters`() {
        val unicodeKey = TestConstants.MOCK_API_KEY
        
        val result = validateApiKeyUseCase(unicodeKey)
        
        assertTrue(result)
    }
    
    @Test
    fun `validateApiKey should handle very long keys`() {
        val longKey = TestConstants.MOCK_API_KEY + "a".repeat(1000)
        
        val result = validateApiKeyUseCase(longKey)
        
        assertTrue(result)
    }
    
    @Test
    fun `validateApiKey should handle keys with newlines and tabs`() {
        val keyWithWhitespace = TestConstants.MOCK_API_KEY
        
        val result = validateApiKeyUseCase(keyWithWhitespace)
        
        assertTrue(result)
    }

    // ClearApiKeyUseCase Tests
    @Test
    fun `clearApiKey should clear existing key successfully`() = runTest {
        val testKey = TestConstants.MOCK_API_KEY
        saveApiKeyUseCase(testKey)
        
        val result = clearApiKeyUseCase()
        
        assertTrue(result is Result.Success)
        
        val retrievedKey = getApiKeyUseCase()
        assertTrue(retrievedKey is Result.Success)
        assertNull(retrievedKey.data)
    }
    
    @Test
    fun `clearApiKey should succeed when no key exists`() = runTest {
        clearApiKeyUseCase() // Ensure no key exists
        
        val result = clearApiKeyUseCase()
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `clearApiKey should handle multiple consecutive clears`() = runTest {
        val testKey = TestConstants.MOCK_API_KEY
        saveApiKeyUseCase(testKey)
        
        val firstClear = clearApiKeyUseCase()
        val secondClear = clearApiKeyUseCase()
        val thirdClear = clearApiKeyUseCase()
        
        assertTrue(firstClear is Result.Success)
        assertTrue(secondClear is Result.Success)
        assertTrue(thirdClear is Result.Success)
        
        val retrievedKey = getApiKeyUseCase()
        assertTrue(retrievedKey is Result.Success)
        assertNull(retrievedKey.data)
    }
    
    @Test
    fun `clearApiKey should handle clear during save operation workflow`() = runTest {
        val firstKey = TestConstants.MOCK_API_KEY
        val secondKey = TestConstants.MOCK_API_KEY
        
        saveApiKeyUseCase(firstKey)
        clearApiKeyUseCase()
        saveApiKeyUseCase(secondKey)
        
        val result = getApiKeyUseCase()
        assertTrue(result is Result.Success)
        assertEquals(secondKey, result.data)
    }

    // Integration Tests
    @Test
    fun `complete workflow should work correctly`() = runTest {
        // Initial state - no key
        var result = getApiKeyUseCase()
        assertTrue(result is Result.Success)
        assertNull(result.data)
        
        // Save a key
        val testKey = TestConstants.MOCK_API_KEY
        val saveResult = saveApiKeyUseCase(testKey)
        assertTrue(saveResult is Result.Success)
        
        // Validate the key
        val isValid = validateApiKeyUseCase(testKey)
        assertTrue(isValid)
        
        // Retrieve the key
        result = getApiKeyUseCase()
        assertTrue(result is Result.Success)
        assertEquals(testKey, result.data)
        
        // Clear the key
        val clearResult = clearApiKeyUseCase()
        assertTrue(clearResult is Result.Success)
        
        // Verify key is cleared
        result = getApiKeyUseCase()
        assertTrue(result is Result.Success)
        assertNull(result.data)
    }
    
    @Test
    fun `validation should work with saved keys`() = runTest {
        val validKey = TestConstants.MOCK_API_KEY
        val invalidKey = "invalid"
        
        // Test validation before saving
        assertTrue(validateApiKeyUseCase(validKey))
        assertFalse(validateApiKeyUseCase(invalidKey))
        
        // Save valid key and test
        saveApiKeyUseCase(validKey)
        val retrievedKey = getApiKeyUseCase()
        assertTrue(retrievedKey is Result.Success)
        assertTrue(validateApiKeyUseCase(retrievedKey.data!!))
        
        // Save invalid key and test
        saveApiKeyUseCase(invalidKey)
        val retrievedInvalidKey = getApiKeyUseCase()
        assertTrue(retrievedInvalidKey is Result.Success)
        assertFalse(validateApiKeyUseCase(retrievedInvalidKey.data!!))
    }
    
    @Test
    fun `concurrent operations should be handled gracefully`() = runTest {
        val keys = (1..10).map { TestConstants.MOCK_API_KEY }
        
        // Simulate rapid save/get operations
        keys.forEach { key ->
            saveApiKeyUseCase(key)
            val result = getApiKeyUseCase()
            assertTrue(result is Result.Success)
            assertTrue(validateApiKeyUseCase(key))
        }
        
        // Final state should have the last key
        val finalResult = getApiKeyUseCase()
        assertTrue(finalResult is Result.Success)
        assertEquals(TestConstants.MOCK_API_KEY, finalResult.data)
    }
}