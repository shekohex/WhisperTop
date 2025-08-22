package me.shadykhalifa.whispertop.presentation.viewmodels

import androidx.paging.PagingData
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.models.DateRange
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.ExportResult
import me.shadykhalifa.whispertop.domain.models.SortOption
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistory
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.domain.models.TranscriptionStatistics
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private lateinit var mockRepository: MockTranscriptionHistoryRepository
    private lateinit var viewModel: HistoryViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        mockRepository = MockTranscriptionHistoryRepository()
        viewModel = HistoryViewModel(mockRepository)
    }

    @Test
    fun `initial state is correct`() = runTest(testDispatcher) {
        val uiState = viewModel.uiState.value
        
        assertFalse(uiState.isSearching)
        assertFalse(uiState.isSelectionMode)
        assertEquals(0, uiState.selectedItemsCount)
        assertFalse(uiState.hasSelection)
        assertFalse(uiState.isDeleting)
        assertFalse(uiState.isExporting)
        assertFalse(uiState.showFilters)
    }

    @Test
    fun `search query is debounced correctly`() = runTest(testDispatcher) {
        viewModel.searchQuery.test {
            // Initial empty query
            assertEquals("", awaitItem())
            
            // Update search query rapidly
            viewModel.updateSearchQuery("test")
            viewModel.updateSearchQuery("testing")
            viewModel.updateSearchQuery("test query")
            
            // Should only receive the final query after debounce
            testScheduler.advanceTimeBy(300)
            assertEquals("test query", expectMostRecentItem())
        }
    }

    @Test
    fun `search state updates correctly`() = testDispatcher.runBlockingTest {
        assertFalse(viewModel.uiState.value.isSearching)
        
        viewModel.updateSearchQuery("test")
        assertTrue(viewModel.uiState.value.isSearching)
        
        viewModel.clearSearch()
        assertFalse(viewModel.uiState.value.isSearching)
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `sort option updates correctly`() = testDispatcher.runBlockingTest {
        assertEquals(SortOption.DateNewest, viewModel.selectedSortOption.value)
        
        viewModel.uiEvents.test {
            viewModel.updateSortOption(SortOption.DurationLongest)
            
            assertEquals(SortOption.DurationLongest, viewModel.selectedSortOption.value)
            
            val event = awaitItem() as HistoryUiEvent.ShowMessage
            assertEquals("Sorted by Duration (Longest)", event.message)
        }
    }

    @Test
    fun `date range updates correctly`() = testDispatcher.runBlockingTest {
        assertEquals(DateRange.all(), viewModel.selectedDateRange.value)
        
        viewModel.uiEvents.test {
            viewModel.updateDateRange(DateRange.today())
            
            assertEquals(DateRange.today(), viewModel.selectedDateRange.value)
            
            val event = awaitItem() as HistoryUiEvent.ShowMessage
            assertEquals("Showing today's transcriptions", event.message)
        }
    }

    @Test
    fun `item selection works correctly`() = testDispatcher.runBlockingTest {
        // Initially no items selected
        assertEquals(emptySet<String>(), viewModel.selectedItems.value)
        assertFalse(viewModel.isSelectionMode.value)
        
        // Select first item
        viewModel.toggleItemSelection("1")
        assertEquals(setOf("1"), viewModel.selectedItems.value)
        assertTrue(viewModel.isSelectionMode.value)
        
        // Select second item
        viewModel.toggleItemSelection("2")
        assertEquals(setOf("1", "2"), viewModel.selectedItems.value)
        
        // Deselect first item
        viewModel.toggleItemSelection("1")
        assertEquals(setOf("2"), viewModel.selectedItems.value)
        
        // Deselect last item - should exit selection mode
        viewModel.toggleItemSelection("2")
        assertEquals(emptySet<String>(), viewModel.selectedItems.value)
        assertFalse(viewModel.isSelectionMode.value)
    }

    @Test
    fun `select all works correctly`() = testDispatcher.runBlockingTest {
        val ids = listOf("1", "2", "3", "4", "5")
        
        viewModel.selectAll(ids)
        
        assertEquals(ids.toSet(), viewModel.selectedItems.value)
        assertTrue(viewModel.isSelectionMode.value)
        assertTrue(viewModel.uiState.value.isSelectionMode)
        assertEquals(5, viewModel.uiState.value.selectedItemsCount)
    }

    @Test
    fun `clear selection works correctly`() = testDispatcher.runBlockingTest {
        // First select some items
        viewModel.selectAll(listOf("1", "2", "3"))
        assertTrue(viewModel.isSelectionMode.value)
        
        // Then clear selection
        viewModel.clearSelection()
        
        assertEquals(emptySet<String>(), viewModel.selectedItems.value)
        assertFalse(viewModel.isSelectionMode.value)
        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertEquals(0, viewModel.uiState.value.selectedItemsCount)
    }

    @Test
    fun `delete selected items works correctly`() = testDispatcher.runBlockingTest {
        mockRepository.deleteMultipleResult = Result.Success(3)
        
        // Select some items
        viewModel.selectAll(listOf("1", "2", "3"))
        
        viewModel.uiEvents.test {
            viewModel.deleteSelectedItems()
            
            // Should show success message
            val event = awaitItem() as HistoryUiEvent.ShowMessage
            assertEquals("Deleted 3 transcription(s)", event.message)
            
            // Selection should be cleared
            assertEquals(emptySet<String>(), viewModel.selectedItems.value)
            assertFalse(viewModel.isSelectionMode.value)
        }
    }

    @Test
    fun `delete selected items handles error correctly`() = testDispatcher.runBlockingTest {
        val error = Exception("Database error")
        mockRepository.deleteMultipleResult = Result.Error(error)
        
        viewModel.selectAll(listOf("1", "2", "3"))
        
        viewModel.uiEvents.test {
            viewModel.deleteSelectedItems()
            
            val event = awaitItem() as HistoryUiEvent.ShowError
            assertEquals("Failed to delete items: Database error", event.message)
        }
    }

    @Test
    fun `delete single transcription works correctly`() = testDispatcher.runBlockingTest {
        mockRepository.deleteResult = Result.Success(Unit)
        
        viewModel.uiEvents.test {
            viewModel.deleteTranscription("1")
            
            val event = awaitItem() as HistoryUiEvent.ShowMessage
            assertEquals("Transcription deleted", event.message)
        }
    }

    @Test
    fun `export as JSON works correctly`() = testDispatcher.runBlockingTest {
        mockRepository.exportResult = flowOf(
            ExportResult.InProgress,
            ExportResult.Success("export.json", 10)
        )
        
        viewModel.uiEvents.test {
            viewModel.exportAsJson()
            
            val event = awaitItem() as HistoryUiEvent.ExportCompleted
            assertEquals(ExportFormat.JSON, event.format)
            assertEquals("export.json", event.filePath)
            assertEquals(10, event.itemCount)
        }
    }

    @Test
    fun `export as CSV works correctly`() = testDispatcher.runBlockingTest {
        mockRepository.exportResult = flowOf(
            ExportResult.InProgress,
            ExportResult.Success("export.csv", 5)
        )
        
        viewModel.uiEvents.test {
            viewModel.exportAsCsv()
            
            val event = awaitItem() as HistoryUiEvent.ExportCompleted
            assertEquals(ExportFormat.CSV, event.format)
            assertEquals("export.csv", event.filePath)
            assertEquals(5, event.itemCount)
        }
    }

    @Test
    fun `export handles error correctly`() = testDispatcher.runBlockingTest {
        mockRepository.exportResult = flowOf(
            ExportResult.InProgress,
            ExportResult.Error("Export failed")
        )
        
        viewModel.uiEvents.test {
            viewModel.exportAsJson()
            
            val event = awaitItem() as HistoryUiEvent.ShowError
            assertEquals("Export failed", event.message)
        }
    }

    @Test
    fun `export updates loading state correctly`() = testDispatcher.runBlockingTest {
        mockRepository.exportResult = flow {
            emit(ExportResult.InProgress)
            delay(100)
            emit(ExportResult.Success("export.json", 10))
        }
        
        assertFalse(viewModel.uiState.value.isExporting)
        
        viewModel.exportAsJson()
        advanceTimeBy(50) // Progress should be in progress
        assertTrue(viewModel.uiState.value.isExporting)
        
        advanceTimeBy(100) // Should complete
        assertFalse(viewModel.uiState.value.isExporting)
    }

    @Test
    fun `refresh triggers correctly`() = testDispatcher.runBlockingTest {
        viewModel.uiEvents.test {
            viewModel.refresh()
            
            val refreshingEvent = awaitItem() as HistoryUiEvent.ShowMessage
            assertEquals("Refreshing...", refreshingEvent.message)
            
            advanceTimeBy(500)
            
            val completedEvent = awaitItem() as HistoryUiEvent.RefreshCompleted
            // Event should be RefreshCompleted
        }
    }
}

// Mock repository for testing
private class MockTranscriptionHistoryRepository : TranscriptionHistoryRepository {
    var deleteResult: Result<Unit> = Result.Success(Unit)
    var deleteMultipleResult: Result<Int> = Result.Success(0)
    var exportResult: Flow<ExportResult> = flowOf(ExportResult.Success("test.json", 0))
    
    override suspend fun saveTranscription(
        text: String,
        duration: Float?,
        audioFilePath: String?,
        confidence: Float?,
        customPrompt: String?,
        temperature: Float?,
        language: String?,
        model: String?
    ): Result<String> = Result.Success("test-id")

    override suspend fun getTranscription(id: String): Result<TranscriptionHistoryItem?> = 
        Result.Success(null)

    override suspend fun updateTranscription(transcription: TranscriptionHistoryItem): Result<Unit> = 
        Result.Success(Unit)

    override suspend fun deleteTranscription(id: String): Result<Unit> = deleteResult

    override suspend fun deleteAllTranscriptions(): Result<Unit> = Result.Success(Unit)

    override fun getAllTranscriptions(): Flow<List<TranscriptionHistoryItem>> = flowOf(emptyList())

    override fun getAllTranscriptionsFlow(): Flow<List<TranscriptionHistoryItem>> = flowOf(emptyList())

    override suspend fun getRecentTranscriptions(limit: Int): Result<List<TranscriptionHistoryItem>> = 
        Result.Success(emptyList())

    override fun searchTranscriptions(query: String): Flow<List<TranscriptionHistoryItem>> = 
        flowOf(emptyList())

    override fun getTranscriptionsByDateRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<TranscriptionHistoryItem>> = flowOf(emptyList())

    override fun searchTranscriptionsByTextAndDateRange(
        query: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<TranscriptionHistoryItem>> = flowOf(emptyList())

    override suspend fun getTranscriptionStatistics(): Result<TranscriptionStatistics> = 
        Result.Success(
            TranscriptionStatistics(
                totalCount = 0,
                totalDuration = null,
                averageConfidence = null,
                mostUsedLanguage = null,
                mostUsedModel = null,
                dailyTranscriptionCount = 0
            )
        )

    override fun getTranscriptionsPaged(sortOption: SortOption): Flow<PagingData<TranscriptionHistory>> = 
        flowOf(PagingData.empty())

    override fun searchTranscriptionsPaged(
        query: String,
        sortOption: SortOption
    ): Flow<PagingData<TranscriptionHistory>> = flowOf(PagingData.empty())

    override fun getTranscriptionsByDateRangePaged(
        dateRange: DateRange,
        sortOption: SortOption
    ): Flow<PagingData<TranscriptionHistory>> = flowOf(PagingData.empty())

    override fun searchTranscriptionsWithFiltersPaged(
        query: String,
        dateRange: DateRange,
        sortOption: SortOption
    ): Flow<PagingData<TranscriptionHistory>> = flowOf(PagingData.empty())

    override suspend fun deleteTranscriptions(ids: List<String>): Result<Int> = deleteMultipleResult

    override suspend fun exportTranscriptions(
        format: ExportFormat,
        dateRange: DateRange
    ): Flow<ExportResult> = exportResult
}