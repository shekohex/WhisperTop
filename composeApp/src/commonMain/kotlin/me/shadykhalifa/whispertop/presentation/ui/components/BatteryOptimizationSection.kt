package me.shadykhalifa.whispertop.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryUnknown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
expect fun getBatteryOptimizationStatus(): BatteryOptimizationStatus?

expect fun requestBatteryOptimizationExemption(): Boolean

expect fun openBatteryOptimizationSettings(): Boolean

data class BatteryOptimizationStatus(
    val isIgnoringBatteryOptimizations: Boolean,
    val canRequestIgnore: Boolean,
    val isFeatureAvailable: Boolean,
    val explanation: String,
    val hasCustomOptimization: Boolean = false,
    val manufacturerGuidance: String = ""
)

@Composable
fun BatteryOptimizationSection(
    modifier: Modifier = Modifier,
    onStatusUpdate: ((BatteryOptimizationStatus?) -> Unit)? = null
) {
    var batteryStatus by remember { mutableStateOf<BatteryOptimizationStatus?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showExplanationDialog by remember { mutableStateOf(false) }
    
    // Get current status
    val currentStatus = getBatteryOptimizationStatus()
    
    // Update local state when status changes
    LaunchedEffect(currentStatus) {
        batteryStatus = currentStatus
        onStatusUpdate?.invoke(currentStatus)
    }
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Battery Optimization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            batteryStatus?.let { status ->
                BatteryOptimizationStatusCard(
                    status = status,
                    onRequestExemption = {
                        val success = requestBatteryOptimizationExemption()
                        if (!success) {
                            openBatteryOptimizationSettings()
                        }
                    },
                    onOpenSettings = {
                        openBatteryOptimizationSettings()
                    },
                    onShowExplanation = { showExplanationDialog = true }
                )
            } ?: run {
                if (!isLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryUnknown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Battery optimization status unavailable",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Explanation Dialog
            if (showExplanationDialog) {
                BatteryOptimizationExplanationDialog(
                    onDismiss = { showExplanationDialog = false },
                    status = batteryStatus
                )
            }
        }
    }
}

@Composable
private fun BatteryOptimizationStatusCard(
    status: BatteryOptimizationStatus,
    onRequestExemption: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowExplanation: () -> Unit
) {
    val statusColor = when {
        status.isIgnoringBatteryOptimizations -> Color.Green
        status.canRequestIgnore -> Color(0xFFFF9800) // Orange
        else -> Color.Red
    }
    
    val statusIcon = when {
        status.isIgnoringBatteryOptimizations -> Icons.Default.Check
        status.canRequestIgnore -> Icons.Default.Warning
        else -> Icons.Default.Error
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = when {
                            status.isIgnoringBatteryOptimizations -> "Optimized for Background Recording"
                            status.canRequestIgnore -> "Battery Optimization Active"
                            else -> "Battery Optimization Required"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = status.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Action buttons
            if (!status.isIgnoringBatteryOptimizations) {
                Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.spacedBy(8.dp)
                 ) {
                     if (status.canRequestIgnore) {
                         Button(
                             onClick = onRequestExemption,
                             modifier = Modifier.weight(1f),
                             contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                         ) {
                             Text(
                                 text = "Request Exemption",
                                 style = MaterialTheme.typography.bodyMedium
                             )
                         }
                     }
                     
                     OutlinedButton(
                         onClick = onOpenSettings,
                         modifier = Modifier.weight(1f),
                         contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                     ) {
                         Icon(
                             imageVector = Icons.Default.Settings,
                             contentDescription = null,
                             modifier = Modifier.size(16.dp)
                         )
                         Spacer(modifier = Modifier.width(4.dp))
                         Text(
                             text = "Settings",
                             style = MaterialTheme.typography.bodyMedium
                         )
                     }
                 }
            }
            
            // Manufacturer-specific guidance
            if (status.hasCustomOptimization && status.manufacturerGuidance.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Device-Specific Instructions:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = status.manufacturerGuidance,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Learn more link
            TextButton(
                onClick = onShowExplanation,
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Learn More",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun BatteryOptimizationExplanationDialog(
    onDismiss: () -> Unit,
    status: BatteryOptimizationStatus?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Battery Optimization for WhisperTop")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Why Battery Optimization Matters:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "WhisperTop needs to run in the background to capture audio when you trigger recording. " +
                            "Modern Android devices use battery optimization to limit background app activity, " +
                            "which can interrupt audio recording.",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "What happens when optimized:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• Recording may stop unexpectedly\n" +
                            "• Overlay button may disappear\n" +
                            "• App may not respond to triggers\n" +
                            "• Transcription may fail or be delayed",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Recommended Action:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Add WhisperTop to your device's battery optimization whitelist to ensure " +
                            "reliable background operation and uninterrupted audio recording.",
                    style = MaterialTheme.typography.bodySmall
                )
                
                if (status?.hasCustomOptimization == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Device-Specific Note:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your device manufacturer has additional battery optimization features. " +
                                "You may need to whitelist WhisperTop in multiple settings for optimal performance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}