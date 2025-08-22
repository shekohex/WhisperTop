package me.shadykhalifa.whispertop.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.shadykhalifa.whispertop.domain.models.DateRange
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.ExportResult
import me.shadykhalifa.whispertop.domain.models.SortOption
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistory
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.utils.Result

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val transcriptionHistoryRepository: TranscriptionHistoryRepository
) : ViewModel() {

    companion object {
        private const val SEARCH_DEBOUNCE_DELAY = 300L
        private const val DEFAULT_PAGE_SIZE = 20
    }

    // UI State
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    // Search query with debounce
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filters
    private val _selectedSortOption = MutableStateFlow<SortOption>(SortOption.DateNewest)
    val selectedSortOption: StateFlow<SortOption> = _selectedSortOption.asStateFlow()

    private val _selectedDateRange = MutableStateFlow<DateRange>(DateRange.all())
    val selectedDateRange: StateFlow<DateRange> = _selectedDateRange.asStateFlow()

    // Events
    private val _uiEvents = MutableSharedFlow<HistoryUiEvent>()
    val uiEvents: SharedFlow<HistoryUiEvent> = _uiEvents.asSharedFlow()

    // Selection state
    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    // Paging data with reactive search and filters
    val transcriptions: Flow<PagingData<TranscriptionHistory>> = combine(
        _searchQuery.debounce(SEARCH_DEBOUNCE_DELAY).distinctUntilChanged(),
        _selectedDateRange,
        _selectedSortOption
    ) { query, dateRange, sortOption ->
        Triple(query.trim(), dateRange, sortOption)
    }.flatMapLatest { (query, dateRange, sortOption) ->
        when {
            query.isEmpty() && dateRange == DateRange.all() -> {
                transcriptionHistoryRepository.getTranscriptionsPaged(sortOption)
            }
            query.isNotEmpty() && dateRange == DateRange.all() -> {
                transcriptionHistoryRepository.searchTranscriptionsPaged(query, sortOption)
            }
            query.isEmpty() && dateRange != DateRange.all() -> {
                transcriptionHistoryRepository.getTranscriptionsByDateRangePaged(dateRange, sortOption)
            }
            else -> {
                transcriptionHistoryRepository.searchTranscriptionsWithFiltersPaged(
                    query, dateRange, sortOption
                )
            }
        }
    }.cachedIn(viewModelScope)

    init {
        observeSelectionState()
    }

    private fun observeSelectionState() {
        viewModelScope.launch {
            _selectedItems.collect { selectedItems ->
                _uiState.value = _uiState.value.copy(
                    selectedItemsCount = selectedItems.size,
                    hasSelection = selectedItems.isNotEmpty()
                )
            }
        }
    }

    // Search functions
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(isSearching = query.isNotEmpty())
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.value = _uiState.value.copy(isSearching = false)
    }

    // Filter functions
    fun updateSortOption(sortOption: SortOption) {
        _selectedSortOption.value = sortOption
        viewModelScope.launch {
            _uiEvents.emit(HistoryUiEvent.ShowMessage("Sorted by ${sortOption.displayName}"))
        }
    }

    fun updateDateRange(dateRange: DateRange) {
        _selectedDateRange.value = dateRange
        val message = when {
            dateRange == DateRange.all() -> "Showing all transcriptions"
            dateRange == DateRange.today() -> "Showing today's transcriptions"
            dateRange == DateRange.lastWeek() -> "Showing last week's transcriptions"
            dateRange == DateRange.lastMonth() -> "Showing last month's transcriptions"
            else -> "Custom date range applied"
        }
        viewModelScope.launch {
            _uiEvents.emit(HistoryUiEvent.ShowMessage(message))
        }
    }

    // Selection functions - thread-safe with atomic updates
    fun toggleItemSelection(id: String) {
        var newSelection: Set<String> = emptySet()
        
        _selectedItems.update { currentSelection ->
            newSelection = if (currentSelection.contains(id)) {
                currentSelection - id
            } else {
                currentSelection + id
            }
            newSelection
        }
        
        // Update selection mode based on new selection
        if (newSelection.isEmpty()) {
            exitSelectionMode()
        } else if (!_isSelectionMode.value) {
            enterSelectionMode()
        }
    }

    fun selectAll(ids: List<String>) {
        _selectedItems.value = ids.toSet()
        if (!_isSelectionMode.value) {
            enterSelectionMode()
        }
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
        exitSelectionMode()
    }

    private fun enterSelectionMode() {
        _isSelectionMode.value = true
        _uiState.value = _uiState.value.copy(isSelectionMode = true)
    }

    private fun exitSelectionMode() {
        _isSelectionMode.value = false
        _uiState.value = _uiState.value.copy(isSelectionMode = false)
    }

    // Deletion functions
    fun deleteSelectedItems() {
        val selectedIds = _selectedItems.value.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            try {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            
            when (val result = transcriptionHistoryRepository.deleteTranscriptions(selectedIds)) {
                is Result.Success -> {
                    val deletedCount = result.data
                    clearSelection()
                    _uiEvents.emit(
                        HistoryUiEvent.ShowMessage("Deleted $deletedCount transcription(s)")
                    )
                }
                is Result.Error -> {
                    _uiEvents.emit(
                        HistoryUiEvent.ShowError("Failed to delete items: ${result.exception.message}")
                    )
                }
                is Result.Loading -> {
                    // Loading state is handled by isDeleting flag
                    // This case should not occur with current repository implementation
                    // but we handle it for completeness
                }
            }
            
            _uiState.value = _uiState.value.copy(isDeleting = false)
            } catch (throwable: Throwable) {
                _uiState.value = _uiState.value.copy(isDeleting = false)
                _uiEvents.emit(HistoryUiEvent.ShowError("Failed to delete items: ${throwable.message}"))
            }
        }
    }

    fun deleteTranscription(id: String) {
        viewModelScope.launch {
            try {
            when (val result = transcriptionHistoryRepository.deleteTranscription(id)) {
                is Result.Success -> {
                    _uiEvents.emit(HistoryUiEvent.ShowMessage("Transcription deleted"))
                }
                is Result.Error -> {
                    _uiEvents.emit(
                        HistoryUiEvent.ShowError("Failed to delete transcription: ${result.exception.message}")
                    )
                }
                is Result.Loading -> {
                    // Loading state would be handled by UI if needed
                    // This case should not occur with current repository implementation
                }
            }
            } catch (throwable: Throwable) {
                _uiEvents.emit(HistoryUiEvent.ShowError("Failed to delete transcription: ${throwable.message}"))
            }
        }
    }

    // Export functions
    fun exportAsJson(dateRange: DateRange = DateRange.all()) {
        exportTranscriptions(ExportFormat.JSON, dateRange)
    }

    fun exportAsCsv(dateRange: DateRange = DateRange.all()) {
        exportTranscriptions(ExportFormat.CSV, dateRange)
    }

    private fun exportTranscriptions(format: ExportFormat, dateRange: DateRange) {
        viewModelScope.launch {
            try {
            transcriptionHistoryRepository.exportTranscriptions(format, dateRange).collect { result ->
                when (result) {
                    is ExportResult.InProgress -> {
                        _uiState.value = _uiState.value.copy(isExporting = true)
                    }
                    is ExportResult.Success -> {
                        _uiState.value = _uiState.value.copy(isExporting = false)
                        _uiEvents.emit(
                            HistoryUiEvent.ExportCompleted(
                                format = format,
                                filePath = result.filePath,
                                itemCount = result.itemCount
                            )
                        )
                    }
                    is ExportResult.Error -> {
                        _uiState.value = _uiState.value.copy(isExporting = false)
                        _uiEvents.emit(HistoryUiEvent.ShowError(result.message))
                    }
                }
            }
            } catch (throwable: Throwable) {
                _uiState.value = _uiState.value.copy(isExporting = false)
                _uiEvents.emit(HistoryUiEvent.ShowError("Export failed: ${throwable.message}"))
            }
        }
    }

    // Refresh functionality
    fun refresh() {
        viewModelScope.launch {
            _uiEvents.emit(HistoryUiEvent.ShowMessage("Refreshing..."))
            // PagingData will automatically refresh when we emit new parameters
            delay(500) // Small delay to show refresh state
            _uiEvents.emit(HistoryUiEvent.RefreshCompleted)
        }
    }

    fun retryLastOperation() {
        viewModelScope.launch {
            _uiEvents.emit(HistoryUiEvent.ShowMessage("Retrying..."))
            // Clear any error state
            // In a more sophisticated implementation, this could retry the last failed operation
            // For now, just trigger a refresh
            refresh()
        }
    }
}

// UI State
data class HistoryUiState(
    val isSearching: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedItemsCount: Int = 0,
    val hasSelection: Boolean = false,
    val isDeleting: Boolean = false,
    val isExporting: Boolean = false,
    val showFilters: Boolean = false
)

// UI Events
sealed class HistoryUiEvent {
    data class ShowMessage(val message: String) : HistoryUiEvent()
    data class ShowError(val message: String) : HistoryUiEvent()
    data class ExportCompleted(
        val format: ExportFormat,
        val filePath: String,
        val itemCount: Int
    ) : HistoryUiEvent()
    data object RefreshCompleted : HistoryUiEvent()
}