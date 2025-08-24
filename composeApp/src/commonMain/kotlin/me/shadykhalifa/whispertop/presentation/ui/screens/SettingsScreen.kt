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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.models.WhisperModel
import me.shadykhalifa.whispertop.domain.models.OpenAIModel
import me.shadykhalifa.whispertop.domain.models.ModelUseCase
import me.shadykhalifa.whispertop.domain.models.ModelCapability
import me.shadykhalifa.whispertop.domain.models.PricingInfo
import me.shadykhalifa.whispertop.presentation.viewmodels.SettingsViewModel
import me.shadykhalifa.whispertop.presentation.viewmodels.ConnectionTestResult
import me.shadykhalifa.whispertop.presentation.viewmodels.ModelSelectionViewModel
import me.shadykhalifa.whispertop.presentation.ui.components.ModelSelectionDropdown
import me.shadykhalifa.whispertop.presentation.ui.components.ModelCapabilityCard
import me.shadykhalifa.whispertop.presentation.ui.components.ModelRecommendationChip
import me.shadykhalifa.whispertop.presentation.ui.components.CustomModelInput
import me.shadykhalifa.whispertop.presentation.ui.components.LanguagePreferenceSection
import me.shadykhalifa.whispertop.presentation.ui.components.BatteryOptimizationSection
import me.shadykhalifa.whispertop.presentation.ui.components.TranscriptionCustomizationSection
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // OpenAI API Configuration
                ApiConfigurationSection(
                    apiKey = uiState.apiKey,
                    customEndpoint = uiState.customEndpoint,
                    isValid = uiState.isValid,
                    timeout = uiState.timeout,
                    onApiKeyChanged = viewModel::updateApiKey,
                    onCustomEndpointChanged = viewModel::updateCustomEndpoint,
                    onTimeoutChanged = viewModel::updateTimeout,
                    onValidateCredentials = viewModel::validateCredentials,
                    onResetToDefault = viewModel::resetToDefaults,
                    onDeleteAllData = viewModel::deleteAllData,
                    onToggleCustomEndpoint = viewModel::toggleCustomEndpoint,
                    onToggleDebugMode = viewModel::toggleDebugMode,
                    currentState = uiState,
                    
                    // New Model Selection parameters
                    availableModels = modelSelectionState.availableModels,
                    selectedModel = modelSelectionViewModel::selectModel,
                    onRefreshModels = modelSelectionViewModel::refreshModels,
                    onAddCustomModel = modelSelectionViewModel::addCustomModel,
                    onRemoveCustomModel = modelSelectionViewModel::removeCustomModel,
                    onSelectUseCaseRecommendation = modelSelectionViewModel::selectUseCaseRecommendation,
                    onUpdateModelParameters = modelSelectionViewModel::updateModelParameters
                )

                // Data Privacy Section 
                DataPrivacySection(
                    retentionDays = uiState.retentionDays,
                    autoDelete = uiState.autoDelete ?: false,
                    onRetentionDaysChanged = viewModel::updateRetentionDays,
                    onAutoDeleteChanged = viewModel::updateAutoDelete
                )

                // Enhanced Model Selection Section
                EnhancedModelSelectionSection(
                    settings = uiState,
                    modelSelectionState = modelSelectionState,
                    onModelSelected = modelSelectionViewModel::selectModel,
                    onUseCaseRecommendationSelected = modelSelectionViewModel::selectUseCaseRecommendation,
                    onAddCustomModel = modelSelectionViewModel::addCustomModel,
                    onRemoveCustomModel = modelSelectionViewModel::removeCustomModel,
                    onRefreshModels = modelSelectionViewModel::refreshModels,
                    onUpdateModelParameters = modelSelectionViewModel::updateModelParameters,
                    onTestModel = modelSelectionViewModel::testModel,
                    onSaveModelConfiguration = modelSelectionViewModel::saveConfiguration,
                    onResetModelConfiguration = modelSelectionViewModel::resetConfiguration
                )

                // Loading indicator
                if (isLoading || modelSelectionState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WpmConfigurationSection(
    currentWpm: Int,
    onWpmChanged: (Int) -> Unit
) {
    var wpmInput by remember(currentWpm) { mutableStateOf(currentWpm.toString()) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Typing Speed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Used to calculate time saved by speech-to-text",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // WPM Display Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$currentWpm WPM",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Words Per Minute",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // WPM Slider
        Text(
            text = "Adjust typing speed (20-60 WPM)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Slider(
            value = currentWpm.toFloat(),
            onValueChange = { value ->
                val newWpm = value.toInt()
                onWpmChanged(newWpm)
            },
            valueRange = 20f..60f,
            steps = 40,
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "20 WPM\nBeginner",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "60 WPM\nExpert",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
private fun EnhancedModelSelectionSection(
    settings: AppSettings,
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
            
            // Conditional Model Selection based on endpoint type
            if (settings.isOpenAIEndpoint()) {
                // Show dropdown for OpenAI endpoints
                ModelSelectionDropdown(
                    selectedModel = modelSelectionState.selectedModel,
                    availableModels = modelSelectionState.availableModels,
                    onModelSelected = onModelSelected,
                    onAddCustomModel = onAddCustomModel,
                    enabled = !modelSelectionState.isLoading
                )
            } else {
                // Show custom model input for custom endpoints (no dialog, direct editing)
                CustomModelInput(
                    customModelName = settings.selectedModel,
                    onCustomModelNameChange = { newModel ->
                        // Only save if the model is not blank to avoid clearing saved models
                        if (newModel.isNotBlank()) {
                            // Convert string to OpenAIModel and call onModelSelected
                            val customModel = OpenAIModel(
                                modelId = newModel,
                                displayName = newModel,
                                description = "Custom model",
                                capabilities = ModelCapability.BALANCED,
                                pricing = PricingInfo(pricePerMinute = 0.0),
                                useCase = ModelUseCase.GENERAL_PURPOSE,
                                isCustom = true
                            )
                            onModelSelected(customModel)
                        }
                    },
                    onAddCustomModel = { /* Not used in direct mode */ },
                    onCancel = { /* Not used in direct mode */ },
                    isValid = settings.selectedModel.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    directMode = true // Enable direct mode to hide buttons
                )
            }
            
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
            
            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (apiKeyValue.isNotEmpty()) {
                    Button(
                        onClick = onValidateAndSave,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save API Key")
                    }
                }
                
                if (apiKey.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTestConnection,
                            enabled = !testingConnection && !isLoading,
                            modifier = Modifier.weight(1f)
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
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear API Key")
                        }
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
    settings: AppSettings,
    showClearAllDataDialog: Boolean,
    showPrivacyPolicyDialog: Boolean,
    clearingAllData: Boolean,
    cleaningTempFiles: Boolean,
    onToggleHapticFeedback: () -> Unit,

    onToggleUsageAnalytics: () -> Unit,
    onToggleApiCallLogging: () -> Unit,
    onToggleAutoCleanupTempFiles: () -> Unit,
    onUpdateTempFileRetentionDays: (Int) -> Unit,
    onShowClearAllDataDialog: () -> Unit,
    onConfirmClearAllData: () -> Unit,
    onDismissClearAllDataDialog: () -> Unit,
    onCleanupTemporaryFiles: () -> Unit,
    onShowPrivacyPolicyDialog: () -> Unit,
    onDismissPrivacyPolicyDialog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Privacy & Data Management",
                style = MaterialTheme.typography.titleMedium
            )
            
            // Data Usage Transparency
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Data Usage",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "WhisperTop only uses your own OpenAI API key to process audio.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Audio files are processed locally and sent directly to OpenAI",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• No data is stored on our servers",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• API keys are stored securely on your device",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            HorizontalDivider()
            
            // Privacy Settings
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
                        text = "Usage Analytics",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Help improve the app by sharing anonymous usage data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.enableUsageAnalytics,
                    onCheckedChange = { onToggleUsageAnalytics() }
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
                        text = "API Call Logging",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Log API calls for debugging purposes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.enableApiCallLogging,
                    onCheckedChange = { onToggleApiCallLogging() }
                )
            }
            
            HorizontalDivider()
            
            // Temporary File Management
            Text(
                text = "File Management",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
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
                        text = "Auto-cleanup Temporary Files",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Automatically delete audio files after ${settings.tempFileRetentionDays} days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.autoCleanupTempFiles,
                    onCheckedChange = { onToggleAutoCleanupTempFiles() }
                )
            }
            
            if (settings.autoCleanupTempFiles) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Retention Days: ${settings.tempFileRetentionDays}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onUpdateTempFileRetentionDays(settings.tempFileRetentionDays - 1) },
                            enabled = settings.tempFileRetentionDays > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease days")
                        }
                        IconButton(
                            onClick = { onUpdateTempFileRetentionDays(settings.tempFileRetentionDays + 1) },
                            enabled = settings.tempFileRetentionDays < 30
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase days")
                        }
                    }
                }
            }
            
            Button(
                onClick = onCleanupTemporaryFiles,
                modifier = Modifier.fillMaxWidth(),
                enabled = !cleaningTempFiles
            ) {
                if (cleaningTempFiles) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Clean Up Temporary Files Now")
            }
            
            HorizontalDivider()
            
            // Performance Settings
            Text(
                text = "Performance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
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
                    checked = settings.enableHapticFeedback,
                    onCheckedChange = { onToggleHapticFeedback() }
                )
            }
            

            
            HorizontalDivider()
            
            // Data Management Actions
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onShowPrivacyPolicyDialog,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Privacy Policy")
                }
                
                Button(
                    onClick = onShowClearAllDataDialog,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !clearingAllData
                ) {
                    if (clearingAllData) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Clear All Data")
                }
            }
            
            // Clear All Data Confirmation Dialog
            if (showClearAllDataDialog) {
                AlertDialog(
                    onDismissRequest = onDismissClearAllDataDialog,
                    title = {
                        Text("Clear All Data")
                    },
                    text = {
                        Text(
                            "This will permanently delete:\n" +
                                    "• Your API key and settings\n" +
                                    "• All temporary audio files\n" +
                                    "• All stored preferences\n\n" +
                                    "This action cannot be undone."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = onConfirmClearAllData,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Clear All Data")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismissClearAllDataDialog) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Privacy Policy Dialog
            if (showPrivacyPolicyDialog) {
                AlertDialog(
                    onDismissRequest = onDismissPrivacyPolicyDialog,
                    title = {
                        Text("Privacy Policy")
                    },
                    text = {
                        Text(
                            "WhisperTop Privacy Policy\n\n" +
                                    "Data Collection:\n" +
                                    "• We do not collect any personal data\n" +
                                    "• Audio files are processed locally\n" +
                                    "• API keys are stored securely on your device\n\n" +
                                    "Data Usage:\n" +
                                    "• Audio is sent directly to OpenAI using your API key\n" +
                                    "• No data passes through our servers\n" +
                                    "• Usage analytics are anonymous and optional\n\n" +
                                    "Data Storage:\n" +
                                    "• Temporary audio files are automatically cleaned up\n" +
                                    "• Settings are stored locally\n" +
                                    "• You can clear all data at any time\n\n" +
                                    "Contact: For questions about privacy, please contact support."
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = onDismissPrivacyPolicyDialog) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}