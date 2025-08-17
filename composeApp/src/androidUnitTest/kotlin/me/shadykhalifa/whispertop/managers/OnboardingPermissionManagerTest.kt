package me.shadykhalifa.whispertop.managers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.presentation.models.PermissionState
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import me.shadykhalifa.whispertop.managers.PermissionHandler
import me.shadykhalifa.whispertop.managers.SystemSettingsProvider

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class OnboardingPermissionManagerTest : KoinTest {
    
    private lateinit var mockActivity: Activity
    private lateinit var mockSystemSettingsProvider: SystemSettingsProvider
    private lateinit var permissionManager: OnboardingPermissionManager
    
    @Before
    fun setup() {
        stopKoin()
        
        mockActivity = mock()
        mockSystemSettingsProvider = mock()
        
        startKoin {
            modules(
                module {
                    single<Context> { mockActivity }
                    single<SystemSettingsProvider> { mockSystemSettingsProvider }
                    single<PermissionHandler> { mock<PermissionHandler>() }
                }
            )
        }
        
        permissionManager = OnboardingPermissionManager()
    }
    
    @Test
    fun `should detect granted audio permission correctly`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        whenever(ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.FOREGROUND_SERVICE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        whenever(ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.POST_NOTIFICATIONS))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        
        permissionManager.refreshAllPermissionStates()
        
        val state = permissionManager.onboardingPermissionState.first()
        assertEquals(PermissionState.Granted, state.audioRecording.state)
        assertEquals(PermissionState.Granted, state.foregroundService.state)
        assertEquals(PermissionState.Granted, state.notifications.state)
    }
    
    @Test
    fun `should detect denied audio permission correctly`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        whenever(ActivityCompat.shouldShowRequestPermissionRationale(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(true) // This makes it show as "Denied" instead of "PermanentlyDenied"
        
        permissionManager.refreshAllPermissionStates()
        
        val state = permissionManager.onboardingPermissionState.first()
        assertEquals(PermissionState.Denied, state.audioRecording.state)
    }
    
    @Test
    fun `should detect permanently denied permission correctly`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        whenever(ActivityCompat.shouldShowRequestPermissionRationale(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(false)
        
        permissionManager.refreshAllPermissionStates()
        
        val state = permissionManager.onboardingPermissionState.first()
        assertEquals(PermissionState.PermanentlyDenied, state.audioRecording.state)
    }
    
    @Test
    fun `should detect overlay permission correctly when granted`() = runTest {
        whenever(mockSystemSettingsProvider.canDrawOverlays()).thenReturn(true)
        
        permissionManager.refreshAllPermissionStates()
        
        val state = permissionManager.onboardingPermissionState.first()
        assertEquals(PermissionState.Granted, state.overlay.state)
    }
    
    @Test
    fun `should detect overlay permission correctly when denied`() = runTest {
        whenever(mockSystemSettingsProvider.canDrawOverlays()).thenReturn(false)
        
        permissionManager.refreshAllPermissionStates()
        
        val state = permissionManager.onboardingPermissionState.first()
        assertEquals(PermissionState.NotRequested, state.overlay.state)
    }
    
    @Test
    fun `should detect accessibility service correctly when enabled`() = runTest {
        whenever(mockActivity.packageName).thenReturn("me.shadykhalifa.whispertop")
        whenever(mockSystemSettingsProvider.isAccessibilityEnabled()).thenReturn(true)
        whenever(mockSystemSettingsProvider.getEnabledAccessibilityServices())
            .thenReturn("me.shadykhalifa.whispertop/me.shadykhalifa.whispertop.service.WhisperTopAccessibilityService")
        
        permissionManager.refreshAllPermissionStates()
        
        val state = permissionManager.onboardingPermissionState.first()
        assertEquals(PermissionState.Granted, state.accessibility.state)
    }
    
    @Test
    fun `should detect accessibility service correctly when disabled`() = runTest {
        whenever(mockSystemSettingsProvider.isAccessibilityEnabled()).thenReturn(false)
        
        permissionManager.refreshAllPermissionStates()
        
        val state = permissionManager.onboardingPermissionState.first()
        assertEquals(PermissionState.NotRequested, state.accessibility.state)
    }
    
    @Test
    fun `should determine permission denial reason correctly for granted permission`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        
        permissionManager.refreshAllPermissionStates()
        
        val reason = permissionManager.getPermissionDenialReason(Manifest.permission.RECORD_AUDIO)
        assertEquals(OnboardingPermissionManager.PermissionDenialReason.Granted, reason)
    }
    
    @Test
    fun `should determine permission denial reason correctly for denied permission`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        whenever(ActivityCompat.shouldShowRequestPermissionRationale(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(true)
        
        permissionManager.refreshAllPermissionStates()
        
        val reason = permissionManager.getPermissionDenialReason(Manifest.permission.RECORD_AUDIO)
        assertEquals(OnboardingPermissionManager.PermissionDenialReason.NeedsRationale, reason)
    }
    
    @Test
    fun `should determine permission denial reason correctly for permanently denied permission`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        whenever(ActivityCompat.shouldShowRequestPermissionRationale(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(false)
        
        permissionManager.refreshAllPermissionStates()
        
        val reason = permissionManager.getPermissionDenialReason(Manifest.permission.RECORD_AUDIO)
        assertEquals(OnboardingPermissionManager.PermissionDenialReason.PermanentlyDenied, reason)
    }
    
    @Test
    fun `should check all critical permissions correctly when all granted`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        whenever(ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.FOREGROUND_SERVICE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        whenever(ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.POST_NOTIFICATIONS))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        
        whenever(mockActivity.packageName).thenReturn("me.shadykhalifa.whispertop")
        whenever(mockSystemSettingsProvider.canDrawOverlays()).thenReturn(true)
        whenever(mockSystemSettingsProvider.isAccessibilityEnabled()).thenReturn(true)
        whenever(mockSystemSettingsProvider.getEnabledAccessibilityServices())
            .thenReturn("me.shadykhalifa.whispertop/me.shadykhalifa.whispertop.service.WhisperTopAccessibilityService")
        
        permissionManager.refreshAllPermissionStates()
        
        val state = permissionManager.onboardingPermissionState.first()
        assertTrue(state.allCriticalPermissionsGranted)
        assertFalse(state.hasAnyDeniedPermissions)
        assertFalse(state.hasPermanentlyDeniedPermissions)
    }
    
    @Test
    fun `should check all critical permissions correctly when some denied`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        whenever(ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.FOREGROUND_SERVICE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        whenever(ActivityCompat.shouldShowRequestPermissionRationale(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(true) // Make it show as denied rather than permanently denied
        
        permissionManager.refreshAllPermissionStates()
        
        val state = permissionManager.onboardingPermissionState.first()
        assertFalse(state.allCriticalPermissionsGranted)
        assertTrue(state.hasAnyDeniedPermissions)
    }
}