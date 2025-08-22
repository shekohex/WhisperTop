package me.shadykhalifa.whispertop.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AndroidHomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "WhisperTop",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onNavigateToSettings,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Settings")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onNavigateToPermissions,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Permissions Dashboard")
        }
    }
}