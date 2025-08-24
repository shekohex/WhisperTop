package me.shadykhalifa.whispertop.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.presentation.utils.ViewModelErrorHandler
import me.shadykhalifa.whispertop.utils.Result

data class TranscriptionDetailUiState(
    val transcription: TranscriptionHistoryItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface TranscriptionDetailUiEvent {
    data class ShowMessage(val message: String) : TranscriptionDetailUiEvent
    data class ShowError(val message: String) : TranscriptionDetailUiEvent
    data class NavigateBack(val deleted: Boolean = false) : TranscriptionDetailUiEvent
    data class ShareText(val text: String) : TranscriptionDetailUiEvent
    data class CopyToClipboard(val text: String) : TranscriptionDetailUiEvent
}

class TranscriptionDetailViewModel(
    private val transcriptionHistoryRepository: TranscriptionHistoryRepository,
    private val errorHandler: ViewModelErrorHandler
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TranscriptionDetailUiState())
    val uiState: StateFlow<TranscriptionDetailUiState> = _uiState.asStateFlow()
    
    private val _uiEvents = MutableStateFlow<TranscriptionDetailUiEvent?>(null)
    val uiEvents: StateFlow<TranscriptionDetailUiEvent?> = _uiEvents.asStateFlow()
    
    fun loadTranscription(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                when (val result = transcriptionHistoryRepository.getTranscription(id)) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            transcription = result.data,
                            isLoading = false,
                            error = if (result.data == null) "Transcription not found" else null
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load transcription"
                        )
                        errorHandler.handleError(result.exception, "Loading transcription")
                    }
                    is Result.Loading -> {
                        // Loading state is handled by the isLoading flag
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load transcription"
                )
                errorHandler.handleError(e, "Loading transcription")
            }
        }
    }
    
    fun deleteTranscription() {
        val transcription = _uiState.value.transcription ?: return
        
        viewModelScope.launch {
            try {
                when (val result = transcriptionHistoryRepository.deleteTranscription(transcription.id)) {
                    is Result.Success -> {
                        _uiEvents.value = TranscriptionDetailUiEvent.ShowMessage("Transcription deleted")
                        _uiEvents.value = TranscriptionDetailUiEvent.NavigateBack(deleted = true)
                    }
                    is Result.Error -> {
                        _uiEvents.value = TranscriptionDetailUiEvent.ShowError("Failed to delete transcription")
                        errorHandler.handleError(result.exception, "Deleting transcription")
                    }
                    is Result.Loading -> {
                        // Loading handled by global loading state
                    }
                }
            } catch (e: Exception) {
                _uiEvents.value = TranscriptionDetailUiEvent.ShowError("Failed to delete transcription")
                errorHandler.handleError(e, "Deleting transcription")
            }
        }
    }
    
    fun shareTranscription() {
        val transcription = _uiState.value.transcription ?: return
        _uiEvents.value = TranscriptionDetailUiEvent.ShareText(transcription.text)
    }
    
    fun copyToClipboard() {
        val transcription = _uiState.value.transcription ?: return
        _uiEvents.value = TranscriptionDetailUiEvent.CopyToClipboard(transcription.text)
    }
    
    fun clearEvent() {
        _uiEvents.value = null
    }
}