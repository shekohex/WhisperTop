package me.shadykhalifa.whispertop.presentation.models

import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.ErrorClassifier
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.models.ServiceConnectionState
import me.shadykhalifa.whispertop.domain.models.TranscriptionError
import me.shadykhalifa.whispertop.domain.usecases.WorkflowState
import me.shadykhalifa.whispertop.presentation.AudioRecordingUiState

fun WorkflowState.toRecordingStatus(): RecordingStatus {
    return when (this) {
        is WorkflowState.Idle -> RecordingStatus.Idle
        is WorkflowState.Recording -> RecordingStatus.Recording
        is WorkflowState.Processing -> RecordingStatus.Processing
        is WorkflowState.InsertingText -> RecordingStatus.InsertingText
        is WorkflowState.Success -> RecordingStatus.Success
        is WorkflowState.Error -> RecordingStatus.Error
    }
}

fun WorkflowState.toTranscriptionDisplayModel(): TranscriptionDisplayModel? {
    return when (this) {
        is WorkflowState.Success -> transcription.toDisplayModel(
            insertionStatus = if (textInserted) TextInsertionStatus.Completed else TextInsertionStatus.Failed
        )
        else -> null
    }
}

fun AudioFile?.toAudioFilePresentationModel(): AudioFilePresentationModel? {
    return this?.toPresentationModel()
}

fun TranscriptionError.toDisplayMessage(): String {
    val errorInfo = ErrorClassifier.classifyError(this)
    return errorInfo.message
}

fun WorkflowState.toUiState(currentUiState: AudioRecordingUiState): AudioRecordingUiState {
    val recordingStatus = this.toRecordingStatus()
    val transcriptionResult = this.toTranscriptionDisplayModel()
    val isLoading = recordingStatus == RecordingStatus.Processing || recordingStatus == RecordingStatus.InsertingText
    val errorMessage = if (recordingStatus == RecordingStatus.Error) {
        (this as? WorkflowState.Error)?.error?.toDisplayMessage()
    } else null
    
    return currentUiState.copy(
        recordingStatus = recordingStatus,
        transcriptionResult = transcriptionResult?.fullText,
        transcriptionDisplayModel = transcriptionResult,
        isLoading = isLoading,
        errorMessage = errorMessage
    )
}