package me.shadykhalifa.whispertop.presentation.viewmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTranscriptionCustomizationTest {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var mockSettingsRepository: MockSettingsRepository
    private lateinit var mockSecurePreferencesRepository: MockSecurePreferencesRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSettingsRepository = MockSettingsRepository()
        mockSecurePreferencesRepository = MockSecurePreferencesRepository()
        viewModel = SettingsViewModel(mockSettingsRepository, mockSecurePreferencesRepository)
    }
    
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should initialize with default transcription customization values`() = runTest(testDispatcher) {
        val initialState = viewModel.uiState.value
        
        assertEquals("", initialState.customPrompt, "Should initialize with empty custom prompt")
        assertEquals(0.0f, initialState.temperature, "Should initialize with default temperature")
        assertNull(initialState.validationErrors["customPrompt"], "Should not have prompt validation errors initially")
        assertNull(initialState.validationErrors["temperature"], "Should not have temperature validation errors initially")
        assertFalse(initialState.savingCustomPrompt, "Should not be saving custom prompt initially")
        assertFalse(initialState.savingTemperature, "Should not be saving temperature initially")
    }

    @Test
    fun `should validate custom prompt length correctly`() = runTest(testDispatcher) {
        // Test valid prompts
        val validPrompts = listOf(
            "",
            "Short prompt",
            "A".repeat(100),
            "A".repeat(896) // Exactly at limit
        )

        validPrompts.forEach { prompt ->
            viewModel.updateCustomPrompt(prompt)
            val errors = viewModel.uiState.value.validationErrors
            assertNull(errors["customPrompt"], "Should not have validation error for prompt length ${prompt.length}")
        }
    }

    @Test
    fun `should validate custom prompt length over limit`() = runTest(testDispatcher) {
        val overLimitPrompt = "A".repeat(897) // Over 896 character limit
        
        viewModel.updateCustomPrompt(overLimitPrompt)
        advanceUntilIdle() // Wait for async validation to complete
        
        val errors = viewModel.uiState.value.validationErrors
        assertTrue(
            errors["customPrompt"]?.contains("too long") == true,
            "Should have validation error for over-limit prompt"
        )
    }

    @Test
    fun `should validate temperature range correctly`() = runTest(testDispatcher) {
        // Test valid temperatures
        val validTemperatures = listOf(0.0f, 0.5f, 1.0f, 1.5f, 2.0f)

        validTemperatures.forEach { temp ->
            viewModel.updateTemperature(temp)
            val errors = viewModel.uiState.value.validationErrors
            assertNull(errors["temperature"], "Should not have validation error for temperature $temp")
        }
    }

    @Test
    fun `should validate temperature below range`() = runTest(testDispatcher) {
        val belowRangeTemperatures = listOf(-0.1f, -1.0f, -10.0f)

        belowRangeTemperatures.forEach { temp ->
            viewModel.updateTemperature(temp)
            advanceUntilIdle() // Wait for async validation to complete
            val errors = viewModel.uiState.value.validationErrors
            assertTrue(
                errors["temperature"]?.contains("between 0.0 and 2.0") == true,
                "Should have validation error for temperature $temp below range"
            )
        }
    }

    @Test
    fun `should validate temperature above range`() = runTest(testDispatcher) {
        val aboveRangeTemperatures = listOf(2.1f, 3.0f, 10.0f)

        aboveRangeTemperatures.forEach { temp ->
            viewModel.updateTemperature(temp)
            advanceUntilIdle() // Wait for async validation to complete
            val errors = viewModel.uiState.value.validationErrors
            assertTrue(
                errors["temperature"]?.contains("between 0.0 and 2.0") == true,
                "Should have validation error for temperature $temp above range"
            )
        }
    }

    @Test
    fun `should update custom prompt immediately in UI`() = runTest(testDispatcher) {
        val testPrompt = "Test prompt for immediate update"
        
        viewModel.updateCustomPrompt(testPrompt)
        
        assertEquals(testPrompt, viewModel.uiState.value.customPrompt, "Should update prompt immediately in UI")
    }

    @Test
    fun `should update temperature immediately in UI`() = runTest(testDispatcher) {
        val testTemperature = 0.8f
        
        viewModel.updateTemperature(testTemperature)
        
        assertEquals(testTemperature, viewModel.uiState.value.temperature, "Should update temperature immediately in UI")
    }

    @Test
    fun `should clear validation errors when updating with valid values`() = runTest(testDispatcher) {
        // First, set invalid values to create errors
        viewModel.updateCustomPrompt("A".repeat(1000)) // Over limit
        viewModel.updateTemperature(3.0f) // Over limit
        advanceUntilIdle() // Wait for validation errors to be set
        
        // Verify errors exist
        val errorsAfterInvalid = viewModel.uiState.value.validationErrors
        assertTrue(errorsAfterInvalid.containsKey("customPrompt"), "Should have prompt error")
        assertTrue(errorsAfterInvalid.containsKey("temperature"), "Should have temperature error")
        
        // Update with valid values
        viewModel.updateCustomPrompt("Valid prompt")
        viewModel.updateTemperature(1.0f)
        advanceUntilIdle() // Wait for validation to complete and errors to clear
        
        // Verify errors are cleared
        val errorsAfterValid = viewModel.uiState.value.validationErrors
        assertFalse(errorsAfterValid.containsKey("customPrompt"), "Should clear prompt error")
        assertFalse(errorsAfterValid.containsKey("temperature"), "Should clear temperature error")
    }

    @Test
    fun `should handle special characters in custom prompt validation`() = runTest(testDispatcher) {
        val specialPrompts = listOf(
            "Contains émoji 🎤",
            "Has \"quotes\" and 'apostrophes'",
            "Includes\nnewlines\nand\ttabs",
            "Unicode: 中文, العربية, русский",
            "Symbols: @#$%^&*()"
        )
        
        specialPrompts.forEach { prompt ->
            viewModel.updateCustomPrompt(prompt)
            val errors = viewModel.uiState.value.validationErrors
            assertNull(errors["customPrompt"], "Should handle special characters in prompt: $prompt")
        }
    }

    @Test
    fun `should handle empty and whitespace prompts correctly`() = runTest(testDispatcher) {
        val emptyPrompts = listOf(
            "",
            "   ",
            "\n\t ",
            "  \n  \t  \n  "
        )
        
        emptyPrompts.forEach { prompt ->
            viewModel.updateCustomPrompt(prompt)
            val errors = viewModel.uiState.value.validationErrors
            assertNull(errors["customPrompt"], "Should allow empty/whitespace prompt: '$prompt'")
        }
    }

    @Test
    fun `should calculate token approximation correctly in validation message`() = runTest(testDispatcher) {
        val overLimitPrompt = "A".repeat(900) // Over 896 limit
        
        viewModel.updateCustomPrompt(overLimitPrompt)
        advanceUntilIdle() // Wait for async validation to complete
        
        val error = viewModel.uiState.value.validationErrors["customPrompt"]
        assertTrue(
            error?.contains("224 tokens") == true,
            "Should mention token limit in validation message"
        )
        assertTrue(
            error?.contains("900/896") == true,
            "Should show current/max characters in validation message"
        )
    }

    // Mock implementations for testing
    private class MockSettingsRepository : SettingsRepository {
        private var currentSettings = AppSettings()
        
        override val settings = kotlinx.coroutines.flow.MutableStateFlow(currentSettings)
        
        override suspend fun getSettings(): AppSettings = currentSettings
        
        override suspend fun updateApiKey(apiKey: String): Result<Unit> = Result.Success(Unit)
        
        override suspend fun updateSelectedModel(model: String): Result<Unit> = Result.Success(Unit)
        
        override suspend fun updateLanguage(language: String?): Result<Unit> = Result.Success(Unit)
        
        override suspend fun updateTheme(theme: me.shadykhalifa.whispertop.domain.models.Theme): Result<Unit> = Result.Success(Unit)
        
        override suspend fun updateBaseUrl(baseUrl: String): Result<Unit> = Result.Success(Unit)
        
        override suspend fun updateCustomEndpoint(isCustom: Boolean): Result<Unit> = Result.Success(Unit)
        
        override suspend fun updateCustomPrompt(prompt: String?): Result<Unit> {
            currentSettings = currentSettings.copy(customPrompt = prompt)
            settings.value = currentSettings
            return Result.Success(Unit)
        }
        
        override suspend fun updateTemperature(temperature: Float): Result<Unit> {
            currentSettings = currentSettings.copy(temperature = temperature)
            settings.value = currentSettings
            return Result.Success(Unit)
        }
        
        override suspend fun updateSettings(settings: AppSettings): Result<Unit> = Result.Success(Unit)
        
        override suspend fun clearApiKey(): Result<Unit> = Result.Success(Unit)
        
        override suspend fun clearAllData(): Result<Unit> = Result.Success(Unit)
        
        override suspend fun cleanupTemporaryFiles(): Result<Unit> = Result.Success(Unit)
        
        private var wordsPerMinute: Int = 60
        private var wpmOnboardingCompleted: Boolean = false
        
        override suspend fun updateWordsPerMinute(wpm: Int): Result<Unit> {
            wordsPerMinute = wpm
            return Result.Success(Unit)
        }
        
        override suspend fun updateWpmOnboardingCompleted(completed: Boolean): Result<Unit> {
            wpmOnboardingCompleted = completed
            return Result.Success(Unit)
        }
        
        override suspend fun getWordsPerMinute(): Int = wordsPerMinute
        
        override suspend fun isWpmOnboardingCompleted(): Boolean = wpmOnboardingCompleted
    }
    
    private class MockSecurePreferencesRepository : SecurePreferencesRepository {
        override suspend fun saveApiKey(apiKey: String): Result<Unit> = Result.Success(Unit)
        override suspend fun getApiKey(): Result<String?> = Result.Success(null)
        override suspend fun clearApiKey(): Result<Unit> = Result.Success(Unit)
        override suspend fun hasApiKey(): Result<Boolean> = Result.Success(false)
        override suspend fun saveApiEndpoint(endpoint: String): Result<Unit> = Result.Success(Unit)
        override suspend fun getApiEndpoint(): Result<String> = Result.Success("https://api.openai.com/v1/")
        override fun validateApiKey(key: String, isOpenAIEndpoint: Boolean): Boolean = true
        
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
}