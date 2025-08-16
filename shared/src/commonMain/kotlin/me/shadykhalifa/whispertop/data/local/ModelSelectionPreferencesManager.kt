package me.shadykhalifa.whispertop.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.shadykhalifa.whispertop.domain.models.OpenAIModel

class ModelSelectionPreferencesManager(
    private val preferencesDataSource: PreferencesDataSource
) {
    
    suspend fun getSelectedModel(): String {
        return preferencesDataSource.getSettings().selectedModel
    }
    
    suspend fun setSelectedModel(modelId: String) {
        val currentSettings = preferencesDataSource.getSettings()
        val updatedSettings = currentSettings.copy(selectedModel = modelId)
        preferencesDataSource.saveSettings(updatedSettings)
    }
    
    fun getSelectedModelFlow(): Flow<String> {
        return preferencesDataSource.getSettingsFlow().map { it.selectedModel }
    }
    
    suspend fun getCustomModels(): List<String> {
        return preferencesDataSource.getSettings().customModels
    }
    
    suspend fun addCustomModel(modelId: String) {
        val currentSettings = preferencesDataSource.getSettings()
        val updatedCustomModels = currentSettings.customModels.toMutableList()
        if (!updatedCustomModels.contains(modelId)) {
            updatedCustomModels.add(modelId)
        }
        val updatedSettings = currentSettings.copy(customModels = updatedCustomModels)
        preferencesDataSource.saveSettings(updatedSettings)
    }
    
    suspend fun removeCustomModel(modelId: String) {
        val currentSettings = preferencesDataSource.getSettings()
        val updatedCustomModels = currentSettings.customModels.toMutableList()
        updatedCustomModels.remove(modelId)
        val updatedSettings = currentSettings.copy(customModels = updatedCustomModels)
        preferencesDataSource.saveSettings(updatedSettings)
    }
    
    fun getCustomModelsFlow(): Flow<List<String>> {
        return preferencesDataSource.getSettingsFlow().map { it.customModels }
    }
    
    suspend fun getModelPreference(modelId: String): String? {
        return preferencesDataSource.getSettings().modelPreferences[modelId]
    }
    
    suspend fun setModelPreference(modelId: String, useCase: String) {
        val currentSettings = preferencesDataSource.getSettings()
        val updatedPreferences = currentSettings.modelPreferences.toMutableMap()
        updatedPreferences[modelId] = useCase
        val updatedSettings = currentSettings.copy(modelPreferences = updatedPreferences)
        preferencesDataSource.saveSettings(updatedSettings)
    }
    
    fun getModelPreferencesFlow(): Flow<Map<String, String>> {
        return preferencesDataSource.getSettingsFlow().map { it.modelPreferences }
    }
    
    suspend fun getAllAvailableModels(): List<OpenAIModel> {
        val customModels = getCustomModels()
        val customModelObjects = customModels.map { modelId ->
            OpenAIModel(
                modelId = modelId,
                displayName = modelId,
                description = "Custom model",
                capabilities = me.shadykhalifa.whispertop.domain.models.ModelCapability.BALANCED,
                pricing = me.shadykhalifa.whispertop.domain.models.PricingInfo(pricePerMinute = 0.0),
                useCase = me.shadykhalifa.whispertop.domain.models.ModelUseCase.GENERAL_PURPOSE,
                isCustom = true
            )
        }
        return OpenAIModel.PREDEFINED_MODELS + customModelObjects
    }
    
    suspend fun getSelectedModelObject(): OpenAIModel {
        val selectedModelId = getSelectedModel()
        return getAllAvailableModels().find { it.modelId == selectedModelId }
            ?: OpenAIModel.getDefaultModel()
    }
    
    suspend fun selectDefaultModelByUseCase(useCase: me.shadykhalifa.whispertop.domain.models.ModelUseCase): OpenAIModel {
        val model = OpenAIModel.PREDEFINED_MODELS.find { it.useCase == useCase }
            ?: OpenAIModel.getDefaultModel()
        setSelectedModel(model.modelId)
        return model
    }
}