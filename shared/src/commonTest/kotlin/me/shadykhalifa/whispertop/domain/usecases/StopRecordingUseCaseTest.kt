package me.shadykhalifa.whispertop.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.Language
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.models.TranscriptionRequest
import me.shadykhalifa.whispertop.domain.models.TranscriptionResponse
import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.utils.Result
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StopRecordingUseCaseTest {
    
    @Test
    fun `stopRecording should succeed with valid recording and transcription`() = runTest {
        val mockAudioFile = AudioFile("/test/path.wav", 5000L, 1024L)
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Success(mockAudioFile)
        )
        val mockTranscriptionRepository = MockTranscriptionRepository(
            transcribeResult = Result.Success(TranscriptionResponse("Hello world"))
        )
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Success)
        assertEquals("Hello world", result.data)
        assertTrue(mockTranscriptionRepository.transcribeCalled)
    }
    
    @Test
    fun `stopRecording should fail when not currently recording`() = runTest {
        val mockAudioRepository = MockAudioRepository(isRecording = false)
        val mockTranscriptionRepository = MockTranscriptionRepository()
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is IllegalStateException)
        assertEquals("Not currently recording", result.exception.message)
        assertTrue(!mockTranscriptionRepository.transcribeCalled)
    }
    
    @Test
    fun `stopRecording should handle audio file creation failure`() = runTest {
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Error(IOException("Failed to save audio file"))
        )
        val mockTranscriptionRepository = MockTranscriptionRepository()
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is IOException)
        assertEquals("Failed to save audio file", result.exception.message)
        assertTrue(!mockTranscriptionRepository.transcribeCalled)
    }
    
    @Test
    fun `stopRecording should handle transcription API failure`() = runTest {
        val mockAudioFile = AudioFile("/test/path.wav", 5000L, 1024L)
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Success(mockAudioFile)
        )
        val mockTranscriptionRepository = MockTranscriptionRepository(
            transcribeResult = Result.Error(RuntimeException("API Error 500"))
        )
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertEquals("API Error 500", result.exception.message)
        assertTrue(mockTranscriptionRepository.transcribeCalled)
    }
    
    @Test
    fun `stopRecording should handle network timeout during transcription`() = runTest {
        val mockAudioFile = AudioFile("/test/path.wav", 5000L, 1024L)
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Success(mockAudioFile)
        )
        val mockTranscriptionRepository = MockTranscriptionRepository(
            transcribeResult = Result.Error(SocketTimeoutException("Request timed out"))
        )
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is SocketTimeoutException)
        assertEquals("Request timed out", result.exception.message)
    }
    
    @Test
    fun `stopRecording should handle network connection failure`() = runTest {
        val mockAudioFile = AudioFile("/test/path.wav", 5000L, 1024L)
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Success(mockAudioFile)
        )
        val mockTranscriptionRepository = MockTranscriptionRepository(
            transcribeResult = Result.Error(ConnectException("Connection refused"))
        )
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is ConnectException)
        assertEquals("Connection refused", result.exception.message)
    }
    
    @Test
    fun `stopRecording should handle empty audio file`() = runTest {
        val emptyAudioFile = AudioFile("/test/empty.wav", 0L, 0L)
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Success(emptyAudioFile)
        )
        val mockTranscriptionRepository = MockTranscriptionRepository(
            transcribeResult = Result.Success(TranscriptionResponse(""))
        )
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Success)
        assertEquals("", result.data)
    }
    
    @Test
    fun `stopRecording should handle very large audio file`() = runTest {
        val largeAudioFile = AudioFile("/test/large.wav", 600000L, 104857600L) // 10 minutes, 100MB
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Success(largeAudioFile)
        )
        val mockTranscriptionRepository = MockTranscriptionRepository(
            transcribeResult = Result.Success(TranscriptionResponse("Long transcription text"))
        )
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Success)
        assertEquals("Long transcription text", result.data)
    }
    
    @Test
    fun `stopRecording should use language setting for transcription request`() = runTest {
        val mockAudioFile = AudioFile("/test/path.wav", 5000L, 1024L)
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Success(mockAudioFile)
        )
        val mockTranscriptionRepository = MockTranscriptionRepository(
            transcribeResult = Result.Success(TranscriptionResponse("Hola mundo"))
        )
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(
                apiKey = "sk-test123",
                language = "es",
                autoDetectLanguage = false,
                selectedModel = "whisper-1"
            )
        )
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Success)
        assertEquals("Hola mundo", result.data)
        
        val capturedRequest = mockTranscriptionRepository.lastRequest!!
        assertEquals("es", capturedRequest.language)
        assertEquals("whisper-1", capturedRequest.model)
        assertEquals(mockAudioFile, capturedRequest.audioFile)
    }
    
    @Test
    fun `stopRecording should use auto-detect when autoDetectLanguage is true`() = runTest {
        val mockAudioFile = AudioFile("/test/path.wav", 5000L, 1024L)
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Success(mockAudioFile)
        )
        val mockTranscriptionRepository = MockTranscriptionRepository(
            transcribeResult = Result.Success(TranscriptionResponse("Auto detected text"))
        )
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(
                apiKey = "sk-test123",
                language = "es", // This should be ignored
                autoDetectLanguage = true,
                selectedModel = "whisper-1"
            )
        )
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Success)
        assertEquals("Auto detected text", result.data)
        
        val capturedRequest = mockTranscriptionRepository.lastRequest!!
        assertEquals(null, capturedRequest.language) // Should be null for auto-detect
        assertEquals("whisper-1", capturedRequest.model)
    }
    
    @Test
    fun `stopRecording should handle rate limit exceeded error`() = runTest {
        val mockAudioFile = AudioFile("/test/path.wav", 5000L, 1024L)
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Success(mockAudioFile)
        )
        val mockTranscriptionRepository = MockTranscriptionRepository(
            transcribeResult = Result.Error(RuntimeException("Rate limit exceeded. Please try again later."))
        )
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertEquals("Rate limit exceeded. Please try again later.", result.exception.message)
    }
    
    @Test
    fun `stopRecording should handle invalid API key error`() = runTest {
        val mockAudioFile = AudioFile("/test/path.wav", 5000L, 1024L)
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Success(mockAudioFile)
        )
        val mockTranscriptionRepository = MockTranscriptionRepository(
            transcribeResult = Result.Error(SecurityException("Invalid API key"))
        )
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is SecurityException)
        assertEquals("Invalid API key", result.exception.message)
    }
    
    @Test
    fun `stopRecording should handle malformed audio data error`() = runTest {
        val mockAudioFile = AudioFile("/test/corrupted.wav", 5000L, 1024L)
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Success(mockAudioFile)
        )
        val mockTranscriptionRepository = MockTranscriptionRepository(
            transcribeResult = Result.Error(IllegalArgumentException("Unsupported audio format"))
        )
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is IllegalArgumentException)
        assertEquals("Unsupported audio format", result.exception.message)
    }
    
    @Test
    fun `stopRecording should handle concurrent stop attempts`() = runTest {
        val mockAudioRepository = MockAudioRepository(
            isRecording = false // Already stopped
        )
        val mockTranscriptionRepository = MockTranscriptionRepository()
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is IllegalStateException)
        assertEquals("Not currently recording", result.exception.message)
    }
    
    @Test
    fun `stopRecording should handle disk space error during audio save`() = runTest {
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Error(IOException("No space left on device"))
        )
        val mockTranscriptionRepository = MockTranscriptionRepository()
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is IOException)
        assertEquals("No space left on device", result.exception.message)
    }
    
    @Test
    fun `stopRecording should handle audio permission revoked during recording`() = runTest {
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Error(SecurityException("Audio permission revoked"))
        )
        val mockTranscriptionRepository = MockTranscriptionRepository()
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is SecurityException)
        assertEquals("Audio permission revoked", result.exception.message)
    }
    
    @Test
    fun `stopRecording should handle loading state from transcription`() = runTest {
        val mockAudioFile = AudioFile("/test/path.wav", 5000L, 1024L)
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Success(mockAudioFile)
        )
        val mockTranscriptionRepository = MockTranscriptionRepository(
            transcribeResult = Result.Loading
        )
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Loading)
    }
    
    @Test
    fun `stopRecording should handle custom model selection`() = runTest {
        val mockAudioFile = AudioFile("/test/path.wav", 5000L, 1024L)
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Success(mockAudioFile)
        )
        val mockTranscriptionRepository = MockTranscriptionRepository(
            transcribeResult = Result.Success(TranscriptionResponse("Custom model result"))
        )
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(
                apiKey = "sk-test123",
                selectedModel = "whisper-large-v3",
                autoDetectLanguage = true
            )
        )
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Success)
        assertEquals("Custom model result", result.data)
        
        val capturedRequest = mockTranscriptionRepository.lastRequest!!
        assertEquals("whisper-large-v3", capturedRequest.model)
        assertEquals(null, capturedRequest.language)
    }
    
    @Test
    fun `stopRecording should handle very short audio recording`() = runTest {
        val shortAudioFile = AudioFile("/test/short.wav", 100L, 128L) // 0.1 second
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Success(shortAudioFile)
        )
        val mockTranscriptionRepository = MockTranscriptionRepository(
            transcribeResult = Result.Success(TranscriptionResponse("Hi"))
        )
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Success)
        assertEquals("Hi", result.data)
    }
    
    @Test
    fun `stopRecording should handle transcription with special characters`() = runTest {
        val mockAudioFile = AudioFile("/test/path.wav", 5000L, 1024L)
        val mockAudioRepository = MockAudioRepository(
            isRecording = true,
            stopRecordingResult = Result.Success(mockAudioFile)
        )
        val specialText = "Hello! @#$%^&*()_+-=[]{}|;:,.<>? 🎵🎤 测试"
        val mockTranscriptionRepository = MockTranscriptionRepository(
            transcribeResult = Result.Success(TranscriptionResponse(specialText))
        )
        val mockSettingsRepository = MockSettingsRepository()
        val useCase = StopRecordingUseCase(mockAudioRepository, mockTranscriptionRepository, mockSettingsRepository)
        
        val result = useCase()
        
        assertTrue(result is Result.Success)
        assertEquals(specialText, result.data)
    }
    
    private class MockAudioRepository(
        private val isRecording: Boolean,
        var stopRecordingResult: Result<AudioFile> = Result.Success(AudioFile("/mock/path.wav", 1000L, 1024L))
    ) : AudioRepository {
        
        override val recordingState: Flow<RecordingState> = flowOf(RecordingState.Idle)
        
        override suspend fun startRecording(): Result<Unit> = Result.Success(Unit)
        
        override suspend fun stopRecording(): Result<AudioFile> = stopRecordingResult
        
        override suspend fun cancelRecording(): Result<Unit> = Result.Success(Unit)
        
        override fun isRecording(): Boolean = isRecording
        
        override suspend fun getLastRecording(): AudioFile? = null
    }
    
    private class MockTranscriptionRepository(
        var transcribeResult: Result<TranscriptionResponse> = Result.Success(TranscriptionResponse("Mock transcription"))
    ) : TranscriptionRepository {
        var transcribeCalled = false
        var lastRequest: TranscriptionRequest? = null
        
        override suspend fun transcribe(request: TranscriptionRequest): Result<TranscriptionResponse> {
            transcribeCalled = true
            lastRequest = request
            return transcribeResult
        }
        
        override suspend fun transcribeWithLanguageDetection(
            request: TranscriptionRequest,
            userLanguageOverride: Language?
        ): Result<TranscriptionResponse> {
            transcribeCalled = true
            lastRequest = request
            return transcribeResult
        }
        
        override suspend fun validateApiKey(apiKey: String): Result<Boolean> = Result.Success(true)
        
        override suspend fun isConfigured(): Boolean = true
    }
    
    private class MockSettingsRepository(
        private val appSettings: AppSettings = AppSettings(apiKey = "sk-test123")
    ) : SettingsRepository {
        override val settings: Flow<AppSettings> = flowOf(this.appSettings)
        
        override suspend fun getSettings(): AppSettings = appSettings
        
        override suspend fun updateApiKey(apiKey: String): Result<Unit> = Result.Success(Unit)
        
        override suspend fun updateSelectedModel(model: String): Result<Unit> = Result.Success(Unit)
        
        override suspend fun updateLanguage(language: String?): Result<Unit> = Result.Success(Unit)
        
        override suspend fun updateTheme(theme: Theme): Result<Unit> = Result.Success(Unit)
        
        override suspend fun updateSettings(settings: AppSettings): Result<Unit> = Result.Success(Unit)
        
        override suspend fun clearApiKey(): Result<Unit> = Result.Success(Unit)
        
        override suspend fun clearAllData(): Result<Unit> = Result.Success(Unit)
        
        override suspend fun updateBaseUrl(baseUrl: String): Result<Unit> = Result.Success(Unit)
        
        override suspend fun updateCustomEndpoint(isCustom: Boolean): Result<Unit> = Result.Success(Unit)
        
        override suspend fun updateCustomPrompt(prompt: String?): Result<Unit> = Result.Success(Unit)
        
        override suspend fun updateTemperature(temperature: Float): Result<Unit> = Result.Success(Unit)
        
        override suspend fun cleanupTemporaryFiles(): Result<Unit> = Result.Success(Unit)
    }
}