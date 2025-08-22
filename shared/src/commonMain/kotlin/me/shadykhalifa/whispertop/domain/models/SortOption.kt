package me.shadykhalifa.whispertop.domain.models

sealed class SortOption(val displayName: String) {
    data object DateNewest : SortOption("Date (Newest)")
    data object DateOldest : SortOption("Date (Oldest)")
    data object DurationLongest : SortOption("Duration (Longest)")
    data object DurationShortest : SortOption("Duration (Shortest)")
    data object WordCountMost : SortOption("Word Count (Most)")
    data object WordCountLeast : SortOption("Word Count (Least)")
    data object ConfidenceHighest : SortOption("Confidence (Highest)")
    data object ConfidenceLowest : SortOption("Confidence (Lowest)")
    
    companion object {
        fun allOptions(): List<SortOption> = listOf(
            DateNewest,
            DateOldest,
            DurationLongest,
            DurationShortest,
            WordCountMost,
            WordCountLeast,
            ConfidenceHighest,
            ConfidenceLowest
        )
    }
}