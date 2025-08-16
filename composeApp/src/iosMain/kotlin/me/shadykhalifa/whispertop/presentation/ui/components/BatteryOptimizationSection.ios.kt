package me.shadykhalifa.whispertop.presentation.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun getBatteryOptimizationStatus(): BatteryOptimizationStatus? {
    // iOS doesn't have battery optimization in the same way as Android
    return null
}

@Composable
actual fun requestBatteryOptimizationExemption(): Boolean {
    // Not applicable on iOS
    return false
}

@Composable
actual fun openBatteryOptimizationSettings(): Boolean {
    // Not applicable on iOS
    return false
}