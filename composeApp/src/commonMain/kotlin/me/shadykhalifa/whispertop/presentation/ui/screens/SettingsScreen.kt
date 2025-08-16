package me.shadykhalifa.whispertop.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.models.WhisperModel
import me.shadykhalifa.whispertop.presentation.viewmodels.SettingsViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // API Configuration Section
                ApiConfigurationSection(
                    apiKey = uiState.settings.apiKey,
                    isApiKeyVisible = uiState.isApiKeyVisible,
                    onApiKeyChange = viewModel::updateApiKey,
                    onToggleApiKeyVisibility = viewModel::toggleApiKeyVisibility,
                    onClearApiKey = viewModel::clearApiKey,
                    validationError = uiState.validationErrors["apiKey"],
                    isLoading = uiState.savingApiKey
                )
                
                HorizontalDivider()
                
                // Model Selection Section
                ModelSelectionSection(
                    selectedModel = uiState.settings.selectedModel,
                    availableModels = uiState.availableModels,
                    onModelSelected = viewModel::updateSelectedModel
                )
                
                HorizontalDivider()
                
                // Language Preferences Section
                LanguagePreferencesSection(
                    language = uiState.settings.language,
                    autoDetectLanguage = uiState.settings.autoDetectLanguage,
                    onLanguageChange = viewModel::updateLanguage,
                    onToggleAutoDetect = viewModel::toggleAutoDetectLanguage
                )
                
                HorizontalDivider()
                
                // Theme Customization Section
                ThemeCustomizationSection(
                    selectedTheme = uiState.settings.theme,
                    availableThemes = uiState.availableThemes,
                    onThemeSelected = viewModel::updateTheme
                )
                
                HorizontalDivider()
                
                // Privacy Controls Section
                PrivacyControlsSection(
                    enableHapticFeedback = uiState.settings.enableHapticFeedback,
                    enableBatteryOptimization = uiState.settings.enableBatteryOptimization,
                    onToggleHapticFeedback = viewModel::toggleHapticFeedback,
                    onToggleBatteryOptimization = viewModel::toggleBatteryOptimization
                )
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Saving settings...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiConfigurationSection(
    apiKey: String,
    isApiKeyVisible: Boolean,
    onApiKeyChange: (String) -> Unit,
    onToggleApiKeyVisibility: () -> Unit,
    onClearApiKey: () -> Unit,
    validationError: String?,
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "API Configuration",
                style = MaterialTheme.typography.titleMedium
            )
            
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("OpenAI API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isApiKeyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    Row {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        IconButton(onClick = onToggleApiKeyVisibility) {
                            Icon(
                                imageVector = if (isApiKeyVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (isApiKeyVisible) {
                                    "Hide API Key"
                                } else {
                                    "Show API Key"
                                }
                            )
                        }
                    }
                },
                isError = validationError != null,
                supportingText = validationError?.let { { Text(it) } }
            )
            
            if (apiKey.isNotBlank()) {
                OutlinedButton(
                    onClick = onClearApiKey,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Clear API Key")
                }
            }
        }
    }
}

@Composable
private fun ModelSelectionSection(
    selectedModel: String,
    availableModels: List<WhisperModel>,
    onModelSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Model Selection",
                style = MaterialTheme.typography.titleMedium
            )
            
            availableModels.forEach { model ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedModel == model.id,
                        onClick = { onModelSelected(model.id) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = model.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = model.id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguagePreferencesSection(
    language: String?,
    autoDetectLanguage: Boolean,
    onLanguageChange: (String?) -> Unit,
    onToggleAutoDetect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Language Preferences",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Auto-detect Language",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Automatically detect the language in audio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoDetectLanguage,
                    onCheckedChange = { onToggleAutoDetect() }
                )
            }
            
            if (!autoDetectLanguage) {
                OutlinedTextField(
                    value = language ?: "",
                    onValueChange = { onLanguageChange(it.takeIf { it.isNotBlank() }) },
                    label = { Text("Language Code (e.g., en, es, fr)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Leave empty for auto-detection") }
                )
            }
        }
    }
}

@Composable
private fun ThemeCustomizationSection(
    selectedTheme: Theme,
    availableThemes: List<Theme>,
    onThemeSelected: (Theme) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium
            )
            
            availableThemes.forEach { theme ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedTheme == theme,
                        onClick = { onThemeSelected(theme) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (theme) {
                            Theme.Light -> "Light"
                            Theme.Dark -> "Dark"
                            Theme.System -> "Follow System"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyControlsSection(
    enableHapticFeedback: Boolean,
    enableBatteryOptimization: Boolean,
    onToggleHapticFeedback: () -> Unit,
    onToggleBatteryOptimization: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Privacy & Performance",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Haptic Feedback",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Vibrate on interactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enableHapticFeedback,
                    onCheckedChange = { onToggleHapticFeedback() }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Battery Optimization",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Optimize for battery life",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enableBatteryOptimization,
                    onCheckedChange = { onToggleBatteryOptimization() }
                )
            }
        }
    }
}