package me.shadykhalifa.whispertop.domain.models

data class PaginatedResult<T>(
    val items: List<T>,
    val currentPage: Int,
    val pageSize: Int,
    val totalItems: Long,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean
) {
    val totalPages: Int
        get() = if (pageSize > 0) ((totalItems + pageSize - 1) / pageSize).toInt() else 0
    
    val isFirstPage: Boolean
        get() = currentPage <= 1
    
    val isLastPage: Boolean
        get() = !hasNextPage
}

data class PageRequest(
    val page: Int = 1,
    val pageSize: Int = 20,
    val sortBy: String? = null,
    val sortDirection: SortDirection = SortDirection.DESC
) {
    val offset: Int
        get() = (page - 1) * pageSize
}

enum class SortDirection {
    ASC, DESC
}

sealed class LoadingState<T> {
    class Loading<T> : LoadingState<T>()
    data class Success<T>(val data: T) : LoadingState<T>()
    data class Error<T>(val error: Throwable) : LoadingState<T>()
    class Empty<T> : LoadingState<T>()
}