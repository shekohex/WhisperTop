package me.shadykhalifa.whispertop.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.models.WhisperModel
import me.shadykhalifa.whispertop.domain.models.OpenAIModel
import me.shadykhalifa.whispertop.domain.models.ModelUseCase
import me.shadykhalifa.whispertop.presentation.viewmodels.SettingsViewModel
import me.shadykhalifa.whispertop.presentation.viewmodels.ConnectionTestResult
import me.shadykhalifa.whispertop.presentation.viewmodels.ModelSelectionViewModel
import me.shadykhalifa.whispertop.presentation.ui.components.ModelSelectionDropdown
import me.shadykhalifa.whispertop.presentation.ui.components.ModelCapabilityCard
import me.shadykhalifa.whispertop.presentation.ui.components.ModelRecommendationChip
import me.shadykhalifa.whispertop.presentation.ui.components.CustomModelInput
import me.shadykhalifa.whispertop.presentation.ui.components.LanguagePreferenceSection
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinInject(),
    modelSelectionViewModel: ModelSelectionViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val modelSelectionState by modelSelectionViewModel.uiState.collectAsStateWithLifecycle()
    
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
    
    LaunchedEffect(modelSelectionState.error) {
        modelSelectionState.error?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            modelSelectionViewModel.clearModelSelectionError()
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
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // API Configuration Section
                ApiConfigurationSection(
                    apiKey = uiState.optimisticApiKey ?: uiState.settings.apiKey,
                    apiKeyValue = uiState.apiKeyValue,
                    apiEndpoint = uiState.apiEndpoint,
                    isApiKeyVisible = uiState.isApiKeyVisible,
                    validationErrors = uiState.validationErrors,
                    testingConnection = uiState.testingConnection,
                    connectionTestResult = uiState.connectionTestResult,
                    showClearApiKeyDialog = uiState.showClearApiKeyDialog,
                    onApiKeyChange = viewModel::updateApiKey,
                    onApiKeyValueChange = viewModel::updateApiKeyValue,
                    onApiEndpointChange = viewModel::updateApiEndpoint,
                    onToggleApiKeyVisibility = viewModel::toggleApiKeyVisibility,
                    onClearApiKey = viewModel::clearApiKey,
                    onConfirmClearApiKey = viewModel::confirmClearApiKey,
                    onDismissClearApiKeyDialog = viewModel::dismissClearApiKeyDialog,
                    onValidateAndSave = viewModel::validateAndSaveApiKey,
                    onTestConnection = viewModel::testConnection,
                    onClearConnectionResult = viewModel::clearConnectionTestResult,
                    isLoading = uiState.savingApiKey
                )
                
                HorizontalDivider()
                
                // Enhanced Model Selection Section
                EnhancedModelSelectionSection(
                    modelSelectionState = modelSelectionState,
                    onModelSelected = modelSelectionViewModel::selectModel,
                    onUseCaseRecommendationSelected = modelSelectionViewModel::selectModelByUseCase,
                    onAddCustomModel = modelSelectionViewModel::showAddCustomModelDialog,
                    onRemoveCustomModel = modelSelectionViewModel::removeCustomModel,
                    onCustomModelInputChange = modelSelectionViewModel::updateCustomModelInput,
                    onConfirmAddCustomModel = modelSelectionViewModel::addCustomModel,
                    onCancelAddCustomModel = modelSelectionViewModel::hideAddCustomModelDialog
                )
                
                HorizontalDivider()
                
                // Language Preferences Section
                LanguagePreferenceSection(
                    preference = uiState.settings.languagePreference,
                    onPreferenceChange = viewModel::updateLanguagePreference
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
            
            if (isLoading || modelSelectionState.isLoading) {
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
private fun EnhancedModelSelectionSection(
    modelSelectionState: me.shadykhalifa.whispertop.presentation.viewmodels.ModelSelectionUiState,
    onModelSelected: (OpenAIModel) -> Unit,
    onUseCaseRecommendationSelected: (ModelUseCase) -> Unit,
    onAddCustomModel: () -> Unit,
    onRemoveCustomModel: (String) -> Unit,
    onCustomModelInputChange: (String) -> Unit,
    onConfirmAddCustomModel: () -> Unit,
    onCancelAddCustomModel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Model Selection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Model Selection Dropdown
            ModelSelectionDropdown(
                selectedModel = modelSelectionState.selectedModel,
                availableModels = modelSelectionState.availableModels,
                onModelSelected = onModelSelected,
                onAddCustomModel = onAddCustomModel,
                enabled = !modelSelectionState.isLoading
            )
            
            // Show selected model capabilities
            modelSelectionState.selectedModel?.let { selectedModel ->
                if (!selectedModel.isCustom) {
                    ModelCapabilityCard(
                        model = selectedModel
                    )
                }
            }
            
            // Use case recommendations
            if (modelSelectionState.recommendations.isNotEmpty()) {
                Text(
                    text = "Quick Selection",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(
                        items = modelSelectionState.recommendations.entries.toList(),
                        key = { it.key }
                    ) { (useCase, model) ->
                        ModelRecommendationChip(
                            useCase = useCase,
                            recommendedModel = model,
                            onRecommendationSelected = { onUseCaseRecommendationSelected(useCase) }
                        )
                    }
                }
            }
            
            // Custom Model Input Dialog
            if (modelSelectionState.showAddCustomModelDialog) {
                CustomModelInput(
                    customModelName = modelSelectionState.customModelInput,
                    onCustomModelNameChange = onCustomModelInputChange,
                    onAddCustomModel = onConfirmAddCustomModel,
                    onCancel = onCancelAddCustomModel,
                    isValid = modelSelectionState.customModelError == null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ApiConfigurationSection(
    apiKey: String,
    apiKeyValue: String,
    apiEndpoint: String,
    isApiKeyVisible: Boolean,
    validationErrors: Map<String, String>,
    testingConnection: Boolean,
    connectionTestResult: ConnectionTestResult?,
    showClearApiKeyDialog: Boolean,
    onApiKeyChange: (String) -> Unit,
    onApiKeyValueChange: (String) -> Unit,
    onApiEndpointChange: (String) -> Unit,
    onToggleApiKeyVisibility: () -> Unit,
    onClearApiKey: () -> Unit,
    onConfirmClearApiKey: () -> Unit,
    onDismissClearApiKeyDialog: () -> Unit,
    onValidateAndSave: () -> Unit,
    onTestConnection: () -> Unit,
    onClearConnectionResult: () -> Unit,
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "API Configuration",
                style = MaterialTheme.typography.titleMedium
            )
            
            // API Endpoint Configuration
            OutlinedTextField(
                value = apiEndpoint,
                onValueChange = onApiEndpointChange,
                label = { Text("API Endpoint") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://api.openai.com/v1/") }
            )
            
            // API Key Input
            OutlinedTextField(
                value = if (apiKeyValue.isNotEmpty()) apiKeyValue else apiKey,
                onValueChange = onApiKeyValueChange,
                label = { Text("OpenAI API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isApiKeyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                isError = validationErrors.containsKey("apiKey"),
                supportingText = {
                    validationErrors["apiKey"]?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
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
                }
            )
            
            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                if (apiKeyValue.isNotEmpty()) {
                    Button(
                        onClick = onValidateAndSave,
                        enabled = !isLoading
                    ) {
                        Text("Save API Key")
                    }
                }
                
                if (apiKey.isNotBlank()) {
                    Button(
                        onClick = onTestConnection,
                        enabled = !testingConnection && !isLoading
                    ) {
                        if (testingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Test Connection")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onClearApiKey,
                        enabled = !isLoading
                    ) {
                        Text("Clear API Key")
                    }
                }
            }
            
            // Connection Test Result
            connectionTestResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (result) {
                            ConnectionTestResult.SUCCESS -> Color.Green.copy(alpha = 0.1f)
                            ConnectionTestResult.INVALID_KEY -> Color.Red.copy(alpha = 0.1f)
                            ConnectionTestResult.NETWORK_ERROR -> Color(0xFFFF9800).copy(alpha = 0.1f)
                            ConnectionTestResult.SERVER_ERROR -> Color.Red.copy(alpha = 0.1f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when (result) {
                                    ConnectionTestResult.SUCCESS -> Icons.Default.Check
                                    ConnectionTestResult.INVALID_KEY -> Icons.Default.Close
                                    ConnectionTestResult.NETWORK_ERROR -> Icons.Default.Warning
                                    ConnectionTestResult.SERVER_ERROR -> Icons.Default.Close
                                },
                                contentDescription = null,
                                tint = when (result) {
                                    ConnectionTestResult.SUCCESS -> Color.Green
                                    ConnectionTestResult.INVALID_KEY -> Color.Red
                                    ConnectionTestResult.NETWORK_ERROR -> Color(0xFFFF9800)
                                    ConnectionTestResult.SERVER_ERROR -> Color.Red
                                }
                            )
                            
                            Text(
                                text = when (result) {
                                    ConnectionTestResult.SUCCESS -> "Connection successful! API key is valid."
                                    ConnectionTestResult.INVALID_KEY -> "Invalid API key. Please check your key."
                                    ConnectionTestResult.NETWORK_ERROR -> "Network error. Please check your connection."
                                    ConnectionTestResult.SERVER_ERROR -> "Server error. Please try again later."
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        IconButton(onClick = onClearConnectionResult) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss"
                            )
                        }
                    }
                }
            }
            
            // Clear API Key Confirmation Dialog
            if (showClearApiKeyDialog) {
                AlertDialog(
                    onDismissRequest = onDismissClearApiKeyDialog,
                    title = {
                        Text("Clear API Key")
                    },
                    text = {
                        Text("Are you sure you want to clear your API key? This action cannot be undone.")
                    },
                    confirmButton = {
                        TextButton(onClick = onConfirmClearApiKey) {
                            Text("Clear")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismissClearApiKeyDialog) {
                            Text("Cancel")
                        }
                    }
                )
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
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
private fun ThemeCustomizationSection(
    selectedTheme: Theme,
    availableThemes: List<Theme>,
    onThemeSelected: (Theme) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
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
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
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