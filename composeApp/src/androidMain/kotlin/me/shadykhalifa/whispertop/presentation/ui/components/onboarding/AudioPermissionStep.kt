package me.shadykhalifa.whispertop.presentation.ui.components.onboarding

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import me.shadykhalifa.whispertop.presentation.models.IndividualPermissionState
import me.shadykhalifa.whispertop.presentation.models.PermissionState

@Composable
fun AudioPermissionStep(
    permissionState: IndividualPermissionState,
    notificationPermissionState: IndividualPermissionState,
    foregroundServicePermissionState: IndividualPermissionState,
    foregroundServiceMicrophonePermissionState: IndividualPermissionState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    canProceed: Boolean,
    modifier: Modifier = Modifier
) {
    OnboardingStepLayout(
        modifier = modifier
    ) {
        // Animated microphone icon
        MicrophoneAnimation(
            isPermissionGranted = permissionState.state == PermissionState.Granted
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Microphone Access",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "WhisperTop needs microphone access to record your speech for transcription",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Permission status cards
        PermissionStatusCard(
            title = "Microphone Access",
            description = "Required for voice recording",
            icon = Icons.Default.Mic,
            permissionState = permissionState,
            isRequired = true
        )
        
        if (notificationPermissionState.isRequired) {
            Spacer(modifier = Modifier.height(12.dp))
            PermissionStatusCard(
                title = "Notifications",
                description = "Shows recording status",
                icon = Icons.Default.Notifications,
                permissionState = notificationPermissionState,
                isRequired = true
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        PermissionStatusCard(
            title = "Foreground Service",
            description = "Enables background recording",
            icon = Icons.Default.PlayArrow,
            permissionState = foregroundServicePermissionState,
            isRequired = true
        )
        
        if (foregroundServiceMicrophonePermissionState.isRequired) {
            Spacer(modifier = Modifier.height(12.dp))
            PermissionStatusCard(
                title = "Microphone Service",
                description = "Background microphone access",
                icon = Icons.Default.MicNone,
                permissionState = foregroundServiceMicrophonePermissionState,
                isRequired = true
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Action buttons
        when (permissionState.state) {
            PermissionState.NotRequested -> {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant Permission")
                }
            }
            PermissionState.Denied -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Try Again")
                    }
                    
                    Text(
                        text = "WhisperTop needs microphone access to capture your speech for transcription. Your audio is processed securely using your own OpenAI API key.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            PermissionState.PermanentlyDenied -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Settings")
                    }
                    
                    Text(
                        text = "Permission was permanently denied. Please enable it in settings.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            PermissionState.Granted -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Audio permissions granted!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Button(
                        onClick = onContinue,
                        enabled = canProceed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Continue")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Back button
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back")
        }
    }
}

@Composable
private fun MicrophoneAnimation(
    isPermissionGranted: Boolean
) {
    var isAnimating by remember { mutableStateOf(!isPermissionGranted) }
    
    LaunchedEffect(isPermissionGranted) {
        isAnimating = !isPermissionGranted
    }
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(
                if (isPermissionGranted) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .animateContentSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPermissionGranted) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = if (isPermissionGranted) 
                MaterialTheme.colorScheme.onPrimaryContainer 
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    description: String,
    icon: ImageVector,
    permissionState: IndividualPermissionState,
    isRequired: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (permissionState.state) {
                PermissionState.Granted -> MaterialTheme.colorScheme.primaryContainer
                PermissionState.PermanentlyDenied -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when (permissionState.state) {
                    PermissionState.Granted -> MaterialTheme.colorScheme.primary
                    PermissionState.PermanentlyDenied -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = when (permissionState.state) {
                    PermissionState.Granted -> Icons.Default.CheckCircle
                    PermissionState.PermanentlyDenied -> Icons.Default.Cancel
                    else -> Icons.Default.RadioButtonUnchecked
                },
                contentDescription = null,
                tint = when (permissionState.state) {
                    PermissionState.Granted -> MaterialTheme.colorScheme.primary
                    PermissionState.PermanentlyDenied -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}