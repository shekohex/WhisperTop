package me.shadykhalifa.whispertop.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.domain.models.DateRange
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.ExportResult
import me.shadykhalifa.whispertop.domain.models.RetentionPolicy
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionDatabaseRepository
import me.shadykhalifa.whispertop.domain.services.ExportService
import me.shadykhalifa.whispertop.utils.Result
import me.shadykhalifa.whispertop.domain.models.DataSummary
import java.time.LocalDate

class DataExportViewModel(
    private val exportService: ExportService,
    private val transcriptionDatabaseRepository: TranscriptionDatabaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataExportUiState())
    val uiState: StateFlow<DataExportUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<DataExportUiEvent>()
    val uiEvents: SharedFlow<DataExportUiEvent> = _uiEvents.asSharedFlow()

    fun updateSelectedFormat(format: ExportFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun updateDateRange(startDate: LocalDate?, endDate: LocalDate?) {
        val dateRange = when {
            startDate != null && endDate != null -> DateRange(startDate, endDate)
            else -> DateRange.all()
        }
        _uiState.update { 
            it.copy(
                selectedDateRange = dateRange,
                customDateStart = startDate,
                customDateEnd = endDate
            ) 
        }
    }

    fun updateIncludeProtectedData(include: Boolean) {
        _uiState.update { it.copy(includeProtectedData = include) }
    }

    fun showFormatSelection() {
        _uiState.update { it.copy(showFormatDialog = true) }
    }

    fun hideFormatSelection() {
        _uiState.update { it.copy(showFormatDialog = false) }
    }

    fun showDateRangePicker() {
        _uiState.update { it.copy(showDateRangeDialog = true) }
    }

    fun hideDateRangePicker() {
        _uiState.update { it.copy(showDateRangeDialog = false) }
    }

    fun startExport() {
        val currentState = _uiState.value
        
        if (currentState.isExporting) return

        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isExporting = true, 
                    exportProgress = 0f,
                    exportError = null
                ) 
            }

            try {
                exportService.exportData(
                    format = currentState.selectedFormat,
                    dateRange = currentState.selectedDateRange,
                    includeProtectedData = currentState.includeProtectedData
                ).collect { result ->
                    when (result) {
                        is ExportResult.InProgress -> {
                            _uiState.update { 
                                it.copy(exportProgress = result.progress) 
                            }
                        }
                        is ExportResult.Success -> {
                            _uiState.update { 
                                it.copy(
                                    isExporting = false,
                                    exportProgress = 1.0f,
                                    lastExportResult = result
                                ) 
                            }
                            _uiEvents.emit(
                                DataExportUiEvent.ExportCompleted(
                                    format = currentState.selectedFormat,
                                    filePath = result.filePath,
                                    itemCount = result.itemCount
                                )
                            )
                        }
                        is ExportResult.Error -> {
                            _uiState.update { 
                                it.copy(
                                    isExporting = false,
                                    exportError = result.message
                                ) 
                            }
                            _uiEvents.emit(DataExportUiEvent.ShowError(result.message))
                        }
                    }
                }
            } catch (exception: Exception) {
                _uiState.update { 
                    it.copy(
                        isExporting = false,
                        exportError = exception.message ?: "Unknown error occurred"
                    ) 
                }
                _uiEvents.emit(
                    DataExportUiEvent.ShowError(
                        exception.message ?: "Export failed with unknown error"
                    )
                )
            }
        }
    }

    fun cancelExport() {
        _uiState.update { 
            it.copy(
                isExporting = false,
                exportProgress = 0f
            ) 
        }
        viewModelScope.launch {
            _uiEvents.emit(DataExportUiEvent.ShowMessage("Export cancelled"))
        }
    }

    fun clearExportError() {
        _uiState.update { it.copy(exportError = null) }
    }

    fun showRetentionSettings() {
        _uiState.update { it.copy(showRetentionDialog = true) }
    }

    fun hideRetentionSettings() {
        _uiState.update { it.copy(showRetentionDialog = false) }
    }

    fun updateRetentionPolicy(policy: RetentionPolicy) {
        _uiState.update { it.copy(selectedRetentionPolicy = policy) }
        viewModelScope.launch {
            try {
                when (val result = transcriptionDatabaseRepository.updateRetentionPolicy(policy)) {
                    is Result.Success -> {
                        _uiEvents.emit(
                            DataExportUiEvent.ShowMessage(
                                "Retention policy updated to ${policy.displayName}"
                            )
                        )
                    }
                    is Result.Error -> {
                        _uiEvents.emit(
                            DataExportUiEvent.ShowError(
                                "Failed to update retention policy: ${result.exception.message}"
                            )
                        )
                    }
                    is Result.Loading -> {
                        // Loading state is handled by UI if needed
                    }
                }
            } catch (exception: Exception) {
                _uiEvents.emit(
                    DataExportUiEvent.ShowError(
                        "Failed to update retention policy: ${exception.message}"
                    )
                )
            }
        }
    }

    fun getDataSummary() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingDataSummary = true) }
                
                when (val result = transcriptionDatabaseRepository.getDataSummary()) {
                    is Result.Success -> {
                        _uiState.update { 
                            it.copy(
                                isLoadingDataSummary = false,
                                dataSummary = result.data
                            ) 
                        }
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isLoadingDataSummary = false) }
                        _uiEvents.emit(
                            DataExportUiEvent.ShowError(
                                "Failed to load data summary: ${result.exception.message}"
                            )
                        )
                    }
                    is Result.Loading -> {
                        // Loading state already set above
                    }
                }
            } catch (exception: Exception) {
                _uiState.update { it.copy(isLoadingDataSummary = false) }
                _uiEvents.emit(
                    DataExportUiEvent.ShowError(
                        "Failed to load data summary: ${exception.message}"
                    )
                )
            }
        }
    }

    fun shareExportedFile() {
        val exportResult = _uiState.value.lastExportResult
        if (exportResult != null) {
            viewModelScope.launch {
                _uiEvents.emit(DataExportUiEvent.ShareFile(exportResult.filePath))
            }
        }
    }

    fun retryExport() {
        clearExportError()
        startExport()
    }

    init {
        getDataSummary()
    }
}

data class DataExportUiState(
    val selectedFormat: ExportFormat = ExportFormat.JSON,
    val selectedDateRange: DateRange = DateRange.all(),
    val customDateStart: LocalDate? = null,
    val customDateEnd: LocalDate? = null,
    val includeProtectedData: Boolean = false,
    val selectedRetentionPolicy: RetentionPolicy = RetentionPolicy.UNLIMITED,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportError: String? = null,
    val lastExportResult: ExportResult.Success? = null,
    val showFormatDialog: Boolean = false,
    val showDateRangeDialog: Boolean = false,
    val showRetentionDialog: Boolean = false,
    val isLoadingDataSummary: Boolean = false,
    val dataSummary: DataSummary? = null
)



sealed class DataExportUiEvent {
    data class ShowMessage(val message: String) : DataExportUiEvent()
    data class ShowError(val message: String) : DataExportUiEvent()
    data class ExportCompleted(
        val format: ExportFormat,
        val filePath: String,
        val itemCount: Int
    ) : DataExportUiEvent()
    data class ShareFile(val filePath: String) : DataExportUiEvent()
}