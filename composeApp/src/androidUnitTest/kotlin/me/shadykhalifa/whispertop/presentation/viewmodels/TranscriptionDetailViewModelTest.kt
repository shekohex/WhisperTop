package me.shadykhalifa.whispertop.presentation.viewmodels

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.presentation.utils.ViewModelErrorHandler
import me.shadykhalifa.whispertop.utils.Result
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TranscriptionDetailViewModelTest {
    
    private lateinit var viewModel: TranscriptionDetailViewModel
    private lateinit var mockRepository: TranscriptionHistoryRepository
    private lateinit var mockErrorHandler: ViewModelErrorHandler
    private val testDispatcher = StandardTestDispatcher()
    
    private val sampleTranscription = TranscriptionHistoryItem(
        id = "test-id",
        text = "This is a test transcription with a URL https://example.com and an email test@example.com",
        timestamp = 1234567890000L,
        duration = 30.5f,
        audioFilePath = "/path/to/audio.wav",
        confidence = 0.95f,
        customPrompt = "Custom prompt for testing",
        temperature = 0.3f,
        language = "en",
        model = "whisper-1"
    )
    
    @Before
    fun setup() {
        mockRepository = mockk()
        mockErrorHandler = mockk()
        
        // Mock error handler to return empty error info
        every { mockErrorHandler.handleError(any(), any(), any()) } returns mockk(relaxed = true)
        every { mockErrorHandler.handleErrorWithNotification(any(), any()) } returns mockk(relaxed = true)
        
        viewModel = TranscriptionDetailViewModel(
            transcriptionHistoryRepository = mockRepository,
            errorHandler = mockErrorHandler
        )
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
    }
    
    @Test
    fun `loadTranscription success updates state with transcription`() = runTest(testDispatcher) {
        // Given
        coEvery { mockRepository.getTranscription("test-id") } returns Result.Success(sampleTranscription)
        
        // When
        viewModel.loadTranscription("test-id")
        
        // Then
        val state = viewModel.uiState.value
        assertEquals(sampleTranscription, state.transcription)
        assertEquals(false, state.isLoading)
        assertNull(state.error)
    }
    
    @Test
    fun `loadTranscription not found updates state with error`() = runTest(testDispatcher) {
        // Given
        coEvery { mockRepository.getTranscription("invalid-id") } returns Result.Success(null)
        
        // When
        viewModel.loadTranscription("invalid-id")
        
        // Then
        val state = viewModel.uiState.value
        assertNull(state.transcription)
        assertEquals(false, state.isLoading)
        assertEquals("Transcription not found", state.error)
    }
    
    @Test
    fun `loadTranscription failure updates state with error and calls error handler`() = runTest(testDispatcher) {
        // Given
        val exception = RuntimeException("Network error")
        coEvery { mockRepository.getTranscription("test-id") } returns Result.Error(exception)
        
        // When
        viewModel.loadTranscription("test-id")
        
        // Then
        val state = viewModel.uiState.value
        assertNull(state.transcription)
        assertEquals(false, state.isLoading)
        assertEquals("Failed to load transcription", state.error)
        
        verify { mockErrorHandler.handleError(exception, "Loading transcription") }
    }
    
    @Test
    fun `loadTranscription sets loading state during operation`() = runTest(testDispatcher) {
        // Given
        coEvery { mockRepository.getTranscription("test-id") } coAnswers {
            // Simulate delay to check loading state
            viewModel.uiState.value.let { state ->
                assertTrue(state.isLoading)
                assertNull(state.error)
            }
            Result.Success(sampleTranscription)
        }
        
        // When
        viewModel.loadTranscription("test-id")
        
        // Then loading state should be false after completion
        assertEquals(false, viewModel.uiState.value.isLoading)
    }
    
    @Test
    fun `deleteTranscription success triggers success message and navigation`() = runTest(testDispatcher) {
        // Given
        viewModel.loadTranscription("test-id") // Load transcription first
        coEvery { mockRepository.getTranscription("test-id") } returns Result.Success(sampleTranscription)
        coEvery { mockRepository.deleteTranscription("test-id") } returns Result.Success(Unit)
        
        viewModel.loadTranscription("test-id")
        
        // When
        viewModel.deleteTranscription()
        
        // Then
        val event = viewModel.uiEvents.value
        assertTrue(event is TranscriptionDetailUiEvent.ShowMessage)
        assertEquals("Transcription deleted", (event as TranscriptionDetailUiEvent.ShowMessage).message)
        
        // Verify repository call
        coVerify { mockRepository.deleteTranscription("test-id") }
    }
    
    @Test
    fun `deleteTranscription failure triggers error message and calls error handler`() = runTest(testDispatcher) {
        // Given
        viewModel.loadTranscription("test-id")
        coEvery { mockRepository.getTranscription("test-id") } returns Result.Success(sampleTranscription)
        
        val exception = RuntimeException("Delete failed")
        coEvery { mockRepository.deleteTranscription("test-id") } returns Result.Error(exception)
        
        viewModel.loadTranscription("test-id")
        
        // When
        viewModel.deleteTranscription()
        
        // Then
        val event = viewModel.uiEvents.value
        assertTrue(event is TranscriptionDetailUiEvent.ShowError)
        assertEquals("Failed to delete transcription", (event as TranscriptionDetailUiEvent.ShowError).message)
        
        verify { mockErrorHandler.handleError(exception, "Deleting transcription") }
    }
    
    @Test
    fun `deleteTranscription with no loaded transcription does nothing`() = runTest(testDispatcher) {
        // Given - no transcription loaded
        
        // When
        viewModel.deleteTranscription()
        
        // Then
        assertNull(viewModel.uiEvents.value)
        coVerify(exactly = 0) { mockRepository.deleteTranscription(any()) }
    }
    
    @Test
    fun `shareTranscription triggers share text event`() = runTest(testDispatcher) {
        // Given
        coEvery { mockRepository.getTranscription("test-id") } returns Result.Success(sampleTranscription)
        viewModel.loadTranscription("test-id")
        
        // When
        viewModel.shareTranscription()
        
        // Then
        val event = viewModel.uiEvents.value
        assertTrue(event is TranscriptionDetailUiEvent.ShareText)
        assertEquals(sampleTranscription.text, (event as TranscriptionDetailUiEvent.ShareText).text)
    }
    
    @Test
    fun `shareTranscription with no loaded transcription does nothing`() = runTest(testDispatcher) {
        // Given - no transcription loaded
        
        // When
        viewModel.shareTranscription()
        
        // Then
        assertNull(viewModel.uiEvents.value)
    }
    
    @Test
    fun `copyToClipboard triggers copy to clipboard event`() = runTest(testDispatcher) {
        // Given
        coEvery { mockRepository.getTranscription("test-id") } returns Result.Success(sampleTranscription)
        viewModel.loadTranscription("test-id")
        
        // When
        viewModel.copyToClipboard()
        
        // Then
        val event = viewModel.uiEvents.value
        assertTrue(event is TranscriptionDetailUiEvent.CopyToClipboard)
        assertEquals(sampleTranscription.text, (event as TranscriptionDetailUiEvent.CopyToClipboard).text)
    }
    
    @Test
    fun `copyToClipboard with no loaded transcription does nothing`() = runTest(testDispatcher) {
        // Given - no transcription loaded
        
        // When
        viewModel.copyToClipboard()
        
        // Then
        assertNull(viewModel.uiEvents.value)
    }
    
    @Test
    fun `clearEvent sets uiEvents to null`() = runTest(testDispatcher) {
        // Given
        coEvery { mockRepository.getTranscription("test-id") } returns Result.Success(sampleTranscription)
        viewModel.loadTranscription("test-id")
        viewModel.shareTranscription() // Trigger an event
        
        assertNotNull(viewModel.uiEvents.value)
        
        // When
        viewModel.clearEvent()
        
        // Then
        assertNull(viewModel.uiEvents.value)
    }
    
    @Test
    fun `initial state is correct`() {
        // When - ViewModel is created
        
        // Then
        val state = viewModel.uiState.value
        assertNull(state.transcription)
        assertEquals(false, state.isLoading)
        assertNull(state.error)
        assertNull(viewModel.uiEvents.value)
    }
}