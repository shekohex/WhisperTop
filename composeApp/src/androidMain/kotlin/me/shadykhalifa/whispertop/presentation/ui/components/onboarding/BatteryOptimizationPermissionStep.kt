package me.shadykhalifa.whispertop.presentation.ui.components.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.shadykhalifa.whispertop.presentation.ui.components.onboarding.OnboardingStepLayout

@Composable
fun BatteryOptimizationPermissionStep(
    onPermissionGranted: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingStepLayout(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Battery Optimization Status",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Tap Grant Permission to configure battery optimization settings for reliable background operation.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Skip")
                }
                
                Button(
                    onClick = onPermissionGranted,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}