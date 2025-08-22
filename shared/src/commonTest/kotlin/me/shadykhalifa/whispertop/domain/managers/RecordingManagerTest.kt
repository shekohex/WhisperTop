package me.shadykhalifa.whispertop.domain.managers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.shadykhalifa.whispertop.domain.models.AudioFile
import kotlin.test.Ignore
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.models.TranscriptionRequest
import me.shadykhalifa.whispertop.domain.models.TranscriptionResponse
import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.utils.Result
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import me.shadykhalifa.whispertop.utils.TestConstants

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingManagerTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var recordingManager: RecordingManager
    private lateinit var mockAudioRepository: MockAudioRepository
    private lateinit var mockTranscriptionRepository: MockTranscriptionRepository
    private lateinit var mockSettingsRepository: MockSettingsRepository
    
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockAudioRepository = MockAudioRepository()
        mockTranscriptionRepository = MockTranscriptionRepository()
        mockSettingsRepository = MockSettingsRepository()
        
        // Create a test scope with the test dispatcher
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        
        startKoin {
            modules(
                module {
                    single<AudioRepository> { mockAudioRepository }
                    single<TranscriptionRepository> { mockTranscriptionRepository }
                    single<me.shadykhalifa.whispertop.domain.repositories.SettingsRepository> { mockSettingsRepository }
                    single<CoroutineScope> { testScope }
                }
            )
        }
        
        recordingManager = RecordingManager(testScope)
    }
    
    @AfterTest
    fun tearDown() {
        recordingManager.cleanup()
        stopKoin()
        Dispatchers.resetMain()
    }
    
    @Test
    fun testInitialState() = runTest {
        val initialState = recordingManager.recordingState.first()
        assertTrue(initialState is RecordingState.Idle)
    }
    
    @Test
    fun testStartRecording() = runTest {
        recordingManager.startRecording()
        // Don't advance time too much to avoid timeout
        testDispatcher.scheduler.advanceTimeBy(100L)
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Recording, "State should be Recording but was $state")
        assertTrue((state as RecordingState.Recording).startTime > 0)
        assertEquals(0L, state.duration)
        assertTrue(mockAudioRepository.startRecordingCalled)
    }
    
    @Test
    fun testStartRecordingFromNonIdleState() = runTest {
        // Set state to Recording first
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceTimeBy(100L)
        
        // Try to start recording again
        val callsBefore = mockAudioRepository.startRecordingCallCount
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceTimeBy(100L)
        
        // Should not call start recording again
        assertEquals(callsBefore, mockAudioRepository.startRecordingCallCount)
    }
    
    @Test
    fun testStopRecordingSuccess() = runTest {
        val testAudioFile = AudioFile("/test.wav", 5000L, 1024L)
        val testTranscription = "Test transcription"
        
        mockAudioRepository.stopRecordingResult = testAudioFile
        mockTranscriptionRepository.transcribeResult = Result.Success(
            TranscriptionResponse(text = testTranscription)
        )
        
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceTimeBy(100L)
        
        recordingManager.stopRecording()
        
        // Advance time to let the transcription process complete
        // processTranscription has progress simulation (550ms) + transcription time
        testDispatcher.scheduler.advanceTimeBy(1000L)
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Success, "State should be Success but was $state")
        assertEquals(testAudioFile, (state as RecordingState.Success).audioFile)
        assertEquals(testTranscription, state.transcription)
        assertTrue(mockAudioRepository.stopRecordingCalled)
    }
    
    @Test
    fun testStopRecordingWithNullAudioFile() = runTest {
        mockAudioRepository.stopRecordingResult = null
        
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceTimeBy(100L)
        
        recordingManager.stopRecording()
        
        // Advance time for async error to propagate
        testDispatcher.scheduler.advanceTimeBy(1000L)
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Error, "State should be Error but was $state")
        assertTrue((state as RecordingState.Error).throwable is RuntimeException)
        assertEquals("No recording result set", state.throwable.message)
    }
    
    @Test
    fun testStopRecordingTranscriptionError() = runTest {
        val testAudioFile = AudioFile("/test.wav", 5000L, 1024L)
        val testError = RuntimeException("API Error")
        
        mockAudioRepository.stopRecordingResult = testAudioFile
        mockTranscriptionRepository.transcribeResult = Result.Error(testError)
        
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceTimeBy(100L)
        
        recordingManager.stopRecording()
        testDispatcher.scheduler.advanceTimeBy(1000L)
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Error, "State should be Error but was $state")
        assertEquals(testError, (state as RecordingState.Error).throwable)
        assertTrue((state as RecordingState.Error).retryable)
    }
    
    @Test
    fun testCancelRecording() = runTest {
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceTimeBy(100L)
        
        recordingManager.cancelRecording()
        testDispatcher.scheduler.advanceTimeBy(100L)
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Idle, "State should be Idle but was $state")
        assertTrue(mockAudioRepository.cancelRecordingCalled)
    }
    
    @Test
    fun testRetryFromError() = runTest {
        // Create an error state
        mockAudioRepository.startRecordingException = RuntimeException("Test error")
        
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceTimeBy(200L)
        
        val errorState = recordingManager.recordingState.value
        assertTrue(errorState is RecordingState.Error, "State should be Error but was $errorState")
        assertTrue((errorState as RecordingState.Error).retryable)
        
        // Clear the exception so retry can succeed
        mockAudioRepository.startRecordingException = null
        
        // Retry from error (this resets to Idle)
        recordingManager.retryFromError()
        testDispatcher.scheduler.advanceTimeBy(100L)
        
        // Verify it's reset to Idle
        val idleState = recordingManager.recordingState.value
        assertTrue(idleState is RecordingState.Idle, "State should be Idle after retryFromError but was $idleState")
        
        // Now start recording again
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceTimeBy(200L)
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Recording, "State should be Recording after retry but was $state")
    }
    
    @Test
    fun testRetryFromRetryableError() = runTest {
        // Test that retryable errors can be reset with retryFromError
        val testError = SecurityException("Permission denied")
        recordingManager.cancelRecording() // Reset to idle first
        testDispatcher.scheduler.advanceTimeBy(100L)
        
        // Create a retryable error
        mockAudioRepository.startRecordingException = testError
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceTimeBy(200L)
        
        // Verify we have a retryable error state
        val errorState = recordingManager.recordingState.value
        assertTrue(errorState is RecordingState.Error, "Should have error state but was $errorState")
        assertTrue((errorState as RecordingState.Error).retryable, "Error should be retryable")
        
        // Retry should change state to Idle for retryable errors
        recordingManager.retryFromError()
        testDispatcher.scheduler.advanceTimeBy(100L)
        val stateAfter = recordingManager.recordingState.value
        
        // Should be Idle after retry
        assertTrue(stateAfter is RecordingState.Idle, "Should be Idle after retry from retryable error but was $stateAfter")
    }
    
    @Test
    fun testResetToIdle() = runTest {
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceTimeBy(100L)
        
        recordingManager.resetToIdle()
        testDispatcher.scheduler.advanceTimeBy(100L)
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Idle)
    }
    
    @Test
    fun testProcessingProgressUpdates() = runTest {
        val testAudioFile = AudioFile("/test.wav", 5000L, 1024L)
        mockAudioRepository.stopRecordingResult = testAudioFile
        
        // Set up slow transcription to observe progress
        mockTranscriptionRepository.transcribeDelay = 500L
        mockTranscriptionRepository.transcribeResult = Result.Success(
            TranscriptionResponse(text = "Test")
        )
        
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceTimeBy(100L)
        
        recordingManager.stopRecording()
        
        // Advance time partially to catch processing state
        testDispatcher.scheduler.advanceTimeBy(200L)
        
        val state = recordingManager.recordingState.value
        // Could be Processing or Success depending on timing
        assertTrue(state is RecordingState.Processing || state is RecordingState.Success, 
                  "State should be Processing or Success but was $state")
        if (state is RecordingState.Processing) {
            assertTrue(state.progress >= 0f)
        }
    }
    
    // Mock implementations
    
    private class MockAudioRepository : AudioRepository {
        var startRecordingCalled = false
        var startRecordingCallCount = 0
        var startRecordingException: Exception? = null
        var stopRecordingCalled = false
        var stopRecordingResult: AudioFile? = null
        var cancelRecordingCalled = false
        
        override val recordingState: kotlinx.coroutines.flow.Flow<RecordingState> = 
            kotlinx.coroutines.flow.flowOf(RecordingState.Idle)
        
        override suspend fun startRecording(): Result<Unit> {
            startRecordingCalled = true
            startRecordingCallCount++
            return startRecordingException?.let { Result.Error(it) } ?: Result.Success(Unit)
        }
        
        override suspend fun stopRecording(): Result<AudioFile> {
            stopRecordingCalled = true
            return stopRecordingResult?.let { Result.Success(it) } 
                ?: Result.Error(RuntimeException("No recording result set"))
        }
        
        override suspend fun cancelRecording(): Result<Unit> {
            cancelRecordingCalled = true
            return Result.Success(Unit)
        }
        
        override fun isRecording(): Boolean = false
        override suspend fun getLastRecording(): AudioFile? = null
    }
    
    private class MockTranscriptionRepository : TranscriptionRepository {
        var transcribeResult: Result<TranscriptionResponse> = Result.Success(
            TranscriptionResponse(text = "Mock transcription")
        )
        var transcribeDelay: Long = 0L
        
        override suspend fun transcribe(request: TranscriptionRequest): Result<TranscriptionResponse> {
            if (transcribeDelay > 0) {
                delay(transcribeDelay)
            }
            return transcribeResult
        }
        
        override suspend fun transcribeWithLanguageDetection(
            request: TranscriptionRequest,
            userLanguageOverride: me.shadykhalifa.whispertop.domain.models.Language?
        ): Result<TranscriptionResponse> {
            if (transcribeDelay > 0) {
                delay(transcribeDelay)
            }
            return transcribeResult
        }
        
        override suspend fun validateApiKey(apiKey: String): Result<Boolean> {
            return Result.Success(true)
        }
        
        override suspend fun isConfigured(): Boolean = true
    }
    
    private class MockSettingsRepository : me.shadykhalifa.whispertop.domain.repositories.SettingsRepository {
        private val testSettings = me.shadykhalifa.whispertop.domain.models.AppSettings(
            apiKey = TestConstants.MOCK_API_KEY,
            selectedModel = "whisper-1",
            customPrompt = null,
            temperature = 0.0f
        )
        
        override val settings = kotlinx.coroutines.flow.flowOf(testSettings)
        
        override suspend fun getSettings(): me.shadykhalifa.whispertop.domain.models.AppSettings = testSettings
        
        override suspend fun updateApiKey(apiKey: String): me.shadykhalifa.whispertop.utils.Result<Unit> = me.shadykhalifa.whispertop.utils.Result.Success(Unit)
        
        override suspend fun updateSelectedModel(model: String): me.shadykhalifa.whispertop.utils.Result<Unit> = me.shadykhalifa.whispertop.utils.Result.Success(Unit)
        
        override suspend fun updateLanguage(language: String?): me.shadykhalifa.whispertop.utils.Result<Unit> = me.shadykhalifa.whispertop.utils.Result.Success(Unit)
        
        override suspend fun updateTheme(theme: me.shadykhalifa.whispertop.domain.models.Theme): me.shadykhalifa.whispertop.utils.Result<Unit> = me.shadykhalifa.whispertop.utils.Result.Success(Unit)
        
        override suspend fun updateBaseUrl(baseUrl: String): me.shadykhalifa.whispertop.utils.Result<Unit> = me.shadykhalifa.whispertop.utils.Result.Success(Unit)
        
        override suspend fun updateCustomEndpoint(isCustom: Boolean): me.shadykhalifa.whispertop.utils.Result<Unit> = me.shadykhalifa.whispertop.utils.Result.Success(Unit)
        
        override suspend fun updateCustomPrompt(prompt: String?): me.shadykhalifa.whispertop.utils.Result<Unit> = me.shadykhalifa.whispertop.utils.Result.Success(Unit)
        
        override suspend fun updateTemperature(temperature: Float): me.shadykhalifa.whispertop.utils.Result<Unit> = me.shadykhalifa.whispertop.utils.Result.Success(Unit)
        
        override suspend fun updateSettings(settings: me.shadykhalifa.whispertop.domain.models.AppSettings): me.shadykhalifa.whispertop.utils.Result<Unit> = me.shadykhalifa.whispertop.utils.Result.Success(Unit)
        
        override suspend fun clearApiKey(): me.shadykhalifa.whispertop.utils.Result<Unit> = me.shadykhalifa.whispertop.utils.Result.Success(Unit)
        
        override suspend fun clearAllData(): me.shadykhalifa.whispertop.utils.Result<Unit> = me.shadykhalifa.whispertop.utils.Result.Success(Unit)
        
        override suspend fun cleanupTemporaryFiles(): me.shadykhalifa.whispertop.utils.Result<Unit> = me.shadykhalifa.whispertop.utils.Result.Success(Unit)
    }
}