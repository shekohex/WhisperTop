package me.shadykhalifa.whispertop.presentation.viewmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelPrivacyTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var mockSettingsRepository: MockSettingsRepository
    private lateinit var mockSecurePreferencesRepository: MockSecurePreferencesRepository
    private lateinit var viewModel: SettingsViewModel

    @BeforeTest
    fun setup() {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        mockSettingsRepository = MockSettingsRepository()
        mockSecurePreferencesRepository = MockSecurePreferencesRepository()
        viewModel = SettingsViewModel(mockSettingsRepository, mockSecurePreferencesRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleUsageAnalytics should toggle the setting`() = runTest {
        // Initial state should have analytics disabled
        assertFalse(viewModel.uiState.value.settings.enableUsageAnalytics)
        
        // Toggle analytics on
        viewModel.toggleUsageAnalytics()
        
        // Verify setting was updated
        assertTrue(mockSettingsRepository.lastUpdatedSettings?.enableUsageAnalytics ?: false)
    }
    
    @Test
    fun `toggleApiCallLogging should toggle the setting`() = runTest {
        // Initial state should have logging disabled
        assertFalse(viewModel.uiState.value.settings.enableApiCallLogging)
        
        // Toggle logging on
        viewModel.toggleApiCallLogging()
        
        // Verify setting was updated
        assertTrue(mockSettingsRepository.lastUpdatedSettings?.enableApiCallLogging ?: false)
    }
    
    @Test
    fun `toggleAutoCleanupTempFiles should toggle the setting`() = runTest {
        // Initial state should have auto cleanup enabled
        assertTrue(viewModel.uiState.value.settings.autoCleanupTempFiles)
        
        // Toggle auto cleanup off
        viewModel.toggleAutoCleanupTempFiles()
        
        // Verify setting was updated
        assertFalse(mockSettingsRepository.lastUpdatedSettings?.autoCleanupTempFiles ?: true)
    }
    
    @Test
    fun `updateTempFileRetentionDays should update and clamp value`() = runTest {
        // Test normal update
        viewModel.updateTempFileRetentionDays(14)
        assertEquals(14, mockSettingsRepository.lastUpdatedSettings?.tempFileRetentionDays)
        
        // Test clamping below minimum
        viewModel.updateTempFileRetentionDays(0)
        assertEquals(1, mockSettingsRepository.lastUpdatedSettings?.tempFileRetentionDays)
        
        // Test clamping above maximum
        viewModel.updateTempFileRetentionDays(50)
        assertEquals(30, mockSettingsRepository.lastUpdatedSettings?.tempFileRetentionDays)
    }
    
    @Test
    fun `showClearAllDataDialog should update dialog state`() = runTest {
        // Initially dialog should be hidden
        assertFalse(viewModel.uiState.value.showClearAllDataDialog)
        
        // Show dialog
        viewModel.showClearAllDataDialog()
        assertTrue(viewModel.uiState.value.showClearAllDataDialog)
        
        // Dismiss dialog
        viewModel.dismissClearAllDataDialog()
        assertFalse(viewModel.uiState.value.showClearAllDataDialog)
    }
    
    @Test
    fun `confirmClearAllData should call repository and hide dialog`() = runTest {
        // Show dialog first
        viewModel.showClearAllDataDialog()
        assertTrue(viewModel.uiState.value.showClearAllDataDialog)
        
        // Confirm clear all data
        viewModel.confirmClearAllData()
        
        // Verify repository method was called
        assertTrue(mockSettingsRepository.clearAllDataCalled)
        
        // Verify dialog was hidden
        assertFalse(viewModel.uiState.value.showClearAllDataDialog)
        
        // Verify loading state was managed
        assertFalse(viewModel.uiState.value.clearingAllData)
    }
    
    @Test
    fun `cleanupTemporaryFiles should call repository and manage loading state`() = runTest {
        // Initially not cleaning
        assertFalse(viewModel.uiState.value.cleaningTempFiles)
        
        // Start cleanup
        viewModel.cleanupTemporaryFiles()
        
        // Verify repository method was called
        assertTrue(mockSettingsRepository.cleanupTemporaryFilesCalled)
        
        // Verify loading state was managed (should be false after completion)
        assertFalse(viewModel.uiState.value.cleaningTempFiles)
    }
    
    @Test
    fun `privacy policy dialog state should be managed correctly`() = runTest {
        // Initially dialog should be hidden
        assertFalse(viewModel.uiState.value.showPrivacyPolicyDialog)
        
        // Show dialog
        viewModel.showPrivacyPolicyDialog()
        assertTrue(viewModel.uiState.value.showPrivacyPolicyDialog)
        
        // Dismiss dialog
        viewModel.dismissPrivacyPolicyDialog()
        assertFalse(viewModel.uiState.value.showPrivacyPolicyDialog)
    }
    
    @Test
    fun `confirmClearAllData should clear optimistic values`() = runTest {
        // Set some optimistic values
        viewModel.updateApiKeyValue("sk-test-key")
        assertEquals("sk-test-key", viewModel.uiState.value.apiKeyValue)
        
        // Clear all data
        viewModel.confirmClearAllData()
        
        // Verify optimistic values were cleared
        assertEquals("", viewModel.uiState.value.apiKeyValue)
        assertEquals(null, viewModel.uiState.value.optimisticApiKey)
        assertTrue(viewModel.uiState.value.validationErrors.isEmpty())
        assertEquals(null, viewModel.uiState.value.connectionTestResult)
    }
}

// Mock implementations for testing
private class MockSettingsRepository : SettingsRepository {
    private val _settings = MutableStateFlow(AppSettings())
    var lastUpdatedSettings: AppSettings? = null
    var clearAllDataCalled = false
    var cleanupTemporaryFilesCalled = false

    override val settings: Flow<AppSettings> = _settings

    override suspend fun getSettings(): AppSettings = _settings.value

    override suspend fun updateApiKey(apiKey: String): Result<Unit> = Result.Success(Unit)

    override suspend fun updateSelectedModel(model: String): Result<Unit> = Result.Success(Unit)

    override suspend fun updateLanguage(language: String?): Result<Unit> = Result.Success(Unit)

    override suspend fun updateTheme(theme: Theme): Result<Unit> = Result.Success(Unit)

    override suspend fun updateSettings(settings: AppSettings): Result<Unit> {
        lastUpdatedSettings = settings
        _settings.value = settings
        return Result.Success(Unit)
    }

    override suspend fun clearApiKey(): Result<Unit> = Result.Success(Unit)

    override suspend fun clearAllData(): Result<Unit> {
        clearAllDataCalled = true
        _settings.value = AppSettings() // Reset to defaults
        return Result.Success(Unit)
    }

    override suspend fun cleanupTemporaryFiles(): Result<Unit> {
        cleanupTemporaryFilesCalled = true
        return Result.Success(Unit)
    }
}

private class MockSecurePreferencesRepository : SecurePreferencesRepository {
    override suspend fun saveApiKey(apiKey: String): Result<Unit> = Result.Success(Unit)
    override suspend fun getApiKey(): Result<String?> = Result.Success(null)
    override suspend fun clearApiKey(): Result<Unit> = Result.Success(Unit)
    override suspend fun hasApiKey(): Result<Boolean> = Result.Success(false)
    override suspend fun saveApiEndpoint(endpoint: String): Result<Unit> = Result.Success(Unit)
    override suspend fun getApiEndpoint(): Result<String> = Result.Success("https://api.openai.com/v1/")
    override fun validateApiKey(apiKey: String): Boolean = apiKey.startsWith("sk-") && apiKey.length >= 51
}