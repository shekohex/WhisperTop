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
import me.shadykhalifa.whispertop.domain.models.Language
import me.shadykhalifa.whispertop.domain.models.LanguagePreference
import me.shadykhalifa.whispertop.domain.models.WhisperModel
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.data.remote.createOpenAIApiService
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
    val optimisticApiKey: String? = null,
    val apiKeyValue: String = "",
    val validationErrors: Map<String, String> = emptyMap(),
    val testingConnection: Boolean = false,
    val connectionTestResult: ConnectionTestResult? = null,
    val apiEndpoint: String = "https://api.openai.com/v1/",
    val showClearApiKeyDialog: Boolean = false,
    val showClearAllDataDialog: Boolean = false,
    val showPrivacyPolicyDialog: Boolean = false,
    val clearingAllData: Boolean = false,
    val cleaningTempFiles: Boolean = false
)

enum class ConnectionTestResult {
    SUCCESS, INVALID_KEY, NETWORK_ERROR, SERVER_ERROR
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val securePreferencesRepository: SecurePreferencesRepository
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
        loadApiEndpoint()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    settings = settings,
                    apiKeyValue = if (_uiState.value.apiKeyValue.isEmpty()) settings.apiKey else _uiState.value.apiKeyValue
                )
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

    fun updateLanguagePreference(preference: LanguagePreference) {
        val currentSettings = _uiState.value.settings
        val updatedSettings = currentSettings.copy(
            languagePreference = preference
        )
        updateSettings(updatedSettings)
    }

    fun updatePreferredLanguage(language: Language) {
        val currentSettings = _uiState.value.settings
        val updatedPreference = currentSettings.languagePreference.copy(
            preferredLanguage = language
        )
        updateLanguagePreference(updatedPreference)
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
    
    fun toggleUsageAnalytics() {
        val currentSettings = _uiState.value.settings
        val updatedSettings = currentSettings.copy(
            enableUsageAnalytics = !currentSettings.enableUsageAnalytics
        )
        updateSettings(updatedSettings)
    }
    
    fun toggleApiCallLogging() {
        val currentSettings = _uiState.value.settings
        val updatedSettings = currentSettings.copy(
            enableApiCallLogging = !currentSettings.enableApiCallLogging
        )
        updateSettings(updatedSettings)
    }
    
    fun toggleAutoCleanupTempFiles() {
        val currentSettings = _uiState.value.settings
        val updatedSettings = currentSettings.copy(
            autoCleanupTempFiles = !currentSettings.autoCleanupTempFiles
        )
        updateSettings(updatedSettings)
    }
    
    fun updateTempFileRetentionDays(days: Int) {
        val currentSettings = _uiState.value.settings
        val updatedSettings = currentSettings.copy(
            tempFileRetentionDays = days.coerceIn(1, 30)
        )
        updateSettings(updatedSettings)
    }
    
    fun showClearAllDataDialog() {
        _uiState.value = _uiState.value.copy(showClearAllDataDialog = true)
    }
    
    fun dismissClearAllDataDialog() {
        _uiState.value = _uiState.value.copy(showClearAllDataDialog = false)
    }
    
    fun confirmClearAllData() {
        _uiState.value = _uiState.value.copy(
            showClearAllDataDialog = false,
            clearingAllData = true
        )
        
        viewModelScope.launch {
            try {
                when (val result = settingsRepository.clearAllData()) {
                    is Result.Success -> {
                        // Clear optimistic values as well
                        _uiState.value = _uiState.value.copy(
                            optimisticApiKey = null,
                            apiKeyValue = "",
                            validationErrors = emptyMap(),
                            connectionTestResult = null
                        )
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
            } finally {
                _uiState.value = _uiState.value.copy(clearingAllData = false)
            }
        }
    }
    
    fun cleanupTemporaryFiles() {
        _uiState.value = _uiState.value.copy(cleaningTempFiles = true)
        
        viewModelScope.launch {
            try {
                when (val result = settingsRepository.cleanupTemporaryFiles()) {
                    is Result.Success -> {
                        // Show success message or update UI as needed
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
            } finally {
                _uiState.value = _uiState.value.copy(cleaningTempFiles = false)
            }
        }
    }
    
    fun showPrivacyPolicyDialog() {
        _uiState.value = _uiState.value.copy(showPrivacyPolicyDialog = true)
    }
    
    fun dismissPrivacyPolicyDialog() {
        _uiState.value = _uiState.value.copy(showPrivacyPolicyDialog = false)
    }
    
    fun toggleApiKeyVisibility() {
        _uiState.value = _uiState.value.copy(
            isApiKeyVisible = !_uiState.value.isApiKeyVisible
        )
    }
    
    fun clearApiKey() {
        _uiState.value = _uiState.value.copy(showClearApiKeyDialog = true)
    }
    
    fun confirmClearApiKey() {
        _uiState.value = _uiState.value.copy(showClearApiKeyDialog = false)
        
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
    
    fun dismissClearApiKeyDialog() {
        _uiState.value = _uiState.value.copy(showClearApiKeyDialog = false)
    }
    
    fun updateApiKeyValue(value: String) {
        _uiState.value = _uiState.value.copy(apiKeyValue = value)
    }
    
    fun validateAndSaveApiKey() {
        val apiKey = _uiState.value.apiKeyValue.trim()
        
        val validationErrors = mutableMapOf<String, String>()
        
        if (apiKey.isEmpty()) {
            validationErrors["apiKey"] = "API Key cannot be empty"
        } else if (!securePreferencesRepository.validateApiKey(apiKey)) {
            if (!apiKey.startsWith("sk-")) {
                validationErrors["apiKey"] = "Invalid API key format. OpenAI API keys start with 'sk-'"
            } else if (apiKey.length < 51) {
                validationErrors["apiKey"] = "API key is too short. OpenAI API keys are typically 51 characters long"
            } else {
                validationErrors["apiKey"] = "Invalid API key format"
            }
        }
        
        _uiState.value = _uiState.value.copy(validationErrors = validationErrors)
        
        if (validationErrors.isEmpty()) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(savingApiKey = true)
                try {
                    when (val result = settingsRepository.updateApiKey(apiKey)) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                validationErrors = emptyMap(),
                                apiKeyValue = ""
                            )
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
    
    fun clearApiKeyValidation() {
        val currentErrors = _uiState.value.validationErrors.toMutableMap()
        currentErrors.remove("apiKey")
        _uiState.value = _uiState.value.copy(validationErrors = currentErrors)
    }
    
    fun testConnection() {
        val apiKey = currentApiKey
        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(
                connectionTestResult = ConnectionTestResult.INVALID_KEY
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(testingConnection = true)
            try {
                val endpoint = _uiState.value.apiEndpoint
                val apiService = createOpenAIApiService(apiKey, endpoint)
                
                // Test with a minimal audio file (1 second of silence)
                val testAudioData = generateSilentWavData()
                
                val response = apiService.transcribe(
                    audioData = testAudioData,
                    fileName = "test.wav",
                    model = "whisper-1"
                )
                
                _uiState.value = _uiState.value.copy(
                    connectionTestResult = ConnectionTestResult.SUCCESS
                )
            } catch (e: Exception) {
                val result = when {
                    e.message?.contains("401") == true || 
                    e.message?.contains("unauthorized") == true ||
                    e.message?.contains("invalid") == true -> ConnectionTestResult.INVALID_KEY
                    e.message?.contains("network") == true ||
                    e.message?.contains("timeout") == true -> ConnectionTestResult.NETWORK_ERROR
                    else -> ConnectionTestResult.SERVER_ERROR
                }
                _uiState.value = _uiState.value.copy(connectionTestResult = result)
            } finally {
                _uiState.value = _uiState.value.copy(testingConnection = false)
            }
        }
    }
    
    fun updateApiEndpoint(endpoint: String) {
        viewModelScope.launch {
            try {
                // Save to secure preferences for API client usage
                when (val secureResult = securePreferencesRepository.saveApiEndpoint(endpoint)) {
                    is Result.Success -> {
                        // Also save to settings repository
                        when (val settingsResult = settingsRepository.updateBaseUrl(endpoint)) {
                            is Result.Success -> {
                                _uiState.value = _uiState.value.copy(apiEndpoint = endpoint)
                            }
                            is Result.Error -> {
                                handleError(settingsResult.exception)
                            }
                            is Result.Loading -> {
                                // Loading state handled by individual operations
                            }
                        }
                    }
                    is Result.Error -> {
                        handleError(secureResult.exception)
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
    
    fun clearConnectionTestResult() {
        _uiState.value = _uiState.value.copy(connectionTestResult = null)
    }
    
    private fun generateSilentWavData(): ByteArray {
        // Generate a minimal 1-second WAV file with silence for testing
        val sampleRate = 16000
        val numSamples = sampleRate // 1 second
        val bitsPerSample = 16
        val numChannels = 1
        val dataSize = numSamples * numChannels * (bitsPerSample / 8)
        val fileSize = 36 + dataSize
        
        return ByteArray(44 + dataSize).apply {
            // WAV header
            "RIFF".toByteArray().copyInto(this, 0)
            putInt(4, fileSize)
            "WAVE".toByteArray().copyInto(this, 8)
            "fmt ".toByteArray().copyInto(this, 12)
            putInt(16, 16) // fmt chunk size
            putShort(20, 1) // PCM format
            putShort(22, numChannels.toShort())
            putInt(24, sampleRate)
            putInt(28, sampleRate * numChannels * (bitsPerSample / 8))
            putShort(32, (numChannels * (bitsPerSample / 8)).toShort())
            putShort(34, bitsPerSample.toShort())
            "data".toByteArray().copyInto(this, 36)
            putInt(40, dataSize)
            // Data section is already zero-filled (silence)
        }
    }
    
    private fun ByteArray.putInt(offset: Int, value: Int) {
        this[offset] = (value and 0xFF).toByte()
        this[offset + 1] = ((value shr 8) and 0xFF).toByte()
        this[offset + 2] = ((value shr 16) and 0xFF).toByte()
        this[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }
    
    private fun ByteArray.putShort(offset: Int, value: Short) {
        this[offset] = (value.toInt() and 0xFF).toByte()
        this[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
    }
    
    private fun loadApiEndpoint() {
        viewModelScope.launch {
            when (val result = securePreferencesRepository.getApiEndpoint()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(apiEndpoint = result.data)
                }
                is Result.Error -> {
                    // Use default endpoint on error
                }
                is Result.Loading -> {
                    // Loading state handled by individual operations
                }
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