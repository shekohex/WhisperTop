package me.shadykhalifa.whispertop.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.AudioFormat
import me.shadykhalifa.whispertop.domain.models.Language
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.models.TranscriptionRequest
import me.shadykhalifa.whispertop.domain.models.TranscriptionResponse
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.utils.Result

class MockSettingsRepository : SettingsRepository {
    
    private val _settings = MutableStateFlow(TestFixtures.createAppSettings())
    private var shouldSimulateError = false
    
    override val settings: Flow<AppSettings> = _settings.asStateFlow()
    
    override suspend fun getSettings(): AppSettings = _settings.value
    
    override suspend fun updateApiKey(apiKey: String): Result<Unit> {
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Failed to update API key"))
        } else {
            _settings.value = _settings.value.copy(apiKey = apiKey)
            Result.Success(Unit)
        }
    }
    
    override suspend fun updateSelectedModel(model: String): Result<Unit> {
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Failed to update model"))
        } else {
            _settings.value = _settings.value.copy(selectedModel = model)
            Result.Success(Unit)
        }
    }
    
    override suspend fun updateLanguage(language: String?): Result<Unit> {
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Failed to update language"))
        } else {
            Result.Success(Unit) // Language not in AppSettings model
        }
    }
    
    override suspend fun updateTheme(theme: Theme): Result<Unit> {
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Failed to update theme"))
        } else {
            _settings.value = _settings.value.copy(theme = theme)
            Result.Success(Unit)
        }
    }
    
    override suspend fun updateBaseUrl(baseUrl: String): Result<Unit> {
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Failed to update base URL"))
        } else {
            Result.Success(Unit) // Base URL not in AppSettings model
        }
    }
    
    override suspend fun updateCustomEndpoint(isCustom: Boolean): Result<Unit> {
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Failed to update custom endpoint"))
        } else {
            Result.Success(Unit)
        }
    }
    
    override suspend fun updateCustomPrompt(prompt: String?): Result<Unit> {
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Failed to update custom prompt"))
        } else {
            _settings.value = _settings.value.copy(customPrompt = prompt)
            Result.Success(Unit)
        }
    }
    
    override suspend fun updateTemperature(temperature: Float): Result<Unit> {
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Failed to update temperature"))
        } else {
            _settings.value = _settings.value.copy(temperature = temperature)
            Result.Success(Unit)
        }
    }
    
    override suspend fun updateSettings(settings: AppSettings): Result<Unit> {
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Failed to update settings"))
        } else {
            _settings.value = settings
            Result.Success(Unit)
        }
    }
    
    override suspend fun clearApiKey(): Result<Unit> {
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Failed to clear API key"))
        } else {
            _settings.value = _settings.value.copy(apiKey = "")
            Result.Success(Unit)
        }
    }
    
    override suspend fun clearAllData(): Result<Unit> {
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Failed to clear all data"))
        } else {
            _settings.value = TestFixtures.createMinimalAppSettings()
            Result.Success(Unit)
        }
    }
    
    override suspend fun cleanupTemporaryFiles(): Result<Unit> {
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedFileSystemError("Failed to cleanup temp files"))
        } else {
            Result.Success(Unit)
        }
    }
    
    // Test utilities
    fun setSettings(settings: AppSettings) {
        _settings.value = settings
    }
    
    fun getCurrentSettings(): AppSettings = _settings.value
    
    fun simulateError(shouldError: Boolean = true) {
        shouldSimulateError = shouldError
    }
}

class MockTranscriptionRepository : TranscriptionRepository {
    
    private var shouldSimulateError = false
    private var simulatedDelay = 0L
    private var isConfigured = true
    
    override suspend fun transcribe(request: TranscriptionRequest): Result<TranscriptionResponse> {
        simulateDelay()
        
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Mock transcription error"))
        } else {
            val response = TestFixtures.createTranscriptionResponse(
                text = "Mock transcription of audio file",
                language = request.language ?: "en",
                duration = (request.audioFile.duration / 1000f) // Convert ms to seconds
            )
            Result.Success(response)
        }
    }
    
    override suspend fun transcribeWithLanguageDetection(
        request: TranscriptionRequest,
        userLanguageOverride: me.shadykhalifa.whispertop.domain.models.Language?
    ): Result<TranscriptionResponse> {
        simulateDelay()
        
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Mock language detection error"))
        } else {
            val detectedLanguage = userLanguageOverride?.code ?: "en"
            val response = TestFixtures.createTranscriptionResponse(
                text = "Mock transcription with language detection",
                language = detectedLanguage,
                duration = (request.audioFile.duration / 1000f) // Convert ms to seconds
            )
            Result.Success(response)
        }
    }
    
    override suspend fun validateApiKey(apiKey: String): Result<Boolean> {
        simulateDelay()
        
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Mock API key validation error"))
        } else {
            val isValid = when {
                apiKey.isBlank() -> false
                apiKey.startsWith("MOCK-sk-") && apiKey.length >= 51 -> true
                apiKey.startsWith("TEST-sk-") && apiKey.length >= 51 -> true
                apiKey.startsWith("FAKE-sk-") && apiKey.length >= 51 -> true
                apiKey == TestConstants.MOCK_API_KEY -> true
                apiKey == TestConstants.MOCK_OPENAI_API_KEY -> true
                apiKey == "INVALID_KEY" -> false
                else -> false
            }
            Result.Success(isValid)
        }
    }
    
    override suspend fun isConfigured(): Boolean {
        return isConfigured
    }
    
    // Test utilities
    fun simulateError(shouldError: Boolean = true) {
        shouldSimulateError = shouldError
    }
    
    fun setSimulatedDelay(delayMs: Long) {
        simulatedDelay = delayMs
    }
    
    fun setConfigured(configured: Boolean) {
        isConfigured = configured
    }
    
    fun clearAll() {
        shouldSimulateError = false
        simulatedDelay = 0L
        isConfigured = true
    }
    
    private suspend fun simulateDelay() {
        if (simulatedDelay > 0) {
            delay(simulatedDelay)
        }
    }
}

class MockAudioRepository : AudioRepository {
    
    private var shouldSimulateError = false
    private var simulatedDelay = 0L
    private var _isRecording = false
    private var lastRecording: AudioFile? = null
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    
    override val recordingState: Flow<RecordingState> = _recordingState.asStateFlow()
    
    override suspend fun startRecording(): Result<Unit> {
        simulateDelay()
        
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Mock recording start error"))
        } else {
            _isRecording = true
            _recordingState.value = RecordingState.Recording(
                startTime = Clock.System.now().toEpochMilliseconds(),
                duration = 0L
            )
            Result.Success(Unit)
        }
    }
    
    override suspend fun stopRecording(): Result<AudioFile> {
        simulateDelay()
        
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Mock recording stop error"))
        } else {
            _isRecording = false
            _recordingState.value = RecordingState.Idle
            val audioFile = TestFixtures.createAudioFile()
            lastRecording = audioFile
            Result.Success(audioFile)
        }
    }
    
    override suspend fun cancelRecording(): Result<Unit> {
        simulateDelay()
        
        return if (shouldSimulateError) {
            Result.Error(TestUtils.SimulatedApiError("Mock recording cancel error"))
        } else {
            _isRecording = false
            _recordingState.value = RecordingState.Idle
            lastRecording = null
            Result.Success(Unit)
        }
    }
    
    override fun isRecording(): Boolean = _isRecording
    
    override suspend fun getLastRecording(): AudioFile? {
        simulateDelay()
        return lastRecording
    }
    
    // Test utilities
    fun simulateError(shouldError: Boolean = true) {
        shouldSimulateError = shouldError
    }
    
    fun setSimulatedDelay(delayMs: Long) {
        simulatedDelay = delayMs
    }
    
    fun setLastRecording(audioFile: AudioFile?) {
        lastRecording = audioFile
    }
    
    fun setRecordingState(state: RecordingState) {
        _recordingState.value = state
        _isRecording = when (state) {
            is RecordingState.Recording -> true
            else -> false
        }
    }
    
    fun clearAll() {
        shouldSimulateError = false
        simulatedDelay = 0L
        _isRecording = false
        lastRecording = null
        _recordingState.value = RecordingState.Idle
    }
    
    private suspend fun simulateDelay() {
        if (simulatedDelay > 0) {
            delay(simulatedDelay)
        }
    }
}

// Composite mock for integration tests
class MockRepositorySet {
    val settingsRepository = MockSettingsRepository()
    val transcriptionRepository = MockTranscriptionRepository()
    val audioRepository = MockAudioRepository()
    
    fun simulateNetworkError(enabled: Boolean = true) {
        transcriptionRepository.simulateError(enabled)
        audioRepository.simulateError(enabled)
    }
    
    fun setNetworkDelay(delayMs: Long) {
        transcriptionRepository.setSimulatedDelay(delayMs)
        audioRepository.setSimulatedDelay(delayMs)
    }
    
    fun clearAll() {
        transcriptionRepository.clearAll()
        audioRepository.clearAll()
        settingsRepository.setSettings(TestFixtures.createAppSettings())
    }
    
    fun setupBasicData() {
        settingsRepository.setSettings(TestFixtures.createAppSettings())
        audioRepository.setLastRecording(TestFixtures.createAudioFile())
    }
    
    fun setupLargeDataset() {
        settingsRepository.setSettings(TestFixtures.createCompleteAppSettings())
    }
    
    fun setupErrorScenarios() {
        simulateNetworkError(true)
        setNetworkDelay(100)
    }
}