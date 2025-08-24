package me.shadykhalifa.whispertop.domain.usecases

import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.ChartTimeRange
import me.shadykhalifa.whispertop.domain.models.DataPrivacyMode
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.utils.Result
import me.shadykhalifa.whispertop.utils.TestConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StartRecordingUseCaseTest {
    
    @Test
    fun `startRecording should succeed with valid API key and not recording`() = runTest {
        val mockAudioRepository = MockAudioRepository(isRecording = false)
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(apiKey = TestConstants.MOCK_API_KEY)
        )
        val useCase = StartRecordingUseCase(mockAudioRepository, mockSettingsRepository, mockk(relaxed = true))
        
        val result = useCase()
        
        assertTrue(result is Result.Success)
        assertTrue(mockAudioRepository.startRecordingCalled)
    }
    
    @Test
    fun `startRecording should fail when API key is blank`() = runTest {
        val mockAudioRepository = MockAudioRepository(isRecording = false)
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(apiKey = "")
        )
        val useCase = StartRecordingUseCase(mockAudioRepository, mockSettingsRepository, mockk(relaxed = true))
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is IllegalStateException)
        assertEquals("API key not configured", result.exception.message)
        assertTrue(!mockAudioRepository.startRecordingCalled)
    }
    
    @Test
    fun `startRecording should fail when API key is whitespace only`() = runTest {
        val mockAudioRepository = MockAudioRepository(isRecording = false)
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(apiKey = "   ")
        )
        val useCase = StartRecordingUseCase(mockAudioRepository, mockSettingsRepository, mockk(relaxed = true))
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is IllegalStateException)
        assertEquals("API key not configured", result.exception.message)
    }
    
    @Test
    fun `startRecording should fail when already recording`() = runTest {
        val mockAudioRepository = MockAudioRepository(isRecording = true)
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(apiKey = TestConstants.MOCK_API_KEY)
        )
        val useCase = StartRecordingUseCase(mockAudioRepository, mockSettingsRepository, mockk(relaxed = true))
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is IllegalStateException)
        assertEquals("Already recording", result.exception.message)
        assertTrue(!mockAudioRepository.startRecordingCalled)
    }
    
    @Test
    fun `startRecording should fail when audio repository start fails`() = runTest {
        val mockAudioRepository = MockAudioRepository(
            isRecording = false,
            startRecordingResult = Result.Error(RuntimeException("Microphone unavailable"))
        )
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(apiKey = TestConstants.MOCK_API_KEY)
        )
        val useCase = StartRecordingUseCase(mockAudioRepository, mockSettingsRepository, mockk(relaxed = true))
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertEquals("Microphone unavailable", result.exception.message)
        assertTrue(mockAudioRepository.startRecordingCalled)
    }
    
    @Test
    fun `startRecording should handle permission denied error`() = runTest {
        val mockAudioRepository = MockAudioRepository(
            isRecording = false,
            startRecordingResult = Result.Error(SecurityException("Permission denied"))
        )
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(apiKey = TestConstants.MOCK_API_KEY)
        )
        val useCase = StartRecordingUseCase(mockAudioRepository, mockSettingsRepository, mockk(relaxed = true))
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is SecurityException)
        assertEquals("Permission denied", result.exception.message)
    }
    
    @Test
    fun `startRecording should handle audio hardware unavailable`() = runTest {
        val mockAudioRepository = MockAudioRepository(
            isRecording = false,
            startRecordingResult = Result.Error(IllegalStateException("Audio hardware not available"))
        )
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(apiKey = TestConstants.MOCK_API_KEY)
        )
        val useCase = StartRecordingUseCase(mockAudioRepository, mockSettingsRepository, mockk(relaxed = true))
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertEquals("Audio hardware not available", result.exception.message)
    }
    
    @Test
    fun `startRecording should handle null API key`() = runTest {
        val mockAudioRepository = MockAudioRepository(isRecording = false)
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings() // Default constructor has empty API key
        )
        val useCase = StartRecordingUseCase(mockAudioRepository, mockSettingsRepository, mockk(relaxed = true))
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is IllegalStateException)
        assertEquals("API key not configured", result.exception.message)
    }
    
    @Test
    fun `startRecording should handle API key with only newlines and tabs`() = runTest {
        val mockAudioRepository = MockAudioRepository(isRecording = false)
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(apiKey = "\n\t\r")
        )
        val useCase = StartRecordingUseCase(mockAudioRepository, mockSettingsRepository, mockk(relaxed = true))
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertTrue(result.exception is IllegalStateException)
        assertEquals("API key not configured", result.exception.message)
    }
    
    @Test
    fun `startRecording should succeed with API key containing valid characters and whitespace`() = runTest {
        val mockAudioRepository = MockAudioRepository(isRecording = false)
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(apiKey = " sk-validkey123456789 ")
        )
        val useCase = StartRecordingUseCase(mockAudioRepository, mockSettingsRepository, mockk(relaxed = true))
        
        val result = useCase()
        
        assertTrue(result is Result.Success)
        assertTrue(mockAudioRepository.startRecordingCalled)
    }
    
    @Test
    fun `startRecording should handle repository initialization failure`() = runTest {
        val mockAudioRepository = MockAudioRepository(
            isRecording = false,
            startRecordingResult = Result.Error(IllegalStateException("Repository not initialized"))
        )
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(apiKey = TestConstants.MOCK_API_KEY)
        )
        val useCase = StartRecordingUseCase(mockAudioRepository, mockSettingsRepository, mockk(relaxed = true))
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertEquals("Repository not initialized", result.exception.message)
    }
    
    @Test
    fun `startRecording should handle concurrent access attempts`() = runTest {
        val mockAudioRepository = MockAudioRepository(
            isRecording = false,
            startRecordingResult = Result.Error(IllegalStateException("Concurrent access detected"))
        )
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(apiKey = TestConstants.MOCK_API_KEY)
        )
        val useCase = StartRecordingUseCase(mockAudioRepository, mockSettingsRepository, mockk(relaxed = true))
        
        val result = useCase()
        
        assertTrue(result is Result.Error)
        assertEquals("Concurrent access detected", result.exception.message)
    }
    
    @Test
    fun `startRecording should succeed after previous failure`() = runTest {
        val mockAudioRepository = MockAudioRepository(isRecording = false)
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(apiKey = TestConstants.MOCK_API_KEY)
        )
        val useCase = StartRecordingUseCase(mockAudioRepository, mockSettingsRepository, mockk(relaxed = true))
        
        // First call fails
        mockAudioRepository.startRecordingResult = Result.Error(RuntimeException("First failure"))
        val firstResult = useCase()
        assertTrue(firstResult is Result.Error)
        
        // Second call succeeds
        mockAudioRepository.startRecordingResult = Result.Success(Unit)
        val secondResult = useCase()
        assertTrue(secondResult is Result.Success)
    }
    
    @Test
    fun `startRecording should handle very long API key`() = runTest {
        val mockAudioRepository = MockAudioRepository(isRecording = false)
        val longApiKey = TestConstants.MOCK_API_KEY + "a".repeat(1000)
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(apiKey = longApiKey)
        )
        val useCase = StartRecordingUseCase(mockAudioRepository, mockSettingsRepository, mockk(relaxed = true))
        
        val result = useCase()
        
        assertTrue(result is Result.Success)
        assertTrue(mockAudioRepository.startRecordingCalled)
    }
    
    @Test
    fun `startRecording should handle API key with special characters`() = runTest {
        val mockAudioRepository = MockAudioRepository(isRecording = false)
        val specialCharApiKey = TestConstants.MOCK_API_KEY
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(apiKey = specialCharApiKey)
        )
        val useCase = StartRecordingUseCase(mockAudioRepository, mockSettingsRepository, mockk(relaxed = true))
        
        val result = useCase()
        
        assertTrue(result is Result.Success)
        assertTrue(mockAudioRepository.startRecordingCalled)
    }
    
    @Test
    fun `startRecording should handle all recording states correctly`() = runTest {
        val mockSettingsRepository = MockSettingsRepository(
            appSettings = AppSettings(apiKey = TestConstants.MOCK_API_KEY)
        )
        
        // Test when not recording
        val notRecordingRepo = MockAudioRepository(isRecording = false)
        val useCase1 = StartRecordingUseCase(notRecordingRepo, mockSettingsRepository, mockk(relaxed = true))
        val result1 = useCase1()
        assertTrue(result1 is Result.Success)
        
        // Test when already recording
        val recordingRepo = MockAudioRepository(isRecording = true)
        val useCase2 = StartRecordingUseCase(recordingRepo, mockSettingsRepository, mockk(relaxed = true))
        val result2 = useCase2()
        assertTrue(result2 is Result.Error)
        assertEquals("Already recording", (result2 as Result.Error).exception.message)
    }
    
    private class MockAudioRepository(
        private val isRecording: Boolean,
        var startRecordingResult: Result<Unit> = Result.Success(Unit)
    ) : AudioRepository {
        var startRecordingCalled = false
        
        override val recordingState: Flow<RecordingState> = flowOf(RecordingState.Idle)
        
        override suspend fun startRecording(): Result<Unit> {
            startRecordingCalled = true
            return startRecordingResult
        }
        
        override suspend fun stopRecording(): Result<AudioFile> {
            return Result.Success(AudioFile("/mock/path", 1000L, 1024L))
        }
        
        override suspend fun cancelRecording(): Result<Unit> {
            return Result.Success(Unit)
        }
        
        override fun isRecording(): Boolean = isRecording
        
        override suspend fun getLastRecording(): AudioFile? = null
    }
    
    private class MockSettingsRepository(
        private val appSettings: AppSettings
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

        // Statistics preferences
        override suspend fun updateStatisticsEnabled(enabled: Boolean): Result<Unit> = Result.Success(Unit)
        override suspend fun updateHistoryRetentionDays(days: Int): Result<Unit> = Result.Success(Unit)
        override suspend fun updateExportFormat(format: ExportFormat): Result<Unit> = Result.Success(Unit)
        override suspend fun updateDashboardMetricsVisible(metrics: Set<String>): Result<Unit> = Result.Success(Unit)
        override suspend fun updateChartTimeRange(range: ChartTimeRange): Result<Unit> = Result.Success(Unit)
        override suspend fun updateNotificationsEnabled(enabled: Boolean): Result<Unit> = Result.Success(Unit)
        override suspend fun updateDataPrivacyMode(mode: DataPrivacyMode): Result<Unit> = Result.Success(Unit)
        override suspend fun updateAllowDataImport(allow: Boolean): Result<Unit> = Result.Success(Unit)
    }
}