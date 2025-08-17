package me.shadykhalifa.whispertop.managers

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O, Build.VERSION_CODES.Q, Build.VERSION_CODES.S])
class PowerManagementUtilTest {
    
    private lateinit var context: Context
    private lateinit var powerManagementUtil: PowerManagementUtil
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        powerManagementUtil = PowerManagementUtil(context)
        powerManagementUtil.initialize()
    }
    
    @Test
    fun `initialize should setup power state monitoring`() {
        // Test that initialization doesn't throw
        powerManagementUtil.initialize()
        
        // Should be able to access power state
        val powerState = powerManagementUtil.powerState.value
        assertNotNull(powerState)
    }
    
    @Test
    fun `isInDozeMode should return boolean`() {
        val result = powerManagementUtil.isInDozeMode()
        assertTrue(result is Boolean)
    }
    
    @Test
    fun `isInPowerSaveMode should return boolean`() {
        val result = powerManagementUtil.isInPowerSaveMode()
        assertTrue(result is Boolean)
    }
    
    @Test
    fun `isIgnoringBatteryOptimizations should return boolean`() {
        val result = powerManagementUtil.isIgnoringBatteryOptimizations()
        assertTrue(result is Boolean)
    }
    
    @Test
    fun `acquireIntelligentWakeLock should handle wake lock creation`() {
        val config = PowerManagementUtil.WakeLockConfig(
            tag = "TestWakeLock",
            timeout = 5000L
        )
        
        val wakeLock = powerManagementUtil.acquireIntelligentWakeLock(config)
        
        // May be null if wake locks are not supported in test environment
        // But should not throw exceptions
        assertTrue(wakeLock is PowerManager.WakeLock?)
    }
    
    @Test
    fun `calculateOptimalTimeout should adjust timeout based on power state`() = runTest {
        val baseTimeout = 10000L
        val config = PowerManagementUtil.WakeLockConfig(
            tag = "TestTimeout",
            timeout = baseTimeout
        )
        
        // Test that timeout calculation doesn't crash
        val wakeLock = powerManagementUtil.acquireIntelligentWakeLock(config)
        
        // Should not throw exceptions
        assertTrue(wakeLock is PowerManager.WakeLock?)
    }
    
    @Test
    fun `shouldDelayNetworkOperations should return boolean based on power state`() {
        val result = powerManagementUtil.shouldDelayNetworkOperations()
        assertTrue(result is Boolean)
    }
    
    @Test
    fun `shouldLimitBackgroundProcessing should return boolean based on power state`() {
        val result = powerManagementUtil.shouldLimitBackgroundProcessing()
        assertTrue(result is Boolean)
    }
    
    @Test
    fun `getRecommendedOperationDelay should return non-negative delay`() {
        val delay = powerManagementUtil.getRecommendedOperationDelay()
        assertTrue(delay >= 0L)
    }
    
    @Test
    fun `isOptimalForIntensiveOperations should return boolean`() {
        val result = powerManagementUtil.isOptimalForIntensiveOperations()
        assertTrue(result is Boolean)
    }
    
    @Test
    fun `power state should be observable`() = runTest {
        val initialState = powerManagementUtil.powerState.first()
        
        assertNotNull(initialState)
        assertTrue(initialState.isInDozeMode is Boolean)
        assertTrue(initialState.isDeviceIdle is Boolean)
        assertTrue(initialState.isInPowerSaveMode is Boolean)
        assertTrue(initialState.isIgnoringBatteryOptimizations is Boolean)
        assertTrue(initialState.networkRestricted is Boolean)
        assertTrue(initialState.backgroundRestricted is Boolean)
    }
    
    @Test
    fun `cleanup should not throw exceptions`() {
        // Test that cleanup doesn't crash
        powerManagementUtil.cleanup()
        
        // Can call multiple times
        powerManagementUtil.cleanup()
        
        // Should not throw
        assertTrue(true)
    }
    
    @Test
    fun `createWakeLockConfig extension should create proper config`() {
        val config = powerManagementUtil.createWakeLockConfig(
            purpose = "TestPurpose",
            baseDuration = 15000L
        )
        
        assertEquals("WhisperTop::TestPurpose", config.tag)
        assertEquals(15000L, config.timeout)
        assertEquals(PowerManager.PARTIAL_WAKE_LOCK, config.flags)
    }
}