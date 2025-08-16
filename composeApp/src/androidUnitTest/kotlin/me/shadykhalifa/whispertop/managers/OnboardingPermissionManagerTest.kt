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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class OnboardingPermissionManagerTest : KoinTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockActivity: Activity
    private lateinit var permissionManager: OnboardingPermissionManager
    
    @Before
    fun setup() {
        stopKoin()
        
        mockContext = mock()
        mockActivity = mock()
        
        startKoin {
            modules(
                module {
                    single<Context> { mockContext }
                }
            )
        }
        
        permissionManager = OnboardingPermissionManager()
    }
    
    @Test
    fun `should detect granted audio permission correctly`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockContext, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        whenever(ContextCompat.checkSelfPermission(mockContext, Manifest.permission.FOREGROUND_SERVICE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        whenever(ContextCompat.checkSelfPermission(mockContext, Manifest.permission.POST_NOTIFICATIONS))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        
        permissionManager.refreshAllPermissionStates()
        
        val state = permissionManager.onboardingPermissionState.first()
        assertEquals(PermissionState.Granted, state.audioRecording.state)
        assertEquals(PermissionState.Granted, state.foregroundService.state)
        assertEquals(PermissionState.Granted, state.notifications.state)
    }
    
    @Test
    fun `should detect denied audio permission correctly`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockContext, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        
        permissionManager.refreshAllPermissionStates()
        
        val state = permissionManager.onboardingPermissionState.first()
        assertEquals(PermissionState.Denied, state.audioRecording.state)
    }
    
    @Test
    fun `should detect permanently denied permission correctly`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockContext, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        whenever(mockContext).thenReturn(mockActivity)
        whenever(ActivityCompat.shouldShowRequestPermissionRationale(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(false)
        
        permissionManager.refreshAllPermissionStates()
        
        val state = permissionManager.onboardingPermissionState.first()
        assertEquals(PermissionState.PermanentlyDenied, state.audioRecording.state)
    }
    
    @Test
    fun `should detect overlay permission correctly when granted`() = runTest {
        mockStatic(Settings::class.java).use { mockedSettings ->
            mockedSettings.`when`<Boolean> { Settings.canDrawOverlays(mockContext) }
                .thenReturn(true)
            
            permissionManager.refreshAllPermissionStates()
            
            val state = permissionManager.onboardingPermissionState.first()
            assertEquals(PermissionState.Granted, state.overlay.state)
        }
    }
    
    @Test
    fun `should detect overlay permission correctly when denied`() = runTest {
        mockStatic(Settings::class.java).use { mockedSettings ->
            mockedSettings.`when`<Boolean> { Settings.canDrawOverlays(mockContext) }
                .thenReturn(false)
            
            permissionManager.refreshAllPermissionStates()
            
            val state = permissionManager.onboardingPermissionState.first()
            assertEquals(PermissionState.NotRequested, state.overlay.state)
        }
    }
    
    @Test
    fun `should detect accessibility service correctly when enabled`() = runTest {
        val mockContentResolver = mock<android.content.ContentResolver>()
        whenever(mockContext.contentResolver).thenReturn(mockContentResolver)
        whenever(mockContext.packageName).thenReturn("me.shadykhalifa.whispertop")
        
        mockStatic(Settings.Secure::class.java).use { mockedSecure ->
            mockedSecure.`when`<Int> { 
                Settings.Secure.getInt(mockContentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) 
            }.thenReturn(1)
            
            mockedSecure.`when`<String> { 
                Settings.Secure.getString(mockContentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) 
            }.thenReturn("me.shadykhalifa.whispertop/me.shadykhalifa.whispertop.service.WhisperTopAccessibilityService")
            
            permissionManager.refreshAllPermissionStates()
            
            val state = permissionManager.onboardingPermissionState.first()
            assertEquals(PermissionState.Granted, state.accessibility.state)
        }
    }
    
    @Test
    fun `should detect accessibility service correctly when disabled`() = runTest {
        val mockContentResolver = mock<android.content.ContentResolver>()
        whenever(mockContext.contentResolver).thenReturn(mockContentResolver)
        
        mockStatic(Settings.Secure::class.java).use { mockedSecure ->
            mockedSecure.`when`<Int> { 
                Settings.Secure.getInt(mockContentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) 
            }.thenReturn(0)
            
            permissionManager.refreshAllPermissionStates()
            
            val state = permissionManager.onboardingPermissionState.first()
            assertEquals(PermissionState.NotRequested, state.accessibility.state)
        }
    }
    
    @Test
    fun `should determine permission denial reason correctly for granted permission`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockContext, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        
        permissionManager.refreshAllPermissionStates()
        
        val reason = permissionManager.getPermissionDenialReason(Manifest.permission.RECORD_AUDIO)
        assertEquals(OnboardingPermissionManager.PermissionDenialReason.Granted, reason)
    }
    
    @Test
    fun `should determine permission denial reason correctly for denied permission`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockContext, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        whenever(mockContext).thenReturn(mockActivity)
        whenever(ActivityCompat.shouldShowRequestPermissionRationale(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(true)
        
        permissionManager.refreshAllPermissionStates()
        
        val reason = permissionManager.getPermissionDenialReason(Manifest.permission.RECORD_AUDIO)
        assertEquals(OnboardingPermissionManager.PermissionDenialReason.NeedsRationale, reason)
    }
    
    @Test
    fun `should determine permission denial reason correctly for permanently denied permission`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockContext, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        whenever(mockContext).thenReturn(mockActivity)
        whenever(ActivityCompat.shouldShowRequestPermissionRationale(mockActivity, Manifest.permission.RECORD_AUDIO))
            .thenReturn(false)
        
        permissionManager.refreshAllPermissionStates()
        
        val reason = permissionManager.getPermissionDenialReason(Manifest.permission.RECORD_AUDIO)
        assertEquals(OnboardingPermissionManager.PermissionDenialReason.PermanentlyDenied, reason)
    }
    
    @Test
    fun `should check all critical permissions correctly when all granted`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockContext, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        whenever(ContextCompat.checkSelfPermission(mockContext, Manifest.permission.FOREGROUND_SERVICE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        whenever(ContextCompat.checkSelfPermission(mockContext, Manifest.permission.POST_NOTIFICATIONS))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        
        mockStatic(Settings::class.java).use { mockedSettings ->
            mockedSettings.`when`<Boolean> { Settings.canDrawOverlays(mockContext) }
                .thenReturn(true)
            
            val mockContentResolver = mock<android.content.ContentResolver>()
            whenever(mockContext.contentResolver).thenReturn(mockContentResolver)
            whenever(mockContext.packageName).thenReturn("me.shadykhalifa.whispertop")
            
            mockStatic(Settings.Secure::class.java).use { mockedSecure ->
                mockedSecure.`when`<Int> { 
                    Settings.Secure.getInt(mockContentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) 
                }.thenReturn(1)
                
                mockedSecure.`when`<String> { 
                    Settings.Secure.getString(mockContentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) 
                }.thenReturn("me.shadykhalifa.whispertop/me.shadykhalifa.whispertop.service.WhisperTopAccessibilityService")
                
                permissionManager.refreshAllPermissionStates()
                
                val state = permissionManager.onboardingPermissionState.first()
                assertTrue(state.allCriticalPermissionsGranted)
                assertFalse(state.hasAnyDeniedPermissions)
                assertFalse(state.hasPermanentlyDeniedPermissions)
            }
        }
    }
    
    @Test
    fun `should check all critical permissions correctly when some denied`() = runTest {
        whenever(ContextCompat.checkSelfPermission(mockContext, Manifest.permission.RECORD_AUDIO))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        whenever(ContextCompat.checkSelfPermission(mockContext, Manifest.permission.FOREGROUND_SERVICE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        
        permissionManager.refreshAllPermissionStates()
        
        val state = permissionManager.onboardingPermissionState.first()
        assertFalse(state.allCriticalPermissionsGranted)
        assertTrue(state.hasAnyDeniedPermissions)
    }
}