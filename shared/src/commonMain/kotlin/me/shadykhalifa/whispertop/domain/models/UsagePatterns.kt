package me.shadykhalifa.whispertop.domain.models

import kotlinx.datetime.LocalDate
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.Serializable

@Serializable
data class UsagePatterns(
    val dateRange: Pair<LocalDate, LocalDate>,
    val hourlyDistribution: List<Int>, // 24 hours, count of sessions per hour
    val peakHours: List<Int>, // top 3 most active hours
    val dailyDistribution: Map<DayOfWeek, Int>, // sessions per day of week
    val topApps: List<Pair<String, Int>>, // app package name to session count
    val averageSessionsPerDay: Double,
    val mostActiveDay: DayOfWeek?
) {
    val peakUsageHour: Int
        get() = peakHours.firstOrNull() ?: 0
    
    val leastActiveHours: List<Int>
        get() = hourlyDistribution.withIndex()
            .sortedBy { it.value }
            .take(3)
            .map { it.index }
    
    val workdayUsage: Int
        get() = dailyDistribution.filterKeys { 
            it in setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        }.values.sum()
    
    val weekendUsage: Int
        get() = dailyDistribution.filterKeys { 
            it in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        }.values.sum()
    
    val isWeekdayUser: Boolean
        get() = workdayUsage > weekendUsage
}