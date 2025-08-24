package me.shadykhalifa.whispertop.presentation.viewmodels

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import me.shadykhalifa.whispertop.data.permissions.PermissionMonitor
import me.shadykhalifa.whispertop.data.repositories.AndroidPermissionRepository
import me.shadykhalifa.whispertop.domain.models.AppPermission
import me.shadykhalifa.whispertop.domain.models.PermissionResult
import me.shadykhalifa.whispertop.domain.repositories.PermissionRepository

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class PermissionsViewModelIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var permissionRepository: PermissionRepository
    private lateinit var permissionMonitor: PermissionMonitor
    private lateinit var viewModel: PermissionsViewModel
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        permissionRepository = AndroidPermissionRepository(context)
        permissionMonitor = PermissionMonitor(context)
        
        viewModel = PermissionsViewModel(
            context = context,
            permissionRepository = permissionRepository,
            permissionMonitor = permissionMonitor
        )
    }
    
    @Test
    fun testPermissionMonitorDetectsSystemChanges() = runTest {
        viewModel.startMonitoring()
        
        // Allow some time for initial check
        kotlinx.coroutines.delay(1000)
        
        val initialStates = viewModel.permissionStates.first()
        
        // Verify we have states for applicable permissions
        val applicablePermissions = AppPermission.getPermissionsForApiLevel(Build.VERSION.SDK_INT)
        assertTrue("Should have permission states", initialStates.isNotEmpty())
        
        // Check that critical permissions are being monitored
        val criticalPermissions = AppPermission.getCriticalPermissions()
            .filter { applicablePermissions.contains(it) }
        
        criticalPermissions.forEach { permission ->
            assertTrue("Should have state for critical permission ${permission.name}", 
                initialStates.containsKey(permission))
        }
        
        viewModel.stopMonitoring()
    }
    
    @Test
    fun testPermissionCategorizationReflectsActualSystem() = runTest {
        viewModel.startMonitoring()
        kotlinx.coroutines.delay(1000)
        
        val criticalPermissions = viewModel.getCriticalPermissions()
        val optionalPermissions = viewModel.getOptionalPermissions()
        
        // Verify categorization makes sense for the current API level
        assertTrue("Should have critical permissions", criticalPermissions.isNotEmpty())
        
        // All critical permissions should be applicable to current API level
        val currentApiLevel = Build.VERSION.SDK_INT
        criticalPermissions.forEach { permission ->
            assertTrue("Critical permission ${permission.name} should be applicable to API $currentApiLevel",
                permission.minSdkVersion <= currentApiLevel)
        }
        
        // Optional permissions should also be applicable
        optionalPermissions.forEach { permission ->
            assertTrue("Optional permission ${permission.name} should be applicable to API $currentApiLevel",
                permission.minSdkVersion <= currentApiLevel)
        }
        
        viewModel.stopMonitoring()
    }
    
    @Test
    fun testInternetPermissionAlwaysGranted() = runTest {
        viewModel.startMonitoring()
        kotlinx.coroutines.delay(1000)
        
        val permissionStates = viewModel.permissionStates.first()
        val internetState = permissionStates[AppPermission.INTERNET]
        
        assertNotNull("Internet permission should have state", internetState)
        assertTrue("Internet permission should be granted", internetState!!.isGranted)
        
        viewModel.stopMonitoring()
    }
    
    @Test
    fun testOverlayPermissionDetection() = runTest {
        viewModel.startMonitoring()
        kotlinx.coroutines.delay(1000)
        
        val permissionStates = viewModel.permissionStates.first()
        val overlayState = permissionStates[AppPermission.SYSTEM_ALERT_WINDOW]
        
        assertNotNull("Overlay permission should have state", overlayState)
        
        // Check against actual system state
        val actualCanDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        
        assertEquals("Overlay permission state should match system state", 
            actualCanDrawOverlays, overlayState!!.isGranted)
        
        viewModel.stopMonitoring()
    }
    
    @Test
    fun testAccessibilityServiceDetection() = runTest {
        viewModel.startMonitoring()
        kotlinx.coroutines.delay(1000)
        
        val permissionStates = viewModel.permissionStates.first()
        val accessibilityState = permissionStates[AppPermission.BIND_ACCESSIBILITY_SERVICE]
        
        assertNotNull("Accessibility permission should have state", accessibilityState)
        
        // This will likely be false in test environment, which is expected
        val actualAccessibilityEnabled = checkAccessibilityServiceEnabled()
        assertEquals("Accessibility permission state should match system state", 
            actualAccessibilityEnabled, accessibilityState!!.isGranted)
        
        viewModel.stopMonitoring()
    }
    
    @Test
    fun testMicrophonePermissionDetection() = runTest {
        viewModel.startMonitoring()
        kotlinx.coroutines.delay(1000)
        
        val permissionStates = viewModel.permissionStates.first()
        val microphoneState = permissionStates[AppPermission.RECORD_AUDIO]
        
        assertNotNull("Microphone permission should have state", microphoneState)
        
        // Check against actual system state
        val actualPermissionGranted = ContextCompat.checkSelfPermission(
            context, 
            AppPermission.RECORD_AUDIO.manifestPermission
        ) == PackageManager.PERMISSION_GRANTED
        
        assertEquals("Microphone permission state should match system state", 
            actualPermissionGranted, microphoneState!!.isGranted)
        
        viewModel.stopMonitoring()
    }
    
    @Test
    fun testUiStateReflectsPermissionChanges() = runTest {
        viewModel.startMonitoring()
        kotlinx.coroutines.delay(1000)
        
        val uiState = viewModel.uiState.first()
        
        // Verify UI state has reasonable values
        assertTrue("Total critical permissions should be positive", 
            uiState.totalCriticalPermissions > 0)
        assertTrue("Critical permissions granted should be non-negative", 
            uiState.criticalPermissionsGranted >= 0)
        assertTrue("Critical permissions granted should not exceed total", 
            uiState.criticalPermissionsGranted <= uiState.totalCriticalPermissions)
        
        // Optional permissions may be zero on some API levels
        assertTrue("Optional permissions granted should be non-negative", 
            uiState.optionalPermissionsGranted >= 0)
        assertTrue("Optional permissions granted should not exceed total", 
            uiState.optionalPermissionsGranted <= uiState.totalOptionalPermissions)
        
        // Check consistency
        val allCriticalGranted = uiState.criticalPermissionsGranted == uiState.totalCriticalPermissions
        assertEquals("allCriticalPermissionsGranted should match calculation", 
            allCriticalGranted, uiState.allCriticalPermissionsGranted)
        
        val hasOptional = uiState.totalOptionalPermissions > 0
        assertEquals("hasOptionalPermissions should match calculation", 
            hasOptional, uiState.hasOptionalPermissions)
        
        viewModel.stopMonitoring()
    }
    
    @Test
    fun testRequestPermissionResultTypes() = runTest {
        // Test requesting a permission that doesn't require special handling
        val wakelock = AppPermission.WAKE_LOCK
        val result = viewModel.requestPermission(wakelock)
        
        // Result should be one of the expected types
        assertTrue("Result should be valid type", 
            result is PermissionResult.AlreadyGranted ||
            result is PermissionResult.Granted ||
            result is PermissionResult.ShowRationale ||
            result is PermissionResult.RequiresSettings ||
            result is PermissionResult.Error)
        
        // If it's AlreadyGranted, check permission state
        if (result is PermissionResult.AlreadyGranted) {
            val actualPermissionGranted = ContextCompat.checkSelfPermission(
                context, 
                wakelock.manifestPermission
            ) == PackageManager.PERMISSION_GRANTED
            assertTrue("AlreadyGranted result should match actual system state", 
                actualPermissionGranted)
        }
    }
    
    @Test
    fun testSpecialPermissionHandling() = runTest {
        // Test overlay permission shows special handling
        val overlayResult = viewModel.requestPermission(AppPermission.SYSTEM_ALERT_WINDOW)
        
        // Should not be a standard permission result if not already granted
        if (overlayResult !is PermissionResult.AlreadyGranted) {
            assertTrue("Overlay permission should have special handling",
                overlayResult is PermissionResult.ShowRationale ||
                overlayResult is PermissionResult.RequiresSettings ||
                overlayResult is PermissionResult.Error)
        }
        
        // Test accessibility permission shows special handling  
        val accessibilityResult = viewModel.requestPermission(AppPermission.BIND_ACCESSIBILITY_SERVICE)
        
        if (accessibilityResult !is PermissionResult.AlreadyGranted) {
            assertTrue("Accessibility permission should have special handling",
                accessibilityResult is PermissionResult.ShowRationale ||
                accessibilityResult is PermissionResult.RequiresSettings ||
                accessibilityResult is PermissionResult.Error)
        }
    }
    
    @Test 
    fun testApiLevelCompatibilityInPractice() = runTest {
        viewModel.startMonitoring()
        kotlinx.coroutines.delay(1000)
        
        val permissionStates = viewModel.permissionStates.first()
        val currentApiLevel = Build.VERSION.SDK_INT
        
        // Check that we only have states for applicable permissions
        permissionStates.keys.forEach { permission ->
            assertTrue("Permission ${permission.name} (min SDK ${permission.minSdkVersion}) should be applicable to current API $currentApiLevel",
                permission.minSdkVersion <= currentApiLevel)
        }
        
        // Check specific API level behaviors
        if (currentApiLevel >= 33) {
            assertTrue("API 33+ should include POST_NOTIFICATIONS", 
                permissionStates.containsKey(AppPermission.POST_NOTIFICATIONS))
        } else {
            assertFalse("API < 33 should not include POST_NOTIFICATIONS", 
                permissionStates.containsKey(AppPermission.POST_NOTIFICATIONS))
        }
        
        if (currentApiLevel >= 34) {
            assertTrue("API 34+ should include FOREGROUND_SERVICE_MICROPHONE", 
                permissionStates.containsKey(AppPermission.FOREGROUND_SERVICE_MICROPHONE))
        } else {
            assertFalse("API < 34 should not include FOREGROUND_SERVICE_MICROPHONE", 
                permissionStates.containsKey(AppPermission.FOREGROUND_SERVICE_MICROPHONE))
        }
        
        viewModel.stopMonitoring()
    }
    
    private fun checkAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        
        if (accessibilityEnabled != 1) return false
        
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        val packageName = context.packageName
        val serviceName = "$packageName/me.shadykhalifa.whispertop.services.WhisperTopAccessibilityService"
        
        return enabledServices.contains(serviceName)
    }
}