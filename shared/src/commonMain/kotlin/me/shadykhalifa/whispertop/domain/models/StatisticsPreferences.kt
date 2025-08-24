package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.Serializable

// Extension functions for ExportFormat to support statistics preferences
fun ExportFormat.Companion.fromString(value: String): ExportFormat {
    return when (value.lowercase()) {
        "json" -> JSON
        "csv" -> CSV
        "txt" -> TXT
        else -> JSON // default fallback
    }
}

fun ExportFormat.Companion.defaultForStatistics(): ExportFormat = JSON

val ExportFormat.displayName: String
    get() = name

val ExportFormat.fileExtension: String
    get() = extension



@Serializable
enum class ChartTimeRange(val displayName: String, val days: Int) {
    DAYS_7("7 Days", 7),
    DAYS_14("14 Days", 14),
    DAYS_30("30 Days", 30);

    companion object {
        fun fromDays(days: Int): ChartTimeRange {
            return entries.find { it.days == days } ?: DAYS_14
        }

        fun fromString(value: String): ChartTimeRange {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: DAYS_14
        }
    }
}

@Serializable
enum class DataPrivacyMode(val displayName: String, val description: String) {
    FULL("Full Data", "Store all transcription data and statistics with full detail"),
    ANONYMIZED("Anonymized", "Store statistics only, hash transcription text for privacy"),
    DISABLED("Disabled", "Minimal data storage, statistics collection disabled");

    companion object {
        fun fromString(value: String): DataPrivacyMode {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: FULL
        }
    }
}

object DefaultDashboardMetrics {
    val ALL_METRICS = setOf(
        "total_transcriptions",
        "total_duration",
        "words_per_minute",
        "time_saved",
        "accuracy_score",
        "sessions_count",
        "most_used_language",
        "most_used_model",
        "daily_usage"
    )

    val ESSENTIAL_METRICS = setOf(
        "total_transcriptions",
        "total_duration", 
        "words_per_minute",
        "time_saved"
    )
}