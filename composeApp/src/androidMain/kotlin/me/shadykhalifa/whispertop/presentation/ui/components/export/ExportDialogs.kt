package me.shadykhalifa.whispertop.presentation.ui.components.export

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.RetentionPolicy
import java.time.LocalDate

@Composable
fun FormatSelectionDialog(
    currentFormat: ExportFormat,
    onFormatSelected: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select Export Format",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.allFormats().forEach { format ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = currentFormat == format,
                                    onClick = { onFormatSelected(format) },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentFormat == format,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = format.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = getFormatDescription(format),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onDateRangeSelected: (LocalDate?, LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    var tempStartDate by remember { mutableStateOf(startDate) }
    var tempEndDate by remember { mutableStateOf(endDate) }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select Date Range",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Quick selection buttons
                Text(
                    text = "Quick Selection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDateRangeSelected(null, null)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("All Data", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            val today = LocalDate.now()
                            onDateRangeSelected(today, today)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Today", maxLines = 1)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val today = LocalDate.now()
                            val lastWeek = today.minusDays(7)
                            onDateRangeSelected(lastWeek, today)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Last 7 Days", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            val today = LocalDate.now()
                            val lastMonth = today.minusMonths(1)
                            onDateRangeSelected(lastMonth, today)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Last 30 Days", maxLines = 1)
                    }
                }

                Divider()

                // Custom date range (simplified for now)
                Text(
                    text = "Custom Range",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tempStartDate?.toString() ?: "",
                        onValueChange = { /* TODO: Implement date parsing */ },
                        label = { Text("Start Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tempEndDate?.toString() ?: "",
                        onValueChange = { /* TODO: Implement date parsing */ },
                        label = { Text("End Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onDateRangeSelected(tempStartDate, tempEndDate)
                            onDismiss()
                        }
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
fun RetentionPolicyDialog(
    currentPolicy: RetentionPolicy,
    onPolicySelected: (RetentionPolicy) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Retention Policy",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Choose how long to keep transcription data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RetentionPolicy.getAllPolicies().forEach { policy ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = currentPolicy == policy,
                                    onClick = { onPolicySelected(policy) },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentPolicy == policy,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = policy.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = getPolicyDescription(policy),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

private fun getFormatDescription(format: ExportFormat): String {
    return when (format) {
        ExportFormat.JSON -> "Structured data with all metadata"
        ExportFormat.CSV -> "Spreadsheet format for analysis"  
        ExportFormat.TXT -> "Plain text format, transcription only"
        else -> "Unknown format"
    }
}

private fun getPolicyDescription(policy: RetentionPolicy): String {
    return policy.description
}