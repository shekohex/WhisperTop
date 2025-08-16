package me.shadykhalifa.whispertop.presentation.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.shadykhalifa.whispertop.managers.BatteryOptimizationUtil
import org.koin.compose.koinInject

@Composable
fun BatteryOptimizationExemptionDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val batteryUtil: BatteryOptimizationUtil = koinInject()
    
    var isRequestingPermission by remember { mutableStateOf(false) }
    var batteryStatus by remember { mutableStateOf(batteryUtil.getBatteryOptimizationStatus()) }
    val hasCustomOptimization = remember { batteryUtil.hasCustomBatteryOptimization() }
    val manufacturerGuidance = remember { batteryUtil.getManufacturerSpecificGuidance() }
    
    // Check if already optimized
    LaunchedEffect(batteryStatus) {
        if (batteryStatus.isIgnoringBatteryOptimizations) {
            onSuccess()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryFull,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Battery Optimization Required")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current status
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )
                        Text(
                            text = "WhisperTop is subject to battery optimization",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Text(
                    text = "To ensure reliable background recording, WhisperTop needs to be exempt from battery optimization. This allows:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BulletPointText("Continuous audio capture when triggered")
                    BulletPointText("Persistent overlay button visibility") 
                    BulletPointText("Uninterrupted transcription processing")
                    BulletPointText("Automatic text insertion")
                }
                
                if (hasCustomOptimization) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Device-Specific Note:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = manufacturerGuidance,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (batteryStatus.canRequestIgnore) {
                Button(
                    onClick = {
                        isRequestingPermission = true
                        requestBatteryOptimizationExemption(
                            context = context,
                            batteryUtil = batteryUtil,
                            onResult = { success ->
                                isRequestingPermission = false
                                if (success) {
                                    // Refresh status
                                    batteryStatus = batteryUtil.getBatteryOptimizationStatus()
                                    if (batteryStatus.isIgnoringBatteryOptimizations) {
                                        onSuccess()
                                    }
                                }
                            }
                        )
                    },
                    enabled = !isRequestingPermission
                ) {
                    if (isRequestingPermission) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Grant Exemption")
                }
            } else {
                Button(
                    onClick = {
                        openBatteryOptimizationSettings(context, batteryUtil)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open Settings")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}

@Composable
private fun BulletPointText(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun requestBatteryOptimizationExemption(
    context: android.content.Context,
    batteryUtil: BatteryOptimizationUtil,
    onResult: (Boolean) -> Unit
) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = batteryUtil.createBatteryOptimizationExemptionIntent()
            if (intent != null) {
                context.startActivity(intent)
                onResult(true)
            } else {
                onResult(false)
            }
        } else {
            onResult(false)
        }
    } catch (e: Exception) {
        onResult(false)
    }
}

private fun openBatteryOptimizationSettings(
    context: android.content.Context,
    batteryUtil: BatteryOptimizationUtil
) {
    try {
        val intent = batteryUtil.createBatteryOptimizationSettingsIntent()
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error silently
    }
}