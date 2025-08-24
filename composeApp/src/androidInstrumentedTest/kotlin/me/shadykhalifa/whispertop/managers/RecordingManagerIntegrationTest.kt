package me.shadykhalifa.whispertop.managers

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.shadykhalifa.whispertop.di.SharedModule
import me.shadykhalifa.whispertop.domain.managers.RecordingManager
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.models.TranscriptionResponse
import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.utils.Result
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RecordingManagerIntegrationTest : KoinTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private val recordingManager: RecordingManager by inject()
    private lateinit var mockAudioRepository: MockAudioRepository
    private lateinit var mockTranscriptionRepository: MockTranscriptionRepository
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockAudioRepository = MockAudioRepository()
        mockTranscriptionRepository = MockTranscriptionRepository()
        
        startKoin {
            androidContext(InstrumentationRegistry.getInstrumentation().targetContext)
            modules(
                SharedModule,
                module(override = true) {
                    single<AudioRepository> { mockAudioRepository }
                    single<TranscriptionRepository> { mockTranscriptionRepository }
                }
            )
        }
    }
    
    @After
    fun tearDown() {
        recordingManager.cleanup()
        stopKoin()
        Dispatchers.resetMain()
    }
    
    @Test
    fun testCompleteRecordingWorkflow() = runTest {
        val testAudioFile = AudioFile("/sdcard/test.wav", 5000L, 1024L)
        val testTranscription = "Hello world"
        
        mockAudioRepository.stopRecordingResult = testAudioFile
        mockTranscriptionRepository.transcribeResult = Result.success(
            TranscriptionResponse(text = testTranscription)
        )
        
        // Start recording
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        var state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Recording, "Expected Recording state after start")
        
        // Stop recording
        recordingManager.stopRecording()
        
        // Advance through processing
        testDispatcher.scheduler.advanceTimeBy(100L)
        state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Processing, "Expected Processing state during transcription")
        
        // Complete transcription
        testDispatcher.scheduler.advanceUntilIdle()
        state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Success, "Expected Success state after completion")
        assertEquals(testAudioFile, (state as RecordingState.Success).audioFile)
        assertEquals(testTranscription, state.transcription)
    }
    
    @Test
    fun testRecordingErrorHandling() = runTest {
        val testError = RuntimeException("Audio recording failed")
        mockAudioRepository.startRecordingException = testError
        
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Error, "Expected Error state after recording failure")
        assertEquals(testError, (state as RecordingState.Error).throwable)
        assertTrue(state.retryable)
    }
    
    @Test
    fun testTranscriptionErrorHandling() = runTest {
        val testAudioFile = AudioFile("/sdcard/test.wav", 5000L, 1024L)
        val testError = RuntimeException("Transcription API failed")
        
        mockAudioRepository.stopRecordingResult = testAudioFile
        mockTranscriptionRepository.transcribeResult = Result.failure(testError)
        
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        recordingManager.stopRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Error, "Expected Error state after transcription failure")
        assertEquals(testError, (state as RecordingState.Error).throwable)
        assertTrue(state.retryable)
    }
    
    @Test
    fun testCancellationWorkflow() = runTest {
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        var state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Recording)
        
        recordingManager.cancelRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Idle, "Expected Idle state after cancellation")
        assertTrue(mockAudioRepository.cancelRecordingCalled)
    }
    
    @Test
    fun testRetryAfterError() = runTest {
        // First, create an error
        val testError = RuntimeException("Network error")
        mockAudioRepository.startRecordingException = testError
        
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        var state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Error)
        assertTrue((state as RecordingState.Error).retryable)
        
        // Retry from error
        recordingManager.retryFromError()
        state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Idle, "Expected Idle state after retry")
        
        // Now fix the error and try again
        mockAudioRepository.startRecordingException = null
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        state = recordingManager.recordingState.value
        assertTrue(state is RecordingState.Recording, "Expected successful recording after retry")
    }
    
    @Test
    fun testDurationUpdates() = runTest {
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val initialState = recordingManager.recordingState.value as RecordingState.Recording
        val initialDuration = initialState.duration
        
        // Advance time to trigger duration updates
        testDispatcher.scheduler.advanceTimeBy(200L)
        
        val updatedState = recordingManager.recordingState.value as RecordingState.Recording
        assertTrue(updatedState.duration >= initialDuration, "Duration should increase over time")
    }
    
    @Test
    fun testStateFlowReactivity() = runTest {
        var stateChangeCount = 0
        val stateHistory = mutableListOf<RecordingState>()
        
        // Collect state changes
        val collectJob = kotlinx.coroutines.launch {
            recordingManager.recordingState.collect { state ->
                stateHistory.add(state)
                stateChangeCount++
            }
        }
        
        // Initial state
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(stateHistory.first() is RecordingState.Idle)
        
        // Start recording
        recordingManager.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Cancel recording
        recordingManager.cancelRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        collectJob.cancel()
        
        assertTrue(stateChangeCount >= 3, "Should have at least 3 state changes (Idle -> Recording -> Idle)")
        assertTrue(stateHistory.any { it is RecordingState.Recording }, "Should have Recording state")
        assertTrue(stateHistory.last() is RecordingState.Idle, "Should end with Idle state")
    }
    
    // Mock implementations for integration testing
    
    private class MockAudioRepository : AudioRepository {
        var startRecordingException: Exception? = null
        var stopRecordingResult: AudioFile? = null
        var cancelRecordingCalled = false
        
        override suspend fun startRecording() {
            startRecordingException?.let { throw it }
        }
        
        override suspend fun stopRecording(): AudioFile? = stopRecordingResult
        
        override suspend fun cancelRecording() {
            cancelRecordingCalled = true
        }
        
        override suspend fun getRecordingDuration(): Long = 0L
        override suspend fun isRecording(): Boolean = false
    }
    
    private class MockTranscriptionRepository : TranscriptionRepository {
        var transcribeResult: Result<TranscriptionResponse> = Result.success(
            TranscriptionResponse(text = "Mock transcription")
        )
        
        override suspend fun transcribeAudio(audioFile: AudioFile): Result<TranscriptionResponse> {
            return transcribeResult
        }
    }
}