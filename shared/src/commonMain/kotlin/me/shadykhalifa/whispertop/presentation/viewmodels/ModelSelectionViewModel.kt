package me.shadykhalifa.whispertop.presentation.viewmodels

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.data.local.ModelSelectionPreferencesManager
import me.shadykhalifa.whispertop.domain.models.ModelUseCase
import me.shadykhalifa.whispertop.domain.models.OpenAIModel

class ModelSelectionViewModel(
    private val preferencesManager: ModelSelectionPreferencesManager
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ModelSelectionUiState())
    val uiState: StateFlow<ModelSelectionUiState> = _uiState.asStateFlow()

    init {
        loadModels()
        observeSelectedModel()
    }

    private fun loadModels() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val allModels = preferencesManager.getAllAvailableModels()
                val selectedModel = preferencesManager.getSelectedModelObject()
                val customModels = preferencesManager.getCustomModels()
                
                _uiState.update { currentState ->
                    currentState.copy(
                        availableModels = allModels,
                        selectedModel = selectedModel,
                        customModels = customModels,
                        recommendations = generateRecommendations(allModels),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load models: ${e.message}"
                    )
                }
            }
        }
    }

    private fun observeSelectedModel() {
        viewModelScope.launch {
            preferencesManager.getSelectedModelFlow()
                .catch { error ->
                    _uiState.update { 
                        it.copy(error = "Failed to observe model changes: ${error.message}")
                    }
                }
                .collect { selectedModelId ->
                    val selectedModel = _uiState.value.availableModels
                        .find { it.modelId == selectedModelId }
                        ?: OpenAIModel.getDefaultModel()
                    
                    _uiState.update { it.copy(selectedModel = selectedModel) }
                }
        }
    }

    fun selectModel(model: OpenAIModel) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                preferencesManager.setSelectedModel(model.modelId)
                
                _uiState.update { currentState ->
                    currentState.copy(
                        selectedModel = model,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to select model: ${e.message}"
                    )
                }
            }
        }
    }

    fun selectModelByUseCase(useCase: ModelUseCase) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val selectedModel = preferencesManager.selectDefaultModelByUseCase(useCase)
                
                _uiState.update { currentState ->
                    currentState.copy(
                        selectedModel = selectedModel,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to select model by use case: ${e.message}"
                    )
                }
            }
        }
    }

    fun showAddCustomModelDialog() {
        _uiState.update { 
            it.copy(
                showAddCustomModelDialog = true,
                customModelInput = "",
                customModelError = null
            )
        }
    }

    fun hideAddCustomModelDialog() {
        _uiState.update { 
            it.copy(
                showAddCustomModelDialog = false,
                customModelInput = "",
                customModelError = null
            )
        }
    }

    fun updateCustomModelInput(input: String) {
        _uiState.update { 
            it.copy(
                customModelInput = input,
                customModelError = null
            )
        }
    }

    fun addCustomModel() {
        val currentState = _uiState.value
        val modelInput = currentState.customModelInput.trim()

        if (modelInput.isEmpty()) {
            _uiState.update { 
                it.copy(customModelError = "Model ID cannot be empty")
            }
            return
        }

        if (currentState.availableModels.any { it.modelId == modelInput }) {
            _uiState.update { 
                it.copy(customModelError = "Model already exists")
            }
            return
        }

        if (!isValidModelId(modelInput)) {
            _uiState.update { 
                it.copy(customModelError = "Invalid model ID format")
            }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                preferencesManager.addCustomModel(modelInput)
                
                // Reload models to include the new custom model
                loadModels()
                
                _uiState.update { 
                    it.copy(
                        showAddCustomModelDialog = false,
                        customModelInput = "",
                        customModelError = null,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        customModelError = "Failed to add custom model: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun removeCustomModel(modelId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                preferencesManager.removeCustomModel(modelId)
                
                // If the removed model was selected, switch to default
                if (_uiState.value.selectedModel?.modelId == modelId) {
                    val defaultModel = OpenAIModel.getDefaultModel()
                    preferencesManager.setSelectedModel(defaultModel.modelId)
                }
                
                // Reload models
                loadModels()
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to remove custom model: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearModelSelectionError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun generateRecommendations(models: List<OpenAIModel>): Map<ModelUseCase, OpenAIModel> {
        return ModelUseCase.entries.associateWith { useCase ->
            models.find { !it.isCustom && it.useCase == useCase }
                ?: models.find { !it.isCustom }
                ?: OpenAIModel.getDefaultModel()
        }
    }

    private fun isValidModelId(modelId: String): Boolean {
        // Basic validation for model ID format
        return modelId.matches(Regex("^[a-zA-Z0-9_-]+$")) && 
               modelId.length >= 3 && 
               modelId.length <= 50
    }
}

data class ModelSelectionUiState(
    val availableModels: List<OpenAIModel> = emptyList(),
    val selectedModel: OpenAIModel? = null,
    val customModels: List<String> = emptyList(),
    val recommendations: Map<ModelUseCase, OpenAIModel> = emptyMap(),
    val showAddCustomModelDialog: Boolean = false,
    val customModelInput: String = "",
    val customModelError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)