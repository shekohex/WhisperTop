package me.shadykhalifa.whispertop.data.repositories

import androidx.paging.PagingSource
import androidx.paging.PagingState
import me.shadykhalifa.whispertop.data.database.entities.TranscriptionHistoryEntity
import me.shadykhalifa.whispertop.data.database.mappers.toDomain
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistory

class TranscriptionHistoryPagingSource(
    private val entityPagingSource: PagingSource<Int, TranscriptionHistoryEntity>
) : PagingSource<Int, TranscriptionHistory>() {

    override fun getRefreshKey(state: PagingState<Int, TranscriptionHistory>): Int? {
        return entityPagingSource.getRefreshKey(
            PagingState(
                pages = state.pages.map { page ->
                    PagingSource.LoadResult.Page(
                        data = page.data.map { it.toEntity() },
                        prevKey = page.prevKey,
                        nextKey = page.nextKey
                    )
                },
                anchorPosition = state.anchorPosition,
                config = state.config,
                leadingPlaceholderCount = 0 // Temporary fix for version compatibility
            )
        )
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TranscriptionHistory> {
        return when (val result = entityPagingSource.load(params)) {
            is LoadResult.Page -> LoadResult.Page(
                data = result.data.toDomain(),
                prevKey = result.prevKey,
                nextKey = result.nextKey
            )
            is LoadResult.Error -> LoadResult.Error(result.throwable)
            is LoadResult.Invalid -> LoadResult.Invalid()
        }
    }
}

private fun TranscriptionHistory.toEntity(): me.shadykhalifa.whispertop.data.database.entities.TranscriptionHistoryEntity {
    return me.shadykhalifa.whispertop.data.database.entities.TranscriptionHistoryEntity(
        id = id,
        text = text,
        timestamp = timestamp,
        duration = duration,
        audioFilePath = audioFilePath,
        confidence = confidence,
        customPrompt = customPrompt,
        temperature = temperature,
        language = language,
        model = model,
        wordCount = wordCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}