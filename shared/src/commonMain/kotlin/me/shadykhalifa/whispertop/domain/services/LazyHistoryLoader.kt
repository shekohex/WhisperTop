package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.shadykhalifa.whispertop.domain.models.LoadingState
import me.shadykhalifa.whispertop.domain.models.PageRequest
import me.shadykhalifa.whispertop.domain.models.PaginatedResult
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.utils.getOrNull

interface LazyHistoryLoader {
    suspend fun loadInitialPage(pageSize: Int = 20): LoadingState<PaginatedResult<TranscriptionHistoryItem>>
    suspend fun loadNextPage(): LoadingState<PaginatedResult<TranscriptionHistoryItem>>
    suspend fun loadPreviousPage(): LoadingState<PaginatedResult<TranscriptionHistoryItem>>
    suspend fun refresh(): LoadingState<PaginatedResult<TranscriptionHistoryItem>>
    suspend fun loadPage(page: Int): LoadingState<PaginatedResult<TranscriptionHistoryItem>>
    suspend fun search(query: String, pageSize: Int = 20): LoadingState<PaginatedResult<TranscriptionHistoryItem>>
    fun observeHistoryItems(): Flow<List<TranscriptionHistoryItem>>
    fun observeLoadingState(): Flow<LoadingState<PaginatedResult<TranscriptionHistoryItem>>>
    fun observePaginationInfo(): Flow<PaginationInfo>
    fun clearCache()
    fun prefetchNextPage()
}

data class PaginationInfo(
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Long,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean,
    val isLoading: Boolean
)

class LazyHistoryLoaderImpl(
    private val historyRepository: TranscriptionHistoryRepository,
    private val performanceMonitor: PerformanceMonitor,
    private val backgroundThreadManager: BackgroundThreadManager
) : LazyHistoryLoader {
    
    private val mutex = Mutex()
    
    // State management
    private var currentPageRequest = PageRequest(page = 1, pageSize = 20)
    private var currentResult: PaginatedResult<TranscriptionHistoryItem>? = null
    private var searchQuery: String? = null
    
    // Caching
    private val pageCache = mutableMapOf<Int, List<TranscriptionHistoryItem>>()
    private val prefetchedPages = mutableSetOf<Int>()
    
    // Reactive state
    private val _loadingState = MutableStateFlow<LoadingState<PaginatedResult<TranscriptionHistoryItem>>>(LoadingState.Empty())
    private val _historyItems = MutableStateFlow<List<TranscriptionHistoryItem>>(emptyList())
    private val _paginationInfo = MutableStateFlow(createPaginationInfo())
    
    override suspend fun loadInitialPage(pageSize: Int): LoadingState<PaginatedResult<TranscriptionHistoryItem>> = mutex.withLock {
        currentPageRequest = PageRequest(page = 1, pageSize = pageSize)
        searchQuery = null
        clearCache()
        
        return loadPageInternal(currentPageRequest)
    }
    
    override suspend fun loadNextPage(): LoadingState<PaginatedResult<TranscriptionHistoryItem>> = mutex.withLock {
        val result = currentResult
        if (result?.hasNextPage == true) {
            currentPageRequest = currentPageRequest.copy(page = currentPageRequest.page + 1)
            return loadPageInternal(currentPageRequest)
        }
        
        return LoadingState.Success(result ?: PaginatedResult(
            items = emptyList(),
            currentPage = 1,
            pageSize = currentPageRequest.pageSize,
            totalItems = 0,
            hasNextPage = false,
            hasPreviousPage = false
        ))
    }
    
    override suspend fun loadPreviousPage(): LoadingState<PaginatedResult<TranscriptionHistoryItem>> = mutex.withLock {
        val result = currentResult
        if (result?.hasPreviousPage == true) {
            currentPageRequest = currentPageRequest.copy(page = currentPageRequest.page - 1)
            return loadPageInternal(currentPageRequest)
        }
        
        return LoadingState.Success(result ?: PaginatedResult(
            items = emptyList(),
            currentPage = 1,
            pageSize = currentPageRequest.pageSize,
            totalItems = 0,
            hasNextPage = false,
            hasPreviousPage = false
        ))
    }
    
    override suspend fun refresh(): LoadingState<PaginatedResult<TranscriptionHistoryItem>> = mutex.withLock {
        clearCache()
        return loadPageInternal(currentPageRequest)
    }
    
    override suspend fun loadPage(page: Int): LoadingState<PaginatedResult<TranscriptionHistoryItem>> = mutex.withLock {
        currentPageRequest = currentPageRequest.copy(page = page)
        return loadPageInternal(currentPageRequest)
    }
    
    override suspend fun search(query: String, pageSize: Int): LoadingState<PaginatedResult<TranscriptionHistoryItem>> = mutex.withLock {
        searchQuery = query
        currentPageRequest = PageRequest(page = 1, pageSize = pageSize)
        clearCache()
        
        return loadPageInternal(currentPageRequest, query)
    }
    
    override fun observeHistoryItems(): Flow<List<TranscriptionHistoryItem>> = _historyItems.asStateFlow()
    
    override fun observeLoadingState(): Flow<LoadingState<PaginatedResult<TranscriptionHistoryItem>>> = _loadingState.asStateFlow()
    
    override fun observePaginationInfo(): Flow<PaginationInfo> = _paginationInfo.asStateFlow()
    
    override fun clearCache() {
        pageCache.clear()
        prefetchedPages.clear()
    }
    
    override fun prefetchNextPage() {
        val result = currentResult
        if (result?.hasNextPage == true) {
            val nextPage = result.currentPage + 1
            if (!prefetchedPages.contains(nextPage)) {
                // Launch prefetch in background
                backgroundThreadManager.launchDatabase("prefetch_page_$nextPage") {
                    try {
                        val nextPageRequest = currentPageRequest.copy(page = nextPage)
                        loadPageFromRepository(nextPageRequest, searchQuery)
                        prefetchedPages.add(nextPage)
                    } catch (e: Exception) {
                        // Ignore prefetch errors silently
                    }
                }
            }
        }
    }
    
    private suspend fun loadPageInternal(
        pageRequest: PageRequest,
        query: String? = searchQuery
    ): LoadingState<PaginatedResult<TranscriptionHistoryItem>> {
        
        _loadingState.value = LoadingState.Loading()
        updatePaginationInfo(isLoading = true)
        
        return try {
            val operationName = if (query.isNullOrBlank()) "load_history_page" else "search_history"
            
            val result = performanceMonitor.measureSuspendOperation(operationName) {
                // Check cache first
                val cachedItems = pageCache[pageRequest.page]
                if (cachedItems != null && query.isNullOrBlank()) {
                    // Use cached result if available and not searching
                    createPaginatedResult(cachedItems, pageRequest)
                } else {
                    // Load from repository
                    loadPageFromRepository(pageRequest, query)
                }
            }
            
            // Update state
            currentResult = result
            _historyItems.value = result.items
            _loadingState.value = LoadingState.Success(result)
            updatePaginationInfo(isLoading = false)
            
            // Cache the result (only for non-search results)
            if (query.isNullOrBlank()) {
                pageCache[pageRequest.page] = result.items
            }
            
            // Prefetch next page if available
            if (result.hasNextPage) {
                prefetchNextPage()
            }
            
            LoadingState.Success(result)
        } catch (e: Exception) {
            val errorState = LoadingState.Error<PaginatedResult<TranscriptionHistoryItem>>(e)
            _loadingState.value = errorState
            updatePaginationInfo(isLoading = false)
            errorState
        }
    }
    
    private suspend fun loadPageFromRepository(
        pageRequest: PageRequest,
        query: String?
    ): PaginatedResult<TranscriptionHistoryItem> {
        
        val historyResult = backgroundThreadManager.executeDatabase<List<TranscriptionHistoryItem>>("load_history_${pageRequest.page}") {
            if (query.isNullOrBlank()) {
                historyRepository.getTranscriptionHistory(
                    offset = pageRequest.offset,
                    limit = pageRequest.pageSize
                ).getOrNull() ?: emptyList()
            } else {
                historyRepository.searchTranscriptionHistory(
                    query = query,
                    offset = pageRequest.offset,
                    limit = pageRequest.pageSize
                ).getOrNull() ?: emptyList()
            }
        }
        
        return createPaginatedResult(historyResult, pageRequest)
    }
    
    private suspend fun createPaginatedResult(
        items: List<TranscriptionHistoryItem>,
        pageRequest: PageRequest
    ): PaginatedResult<TranscriptionHistoryItem> {
        
        // This would typically come from the repository, but for cached results
        // we need to reconstruct pagination info
        val totalItems = backgroundThreadManager.executeDatabase<Long>("count_history") {
            historyRepository.getTotalHistoryCount().getOrNull() ?: 0L
        }
        
        val hasNextPage = pageRequest.offset + items.size < totalItems
        val hasPreviousPage = pageRequest.page > 1
        
        return PaginatedResult(
            items = items,
            currentPage = pageRequest.page,
            pageSize = pageRequest.pageSize,
            totalItems = totalItems,
            hasNextPage = hasNextPage,
            hasPreviousPage = hasPreviousPage
        )
    }
    
    private fun createPaginationInfo(isLoading: Boolean = false): PaginationInfo {
        val result = currentResult
        return PaginationInfo(
            currentPage = result?.currentPage ?: 1,
            totalPages = result?.totalPages ?: 0,
            totalItems = result?.totalItems ?: 0,
            hasNextPage = result?.hasNextPage ?: false,
            hasPreviousPage = result?.hasPreviousPage ?: false,
            isLoading = isLoading
        )
    }
    
    private fun updatePaginationInfo(isLoading: Boolean = false) {
        _paginationInfo.value = createPaginationInfo(isLoading)
    }
}

// Extension functions for easier usage with Compose
fun LazyHistoryLoader.observeItemsWithLoadingState(): Flow<Pair<List<TranscriptionHistoryItem>, Boolean>> {
    return observeHistoryItems().combine(observeLoadingState()) { items, loadingState ->
        val isLoading = loadingState is LoadingState.Loading
        items to isLoading
    }
}

fun LazyHistoryLoader.observeIsEmpty(): Flow<Boolean> {
    return observeLoadingState()
        .map { state -> 
            state is LoadingState.Success && state.data.items.isEmpty() 
        }
        .distinctUntilChanged()
}