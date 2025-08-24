package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.Serializable
import me.shadykhalifa.whispertop.utils.TimeUtils

sealed class ExportResult {
    data class Success(val filePath: String, val itemCount: Int) : ExportResult()
    data class Error(val message: String, val throwable: Throwable? = null) : ExportResult()
    data object InProgress : ExportResult()
}

@Serializable
data class ExportFormat(
    val name: String,
    val extension: String,
    val mimeType: String
) {
    companion object {
        val JSON = ExportFormat(
            name = "JSON",
            extension = "json",
            mimeType = "application/json"
        )
        
        val CSV = ExportFormat(
            name = "CSV", 
            extension = "csv",
            mimeType = "text/csv"
        )
        
        val TXT = ExportFormat(
            name = "TXT",
            extension = "txt", 
            mimeType = "text/plain"
        )
        
        fun allFormats(): List<ExportFormat> = listOf(JSON, CSV, TXT)
    }
}

@Serializable
data class DateRange(
    val startTime: Long?,
    val endTime: Long?
) {
    companion object {
        fun all(): DateRange = DateRange(null, null)
        fun today(): DateRange {
            val now = TimeUtils.currentTimeMillis()
            val startOfDay = TimeUtils.startOfDay(now)
            val endOfDay = TimeUtils.endOfDay(now)
            return DateRange(startOfDay, endOfDay)
        }
        fun lastWeek(): DateRange {
            val now = TimeUtils.currentTimeMillis()
            val weekAgo = now - (7 * 24 * 60 * 60 * 1000)
            return DateRange(weekAgo, now)
        }
        fun lastMonth(): DateRange {
            val now = TimeUtils.currentTimeMillis()
            val monthAgo = now - (30L * 24 * 60 * 60 * 1000)
            return DateRange(monthAgo, now)
        }
    }
}