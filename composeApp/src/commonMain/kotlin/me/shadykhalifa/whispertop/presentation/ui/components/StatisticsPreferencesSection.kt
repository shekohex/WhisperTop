package me.shadykhalifa.whispertop.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.ChartTimeRange
import me.shadykhalifa.whispertop.domain.models.DataPrivacyMode
import me.shadykhalifa.whispertop.domain.models.DefaultDashboardMetrics
import me.shadykhalifa.whispertop.domain.models.displayName

@Composable
fun StatisticsPreferencesSection(
    settings: AppSettings,
    validationErrors: Map<String, String> = emptyMap(),
    optimisticDataPrivacyMode: DataPrivacyMode?,
    onStatisticsEnabledChange: (Boolean) -> Unit,
    onHistoryRetentionDaysChange: (Int) -> Unit,
    onExportFormatChange: (ExportFormat) -> Unit,
    onDashboardMetricsVisibleChange: (Set<String>) -> Unit,
    onChartTimeRangeChange: (ChartTimeRange) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onDataPrivacyModeChange: (DataPrivacyMode) -> Unit,
    onAllowDataImportChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Statistics & Dashboard Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Data Collection Section
            DataCollectionSection(
                statisticsEnabled = settings.statisticsEnabled,
                historyRetentionDays = settings.historyRetentionDays,
                validationErrors = validationErrors,
                onStatisticsEnabledChange = onStatisticsEnabledChange,
                onHistoryRetentionDaysChange = onHistoryRetentionDaysChange
            )
            
            HorizontalDivider()
            
            // Export & Import Section
            ExportImportSection(
                exportFormat = settings.exportFormat,
                allowDataImport = settings.allowDataImport,
                onExportFormatChange = onExportFormatChange,
                onAllowDataImportChange = onAllowDataImportChange
            )
            
            HorizontalDivider()
            
            // Dashboard Configuration Section
            DashboardConfigSection(
                dashboardMetricsVisible = settings.dashboardMetricsVisible,
                chartTimeRange = settings.chartTimeRange,
                onDashboardMetricsVisibleChange = onDashboardMetricsVisibleChange,
                onChartTimeRangeChange = onChartTimeRangeChange
            )
            
            HorizontalDivider()
            
            // Notifications Section
            NotificationsSection(
                notificationsEnabled = settings.notificationsEnabled,
                onNotificationsEnabledChange = onNotificationsEnabledChange
            )
            
            HorizontalDivider()
            
            // Privacy Section
            PrivacySection(
                dataPrivacyMode = settings.dataPrivacyMode,
                optimisticDataPrivacyMode = optimisticDataPrivacyMode,
                onDataPrivacyModeChange = onDataPrivacyModeChange
            )
        }
    }
}

@Composable
private fun DataCollectionSection(
    statisticsEnabled: Boolean,
    historyRetentionDays: Int,
    validationErrors: Map<String, String>,
    onStatisticsEnabledChange: (Boolean) -> Unit,
    onHistoryRetentionDaysChange: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Data Collection",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        // Statistics Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable Statistics Tracking",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Collect usage statistics and transcription metrics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = statisticsEnabled,
                onCheckedChange = onStatisticsEnabledChange
            )
        }
        
        // History Retention Slider
        if (statisticsEnabled) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "History Retention",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$historyRetentionDays days",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Slider(
                    value = historyRetentionDays.toFloat(),
                    onValueChange = { value ->
                        onHistoryRetentionDaysChange(value.toInt())
                    },
                    valueRange = 7f..365f,
                    steps = 51, // Creates stops at useful intervals
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "7 days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "365 days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Validation Error
                validationErrors["historyRetentionDays"]?.let { error ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportImportSection(
    exportFormat: ExportFormat,
    allowDataImport: Boolean,
    onExportFormatChange: (ExportFormat) -> Unit,
    onAllowDataImportChange: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Export & Import",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        // Export Format Selection
        Text(
            text = "Export Format",
            style = MaterialTheme.typography.bodyMedium
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(ExportFormat.allFormats()) { format ->
                FilterChip(
                    onClick = { onExportFormatChange(format) },
                    label = {
                        Text(format.displayName)
                    },
                    selected = format == exportFormat,
                    leadingIcon = if (format == exportFormat) {
                        {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Selected",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
            }
        }
        
        // Allow Data Import Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Allow Data Import",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Enable importing statistics from backup files",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = allowDataImport,
                onCheckedChange = onAllowDataImportChange
            )
        }
    }
}

@Composable
private fun DashboardConfigSection(
    dashboardMetricsVisible: Set<String>,
    chartTimeRange: ChartTimeRange,
    onDashboardMetricsVisibleChange: (Set<String>) -> Unit,
    onChartTimeRangeChange: (ChartTimeRange) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Dashboard Configuration",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        // Chart Time Range Selection
        Text(
            text = "Chart Time Range",
            style = MaterialTheme.typography.bodyMedium
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(ChartTimeRange.entries) { range ->
                FilterChip(
                    onClick = { onChartTimeRangeChange(range) },
                    label = {
                        Text(range.displayName)
                    },
                    selected = range == chartTimeRange
                )
            }
        }
        
        // Metrics Visibility Section
        Text(
            text = "Visible Metrics",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DefaultDashboardMetrics.ALL_METRICS.chunked(2).forEach { metricsPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    metricsPair.forEach { metric ->
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = metric in dashboardMetricsVisible,
                                onCheckedChange = { isChecked ->
                                    val newMetrics = if (isChecked) {
                                        dashboardMetricsVisible + metric
                                    } else {
                                        dashboardMetricsVisible - metric
                                    }
                                    onDashboardMetricsVisibleChange(newMetrics)
                                }
                            )
                            Text(
                                text = metric.replace("_", " ").replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    // Fill empty space if odd number of metrics
                    if (metricsPair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsSection(
    notificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Milestone Notifications",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Get notified when reaching usage milestones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = onNotificationsEnabledChange
            )
        }
    }
}

@Composable
private fun PrivacySection(
    dataPrivacyMode: DataPrivacyMode,
    optimisticDataPrivacyMode: DataPrivacyMode?,
    onDataPrivacyModeChange: (DataPrivacyMode) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Data Privacy",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DataPrivacyMode.entries.forEach { mode ->
                val isSelected = mode == (optimisticDataPrivacyMode ?: dataPrivacyMode)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onDataPrivacyModeChange(mode) }
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        Text(
                            text = mode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}