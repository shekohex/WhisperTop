package me.shadykhalifa.whispertop.domain.models

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class ProductivityTrends(
    val dateRange: Pair<LocalDate, LocalDate>,
    val wordsPerDayTrend: Double, // slope of linear regression
    val speakingTimePerDayTrend: Double, // slope of linear regression
    val successRateTrend: Double, // slope of linear regression
    val dailyMetrics: Map<LocalDate, DailyMetric>
) {
    @Serializable
    data class DailyMetric(
        val words: Int,
        val speakingTimeMs: Long,
        val successRate: Double
    )
    
    val isImprovingProductivity: Boolean
        get() = wordsPerDayTrend > 0 && successRateTrend >= 0
    
    val isDecreasingUsage: Boolean
        get() = wordsPerDayTrend < 0 && speakingTimePerDayTrend < 0
    
    val trendDescription: String
        get() = when {
            isImprovingProductivity -> "Improving"
            isDecreasingUsage -> "Decreasing"
            wordsPerDayTrend > 0 -> "Increasing Usage"
            successRateTrend > 0 -> "Improving Quality"
            else -> "Stable"
        }
}