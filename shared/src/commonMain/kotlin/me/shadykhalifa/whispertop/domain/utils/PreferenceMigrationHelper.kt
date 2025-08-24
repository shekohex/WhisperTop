package me.shadykhalifa.whispertop.domain.utils

import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.ChartTimeRange
import me.shadykhalifa.whispertop.domain.models.DataPrivacyMode
import me.shadykhalifa.whispertop.domain.models.DefaultDashboardMetrics

object PreferenceMigrationHelper {
    
    private const val CURRENT_PREFERENCES_VERSION = 2
    private const val PREF_KEY_VERSION = "preferences_version"

    /**
     * Migrates settings from older versions to current version
     */
    fun migrateSettings(
        currentSettings: AppSettings,
        storedVersion: Int = 1
    ): AppSettings {
        var migratedSettings = currentSettings
        
        // Migrate from version 1 to 2 (adding statistics preferences)
        if (storedVersion < 2) {
            migratedSettings = migrateToVersion2(migratedSettings)
        }
        
        return migratedSettings
    }

    /**
     * Migrate to version 2 - adds statistics preferences with sensible defaults
     */
    private fun migrateToVersion2(settings: AppSettings): AppSettings {
        return settings.copy(
            // Preserve user's existing privacy preferences when setting defaults
            statisticsEnabled = settings.enableUsageAnalytics, // Use existing analytics preference
            historyRetentionDays = if (settings.maxTranscriptionRetentionDays > 0) 
                settings.maxTranscriptionRetentionDays.coerceIn(7, 365) else 90,
            exportFormat = ExportFormat.JSON, // Safe default
            dashboardMetricsVisible = if (settings.enableUsageAnalytics) 
                DefaultDashboardMetrics.ALL_METRICS else DefaultDashboardMetrics.ESSENTIAL_METRICS,
            chartTimeRange = ChartTimeRange.DAYS_14, // Balanced default
            notificationsEnabled = settings.enableHapticFeedback, // Use existing feedback preference
            dataPrivacyMode = when {
                !settings.enableUsageAnalytics -> DataPrivacyMode.DISABLED
                settings.hashTranscriptionText -> DataPrivacyMode.ANONYMIZED  
                else -> DataPrivacyMode.FULL
            },
            allowDataImport = true // Safe default for new feature
        )
    }

    /**
     * Gets the current preferences version
     */
    fun getCurrentVersion(): Int = CURRENT_PREFERENCES_VERSION

    /**
     * Gets the preference key for version storage
     */
    fun getVersionKey(): String = PREF_KEY_VERSION

    /**
     * Validates that migrated settings are consistent
     */
    fun validateMigratedSettings(settings: AppSettings): List<String> {
        val issues = mutableListOf<String>()

        // Check retention days range
        settings.validateHistoryRetentionDays(settings.historyRetentionDays)?.let { 
            issues.add(it) 
        }

        // Check privacy mode consistency
        if (settings.dataPrivacyMode == DataPrivacyMode.DISABLED && settings.statisticsEnabled) {
            issues.add("Statistics enabled but data privacy mode is disabled - inconsistent state")
        }

        // Check dashboard metrics consistency  
        if (settings.dashboardMetricsVisible.isNotEmpty() && !settings.statisticsEnabled) {
            issues.add("Dashboard metrics visible but statistics disabled")
        }

        return issues
    }

    /**
     * Creates a backup-friendly representation of settings for safe migration
     */
    fun createMigrationBackup(settings: AppSettings): Map<String, Any?> {
        return mapOf(
            "version" to getCurrentVersion(),
            "timestamp" to System.currentTimeMillis(),
            "statisticsEnabled" to settings.statisticsEnabled,
            "historyRetentionDays" to settings.historyRetentionDays,
            "exportFormat" to settings.exportFormat.name,
            "chartTimeRange" to settings.chartTimeRange.name,
            "dataPrivacyMode" to settings.dataPrivacyMode.name,
            "dashboardMetricsCount" to settings.dashboardMetricsVisible.size
        )
    }
}