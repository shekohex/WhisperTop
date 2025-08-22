package me.shadykhalifa.whispertop.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.anyOrNull
import me.shadykhalifa.whispertop.data.permissions.PermissionMonitor
import me.shadykhalifa.whispertop.domain.models.AppPermission
import me.shadykhalifa.whispertop.domain.models.PermissionResult
import me.shadykhalifa.whispertop.domain.models.PermissionState
import me.shadykhalifa.whispertop.domain.repositories.PermissionRepository

@ExperimentalCoroutinesApi
class PermissionsViewModelTest {
    
    private lateinit var testDispatcher: TestCoroutineDispatcher
    private lateinit var testScope: TestCoroutineScope
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockPermissionRepository: PermissionRepository
    
    @Mock
    private lateinit var mockPermissionMonitor: PermissionMonitor
    
    private lateinit var viewModel: PermissionsViewModel
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testDispatcher = TestCoroutineDispatcher()
        testScope = TestCoroutineScope(testDispatcher)
        
        whenever(mockContext.packageName).thenReturn("me.shadykhalifa.whispertop")
        
        // Mock PermissionMonitor flows
        whenever(mockPermissionMonitor.permissionStates).thenReturn(
            MutableStateFlow(emptyMap())
        )
        whenever(mockPermissionMonitor.isMonitoring).thenReturn(
            MutableStateFlow(false)
        )
        
        viewModel = PermissionsViewModel(
            context = mockContext,
            permissionRepository = mockPermissionRepository,
            permissionMonitor = mockPermissionMonitor
        )
    }
    
    @Test
    fun testInitialState() = testScope.runBlockingTest {
        val initialUiState = viewModel.uiState.first()
        
        assertEquals(0, initialUiState.criticalPermissionsGranted)
        assertEquals(0, initialUiState.totalCriticalPermissions)
        assertEquals(0, initialUiState.optionalPermissionsGranted)
        assertEquals(0, initialUiState.totalOptionalPermissions)
        assertFalse(initialUiState.allCriticalPermissionsGranted)
        assertFalse(initialUiState.hasOptionalPermissions)
        assertNull(initialUiState.requestingPermission)
    }
    
    @Test
    fun testStartMonitoring() {
        viewModel.startMonitoring()
        verify(mockPermissionMonitor).startMonitoring()
    }
    
    @Test
    fun testStopMonitoring() {
        viewModel.stopMonitoring()
        verify(mockPermissionMonitor).stopMonitoring()
    }
    
    @Test
    fun testPermissionCategorization() {
        val criticalPermissions = viewModel.getCriticalPermissions()
        val optionalPermissions = viewModel.getOptionalPermissions()
        
        assertTrue("Should have critical permissions", criticalPermissions.isNotEmpty())
        assertTrue("Record Audio should be critical", 
            criticalPermissions.contains(AppPermission.RECORD_AUDIO))
        assertTrue("System Alert Window should be critical", 
            criticalPermissions.contains(AppPermission.SYSTEM_ALERT_WINDOW))
        
        if (optionalPermissions.isNotEmpty()) {
            assertFalse("Optional permissions should not overlap with critical", 
                optionalPermissions.any { it in criticalPermissions })
        }
    }
    
    @Test
    fun testRequestPermissionAlreadyGranted() = testScope.runBlockingTest {
        val permission = AppPermission.RECORD_AUDIO
        val grantedState = PermissionState(permission, isGranted = true)
        
        // Mock the permission state as granted
        val stateMap = mapOf(permission to grantedState)
        whenever(mockPermissionMonitor.permissionStates).thenReturn(
            MutableStateFlow(stateMap)
        )
        
        val result = viewModel.requestPermission(permission)
        
        assertEquals(PermissionResult.AlreadyGranted, result)
    }
    
    @Test
    fun testRequestPermissionWithBackoffDelay() = testScope.runBlockingTest {
        val permission = AppPermission.RECORD_AUDIO
        val currentTime = System.currentTimeMillis()
        val deniedState = PermissionState(
            permission = permission,
            isGranted = false,
            denialCount = 2,
            lastDeniedTimestamp = currentTime - 10_000L // 10 seconds ago
        )
        
        // Mock the permission state with recent denial
        val stateMap = mapOf(permission to deniedState)
        whenever(mockPermissionMonitor.permissionStates).thenReturn(
            MutableStateFlow(stateMap)
        )
        
        val result = viewModel.requestPermission(permission)
        
        assertTrue("Should return TooEarly result", result is PermissionResult.TooEarly)
        if (result is PermissionResult.TooEarly) {
            assertEquals(permission, result.permission)
            assertTrue("Retry time should be positive", result.retryAfterMs > 0)
        }
    }
    
    @Test
    fun testRequestPermissionRequiresSettings() = testScope.runBlockingTest {
        val permission = AppPermission.RECORD_AUDIO
        val permanentlyDeniedState = PermissionState(
            permission = permission,
            isGranted = false,
            isPermanentlyDenied = true,
            canShowRationale = false
        )
        
        val stateMap = mapOf(permission to permanentlyDeniedState)
        whenever(mockPermissionMonitor.permissionStates).thenReturn(
            MutableStateFlow(stateMap)
        )
        
        val result = viewModel.requestPermission(permission)
        
        assertEquals(PermissionResult.RequiresSettings(permission), result)
    }
    
    @Test
    fun testRequestPermissionShowRationale() = testScope.runBlockingTest {
        val permission = AppPermission.RECORD_AUDIO
        val rationaleState = PermissionState(
            permission = permission,
            isGranted = false,
            canShowRationale = true,
            isPermanentlyDenied = false
        )
        
        val stateMap = mapOf(permission to rationaleState)
        whenever(mockPermissionMonitor.permissionStates).thenReturn(
            MutableStateFlow(stateMap)
        )
        
        val result = viewModel.requestPermission(permission)
        
        assertEquals(PermissionResult.ShowRationale(permission), result)
    }
    
    @Test
    fun testPermissionStateUpdatesReflectedInUi() = testScope.runBlockingTest {
        val recordAudioState = PermissionState(AppPermission.RECORD_AUDIO, isGranted = true)
        val overlayState = PermissionState(AppPermission.SYSTEM_ALERT_WINDOW, isGranted = false)
        val notificationState = PermissionState(AppPermission.POST_NOTIFICATIONS, isGranted = true)
        
        val initialStates = mapOf(
            AppPermission.RECORD_AUDIO to recordAudioState,
            AppPermission.SYSTEM_ALERT_WINDOW to overlayState,
            AppPermission.POST_NOTIFICATIONS to notificationState
        )
        
        val stateFlow = MutableStateFlow(initialStates)
        whenever(mockPermissionMonitor.permissionStates).thenReturn(stateFlow)
        whenever(mockPermissionMonitor.areAllCriticalPermissionsGranted()).thenReturn(false)
        whenever(mockPermissionMonitor.getCriticalPermissionsStatus()).thenReturn(Pair(1, 2))
        whenever(mockPermissionMonitor.getOptionalPermissionsStatus()).thenReturn(Pair(1, 1))
        
        // Allow the state to propagate
        testDispatcher.scheduler.advanceUntilIdle()
        
        val uiState = viewModel.uiState.first()
        
        assertEquals(1, uiState.criticalPermissionsGranted)
        assertEquals(2, uiState.totalCriticalPermissions)
        assertEquals(1, uiState.optionalPermissionsGranted)
        assertEquals(1, uiState.totalOptionalPermissions)
        assertFalse(uiState.allCriticalPermissionsGranted)
        assertTrue(uiState.hasOptionalPermissions)
    }
    
    @Test
    fun testPermissionBackoffDelayCalculation() {
        val permission = AppPermission.RECORD_AUDIO
        
        // Test no delay for first denial
        val firstDenial = PermissionState(
            permission = permission,
            isGranted = false,
            denialCount = 1,
            lastDeniedTimestamp = System.currentTimeMillis()
        )
        assertEquals(0L, firstDenial.nextRequestAllowedTime - firstDenial.lastDeniedTimestamp)
        
        // Test 30 second delay for second denial
        val secondDenial = PermissionState(
            permission = permission,
            isGranted = false,
            denialCount = 2,
            lastDeniedTimestamp = System.currentTimeMillis()
        )
        assertEquals(30_000L, secondDenial.nextRequestAllowedTime - secondDenial.lastDeniedTimestamp)
        
        // Test 5 minute delay for third denial
        val thirdDenial = PermissionState(
            permission = permission,
            isGranted = false,
            denialCount = 3,
            lastDeniedTimestamp = System.currentTimeMillis()
        )
        assertEquals(300_000L, thirdDenial.nextRequestAllowedTime - thirdDenial.lastDeniedTimestamp)
    }
    
    @Test
    fun testPermissionStateHelpers() {
        val permission = AppPermission.RECORD_AUDIO
        
        // Test needsRationale
        val rationaleState = PermissionState(
            permission = permission,
            isGranted = false,
            canShowRationale = true,
            isPermanentlyDenied = false
        )
        assertTrue("Should need rationale", rationaleState.needsRationale)
        
        // Test requiresSettings
        val settingsState = PermissionState(
            permission = permission,
            isGranted = false,
            canShowRationale = false,
            isPermanentlyDenied = true
        )
        assertTrue("Should require settings", settingsState.requiresSettings)
        
        // Test granted state
        val grantedState = PermissionState(permission, isGranted = true)
        assertFalse("Granted permission should not need rationale", grantedState.needsRationale)
        assertFalse("Granted permission should not require settings", grantedState.requiresSettings)
    }
    
    @Test
    fun testGetPermissionState() {
        val permission = AppPermission.RECORD_AUDIO
        val expectedState = PermissionState(permission, isGranted = true)
        
        val stateMap = mapOf(permission to expectedState)
        whenever(mockPermissionMonitor.permissionStates).thenReturn(
            MutableStateFlow(stateMap)
        )
        
        // Allow state to propagate
        testDispatcher.scheduler.advanceUntilIdle()
        
        val actualState = viewModel.getPermissionState(permission)
        assertEquals(expectedState, actualState)
    }
    
    @Test
    fun testClearLastResult() = testScope.runBlockingTest {
        // Set a result first
        val result = PermissionResult.Granted
        viewModel.clearLastResult()
        
        val lastResult = viewModel.lastPermissionResult.first()
        assertNull("Last result should be cleared", lastResult)
    }
    
    @Test
    fun testCleanupCallsMonitorCleanup() {
        viewModel.onCleared()
        verify(mockPermissionMonitor).cleanup()
    }
    
    @Test 
    fun testApiLevelCompatibility() {
        // Test that permission filtering works correctly for different API levels
        val allPermissions = AppPermission.values()
        val api26Permissions = AppPermission.getPermissionsForApiLevel(26)
        val api33Permissions = AppPermission.getPermissionsForApiLevel(33)
        val api34Permissions = AppPermission.getPermissionsForApiLevel(34)
        
        // API 26 should include basic permissions but not newer ones
        assertTrue("API 26 should include RECORD_AUDIO", 
            api26Permissions.contains(AppPermission.RECORD_AUDIO))
        assertFalse("API 26 should not include POST_NOTIFICATIONS", 
            api26Permissions.contains(AppPermission.POST_NOTIFICATIONS))
        
        // API 33 should include notification permission
        assertTrue("API 33 should include POST_NOTIFICATIONS", 
            api33Permissions.contains(AppPermission.POST_NOTIFICATIONS))
        
        // API 34 should include foreground service microphone permission
        assertTrue("API 34 should include FOREGROUND_SERVICE_MICROPHONE", 
            api34Permissions.contains(AppPermission.FOREGROUND_SERVICE_MICROPHONE))
        
        // Verify size differences
        assertTrue("Higher API levels should include more permissions", 
            api34Permissions.size >= api33Permissions.size)
        assertTrue("Higher API levels should include more permissions", 
            api33Permissions.size >= api26Permissions.size)
    }
}