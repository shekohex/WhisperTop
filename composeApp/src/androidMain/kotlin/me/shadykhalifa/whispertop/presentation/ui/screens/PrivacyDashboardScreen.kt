package me.shadykhalifa.whispertop.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.shadykhalifa.whispertop.presentation.viewmodels.PrivacyViewModel
import org.koin.compose.koinInject
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrivacyViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(viewModel) {
        viewModel.loadPrivacyData()
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
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Privacy Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "Manage your data and privacy preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Data Summary
        DataSummarySection(
            dataSummary = uiState.dataSummary,
            isLoading = uiState.isLoadingData,
            modifier = Modifier.fillMaxWidth()
        )

        // Consent Status
        ConsentStatusSection(
            consentStatus = uiState.consentStatus,
            onConsentChanged = viewModel::updateConsent,
            modifier = Modifier.fillMaxWidth()
        )

        // GDPR Rights
        GdprRightsSection(
            onExportData = viewModel::exportPersonalData,
            onDeleteAllData = viewModel::initiateDataDeletion,
            onViewAuditLog = viewModel::showAuditLog,
            isExporting = uiState.isExportingData,
            isDeletingData = uiState.isDeletingData,
            modifier = Modifier.fillMaxWidth()
        )

        // Audit Information
        if (uiState.showAuditInfo) {
            AuditInfoSection(
                auditStats = uiState.auditStatistics,
                onHideAuditInfo = viewModel::hideAuditInfo,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Confirmation Dialogs
    if (uiState.showDeleteConfirmation) {
        DeleteConfirmationDialog(
            onConfirm = viewModel::confirmDataDeletion,
            onDismiss = viewModel::cancelDataDeletion
        )
    }
}

@Composable
private fun DataSummarySection(
    dataSummary: me.shadykhalifa.whispertop.domain.models.DataSummary?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Your Data",
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
            } else if (dataSummary != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Transcriptions: ${dataSummary.totalTranscriptions}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Protected items: ${dataSummary.protectedItems}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Storage used: ${formatBytes(dataSummary.totalSizeBytes)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (dataSummary.oldestTranscription != null) {
                        Text(
                            text = "Oldest data: ${dataSummary.oldestTranscription}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConsentStatusSection(
    consentStatus: me.shadykhalifa.whispertop.domain.services.ConsentStatus?,
    onConsentChanged: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Privacy Consent",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            if (consentStatus != null) {
                ConsentToggleRow(
                    label = "Data Collection",
                    description = "Allow collection of transcription data",
                    checked = consentStatus.dataCollection,
                    onCheckedChange = { onConsentChanged("dataCollection", it) }
                )
                
                ConsentToggleRow(
                    label = "Data Processing",
                    description = "Allow processing of transcription data",
                    checked = consentStatus.dataProcessing,
                    onCheckedChange = { onConsentChanged("dataProcessing", it) }
                )
                
                ConsentToggleRow(
                    label = "Data Storage",
                    description = "Allow storage of transcription history",
                    checked = consentStatus.dataStorage,
                    onCheckedChange = { onConsentChanged("dataStorage", it) }
                )
                
                ConsentToggleRow(
                    label = "Data Export",
                    description = "Allow export of your data",
                    checked = consentStatus.dataExport,
                    onCheckedChange = { onConsentChanged("dataExport", it) }
                )
                
                ConsentToggleRow(
                    label = "Analytics",
                    description = "Allow anonymous usage analytics",
                    checked = consentStatus.analytics,
                    onCheckedChange = { onConsentChanged("analytics", it) }
                )
                
                ConsentToggleRow(
                    label = "Service Improvement",
                    description = "Allow data use for improving the service",
                    checked = consentStatus.improvement,
                    onCheckedChange = { onConsentChanged("improvement", it) }
                )
                
                if (consentStatus.lastUpdated != null) {
                    Text(
                        text = "Last updated: ${consentStatus.lastUpdated}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text("Loading consent status...")
            }
        }
    }
}

@Composable
private fun ConsentToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun GdprRightsSection(
    onExportData: () -> Unit,
    onDeleteAllData: () -> Unit,
    onViewAuditLog: () -> Unit,
    isExporting: Boolean,
    isDeletingData: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Your Rights (GDPR)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Under GDPR, you have several rights regarding your personal data:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Data Portability
            OutlinedButton(
                onClick = onExportData,
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isExporting) "Exporting..." else "Export My Data"
                )
            }
            
            // Right to be Forgotten
            OutlinedButton(
                onClick = onDeleteAllData,
                enabled = !isDeletingData,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isDeletingData) "Deleting..." else "Delete All My Data"
                )
            }
            
            // Audit Trail
            OutlinedButton(
                onClick = onViewAuditLog,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Data Access History")
            }
        }
    }
}

@Composable
private fun AuditInfoSection(
    auditStats: me.shadykhalifa.whispertop.domain.services.AuditStatistics?,
    onHideAuditInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Data Access History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onHideAuditInfo) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Hide audit info"
                    )
                }
            }
            
            if (auditStats != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Total operations: ${auditStats.totalOperations}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Success rate: ${String.format(Locale.getDefault(), "%.1f", auditStats.successRate * 100)}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (auditStats.lastOperation != null) {
                        Text(
                            text = "Last access: ${auditStats.lastOperation}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "Critical events (24h): ${auditStats.criticalEventsLast24h}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text("Loading audit information...")
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete All Data")
        },
        text = {
            Text(
                "This will permanently delete all your transcription data, including protected items. " +
                "This action cannot be undone. Are you sure you want to continue?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete Everything")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatBytes(bytes: Long): String {
    val kb = 1024
    val mb = kb * 1024
    val gb = mb * 1024

    return when {
        bytes >= gb -> String.format(Locale.getDefault(), "%.1f GB", bytes.toDouble() / gb)
        bytes >= mb -> String.format(Locale.getDefault(), "%.1f MB", bytes.toDouble() / mb)
        bytes >= kb -> String.format(Locale.getDefault(), "%.1f KB", bytes.toDouble() / kb)
        else -> "$bytes bytes"
    }
}