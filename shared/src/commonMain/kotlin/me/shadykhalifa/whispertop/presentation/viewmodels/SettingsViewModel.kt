package me.shadykhalifa.whispertop.presentation.viewmodels

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
    val validationErrors: Map<String, String> = emptyMap(),
    val savingApiKey: Boolean = false,
    val savingModel: Boolean = false,
    val savingLanguage: Boolean = false,
    val savingTheme: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
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
        val validationError = when {
            apiKey.isBlank() -> "API Key cannot be empty"
            !apiKey.startsWith("sk-") -> "Invalid API key format. OpenAI API keys start with 'sk-'"
            apiKey.length < 40 -> "API key is too short. OpenAI API keys are typically 51 characters long"
            else -> null
        }
        
        _uiState.value = _uiState.value.copy(
            validationErrors = _uiState.value.validationErrors.toMutableMap().apply {
                if (validationError != null) {
                    put("apiKey", validationError)
                } else {
                    remove("apiKey")
                }
            }
        )
        
        if (validationError == null) {
            _uiState.value = _uiState.value.copy(savingApiKey = true)
            viewModelScope.launch { 
                try {
                    when (val result = settingsRepository.updateApiKey(apiKey)) {
                        is Result.Success -> {
                            // Success handled by flow collection
                        }
                    is Result.Error -> {
                        handleError(result.exception)
                        }
                        is Result.Loading -> {
                            // Loading state handled by granular state
                        }
                    }
                } catch (e: Exception) {
                    handleError(e)
                } finally {
                    _uiState.value = _uiState.value.copy(savingApiKey = false)
                }
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
        viewModelScope.launch { 
            try {
                when (val result = settingsRepository.clearApiKey()) {
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