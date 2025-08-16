package me.shadykhalifa.whispertop.presentation.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import me.shadykhalifa.whispertop.managers.BatteryOptimizationUtil
import org.koin.compose.koinInject

@Composable
actual fun getBatteryOptimizationStatus(): BatteryOptimizationStatus? {
    val context = LocalContext.current
    
    return remember {
        try {
            val batteryUtil = BatteryOptimizationUtil(context)
            val status = batteryUtil.getBatteryOptimizationStatus()
            BatteryOptimizationStatus(
                isIgnoringBatteryOptimizations = status.isIgnoringBatteryOptimizations,
                canRequestIgnore = status.canRequestIgnore,
                isFeatureAvailable = status.isFeatureAvailable,
                explanation = status.explanation,
                hasCustomOptimization = batteryUtil.hasCustomBatteryOptimization(),
                manufacturerGuidance = batteryUtil.getManufacturerSpecificGuidance()
            )
        } catch (e: Exception) {
            null
        }
    }
}

// Global context holder - this is a temporary solution
private var globalContext: Context? = null

fun setGlobalContext(context: Context) {
    globalContext = context
}

actual fun requestBatteryOptimizationExemption(): Boolean {
    val context = globalContext ?: return false
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val batteryUtil = BatteryOptimizationUtil(context)
            val intent = batteryUtil.createBatteryOptimizationExemptionIntent()
            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}

actual fun openBatteryOptimizationSettings(): Boolean {
    val context = globalContext ?: return false
    return try {
        val batteryUtil = BatteryOptimizationUtil(context)
        val intent = batteryUtil.createBatteryOptimizationSettingsIntent()
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}