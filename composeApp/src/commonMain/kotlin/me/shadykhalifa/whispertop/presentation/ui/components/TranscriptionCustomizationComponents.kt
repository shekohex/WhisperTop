package me.shadykhalifa.whispertop.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPromptField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val maxCharacters = 896 // ~224 tokens
    val currentLength = value.length
    val isNearLimit = currentLength > maxCharacters * 0.8
    val isOverLimit = currentLength > maxCharacters
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.length <= maxCharacters) {
                    onValueChange(newValue)
                }
            },
            enabled = enabled,
            label = { Text("Custom Prompt (Optional)") },
            placeholder = { Text("e.g., The following audio contains technical programming terms and company names.") },
            supportingText = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Character counter
                    Text(
                        text = "$currentLength/$maxCharacters characters (~${(currentLength / 4)} tokens)",
                        color = when {
                            isOverLimit -> MaterialTheme.colorScheme.error
                            isNearLimit -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Error message
                    error?.let { errorText ->
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = { onValueChange("") },
                        enabled = enabled
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear prompt",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            isError = error != null || isOverLimit,
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Helper information
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Improve transcription accuracy",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Add context about technical terms, names, or domain-specific vocabulary to help the AI understand your audio better.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TemperatureSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    error: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        // Header with value display and reset button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Temperature",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "%.1f".format(value),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (value != 0.0f) {
                    IconButton(
                        onClick = { onValueChange(0.0f) },
                        enabled = enabled,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = "Reset to default",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Slider with range indicators
        Column {
            Slider(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                valueRange = 0.0f..2.0f,
                steps = 19, // 0.1 increments
                modifier = Modifier.fillMaxWidth()
            )
            
            // Range indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TemperatureRangeIndicator(
                    label = "Accurate",
                    range = "0.0-0.3",
                    color = MaterialTheme.colorScheme.primary,
                    isActive = value <= 0.3f
                )
                TemperatureRangeIndicator(
                    label = "Balanced",
                    range = "0.4-0.7",
                    color = MaterialTheme.colorScheme.secondary,
                    isActive = value > 0.3f && value <= 0.7f
                )
                TemperatureRangeIndicator(
                    label = "Creative",
                    range = "0.8-2.0",
                    color = MaterialTheme.colorScheme.tertiary,
                    isActive = value > 0.7f
                )
            }
        }
        
        // Error message
        error?.let { errorText ->
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Description card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Control transcription randomness",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Lower values (0.0-0.3) produce more consistent, accurate results. Higher values (0.7-2.0) allow more creative interpretations but may introduce variations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TemperatureRangeIndicator(
    label: String,
    range: String,
    color: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = range,
            style = MaterialTheme.typography.bodySmall,
            color = if (isActive) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun TranscriptionCustomizationSection(
    customPrompt: String,
    temperature: Float,
    promptError: String? = null,
    temperatureError: String? = null,
    savingCustomPrompt: Boolean = false,
    savingTemperature: Boolean = false,
    onCustomPromptChange: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Transcription Customization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (savingCustomPrompt || savingTemperature) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            Text(
                text = "Fine-tune transcription behavior for your specific use case",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Custom Prompt Field
            CustomPromptField(
                value = customPrompt,
                onValueChange = onCustomPromptChange,
                error = promptError,
                enabled = !savingCustomPrompt
            )
            
            // Temperature Slider
            TemperatureSlider(
                value = temperature,
                onValueChange = onTemperatureChange,
                error = temperatureError,
                enabled = !savingTemperature
            )
        }
    }
}