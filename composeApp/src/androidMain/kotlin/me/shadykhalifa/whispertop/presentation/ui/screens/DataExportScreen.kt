package me.shadykhalifa.whispertop.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.presentation.viewmodels.DataExportUiEvent
import me.shadykhalifa.whispertop.presentation.viewmodels.DataExportViewModel
import org.koin.compose.koinInject
import kotlinx.coroutines.flow.collectLatest
import me.shadykhalifa.whispertop.presentation.ui.components.export.FormatSelectionDialog
import me.shadykhalifa.whispertop.presentation.ui.components.export.DateRangePickerDialog
import me.shadykhalifa.whispertop.presentation.ui.components.export.RetentionPolicyDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataExportScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DataExportViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is DataExportUiEvent.ExportCompleted -> {
                    // Handle export completion
                }
                is DataExportUiEvent.ShareFile -> {
                    // Handle file sharing
                }
                is DataExportUiEvent.ShowError -> {
                    // Handle error display
                }
                is DataExportUiEvent.ShowMessage -> {
                    // Handle message display
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Data Export",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "Export your transcription data in various formats",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Data Summary
        if (uiState.dataSummary != null) {
            DataSummaryCard(
                dataSummary = uiState.dataSummary!!,
                isLoading = uiState.isLoadingDataSummary
            )
        }

        // Format Selection
        FormatSelectionCard(
            selectedFormat = uiState.selectedFormat,
            onFormatSelected = viewModel::updateSelectedFormat,
            onShowFormatDialog = viewModel::showFormatSelection
        )

        // Date Range Selection
        DateRangeCard(
            selectedDateRange = uiState.selectedDateRange,
            customDateStart = uiState.customDateStart,
            customDateEnd = uiState.customDateEnd,
            onShowDateRangePicker = viewModel::showDateRangePicker
        )

        // Options
        OptionsCard(
            includeProtectedData = uiState.includeProtectedData,
            onIncludeProtectedDataChanged = viewModel::updateIncludeProtectedData,
            onShowRetentionSettings = viewModel::showRetentionSettings
        )

        // Export Progress
        if (uiState.isExporting) {
            ExportProgressCard(
                progress = uiState.exportProgress,
                onCancel = viewModel::cancelExport
            )
        }

        // Error Display
        uiState.exportError?.let { error ->
            ErrorCard(
                error = error,
                onRetry = viewModel::retryExport,
                onDismiss = viewModel::clearExportError
            )
        }

        // Export Button
        Button(
            onClick = viewModel::startExport,
            enabled = !uiState.isExporting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (uiState.isExporting) "Exporting..." else "Start Export"
            )
        }

        // Share Button (if export completed)
        if (uiState.lastExportResult != null) {
            OutlinedButton(
                onClick = viewModel::shareExportedFile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Exported File")
            }
        }
    }

    // Dialogs
    if (uiState.showFormatDialog) {
        FormatSelectionDialog(
            currentFormat = uiState.selectedFormat,
            onFormatSelected = { format ->
                viewModel.updateSelectedFormat(format)
                viewModel.hideFormatSelection()
            },
            onDismiss = viewModel::hideFormatSelection
        )
    }

    if (uiState.showDateRangeDialog) {
        DateRangePickerDialog(
            startDate = uiState.customDateStart,
            endDate = uiState.customDateEnd,
            onDateRangeSelected = { start, end ->
                viewModel.updateDateRange(start, end)
                viewModel.hideDateRangePicker()
            },
            onDismiss = viewModel::hideDateRangePicker
        )
    }

    if (uiState.showRetentionDialog) {
        RetentionPolicyDialog(
            currentPolicy = uiState.selectedRetentionPolicy,
            onPolicySelected = { policy ->
                viewModel.updateRetentionPolicy(policy)
                viewModel.hideRetentionSettings()
            },
            onDismiss = viewModel::hideRetentionSettings
        )
    }
}

@Composable
private fun DataSummaryCard(
    dataSummary: me.shadykhalifa.whispertop.domain.models.DataSummary,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Data Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Total Transcriptions: ${dataSummary.totalTranscriptions}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Protected Items: ${dataSummary.protectedItems}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (dataSummary.oldestTranscription != null && dataSummary.newestTranscription != null) {
                        Text(
                            text = "Date Range: ${dataSummary.oldestTranscription} to ${dataSummary.newestTranscription}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "Total Size: ${formatBytes(dataSummary.totalSizeBytes)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatSelectionCard(
    selectedFormat: ExportFormat,
    onFormatSelected: (ExportFormat) -> Unit,
    onShowFormatDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Export Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExportFormat.allFormats().forEach { format ->
                    FilterChip(
                        selected = selectedFormat == format,
                        onClick = { onFormatSelected(format) },
                        label = { Text(format.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateRangeCard(
    selectedDateRange: me.shadykhalifa.whispertop.domain.models.DateRange,
    customDateStart: java.time.LocalDate?,
    customDateEnd: java.time.LocalDate?,
    onShowDateRangePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Date Range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            OutlinedButton(
                onClick = onShowDateRangePicker,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        customDateStart != null && customDateEnd != null -> 
                            "$customDateStart to $customDateEnd"
                        selectedDateRange == me.shadykhalifa.whispertop.domain.models.DateRange.all() -> 
                            "All Data"
                        else -> "Select Date Range"
                    }
                )
            }
        }
    }
}

@Composable
private fun OptionsCard(
    includeProtectedData: Boolean,
    onIncludeProtectedDataChanged: (Boolean) -> Unit,
    onShowRetentionSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Include Protected Data")
                Switch(
                    checked = includeProtectedData,
                    onCheckedChange = onIncludeProtectedDataChanged
                )
            }
            
            OutlinedButton(
                onClick = onShowRetentionSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retention Settings")
            }
        }
    }
}

@Composable
private fun ExportProgressCard(
    progress: Float,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Export in Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel Export")
            }
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Export Error",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Retry")
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dismiss")
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = 1024
    val mb = kb * 1024
    val gb = mb * 1024

    return when {
        bytes >= gb -> String.format("%.1f GB", bytes.toDouble() / gb)
        bytes >= mb -> String.format("%.1f MB", bytes.toDouble() / mb)
        bytes >= kb -> String.format("%.1f KB", bytes.toDouble() / kb)
        else -> "$bytes bytes"
    }
}