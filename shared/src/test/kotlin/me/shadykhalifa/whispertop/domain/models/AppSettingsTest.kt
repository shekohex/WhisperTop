package me.shadykhalifa.whispertop.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppSettingsTest {

    @Test
    fun `validateHistoryRetentionDays returns null for valid days`() {
        val settings = AppSettings()
        
        // Valid values
        assertNull(settings.validateHistoryRetentionDays(7))
        assertNull(settings.validateHistoryRetentionDays(90))
        assertNull(settings.validateHistoryRetentionDays(365))
        assertNull(settings.validateHistoryRetentionDays(30))
    }

    @Test
    fun `validateHistoryRetentionDays returns error for invalid days`() {
        val settings = AppSettings()
        
        // Too low
        assertEquals(
            "History retention must be at least 7 days",
            settings.validateHistoryRetentionDays(6)
        )
        assertEquals(
            "History retention must be at least 7 days",
            settings.validateHistoryRetentionDays(0)
        )
        assertEquals(
            "History retention must be at least 7 days",
            settings.validateHistoryRetentionDays(-1)
        )
        
        // Too high
        assertEquals(
            "History retention cannot exceed 365 days (1 year)",
            settings.validateHistoryRetentionDays(366)
        )
        assertEquals(
            "History retention cannot exceed 365 days (1 year)",
            settings.validateHistoryRetentionDays(1000)
        )
    }

    @Test
    fun `withValidatedRetentionDays coerces values to valid range`() {
        val settings = AppSettings()
        
        // Values below minimum
        assertEquals(7, settings.withValidatedRetentionDays(0).historyRetentionDays)
        assertEquals(7, settings.withValidatedRetentionDays(-10).historyRetentionDays)
        
        // Values above maximum
        assertEquals(365, settings.withValidatedRetentionDays(400).historyRetentionDays)
        assertEquals(365, settings.withValidatedRetentionDays(1000).historyRetentionDays)
        
        // Valid values remain unchanged
        assertEquals(30, settings.withValidatedRetentionDays(30).historyRetentionDays)
        assertEquals(90, settings.withValidatedRetentionDays(90).historyRetentionDays)
    }

    @Test
    fun `isStatisticsCollectionEnabled returns correct values`() {
        // Statistics enabled and privacy mode allows it
        var settings = AppSettings(
            statisticsEnabled = true,
            dataPrivacyMode = DataPrivacyMode.FULL
        )
        assertTrue(settings.isStatisticsCollectionEnabled())
        
        settings = AppSettings(
            statisticsEnabled = true,
            dataPrivacyMode = DataPrivacyMode.ANONYMIZED
        )
        assertTrue(settings.isStatisticsCollectionEnabled())
        
        // Statistics disabled
        settings = AppSettings(
            statisticsEnabled = false,
            dataPrivacyMode = DataPrivacyMode.FULL
        )
        assertFalse(settings.isStatisticsCollectionEnabled())
        
        // Privacy mode disabled
        settings = AppSettings(
            statisticsEnabled = true,
            dataPrivacyMode = DataPrivacyMode.DISABLED
        )
        assertFalse(settings.isStatisticsCollectionEnabled())
        
        // Both disabled
        settings = AppSettings(
            statisticsEnabled = false,
            dataPrivacyMode = DataPrivacyMode.DISABLED
        )
        assertFalse(settings.isStatisticsCollectionEnabled())
    }

    @Test
    fun `getEffectiveDashboardMetrics respects privacy mode`() {
        val allMetrics = setOf(
            "total_transcriptions",
            "transcription_text",
            "detailed_content",
            "word_count"
        )
        
        // FULL mode returns all metrics
        var settings = AppSettings(
            dataPrivacyMode = DataPrivacyMode.FULL,
            dashboardMetricsVisible = allMetrics
        )
        assertEquals(allMetrics, settings.getEffectiveDashboardMetrics())
        
        // ANONYMIZED mode filters out sensitive metrics
        settings = AppSettings(
            dataPrivacyMode = DataPrivacyMode.ANONYMIZED,
            dashboardMetricsVisible = allMetrics
        )
        val expected = setOf("total_transcriptions", "word_count")
        assertEquals(expected, settings.getEffectiveDashboardMetrics())
        
        // DISABLED mode returns empty set
        settings = AppSettings(
            dataPrivacyMode = DataPrivacyMode.DISABLED,
            dashboardMetricsVisible = allMetrics
        )
        assertTrue(settings.getEffectiveDashboardMetrics().isEmpty())
    }

    @Test
    fun `default values are set correctly`() {
        val settings = AppSettings()
        
        assertTrue(settings.statisticsEnabled)
        assertEquals(90, settings.historyRetentionDays)
        assertEquals(ExportFormat.JSON, settings.exportFormat)
        assertEquals(DefaultDashboardMetrics.ALL_METRICS, settings.dashboardMetricsVisible)
        assertEquals(ChartTimeRange.DAYS_14, settings.chartTimeRange)
        assertTrue(settings.notificationsEnabled)
        assertEquals(DataPrivacyMode.FULL, settings.dataPrivacyMode)
        assertTrue(settings.allowDataImport)
    }

    @Test
    fun `settings can be copied with new values`() {
        val original = AppSettings()
        
        val modified = original.copy(
            statisticsEnabled = false,
            historyRetentionDays = 30,
            exportFormat = ExportFormat.CSV,
            chartTimeRange = ChartTimeRange.DAYS_7,
            dataPrivacyMode = DataPrivacyMode.ANONYMIZED
        )
        
        assertFalse(modified.statisticsEnabled)
        assertEquals(30, modified.historyRetentionDays)
        assertEquals(ExportFormat.CSV, modified.exportFormat)
        assertEquals(ChartTimeRange.DAYS_7, modified.chartTimeRange)
        assertEquals(DataPrivacyMode.ANONYMIZED, modified.dataPrivacyMode)
        
        // Original remains unchanged
        assertTrue(original.statisticsEnabled)
        assertEquals(90, original.historyRetentionDays)
        assertEquals(ExportFormat.JSON, original.exportFormat)
    }
}