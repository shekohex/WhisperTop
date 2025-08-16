package me.shadykhalifa.whispertop.domain.managers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.shadykhalifa.whispertop.domain.models.AudioFile
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

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingManagerTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var recordingManager: RecordingManager
    private lateinit var mockAudioRepository: MockAudioRepository
    private lateinit var mockTranscriptionRepository: MockTranscriptionRepository
    
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockAudioRepository = MockAudioRepository()
        mockTranscriptionRepository = MockTranscriptionRepository()
        
        startKoin {
            modules(
                module {
                    single<AudioRepository> { mockAudioRepository }
                    single<TranscriptionRepository> { mockTranscriptionRepository }
                }
            )
        }
        
        recordingManager = RecordingManager()
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
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Recording)
        assertTrue(state.startTime > 0)
        assertEquals(0L, state.duration)
        assertTrue(mockAudioRepository.startRecordingCalled)
    }
    
    @Test
    fun testStartRecordingFromNonIdleState() = runTest {
        // Set state to Recording first
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Try to start recording again
        val callsBefore = mockAudioRepository.startRecordingCallCount
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
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
        testDispatcher.scheduler.advanceUntilIdle()
        
        recordingManager.stopRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Success)
        assertEquals(testAudioFile, state.audioFile)
        assertEquals(testTranscription, state.transcription)
        assertTrue(mockAudioRepository.stopRecordingCalled)
    }
    
    @Test
    fun testStopRecordingWithNullAudioFile() = runTest {
        mockAudioRepository.stopRecordingResult = null
        
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        recordingManager.stopRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Error)
        assertTrue(state.throwable is IllegalStateException)
        assertEquals("Failed to save recording", state.throwable.message)
    }
    
    @Test
    fun testStopRecordingTranscriptionError() = runTest {
        val testAudioFile = AudioFile("/test.wav", 5000L, 1024L)
        val testError = RuntimeException("API Error")
        
        mockAudioRepository.stopRecordingResult = testAudioFile
        mockTranscriptionRepository.transcribeResult = Result.Error(testError)
        
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        recordingManager.stopRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Error)
        assertEquals(testError, state.throwable)
        assertTrue(state.retryable)
    }
    
    @Test
    fun testCancelRecording() = runTest {
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        recordingManager.cancelRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Idle)
        assertTrue(mockAudioRepository.cancelRecordingCalled)
    }
    
    @Test
    fun testRetryFromError() = runTest {
        // Create an error state
        mockAudioRepository.startRecordingException = RuntimeException("Test error")
        
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val errorState = recordingManager.recordingState.value
        assertTrue(errorState is RecordingState.Error)
        assertTrue(errorState.retryable)
        
        // Retry from error
        recordingManager.retryFromError()
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Idle)
    }
    
    @Test
    fun testRetryFromNonRetryableError() = runTest {
        // Manually set a non-retryable error state
        val testError = SecurityException("Permission denied")
        recordingManager.cancelRecording() // Reset to idle first
        
        // We need to simulate a non-retryable error
        mockAudioRepository.startRecordingException = testError
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Retry should not change state for non-retryable errors
        val stateBefore = recordingManager.recordingState.value
        recordingManager.retryFromError()
        val stateAfter = recordingManager.recordingState.value
        
        assertEquals(stateBefore, stateAfter)
    }
    
    @Test
    fun testResetToIdle() = runTest {
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        recordingManager.resetToIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        
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
        testDispatcher.scheduler.advanceUntilIdle()
        
        recordingManager.stopRecording()
        
        // Advance time partially to see processing state
        testDispatcher.scheduler.advanceTimeBy(100L)
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Processing)
        assertTrue(state.progress >= 0f)
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
        
        override suspend fun validateApiKey(apiKey: String): Result<Boolean> {
            return Result.Success(true)
        }
        
        override suspend fun isConfigured(): Boolean = true
    }
}