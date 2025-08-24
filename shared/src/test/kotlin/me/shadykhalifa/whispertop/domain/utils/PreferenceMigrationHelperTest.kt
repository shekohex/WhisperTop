package me.shadykhalifa.whispertop.domain.utils

import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.ChartTimeRange
import me.shadykhalifa.whispertop.domain.models.DataPrivacyMode
import me.shadykhalifa.whispertop.domain.models.DefaultDashboardMetrics
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PreferenceMigrationHelperTest {

    @Test
    fun `getCurrentVersion returns expected version`() {
        assertEquals(2, PreferenceMigrationHelper.getCurrentVersion())
    }

    @Test
    fun `getVersionKey returns expected key`() {
        assertEquals("preferences_version", PreferenceMigrationHelper.getVersionKey())
    }

    @Test
    fun `migrateSettings from version 1 to 2 uses existing preferences`() {
        // Create settings that represent version 1 state
        val oldSettings = AppSettings(
            enableUsageAnalytics = true,
            maxTranscriptionRetentionDays = 60,
            enableHapticFeedback = false,
            hashTranscriptionText = false
        )
        
        val migratedSettings = PreferenceMigrationHelper.migrateSettings(oldSettings, storedVersion = 1)
        
        // Check that migration preserved user choices
        assertTrue(migratedSettings.statisticsEnabled) // From enableUsageAnalytics
        assertEquals(60, migratedSettings.historyRetentionDays) // From maxTranscriptionRetentionDays
        assertEquals(ExportFormat.JSON, migratedSettings.exportFormat) // Safe default
        assertFalse(migratedSettings.notificationsEnabled) // From enableHapticFeedback
        assertEquals(DataPrivacyMode.FULL, migratedSettings.dataPrivacyMode) // No hashing
        assertTrue(migratedSettings.allowDataImport) // Safe default
    }

    @Test
    fun `migrateSettings handles analytics disabled case`() {
        val oldSettings = AppSettings(
            enableUsageAnalytics = false,
            maxTranscriptionRetentionDays = 30,
            enableHapticFeedback = true,
            hashTranscriptionText = false
        )
        
        val migratedSettings = PreferenceMigrationHelper.migrateSettings(oldSettings, storedVersion = 1)
        
        assertFalse(migratedSettings.statisticsEnabled) // Analytics was disabled
        assertEquals(DataPrivacyMode.DISABLED, migratedSettings.dataPrivacyMode) // Analytics disabled
        // Should use essential metrics only when analytics disabled
        assertEquals(migratedSettings.dashboardMetricsVisible.size, 
                    DefaultDashboardMetrics.ESSENTIAL_METRICS.size)
    }

    @Test
    fun `migrateSettings handles hashed transcription text case`() {
        val oldSettings = AppSettings(
            enableUsageAnalytics = true,
            hashTranscriptionText = true,
            maxTranscriptionRetentionDays = 120
        )
        
        val migratedSettings = PreferenceMigrationHelper.migrateSettings(oldSettings, storedVersion = 1)
        
        assertEquals(DataPrivacyMode.ANONYMIZED, migratedSettings.dataPrivacyMode)
        assertTrue(migratedSettings.statisticsEnabled)
    }

    @Test
    fun `migrateSettings coerces retention days to valid range`() {
        // Test retention days below minimum
        var oldSettings = AppSettings(maxTranscriptionRetentionDays = 3)
        var migratedSettings = PreferenceMigrationHelper.migrateSettings(oldSettings, storedVersion = 1)
        assertEquals(7, migratedSettings.historyRetentionDays)
        
        // Test retention days above maximum  
        oldSettings = AppSettings(maxTranscriptionRetentionDays = 400)
        migratedSettings = PreferenceMigrationHelper.migrateSettings(oldSettings, storedVersion = 1)
        assertEquals(365, migratedSettings.historyRetentionDays)
        
        // Test zero or negative values
        oldSettings = AppSettings(maxTranscriptionRetentionDays = 0)
        migratedSettings = PreferenceMigrationHelper.migrateSettings(oldSettings, storedVersion = 1)
        assertEquals(90, migratedSettings.historyRetentionDays) // Should use default
    }

    @Test
    fun `migrateSettings handles current version`() {
        val currentSettings = AppSettings(statisticsEnabled = false)
        val migratedSettings = PreferenceMigrationHelper.migrateSettings(
            currentSettings, 
            storedVersion = PreferenceMigrationHelper.getCurrentVersion()
        )
        
        // Should return unchanged when already at current version
        assertEquals(currentSettings, migratedSettings)
    }

    @Test
    fun `validateMigratedSettings detects inconsistencies`() {
        // Valid settings should have no issues
        val validSettings = AppSettings()
        val issues = PreferenceMigrationHelper.validateMigratedSettings(validSettings)
        assertTrue(issues.isEmpty())
        
        // Invalid retention days
        var invalidSettings = AppSettings(historyRetentionDays = 400)
        var validationIssues = PreferenceMigrationHelper.validateMigratedSettings(invalidSettings)
        assertTrue(validationIssues.any { it.contains("retention") })
        
        // Statistics enabled but privacy disabled
        invalidSettings = AppSettings(
            statisticsEnabled = true,
            dataPrivacyMode = DataPrivacyMode.DISABLED
        )
        validationIssues = PreferenceMigrationHelper.validateMigratedSettings(invalidSettings)
        assertTrue(validationIssues.any { it.contains("inconsistent") })
        
        // Dashboard metrics visible but statistics disabled
        invalidSettings = AppSettings(
            statisticsEnabled = false,
            dashboardMetricsVisible = setOf("total_transcriptions")
        )
        validationIssues = PreferenceMigrationHelper.validateMigratedSettings(invalidSettings)
        assertTrue(validationIssues.any { it.contains("Dashboard metrics") })
    }

    @Test
    fun `createMigrationBackup contains expected data`() {
        val settings = AppSettings(
            statisticsEnabled = false,
            historyRetentionDays = 60,
            exportFormat = ExportFormat.CSV,
            chartTimeRange = ChartTimeRange.DAYS_7,
            dataPrivacyMode = DataPrivacyMode.ANONYMIZED
        )
        
        val backup = PreferenceMigrationHelper.createMigrationBackup(settings)
        
        assertEquals(PreferenceMigrationHelper.getCurrentVersion(), backup["version"])
        assertEquals(false, backup["statisticsEnabled"])
        assertEquals(60, backup["historyRetentionDays"])
        assertEquals("CSV", backup["exportFormat"])
        assertEquals("DAYS_7", backup["chartTimeRange"])
        assertEquals("ANONYMIZED", backup["dataPrivacyMode"])
        assertTrue(backup.containsKey("timestamp"))
        assertTrue(backup.containsKey("dashboardMetricsCount"))
    }

    @Test
    fun `migrateToVersion2 preserves chart time range default`() {
        val oldSettings = AppSettings()
        val migratedSettings = PreferenceMigrationHelper.migrateSettings(oldSettings, storedVersion = 1)
        
        assertEquals(ChartTimeRange.DAYS_14, migratedSettings.chartTimeRange)
    }
}