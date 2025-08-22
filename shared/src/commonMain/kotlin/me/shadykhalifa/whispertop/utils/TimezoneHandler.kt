package me.shadykhalifa.whispertop.utils

import kotlinx.datetime.*
import kotlinx.serialization.Serializable

/**
 * Handles timezone-aware operations for statistics aggregation
 * Ensures data consistency across timezone changes and DST transitions
 */
@Serializable
data class TimezoneInfo(
    val zoneId: String,
    val offsetSeconds: Int,
    val isDST: Boolean,
    val lastUpdated: Long
)

object TimezoneHandler {
    
    private const val TIMEZONE_CACHE_KEY = "last_known_timezone"
    private const val DST_TRANSITION_WINDOW_HOURS = 6 // Hours to consider for DST transitions
    
    /**
     * Get current timezone information
     */
    fun getCurrentTimezoneInfo(): TimezoneInfo {
        val currentTime = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val localDateTime = currentTime.toLocalDateTime(timeZone)
        
        return TimezoneInfo(
            zoneId = timeZone.id,
            offsetSeconds = timeZone.offsetAt(currentTime).totalSeconds,
            isDST = isDaylightSavingTime(currentTime, timeZone),
            lastUpdated = currentTime.toEpochMilliseconds()
        )
    }
    
    /**
     * Check if timezone has changed since last known state
     */
    fun hasTimezoneChanged(lastKnownTimezone: TimezoneInfo?): Boolean {
        if (lastKnownTimezone == null) return true
        
        val current = getCurrentTimezoneInfo()
        return current.zoneId != lastKnownTimezone.zoneId || 
               current.offsetSeconds != lastKnownTimezone.offsetSeconds
    }
    
    /**
     * Check if we're in a DST transition period
     */
    fun isInDSTTransition(currentTime: Instant = Clock.System.now()): Boolean {
        val timeZone = TimeZone.currentSystemDefault()
        val localDateTime = currentTime.toLocalDateTime(timeZone)
        
        // Check if we're near typical DST transition times (2 AM)
        val hour = localDateTime.hour
        val isTransitionSeason = localDateTime.month in listOf(Month.MARCH, Month.NOVEMBER)
        val isWeekend = localDateTime.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        
        return isTransitionSeason && isWeekend && hour in (0..4)
    }
    
    /**
     * Adjust aggregation date based on timezone changes
     */
    fun getAdjustedAggregationDate(
        proposedDate: LocalDate,
        lastKnownTimezone: TimezoneInfo?
    ): LocalDate {
        val currentTimezone = getCurrentTimezoneInfo()
        
        if (lastKnownTimezone == null || !hasTimezoneChanged(lastKnownTimezone)) {
            return proposedDate
        }
        
        // If timezone changed significantly, we might need to re-aggregate recent days
        val offsetDifference = currentTimezone.offsetSeconds - lastKnownTimezone.offsetSeconds
        val hoursDifference = offsetDifference / 3600
        
        return when {
            // Significant timezone change (>4 hours), go back further
            kotlin.math.abs(hoursDifference) > 4 -> proposedDate.minus(DatePeriod(days = 2))
            // Minor timezone change, go back 1 day
            kotlin.math.abs(hoursDifference) > 0 -> proposedDate.minus(DatePeriod(days = 1))
            // No significant change
            else -> proposedDate
        }
    }
    
    /**
     * Get the appropriate date range for statistics considering timezone issues
     */
    fun getTimezoneAwareDateRange(
        targetDate: LocalDate,
        lastKnownTimezone: TimezoneInfo?
    ): Pair<Long, Long> {
        val currentTimezone = TimeZone.currentSystemDefault()
        val adjustedDate = getAdjustedAggregationDate(targetDate, lastKnownTimezone)
        
        // Add buffer for timezone uncertainty
        val startOfDay = adjustedDate.atStartOfDayIn(currentTimezone).toEpochMilliseconds()
        val endOfDay = adjustedDate.plus(DatePeriod(days = 1))
            .atStartOfDayIn(currentTimezone).toEpochMilliseconds()
        
        // If we're in DST transition, add buffer
        if (isInDSTTransition()) {
            val bufferMs = 2 * 60 * 60 * 1000L // 2 hours buffer
            return Pair(startOfDay - bufferMs, endOfDay + bufferMs)
        }
        
        return Pair(startOfDay, endOfDay)
    }
    
    /**
     * Validate that a timestamp falls within expected timezone bounds
     */
    fun isTimestampInExpectedRange(
        timestamp: Long,
        expectedDate: LocalDate,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Boolean {
        val timestampDate = Instant.fromEpochMilliseconds(timestamp)
            .toLocalDateTime(timeZone).date
        
        // Allow for timezone uncertainty - timestamp could be in adjacent day
        val allowedDates = listOf(
            expectedDate.minus(DatePeriod(days = 1)),
            expectedDate,
            expectedDate.plus(DatePeriod(days = 1))
        )
        
        return timestampDate in allowedDates
    }
    
    /**
     * Get a list of dates that need re-aggregation due to timezone changes
     */
    fun getDatesNeedingReaggregation(
        lastKnownTimezone: TimezoneInfo?,
        maxDaysBack: Int = 7
    ): List<LocalDate> {
        if (!hasTimezoneChanged(lastKnownTimezone)) {
            return emptyList()
        }
        
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val daysToReaggregate = if (isInDSTTransition()) 3 else 1
        
        return (0 until minOf(daysToReaggregate, maxDaysBack)).map { daysBack ->
            currentDate.minus(DatePeriod(days = daysBack + 1))
        }
    }
    
    private fun isDaylightSavingTime(instant: Instant, timeZone: TimeZone): Boolean {
        val offset = timeZone.offsetAt(instant)
        
        // Compare with standard time offset (usually winter time)
        val januaryOffset = timeZone.offsetAt(
            LocalDate(instant.toLocalDateTime(timeZone).year, Month.JANUARY, 15)
                .atStartOfDayIn(timeZone)
        )
        
        return offset.totalSeconds > januaryOffset.totalSeconds
    }
    
    /**
     * Format timezone info for logging/debugging
     */
    fun formatTimezoneInfo(timezoneInfo: TimezoneInfo): String {
        val offsetHours = timezoneInfo.offsetSeconds / 3600.0
        val dstIndicator = if (timezoneInfo.isDST) " (DST)" else ""
        return "${timezoneInfo.zoneId} UTC${if (offsetHours >= 0) "+" else ""}${offsetHours}$dstIndicator"
    }
}