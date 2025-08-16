package me.shadykhalifa.whispertop.presentation.viewmodels

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.models.WhisperModel
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.utils.Result

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val availableModels: List<WhisperModel> = WhisperModel.entries,
    val availableThemes: List<Theme> = Theme.entries,
    val isApiKeyVisible: Boolean = false,
    val savingApiKey: Boolean = false,
    val savingModel: Boolean = false,
    val savingLanguage: Boolean = false,
    val savingTheme: Boolean = false,
    val optimisticApiKey: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private var saveApiKeyJob: Job? = null
    private val apiKeySaveDebounceMs = 500L
    
    // Computed property to get the current API key value (optimistic or saved)
    val currentApiKey: String
        get() = _uiState.value.optimisticApiKey ?: _uiState.value.settings.apiKey
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
    }
    
    fun updateApiKey(apiKey: String) {
        // Optimistic update - immediately update UI
        _uiState.value = _uiState.value.copy(optimisticApiKey = apiKey)
        
        // Cancel previous save job if still running
        saveApiKeyJob?.cancel()
        
        // Debounce the actual save operation
        saveApiKeyJob = viewModelScope.launch {
            delay(apiKeySaveDebounceMs)
            
            _uiState.value = _uiState.value.copy(savingApiKey = true)
            try {
                when (val result = settingsRepository.updateApiKey(apiKey)) {
                    is Result.Success -> {
                        // Clear optimistic value when successfully saved
                        _uiState.value = _uiState.value.copy(optimisticApiKey = null)
                    }
                    is Result.Error -> {
                        handleError(result.exception)
                        // Keep optimistic value on error so user doesn't lose their input
                    }
                    is Result.Loading -> {
                        // Loading state handled by granular state
                    }
                }
            } catch (e: Exception) {
                handleError(e)
                // Keep optimistic value on error so user doesn't lose their input
            } finally {
                _uiState.value = _uiState.value.copy(savingApiKey = false)
            }
        }
    }
    
    fun updateSelectedModel(model: String) {
        viewModelScope.launch { 
            try {
                when (val result = settingsRepository.updateSelectedModel(model)) {
                    is Result.Success -> {
                        // Success handled by flow collection
                    }
                    is Result.Error -> {
                        handleError(result.exception)
                    }
                    is Result.Loading -> {
                        // Loading state handled by individual operations
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
    
    fun updateLanguage(language: String?) {
        viewModelScope.launch { 
            try {
                when (val result = settingsRepository.updateLanguage(language)) {
                    is Result.Success -> {
                        // Success handled by flow collection
                    }
                    is Result.Error -> {
                        handleError(result.exception)
                    }
                    is Result.Loading -> {
                        // Loading state handled by individual operations
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
    
    fun updateTheme(theme: Theme) {
        viewModelScope.launch { 
            try {
                when (val result = settingsRepository.updateTheme(theme)) {
                    is Result.Success -> {
                        // Success handled by flow collection
                    }
                    is Result.Error -> {
                        handleError(result.exception)
                    }
                    is Result.Loading -> {
                        // Loading state handled by individual operations
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
    
    fun toggleAutoDetectLanguage() {
        val currentSettings = _uiState.value.settings
        val updatedSettings = currentSettings.copy(
            autoDetectLanguage = !currentSettings.autoDetectLanguage
        )
        updateSettings(updatedSettings)
    }
    
    fun toggleHapticFeedback() {
        val currentSettings = _uiState.value.settings
        val updatedSettings = currentSettings.copy(
            enableHapticFeedback = !currentSettings.enableHapticFeedback
        )
        updateSettings(updatedSettings)
    }
    
    fun toggleBatteryOptimization() {
        val currentSettings = _uiState.value.settings
        val updatedSettings = currentSettings.copy(
            enableBatteryOptimization = !currentSettings.enableBatteryOptimization
        )
        updateSettings(updatedSettings)
    }
    
    fun toggleApiKeyVisibility() {
        _uiState.value = _uiState.value.copy(
            isApiKeyVisible = !_uiState.value.isApiKeyVisible
        )
    }
    
    fun clearApiKey() {
        // Cancel any pending save job
        saveApiKeyJob?.cancel()
        
        // Optimistic update - immediately clear the UI
        _uiState.value = _uiState.value.copy(optimisticApiKey = "")
        
        viewModelScope.launch { 
            try {
                when (val result = settingsRepository.clearApiKey()) {
                    is Result.Success -> {
                        // Clear optimistic value when successfully cleared
                        _uiState.value = _uiState.value.copy(optimisticApiKey = null)
                    }
                    is Result.Error -> {
                        handleError(result.exception)
                        // Revert optimistic change on error
                        _uiState.value = _uiState.value.copy(optimisticApiKey = null)
                    }
                    is Result.Loading -> {
                        // Loading state handled by individual operations
                    }
                }
            } catch (e: Exception) {
                handleError(e)
                // Revert optimistic change on error
                _uiState.value = _uiState.value.copy(optimisticApiKey = null)
            }
        }
    }
    
    private fun updateSettings(settings: AppSettings) {
        viewModelScope.launch { 
            try {
                when (val result = settingsRepository.updateSettings(settings)) {
                    is Result.Success -> {
                        // Success handled by flow collection
                    }
                    is Result.Error -> {
                        handleError(result.exception)
                    }
                    is Result.Loading -> {
                        // Loading state handled by individual operations
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}