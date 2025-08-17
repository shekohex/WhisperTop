package me.shadykhalifa.whispertop.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.shadykhalifa.whispertop.domain.models.ModelCapability
import me.shadykhalifa.whispertop.domain.models.ModelUseCase
import me.shadykhalifa.whispertop.domain.models.OpenAIModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionDropdown(
    selectedModel: OpenAIModel?,
    availableModels: List<OpenAIModel>,
    onModelSelected: (OpenAIModel) -> Unit,
    onAddCustomModel: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded && enabled },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedModel?.displayName ?: "Select a model",
            onValueChange = { },
            readOnly = true,
            enabled = enabled,
            label = { Text("Selected Model") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
                // Predefined models
                Text(
                    text = "Official OpenAI Models",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                availableModels.filter { !it.isCustom }.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            ModelDropdownItem(
                                model = model,
                                isSelected = selectedModel?.modelId == model.modelId
                            )
                        },
                        onClick = {
                            onModelSelected(model)
                            expanded = false
                        }
                    )
                }
                
                // Custom models section
                val customModels = availableModels.filter { it.isCustom }
                if (customModels.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = "Custom Models",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    
                    customModels.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                ModelDropdownItem(
                                    model = model,
                                    isSelected = selectedModel?.modelId == model.modelId
                                )
                            },
                            onClick = {
                                onModelSelected(model)
                                expanded = false
                            }
                )
        }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Add custom model option
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Add Custom Model",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    onClick = {
                        onAddCustomModel()
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ModelDropdownItem(
    model: OpenAIModel,
    isSelected: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            if (!model.isCustom) {
                Text(
                    text = "${model.pricing.formatPrice()} • ${model.useCase.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
}

@Composable
fun ModelCapabilityCard(
    model: OpenAIModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (model.isCustom) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Custom") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
            
            if (!model.isCustom) {
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Capability ratings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CapabilityRating(
                        label = "Speed",
                        rating = model.capabilities.speedRating,
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )
                    CapabilityRating(
                        label = "Accuracy",
                        rating = model.capabilities.accuracyRating,
                        icon = Icons.Default.Star,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Pricing info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pricing:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = model.pricing.formatPrice(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilityRating(
    label: String,
    rating: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (index < rating) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }
        }
    }
}

@Composable
fun ModelRecommendationChip(
    useCase: ModelUseCase,
    recommendedModel: OpenAIModel,
    onRecommendationSelected: (OpenAIModel) -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { onRecommendationSelected(recommendedModel) },
        label = {
            Text(
                text = "Best for ${useCase.displayName}: ${recommendedModel.displayName}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        modifier = modifier
    )
}

@OptIn(FlowPreview::class)
@Composable
fun CustomModelInput(
    customModelName: String,
    onCustomModelNameChange: (String) -> Unit,
    onAddCustomModel: () -> Unit,
    onCancel: () -> Unit,
    isValid: Boolean = true,
    modifier: Modifier = Modifier,
    directMode: Boolean = false // New parameter for direct model selection
) {
    var textFieldValue by remember { mutableStateOf(customModelName) }
    val debouncedFlow = remember { MutableStateFlow(customModelName) }
    
    // Update local state when customModelName changes externally
    LaunchedEffect(customModelName) {
        if (textFieldValue != customModelName) {
            textFieldValue = customModelName
            debouncedFlow.value = customModelName
        }
    }
    
    // Track initialization to prevent clearing saved models on first load
    var isInitialized by remember { mutableStateOf(false) }
    
    // Mark as initialized after customModelName is set
    LaunchedEffect(customModelName) {
        if (customModelName.isNotBlank()) {
            isInitialized = true
        }
    }
    
    // Debounce the input changes
    LaunchedEffect(Unit) {
        debouncedFlow
            .debounce(300) // 300ms debounce for better responsiveness
            .distinctUntilChanged()
            .collect { debouncedValue ->
                println("CustomModelInput: Debounced value collected: '$debouncedValue', isInitialized: $isInitialized")
                // Only save changes after initialization to prevent clearing saved models during initial render
                // OR if the user is intentionally providing a non-blank value
                if (isInitialized || debouncedValue.isNotBlank()) {
                    onCustomModelNameChange(debouncedValue)
                    println("CustomModelInput: onCustomModelNameChange called")
                } else {
                    println("CustomModelInput: onCustomModelNameChange SKIPPED - isInitialized: $isInitialized, isNotBlank: ${debouncedValue.isNotBlank()}")
                }
            }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!directMode) {
                Text(
                    text = "Add Custom Model",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    println("CustomModelInput: TextField onValueChange: '$newValue'")
                    textFieldValue = newValue
                    debouncedFlow.value = newValue
                    println("CustomModelInput: debouncedFlow.value set to: '$newValue'")
                },
                label = { Text("Model ID") },
                placeholder = { Text("e.g., whisper-large-v3, claude-3-5-sonnet, gpt-4-turbo") },
                modifier = Modifier.fillMaxWidth(),
                isError = !isValid,
                supportingText = {
                    if (!isValid) {
                        Text(
                            text = "Please enter a valid model ID",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Enter any model name supported by your API endpoint")
                    }
                }
            )
            
            if (!directMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = onCancel
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onAddCustomModel,
                        enabled = textFieldValue.isNotBlank() && isValid
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Model")
                    }
                }
            }
        }
    }
}