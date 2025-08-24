package me.shadykhalifa.whispertop.managers

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.di.androidAppModule
import me.shadykhalifa.whispertop.di.providePlatformModule
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class PermissionHandlerTest : KoinTest {
    
    private val permissionHandler: PermissionHandler by inject()
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        startKoin {
            androidContext(context)
            modules(providePlatformModule(context), androidAppModule)
        }
    }
    
    @After
    fun tearDown() {
        stopKoin()
    }
    
    @Test
    fun testPermissionHandlerCreation() {
        assertNotNull(permissionHandler)
    }
    
    @Test
    fun testInitialPermissionState() = runTest {
        val initialState = permissionHandler.permissionState.first()
        assertTrue(
            initialState == PermissionHandler.PermissionState.GRANTED ||
            initialState == PermissionHandler.PermissionState.DENIED ||
            initialState == PermissionHandler.PermissionState.UNKNOWN
        )
    }
    
    @Test
    fun testPermissionStatusSummary() {
        val summary = permissionHandler.getPermissionStatusSummary()
        assertNotNull(summary)
        
        // In test environment, permissions are typically not granted
        assertFalse(summary.allGranted)
        assertTrue(summary.anyDenied)
    }
    
    @Test
    fun testCheckAllPermissions() {
        val result = permissionHandler.checkAllPermissions()
        assertNotNull(result)
        
        when (result) {
            is PermissionHandler.PermissionCheckResult.ALL_GRANTED -> {
                assertTrue(true) // All permissions granted
            }
            is PermissionHandler.PermissionCheckResult.SOME_DENIED -> {
                assertTrue(result.deniedPermissions.isNotEmpty())
            }
        }
    }
    
    @Test
    fun testPermissionResultWithEmptyArrays() {
        val result = permissionHandler.onPermissionResult(
            arrayOf(),
            intArrayOf()
        )
        
        assertEquals(PermissionHandler.PermissionResult.GRANTED, result)
    }
    
    @Test
    fun testPermissionResultWithDeniedPermissions() {
        val result = permissionHandler.onPermissionResult(
            arrayOf("android.permission.RECORD_AUDIO"),
            intArrayOf(PackageManager.PERMISSION_DENIED)
        )
        
        when (result) {
            is PermissionHandler.PermissionResult.DENIED -> {
                assertTrue(result.deniedPermissions.contains("android.permission.RECORD_AUDIO"))
            }
            else -> {
                assertTrue(false, "Expected DENIED result")
            }
        }
    }
}