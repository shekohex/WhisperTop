package me.shadykhalifa.whispertop.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.runBlocking
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.presentation.viewmodels.SettingsViewModel
import me.shadykhalifa.whispertop.utils.Result
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    
    @Mock
    private lateinit var settingsRepository: SettingsRepository
    
    @Mock
    private lateinit var securePreferencesRepository: SecurePreferencesRepository
    
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    private val defaultSettings = AppSettings(
        apiKey = "test-key",
        selectedModel = "whisper-1",
        language = "en",
        autoDetectLanguage = false,
        theme = Theme.Light,
        enableHapticFeedback = true,
        enableBatteryOptimization = false
    )
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        whenever(settingsRepository.settings).thenReturn(flowOf(defaultSettings))
        whenever(securePreferencesRepository.validateApiKey("sk-proj-new-api-key-that-is-long-enough-for-validation")).thenReturn(true)
        whenever(securePreferencesRepository.validateApiKey("")).thenReturn(false)
        whenever(securePreferencesRepository.validateApiKey("invalid-key")).thenReturn(false)
        whenever(securePreferencesRepository.validateApiKey("sk-short")).thenReturn(false)
        runBlocking {
            whenever(securePreferencesRepository.getApiEndpoint()).thenReturn(Result.Success("https://api.openai.com/v1/"))
        }
        
        viewModel = SettingsViewModel(settingsRepository, securePreferencesRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state should contain default settings`() = runTest {
        // When viewModel is initialized
        // Then
        assertEquals(defaultSettings, viewModel.uiState.value.settings)
    }
    
    @Test
    fun `validateAndSaveApiKey should call repository when valid`() = runTest {
        // Given
        val newApiKey = "sk-proj-new-api-key-that-is-long-enough-for-validation"
        whenever(settingsRepository.updateApiKey(newApiKey)).thenReturn(Result.Success(Unit))
        
        // When
        viewModel.updateApiKeyValue(newApiKey)
        viewModel.validateAndSaveApiKey()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(settingsRepository).updateApiKey(newApiKey)
    }
    
    @Test
    fun `updateApiKeyValue should NOT show validation error`() = runTest {
        // When
        viewModel.updateApiKeyValue("")
        
        // Then
        assertFalse(viewModel.uiState.value.validationErrors.containsKey("apiKey"))
        assertEquals("", viewModel.uiState.value.apiKeyValue)
    }
    
    @Test
    fun `validateAndSaveApiKey should show validation error when empty`() = runTest {
        // Given
        viewModel.updateApiKeyValue("")
        
        // When
        viewModel.validateAndSaveApiKey()
        
        // Then
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("apiKey"))
        assertEquals("API Key cannot be empty", viewModel.uiState.value.validationErrors["apiKey"])
    }
    
    @Test
    fun `updateApiKeyValue should allow typing without validation`() = runTest {
        // When
        viewModel.updateApiKeyValue("invalid-key")
        
        // Then
        assertFalse(viewModel.uiState.value.validationErrors.containsKey("apiKey"))
        assertEquals("invalid-key", viewModel.uiState.value.apiKeyValue)
    }
    
    @Test
    fun `validateAndSaveApiKey should show validation error when invalid format`() = runTest {
        // Given
        viewModel.updateApiKeyValue("invalid-key")
        
        // When
        viewModel.validateAndSaveApiKey()
        
        // Then
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("apiKey"))
        assertEquals("Invalid API key format. OpenAI API keys start with 'sk-'", viewModel.uiState.value.validationErrors["apiKey"])
    }
    
    @Test
    fun `updateSelectedModel should call repository`() = runTest {
        // Given
        val newModel = "whisper-large-v3"
        whenever(settingsRepository.updateSelectedModel(newModel)).thenReturn(Result.Success(Unit))
        
        // When
        viewModel.updateSelectedModel(newModel)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(settingsRepository).updateSelectedModel(newModel)
    }
    
    @Test
    fun `updateLanguage should call repository`() = runTest {
        // Given
        val newLanguage = "es"
        whenever(settingsRepository.updateLanguage(newLanguage)).thenReturn(Result.Success(Unit))
        
        // When
        viewModel.updateLanguage(newLanguage)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(settingsRepository).updateLanguage(newLanguage)
    }
    
    @Test
    fun `updateTheme should call repository`() = runTest {
        // Given
        val newTheme = Theme.Dark
        whenever(settingsRepository.updateTheme(newTheme)).thenReturn(Result.Success(Unit))
        
        // When
        viewModel.updateTheme(newTheme)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(settingsRepository).updateTheme(newTheme)
    }
    
    @Test
    fun `toggleAutoDetectLanguage should update settings`() = runTest {
        // Given
        val updatedSettings = defaultSettings.copy(autoDetectLanguage = true)
        whenever(settingsRepository.updateSettings(updatedSettings)).thenReturn(Result.Success(Unit))
        
        // When
        viewModel.toggleAutoDetectLanguage()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(settingsRepository).updateSettings(updatedSettings)
    }
    
    @Test
    fun `toggleHapticFeedback should update settings`() = runTest {
        // Given
        val updatedSettings = defaultSettings.copy(enableHapticFeedback = false)
        whenever(settingsRepository.updateSettings(updatedSettings)).thenReturn(Result.Success(Unit))
        
        // When
        viewModel.toggleHapticFeedback()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(settingsRepository).updateSettings(updatedSettings)
    }
    
    @Test
    fun `toggleBatteryOptimization should update settings`() = runTest {
        // Given
        val updatedSettings = defaultSettings.copy(enableBatteryOptimization = true)
        whenever(settingsRepository.updateSettings(updatedSettings)).thenReturn(Result.Success(Unit))
        
        // When
        viewModel.toggleBatteryOptimization()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(settingsRepository).updateSettings(updatedSettings)
    }
    
    @Test
    fun `toggleApiKeyVisibility should toggle visibility state`() = runTest {
        // Given
        val initialVisibility = viewModel.uiState.value.isApiKeyVisible
        
        // When
        viewModel.toggleApiKeyVisibility()
        
        // Then
        assertEquals(!initialVisibility, viewModel.uiState.value.isApiKeyVisible)
    }
    
    @Test
    fun `clearApiKey should call repository`() = runTest {
        // Given
        whenever(settingsRepository.clearApiKey()).thenReturn(Result.Success(Unit))
        
        // When
        viewModel.clearApiKey()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(settingsRepository).clearApiKey()
    }
    
    @Test
    fun `clearApiKeyValidation should clear validation errors`() = runTest {
        // Given there's a validation error
        viewModel.updateApiKeyValue("invalid")
        viewModel.validateAndSaveApiKey()
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("apiKey"))
        
        // When
        viewModel.clearApiKeyValidation()
        
        // Then
        assertFalse(viewModel.uiState.value.validationErrors.containsKey("apiKey"))
    }
    
    @Test
    fun `progressive typing should not show validation errors during typing`() = runTest {
        // Test typing 's' -> 'sk' -> 'sk-' progression without validation
        viewModel.updateApiKeyValue("s")
        assertFalse(viewModel.uiState.value.validationErrors.containsKey("apiKey"))
        assertEquals("s", viewModel.uiState.value.apiKeyValue)
        
        viewModel.updateApiKeyValue("sk")
        assertFalse(viewModel.uiState.value.validationErrors.containsKey("apiKey"))
        assertEquals("sk", viewModel.uiState.value.apiKeyValue)
        
        viewModel.updateApiKeyValue("sk-")
        assertFalse(viewModel.uiState.value.validationErrors.containsKey("apiKey"))
        assertEquals("sk-", viewModel.uiState.value.apiKeyValue)
    }
    
    @Test
    fun `validation only happens on validateAndSaveApiKey call`() = runTest {
        // Test typing 's' -> 'sk' -> 'sk-' progression then validate
        viewModel.updateApiKeyValue("s")
        viewModel.updateApiKeyValue("sk")
        viewModel.updateApiKeyValue("sk-")
        
        // No validation errors during typing
        assertFalse(viewModel.uiState.value.validationErrors.containsKey("apiKey"))
        
        // When validation is triggered
        viewModel.validateAndSaveApiKey()
        
        // Then validation error should appear
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("apiKey"))
        assertEquals("API key is too short. OpenAI API keys are typically 51 characters long", viewModel.uiState.value.validationErrors["apiKey"])
    }
    
    @Test
    fun `valid api key should clear validation errors and save`() = runTest {
        // Given there's a validation error
        viewModel.updateApiKeyValue("invalid")
        viewModel.validateAndSaveApiKey()
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("apiKey"))
        
        // When a valid API key is entered and validated
        val validApiKey = "sk-proj-" + "a".repeat(46) // 51 characters total
        whenever(settingsRepository.updateApiKey(validApiKey)).thenReturn(Result.Success(Unit))
        viewModel.updateApiKeyValue(validApiKey)
        viewModel.validateAndSaveApiKey()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then validation error should be cleared and repository called
        assertFalse(viewModel.uiState.value.validationErrors.containsKey("apiKey"))
        verify(settingsRepository).updateApiKey(validApiKey)
    }
    
    @Test
    fun `api key too short should show specific validation error`() = runTest {
        // Given
        viewModel.updateApiKeyValue("sk-short")
        
        // When validation is triggered
        viewModel.validateAndSaveApiKey()
        
        // Then specific error should be shown
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("apiKey"))
        assertEquals("API key is too short. OpenAI API keys are typically 51 characters long", viewModel.uiState.value.validationErrors["apiKey"])
    }
}