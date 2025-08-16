package me.shadykhalifa.whispertop.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.presentation.viewmodels.SettingsViewModel
import me.shadykhalifa.whispertop.utils.Result
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    
    @Mock
    private lateinit var settingsRepository: SettingsRepository
    
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
        
        viewModel = SettingsViewModel(settingsRepository)
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
    fun `updateApiKey should call repository when valid`() = runTest {
        // Given
        val newApiKey = "sk-proj-new-api-key-that-is-long-enough-for-validation"
        whenever(settingsRepository.updateApiKey(newApiKey)).thenReturn(Result.Success(Unit))
        
        // When
        viewModel.updateApiKey(newApiKey)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(settingsRepository).updateApiKey(newApiKey)
    }
    
    @Test
    fun `updateApiKey should show validation error when empty`() = runTest {
        // When
        viewModel.updateApiKey("")
        
        // Then
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("apiKey"))
        assertEquals("API Key cannot be empty", viewModel.uiState.value.validationErrors["apiKey"])
    }
    
    @Test
    fun `updateApiKey should show validation error when invalid format`() = runTest {
        // When
        viewModel.updateApiKey("invalid-key")
        
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
}