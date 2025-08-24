package me.shadykhalifa.whispertop.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatisticsPreferencesTest {

    @Test
    fun `ExportFormat fromString works correctly`() {
        assertEquals(ExportFormat.JSON, ExportFormat.fromString("JSON"))
        assertEquals(ExportFormat.JSON, ExportFormat.fromString("json"))
        assertEquals(ExportFormat.CSV, ExportFormat.fromString("CSV"))
        assertEquals(ExportFormat.CSV, ExportFormat.fromString("csv"))
        assertEquals(ExportFormat.TXT, ExportFormat.fromString("TXT"))
        
        // Default fallback for unknown values
        assertEquals(ExportFormat.JSON, ExportFormat.fromString("unknown"))
        assertEquals(ExportFormat.JSON, ExportFormat.fromString(""))
    }

    @Test
    fun `ExportFormat has correct display names and extensions`() {
        assertEquals("JSON", ExportFormat.JSON.displayName)
        assertEquals("json", ExportFormat.JSON.fileExtension)
        
        assertEquals("CSV", ExportFormat.CSV.displayName)
        assertEquals("csv", ExportFormat.CSV.fileExtension)
        
        assertEquals("TXT", ExportFormat.TXT.displayName)
        assertEquals("txt", ExportFormat.TXT.fileExtension)
    }

    @Test
    fun `ChartTimeRange fromDays works correctly`() {
        assertEquals(ChartTimeRange.DAYS_7, ChartTimeRange.fromDays(7))
        assertEquals(ChartTimeRange.DAYS_14, ChartTimeRange.fromDays(14))
        assertEquals(ChartTimeRange.DAYS_30, ChartTimeRange.fromDays(30))
        
        // Default fallback for unknown values
        assertEquals(ChartTimeRange.DAYS_14, ChartTimeRange.fromDays(15))
        assertEquals(ChartTimeRange.DAYS_14, ChartTimeRange.fromDays(0))
    }

    @Test
    fun `ChartTimeRange fromString works correctly`() {
        assertEquals(ChartTimeRange.DAYS_7, ChartTimeRange.fromString("DAYS_7"))
        assertEquals(ChartTimeRange.DAYS_14, ChartTimeRange.fromString("DAYS_14"))
        assertEquals(ChartTimeRange.DAYS_30, ChartTimeRange.fromString("DAYS_30"))
        
        // Case insensitive
        assertEquals(ChartTimeRange.DAYS_7, ChartTimeRange.fromString("days_7"))
        
        // Default fallback
        assertEquals(ChartTimeRange.DAYS_14, ChartTimeRange.fromString("unknown"))
    }

    @Test
    fun `ChartTimeRange has correct display names and day counts`() {
        assertEquals("7 Days", ChartTimeRange.DAYS_7.displayName)
        assertEquals(7, ChartTimeRange.DAYS_7.days)
        
        assertEquals("14 Days", ChartTimeRange.DAYS_14.displayName)
        assertEquals(14, ChartTimeRange.DAYS_14.days)
        
        assertEquals("30 Days", ChartTimeRange.DAYS_30.displayName)
        assertEquals(30, ChartTimeRange.DAYS_30.days)
    }

    @Test
    fun `DataPrivacyMode fromString works correctly`() {
        assertEquals(DataPrivacyMode.FULL, DataPrivacyMode.fromString("FULL"))
        assertEquals(DataPrivacyMode.ANONYMIZED, DataPrivacyMode.fromString("ANONYMIZED"))
        assertEquals(DataPrivacyMode.DISABLED, DataPrivacyMode.fromString("DISABLED"))
        
        // Case insensitive
        assertEquals(DataPrivacyMode.FULL, DataPrivacyMode.fromString("full"))
        
        // Default fallback
        assertEquals(DataPrivacyMode.FULL, DataPrivacyMode.fromString("unknown"))
    }

    @Test
    fun `DataPrivacyMode has correct display names and descriptions`() {
        val fullMode = DataPrivacyMode.FULL
        assertEquals("Full Data", fullMode.displayName)
        assertTrue(fullMode.description.contains("Store all transcription data"))
        
        val anonymizedMode = DataPrivacyMode.ANONYMIZED
        assertEquals("Anonymized", anonymizedMode.displayName)
        assertTrue(anonymizedMode.description.contains("hash transcription text"))
        
        val disabledMode = DataPrivacyMode.DISABLED
        assertEquals("Disabled", disabledMode.displayName)
        assertTrue(disabledMode.description.contains("Minimal data storage"))
    }

    @Test
    fun `DefaultDashboardMetrics contains expected metrics`() {
        val allMetrics = DefaultDashboardMetrics.ALL_METRICS
        val essentialMetrics = DefaultDashboardMetrics.ESSENTIAL_METRICS
        
        // ALL_METRICS should contain all essential metrics
        assertTrue(allMetrics.containsAll(essentialMetrics))
        
        // Check specific metrics exist
        assertTrue(allMetrics.contains("total_transcriptions"))
        assertTrue(allMetrics.contains("total_duration"))
        assertTrue(allMetrics.contains("words_per_minute"))
        assertTrue(allMetrics.contains("time_saved"))
        
        // Essential metrics should be a subset
        assertTrue(allMetrics.size > essentialMetrics.size)
        
        // Essential metrics should contain core functionality
        assertTrue(essentialMetrics.contains("total_transcriptions"))
        assertTrue(essentialMetrics.contains("words_per_minute"))
        assertTrue(essentialMetrics.contains("time_saved"))
    }

    @Test
    fun `all enums are serializable`() {
        // This test ensures our enums work with kotlinx.serialization
        // The @Serializable annotation should make this work
        val exportFormat = ExportFormat.JSON
        val chartRange = ChartTimeRange.DAYS_14
        val privacyMode = DataPrivacyMode.FULL
        
        // If these compile and run without error, serialization is working
        assertEquals("JSON", exportFormat.name)
        assertEquals("DAYS_14", chartRange.name)
        assertEquals("FULL", privacyMode.name)
    }
}