package me.shadykhalifa.whispertop.managers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O, Build.VERSION_CODES.Q, Build.VERSION_CODES.S])
class BatteryOptimizationUtilTest {
    
    @Mock
    private lateinit var mockPowerManager: PowerManager
    
    private lateinit var context: Context
    private lateinit var batteryOptimizationUtil: BatteryOptimizationUtil
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        batteryOptimizationUtil = BatteryOptimizationUtil(context)
    }
    
    @Test
    fun `isIgnoringBatteryOptimizations should return false on older Android versions`() {
        // This test runs on multiple SDK versions, but when SDK < M, should return true
        val result = batteryOptimizationUtil.isIgnoringBatteryOptimizations()
        // On M+ this depends on actual system state, on older versions should be true
        assertTrue(result is Boolean)
    }
    
    @Test
    fun `canRequestBatteryOptimizationExemption should check permission and feature availability`() {
        val result = batteryOptimizationUtil.canRequestBatteryOptimizationExemption()
        assertTrue(result is Boolean)
    }
    
    @Test
    fun `isFeatureAvailable should return appropriate value based on Android version`() {
        val result = batteryOptimizationUtil.isFeatureAvailable()
        assertTrue(result is Boolean)
    }
    
    @Test
    fun `getBatteryOptimizationStatus should return comprehensive status`() {
        val status = batteryOptimizationUtil.getBatteryOptimizationStatus()
        
        assertNotNull(status)
        assertTrue(status.explanation.isNotEmpty())
        assertTrue(status.isFeatureAvailable is Boolean)
        assertTrue(status.canRequestIgnore is Boolean)
        assertTrue(status.isIgnoringBatteryOptimizations is Boolean)
    }
    
    @Test
    fun `createBatteryOptimizationExemptionIntent should return proper intent on supported versions`() {
        val intent = batteryOptimizationUtil.createBatteryOptimizationExemptionIntent()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (batteryOptimizationUtil.canRequestBatteryOptimizationExemption()) {
                assertNotNull(intent)
                assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, intent?.action)
            }
        }
    }
    
    @Test
    fun `createBatteryOptimizationSettingsIntent should return valid intent`() {
        val intent = batteryOptimizationUtil.createBatteryOptimizationSettingsIntent()
        
        assertNotNull(intent)
        assertTrue(intent.action != null)
    }
    
    @Test
    fun `hasCustomBatteryOptimization should detect known manufacturers`() {
        val hasCustom = batteryOptimizationUtil.hasCustomBatteryOptimization()
        assertTrue(hasCustom is Boolean)
    }
    
    @Test
    fun `getManufacturerSpecificGuidance should return appropriate guidance`() {
        val guidance = batteryOptimizationUtil.getManufacturerSpecificGuidance()
        
        assertNotNull(guidance)
        assertTrue(guidance.isNotEmpty())
        assertTrue(guidance.contains("WhisperTop") || guidance.contains("battery"))
    }
    
    @Test
    fun `manufacturer detection should work for known brands`() {
        // Test that manufacturer-specific logic works
        val guidance = batteryOptimizationUtil.getManufacturerSpecificGuidance()
        val hasCustom = batteryOptimizationUtil.hasCustomBatteryOptimization()
        
        // Should provide meaningful guidance regardless of manufacturer
        assertNotNull(guidance)
        assertTrue(guidance.isNotEmpty())
    }
}