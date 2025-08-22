package me.shadykhalifa.whispertop.presentation.models

import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.models.ServiceConnectionState
import me.shadykhalifa.whispertop.domain.models.TranscriptionError
import me.shadykhalifa.whispertop.domain.usecases.WorkflowState
import me.shadykhalifa.whispertop.presentation.AudioRecordingUiState
import org.junit.Assert.*
import org.junit.Test

class StateMappersTest {

    @Test
    fun `WorkflowState_Idle maps to RecordingStatus_Idle`() {
        val workflowState = WorkflowState.Idle
        val result = workflowState.toRecordingStatus()
        
        assertEquals(RecordingStatus.Idle, result)
    }

    @Test
    fun `WorkflowState_Recording maps to RecordingStatus_Recording`() {
        val workflowState = WorkflowState.Recording
        val result = workflowState.toRecordingStatus()
        
        assertEquals(RecordingStatus.Recording, result)
    }

    @Test
    fun `WorkflowState_Processing maps to RecordingStatus_Processing`() {
        val workflowState = WorkflowState.Processing(0.75f)
        val result = workflowState.toRecordingStatus()
        
        assertEquals(RecordingStatus.Processing, result)
    }

    @Test
    fun `WorkflowState_InsertingText maps to RecordingStatus_InsertingText`() {
        val workflowState = WorkflowState.InsertingText
        val result = workflowState.toRecordingStatus()
        
        assertEquals(RecordingStatus.InsertingText, result)
    }

    @Test
    fun `WorkflowState_Success maps to RecordingStatus_Success`() {
        val workflowState = WorkflowState.Success("Hello world", true)
        val result = workflowState.toRecordingStatus()
        
        assertEquals(RecordingStatus.Success, result)
    }

    @Test
    fun `WorkflowState_Error maps to RecordingStatus_Error`() {
        val error = TranscriptionError.NetworkError(RuntimeException("Network failed"))
        val workflowState = WorkflowState.Error(error, retryable = true)
        val result = workflowState.toRecordingStatus()
        
        assertEquals(RecordingStatus.Error, result)
    }

    @Test
    fun `WorkflowState_Success creates TranscriptionDisplayModel`() {
        val transcription = "Hello world"
        val textInserted = true
        val workflowState = WorkflowState.Success(transcription, textInserted)
        val result = workflowState.toTranscriptionDisplayModel()
        
        assertNotNull(result)
        assertEquals(transcription, result!!.fullText)
        assertEquals(transcription, result.previewText) // Short text should be the same
        assertEquals(TextInsertionStatus.Completed, result.insertionStatus)
        assertNull(result.errorMessage)
    }

    @Test
    fun `WorkflowState_Success with failed insertion creates correct TranscriptionDisplayModel`() {
        val transcription = "Hello world"
        val textInserted = false
        val workflowState = WorkflowState.Success(transcription, textInserted)
        val result = workflowState.toTranscriptionDisplayModel()
        
        assertNotNull(result)
        assertEquals(transcription, result!!.fullText)
        assertEquals(TextInsertionStatus.Failed, result.insertionStatus)
    }

    @Test
    fun `Non-success WorkflowState returns null TranscriptionDisplayModel`() {
        val workflowStates = listOf(
            WorkflowState.Idle,
            WorkflowState.Recording,
            WorkflowState.Processing(0.5f),
            WorkflowState.InsertingText,
            WorkflowState.Error(TranscriptionError.ApiKeyMissing(), true)
        )
        
        workflowStates.forEach { state ->
            assertNull(state.toTranscriptionDisplayModel())
        }
    }

    @Test
    fun `AudioFile maps to AudioFilePresentationModel correctly`() {
        val audioFile = AudioFile(
            path = "/storage/recordings/test.wav",
            durationMs = 5000L,
            sizeBytes = 1024L
        )
        
        val result = audioFile.toAudioFilePresentationModel()
        
        assertNotNull(result)
        assertEquals(audioFile.path, result!!.path)
        assertTrue(result.durationText.isNotEmpty())
        assertTrue(result.fileSizeText.isNotEmpty())
        assertTrue(result.isValid)
    }

    @Test
    fun `Null AudioFile maps to null AudioFilePresentationModel`() {
        val audioFile: AudioFile? = null
        val result = audioFile.toAudioFilePresentationModel()
        
        assertNull(result)
    }

    @Test
    fun `TranscriptionError maps to user-friendly display message`() {
        val testCases = listOf(
            TranscriptionError.ApiKeyMissing() to "OpenAI API key",
            TranscriptionError.NetworkError(RuntimeException()) to "internet connection",
            TranscriptionError.AuthenticationError() to "API key",
            TranscriptionError.RateLimitError() to "wait",
            TranscriptionError.AccessibilityServiceNotEnabled() to "accessibility service"
        )
        
        testCases.forEach { (error, expectedContent) ->
            val result = error.toDisplayMessage()
            assertTrue(
                "Error message '$result' should contain '$expectedContent'",
                result.contains(expectedContent, ignoreCase = true)
            )
        }
    }

    @Test
    fun `WorkflowState_Idle transforms to UiState correctly`() {
        val currentUiState = createTestUiState()
        val workflowState = WorkflowState.Idle
        val result = workflowState.toUiState(currentUiState)
        
        assertEquals(RecordingStatus.Idle, result.recordingStatus)
        assertFalse(result.isLoading)
        assertNull(result.errorMessage)
        assertNull(result.transcriptionDisplayModel)
    }

    @Test
    fun `WorkflowState_Processing transforms to UiState with loading state`() {
        val currentUiState = createTestUiState()
        val workflowState = WorkflowState.Processing(0.5f)
        val result = workflowState.toUiState(currentUiState)
        
        assertEquals(RecordingStatus.Processing, result.recordingStatus)
        assertTrue(result.isLoading)
        assertNull(result.errorMessage)
    }

    @Test
    fun `WorkflowState_InsertingText transforms to UiState with loading state`() {
        val currentUiState = createTestUiState()
        val workflowState = WorkflowState.InsertingText
        val result = workflowState.toUiState(currentUiState)
        
        assertEquals(RecordingStatus.InsertingText, result.recordingStatus)
        assertTrue(result.isLoading)
        assertNull(result.errorMessage)
    }

    @Test
    fun `WorkflowState_Success transforms to UiState with transcription data`() {
        val currentUiState = createTestUiState()
        val transcription = "Hello world"
        val workflowState = WorkflowState.Success(transcription, true)
        val result = workflowState.toUiState(currentUiState)
        
        assertEquals(RecordingStatus.Success, result.recordingStatus)
        assertFalse(result.isLoading)
        assertNull(result.errorMessage)
        assertEquals(transcription, result.transcriptionResult)
        assertNotNull(result.transcriptionDisplayModel)
        assertEquals(transcription, result.transcriptionDisplayModel!!.fullText)
        assertEquals(TextInsertionStatus.Completed, result.transcriptionDisplayModel!!.insertionStatus)
    }

    @Test
    fun `WorkflowState_Error transforms to UiState with error message`() {
        val currentUiState = createTestUiState()
        val error = TranscriptionError.ApiKeyMissing()
        val workflowState = WorkflowState.Error(error, true)
        val result = workflowState.toUiState(currentUiState)
        
        assertEquals(RecordingStatus.Error, result.recordingStatus)
        assertFalse(result.isLoading)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("API key", ignoreCase = true))
    }

    @Test
    fun `WorkflowState transformation preserves other UiState properties`() {
        val currentUiState = createTestUiState().copy(
            serviceConnectionState = ServiceConnectionState.CONNECTED,
            permissionState = true,
            isServiceReady = true,
            showPermissionRationale = true,
            rationalePermissions = listOf("RECORD_AUDIO")
        )
        
        val workflowState = WorkflowState.Recording
        val result = workflowState.toUiState(currentUiState)
        
        // Verify preserved properties
        assertEquals(ServiceConnectionState.CONNECTED, result.serviceConnectionState)
        assertTrue(result.permissionState)
        assertTrue(result.isServiceReady)
        assertTrue(result.showPermissionRationale)
        assertEquals(listOf("RECORD_AUDIO"), result.rationalePermissions)
        
        // Verify transformed properties
        assertEquals(RecordingStatus.Recording, result.recordingStatus)
        assertFalse(result.isLoading)
    }

    private fun createTestUiState() = AudioRecordingUiState(
        serviceConnectionState = ServiceConnectionState.DISCONNECTED,
        recordingState = RecordingState.Idle,
        permissionState = false,
        isServiceReady = false,
        recordingStatus = RecordingStatus.Idle,
        isLoading = false,
        errorMessage = null,
        lastRecording = null,
        lastRecordingPresentation = null,
        transcriptionResult = null,
        transcriptionDisplayModel = null,
        transcriptionLanguage = null,
        showPermissionRationale = false,
        rationalePermissions = emptyList()
    )
}