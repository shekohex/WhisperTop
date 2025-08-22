package me.shadykhalifa.whispertop.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import me.shadykhalifa.whispertop.domain.models.AppPermission
import me.shadykhalifa.whispertop.domain.models.PermissionState
import me.shadykhalifa.whispertop.presentation.ui.components.permissions.*
import me.shadykhalifa.whispertop.presentation.viewmodels.PermissionsViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsDashboardScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PermissionsViewModel = koinInject()
) {
    val permissionStates by viewModel.permissionStates.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val categorizedPermissions = remember(permissionStates) {
        categorizePermissions(permissionStates)
    }
    
    val totalPermissions = permissionStates.size
    val grantedPermissions = permissionStates.values.count { it.isGranted }
    val allGranted = grantedPermissions == totalPermissions && totalPermissions > 0
    
    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopMonitoring()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Permissions Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (totalPermissions > 0) {
                                "$grantedPermissions of $totalPermissions granted"
                            } else {
                                "Loading permissions..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Navigate back"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                        IconButton(
                            onClick = {
                                isRefreshing = true
                                // Refresh is handled by the monitor automatically
                            },
                        enabled = !isRefreshing,
                        modifier = Modifier.semantics {
                            contentDescription = "Refresh permissions status"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (allGranted) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (isRefreshing) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Refreshing...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (allGranted) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎉 All Permissions Granted!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "WhisperTop has all the permissions it needs to work properly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            if (categorizedPermissions.isNotEmpty()) {
                items(
                    items = categorizedPermissions.entries.toList(),
                    key = { it.key }
                ) { (category, permissions) ->
                    PermissionCategorySection(
                        category = category,
                        permissions = permissions,
                        onRequestPermission = { permission ->
                            viewModel.requestPermission(permission)
                        },
                        onOpenSettings = { permission ->
                            viewModel.navigateToSettings(permission)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        initiallyExpanded = !allGranted
                    )
                }
            } else if (!isRefreshing) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading permissions...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(1500)
            isRefreshing = false
        }
    }
    
    uiState.requestingPermission?.let { permission ->
        PermissionRequestDialog(
            permission = permission,
            onDismiss = { /* UI state will update automatically */ }
        )
    }
    
    if (uiState.showRationale) {
        uiState.rationalePermission?.let { permission ->
            PermissionRationaleDialog(
                permission = permission,
                onDismiss = { /* UI state will update automatically */ },
                onRetry = { 
                    viewModel.requestPermission(permission)
                }
            )
        }
    }
}

@Composable
private fun PermissionRequestDialog(
    permission: AppPermission,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Requesting Permission")
        },
        text = {
            Text("Please grant ${permission.displayName} permission in the system dialog.")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun PermissionRationaleDialog(
    permission: AppPermission,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Permission Required")
        },
        text = {
            Text("${permission.displayName} is required for WhisperTop to work properly. ${permission.description}")
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text("Try Again")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}