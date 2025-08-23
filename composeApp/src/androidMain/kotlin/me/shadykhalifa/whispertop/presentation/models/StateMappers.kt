package me.shadykhalifa.whispertop.presentation.models

import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.ErrorClassifier
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.models.ServiceConnectionState
import me.shadykhalifa.whispertop.domain.models.TranscriptionError
import me.shadykhalifa.whispertop.domain.repositories.ServiceStateRepository
import me.shadykhalifa.whispertop.domain.usecases.WorkflowState
import me.shadykhalifa.whispertop.presentation.AudioRecordingUiState

fun RecordingState.toRecordingStatus(): RecordingStatus {
    return when (this) {
        RecordingState.Idle -> RecordingStatus.Idle
        is RecordingState.Recording -> RecordingStatus.Recording
        is RecordingState.Processing -> RecordingStatus.Processing
        is RecordingState.Success -> RecordingStatus.Success
        is RecordingState.Error -> RecordingStatus.Error
    }
}

fun ServiceStateRepository.RecordingState.toRecordingStatus(): RecordingStatus {
    return when (this) {
        ServiceStateRepository.RecordingState.IDLE -> RecordingStatus.Idle
        ServiceStateRepository.RecordingState.RECORDING -> RecordingStatus.Recording
        ServiceStateRepository.RecordingState.PAUSED -> RecordingStatus.Processing
        ServiceStateRepository.RecordingState.PROCESSING -> RecordingStatus.Processing
    }
}

fun WorkflowState.toRecordingStatus(): RecordingStatus {
    return when (this) {
        is WorkflowState.Idle -> RecordingStatus.Idle
        is WorkflowState.ServiceReady -> RecordingStatus.Idle
        is WorkflowState.PermissionDenied -> RecordingStatus.Error
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
    val errorMessage = when {
        recordingStatus == RecordingStatus.Error && this is WorkflowState.Error -> {
            this.error.toDisplayMessage()
        }
        this is WorkflowState.PermissionDenied -> {
            "Required permissions not granted: ${deniedPermissions.joinToString(", ")}"
        }
        else -> null
    }
    
    return currentUiState.copy(
        status = recordingStatus,
        transcription = transcriptionResult,
        isLoading = isLoading,
        errorMessage = errorMessage
    )
}