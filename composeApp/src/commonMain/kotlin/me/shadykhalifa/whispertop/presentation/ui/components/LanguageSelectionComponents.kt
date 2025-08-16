package me.shadykhalifa.whispertop.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.shadykhalifa.whispertop.domain.models.Language
import me.shadykhalifa.whispertop.domain.models.LanguageDetectionResult
import me.shadykhalifa.whispertop.domain.models.LanguagePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionDropdown(
    selectedLanguage: Language,
    availableLanguages: List<Language>,
    onLanguageSelected: (Language) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showPopularFirst: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded && enabled },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedLanguage.getFullDisplayName(),
            onValueChange = { },
            readOnly = true,
            enabled = enabled,
            label = { Text("Language") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            if (showPopularFirst) {
                // Popular languages section
                Text(
                    text = "Popular Languages",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                val popularLanguages = Language.getPopularLanguages()
                popularLanguages.forEach { language ->
                    LanguageDropdownItem(
                        language = language,
                        isSelected = selectedLanguage == language,
                        onClick = {
                            onLanguageSelected(language)
                            expanded = false
                        }
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // All other languages
                Text(
                    text = "All Languages",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            availableLanguages.filterNot { 
                showPopularFirst && Language.getPopularLanguages().contains(it) 
            }.forEach { language ->
                LanguageDropdownItem(
                    language = language,
                    isSelected = selectedLanguage == language,
                    onClick = {
                        onLanguageSelected(language)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageDropdownItem(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = language.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                    if (language.displayName != language.nativeName && language != Language.AUTO) {
                        Text(
                            text = language.nativeName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (language == Language.AUTO) {
                        Text(
                            text = "Let AI detect the language",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        onClick = onClick
    )
}

@Composable
fun LanguageDetectionCard(
    detectionResult: LanguageDetectionResult?,
    onOverrideLanguage: (Language) -> Unit,
    allowOverride: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (detectionResult != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (detectionResult.isManualOverride) Icons.Default.Settings else Icons.Default.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (detectionResult.isManualOverride) "Language Override" else "Detected Language",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = detectionResult.detectedLanguage.getFullDisplayName(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (detectionResult.confidence != null && !detectionResult.isManualOverride) {
                            Text(
                                text = "Confidence: ${detectionResult.getConfidencePercentage()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (detectionResult.isManualOverride) {
                            Text(
                                text = "Manually selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    
                    if (allowOverride && !detectionResult.isManualOverride) {
                        var showLanguageSelection by remember { mutableStateOf(false) }
                        
                        OutlinedButton(
                            onClick = { showLanguageSelection = true }
                        ) {
                            Text("Override")
                        }
                        
                        if (showLanguageSelection) {
                            LanguageOverrideDialog(
                                currentLanguage = detectionResult.detectedLanguage,
                                onLanguageSelected = { language ->
                                    onOverrideLanguage(language)
                                    showLanguageSelection = false
                                },
                                onDismiss = { showLanguageSelection = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageOverrideDialog(
    currentLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Override Language")
        },
        text = {
            Column {
                Text(
                    text = "Current: ${currentLanguage.getFullDisplayName()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                LanguageSelectionDropdown(
                    selectedLanguage = selectedLanguage,
                    availableLanguages = Language.entries,
                    onLanguageSelected = { selectedLanguage = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onLanguageSelected(selectedLanguage)
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LanguagePreferenceSection(
    preference: LanguagePreference,
    onPreferenceChange: (LanguagePreference) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Language Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Auto-detect language switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-detect Language",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Automatically detect the spoken language",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preference.autoDetectEnabled,
                    onCheckedChange = { enabled ->
                        onPreferenceChange(preference.copy(autoDetectEnabled = enabled))
                    }
                )
            }
            
            // Preferred language selection (when auto-detect is off)
            if (!preference.autoDetectEnabled) {
                LanguageSelectionDropdown(
                    selectedLanguage = preference.preferredLanguage,
                    availableLanguages = Language.entries,
                    onLanguageSelected = { language ->
                        onPreferenceChange(preference.copy(preferredLanguage = language))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Show confidence switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show Detection Confidence",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Display confidence level for detected languages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preference.showConfidence,
                    onCheckedChange = { show ->
                        onPreferenceChange(preference.copy(showConfidence = show))
                    }
                )
            }
            
            // Allow manual override switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Allow Manual Override",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Show option to manually override detected language",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preference.allowManualOverride,
                    onCheckedChange = { allow ->
                        onPreferenceChange(preference.copy(allowManualOverride = allow))
                    }
                )
            }
        }
    }
}

@Composable
fun QuickLanguageSelector(
    onLanguageSelected: (Language) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Quick Language Selection",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Popular language chips
            val popularLanguages = Language.getPopularLanguages().take(6)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                popularLanguages.forEach { language ->
                    FilterChip(
                        onClick = { onLanguageSelected(language) },
                        label = { 
                            Text(
                                text = language.displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        selected = false,
                        leadingIcon = if (language == Language.AUTO) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }
}