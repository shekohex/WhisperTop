package me.shadykhalifa.whispertop.presentation.ui.components.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog


@Composable
fun PermissionRationaleDialog(
    permission: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val (title, description, icon) = when (permission) {
        android.Manifest.permission.RECORD_AUDIO -> Triple(
            "Microphone Permission",
            "WhisperTop needs microphone access to capture your speech for transcription. Your audio is processed securely using your own OpenAI API key.",
            Icons.Default.Mic
        )
        android.Manifest.permission.SYSTEM_ALERT_WINDOW -> Triple(
            "Overlay Permission",
            "The overlay permission allows WhisperTop to show a floating microphone button that works on top of any app, making voice transcription easily accessible.",
            Icons.Default.PictureInPicture
        )
        "accessibility_service" -> Triple(
            "Accessibility Service",
            "The accessibility service allows WhisperTop to automatically insert transcribed text directly into input fields, eliminating the need for manual copy-paste.",
            Icons.Default.AccessibilityNew
        )
        else -> Triple(
            "Permission Required",
            "This permission is required for the app to function properly.",
            Icons.Default.Security
        )
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                // Description
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}