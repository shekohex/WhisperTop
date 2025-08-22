package me.shadykhalifa.whispertop.presentation.ui.components.permissions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.shadykhalifa.whispertop.domain.models.AppPermission
import me.shadykhalifa.whispertop.domain.models.PermissionState
import me.shadykhalifa.whispertop.domain.models.isCritical

enum class PermissionPriority {
    Critical, Optional
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionCard(
    appPermission: AppPermission,
    permissionState: PermissionState,
    onRequestPermission: (AppPermission) -> Unit,
    onOpenSettings: (AppPermission) -> Unit,
    modifier: Modifier = Modifier,
    showTooltip: Boolean = true
) {
    val priority = if (appPermission.isCritical) PermissionPriority.Critical else PermissionPriority.Optional
    val scale by animateFloatAsState(
        targetValue = if (permissionState.isGranted) 1.0f else 0.98f,
        animationSpec = spring(),
        label = "PermissionCardScale"
    )
    
    val borderColor = when (priority) {
        PermissionPriority.Critical -> if (permissionState.isGranted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        }
        PermissionPriority.Optional -> MaterialTheme.colorScheme.outline
    }
    
    val cardColors = when {
        permissionState.isGranted -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
        permissionState.requiresSettings -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
        else -> CardDefaults.cardColors()
    }
    
    Card(
        modifier = modifier
            .scale(scale)
            .semantics {
                contentDescription = "${appPermission.displayName}: ${if (permissionState.isGranted) "granted" else "not granted"}"
            },
        colors = cardColors,
        border = BorderStroke(
            width = if (priority == PermissionPriority.Critical) 2.dp else 1.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (permissionState.isGranted) 2.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = getPermissionIcon(appPermission),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = appPermission.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (priority == PermissionPriority.Critical) {
                            Text(
                                text = "Required",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showTooltip) {
                        InfoTooltip(
                            tooltip = appPermission.description
                        )
                    }
                    
                    PermissionStatusBadge(
                        permissionState = permissionState
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = appPermission.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            AnimatedVisibility(
                visible = !permissionState.isGranted,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        ActionButton(
                            permissionState = permissionState,
                            onRequestPermission = { onRequestPermission(appPermission) },
                            onOpenSettings = { onOpenSettings(appPermission) }
                        )
                    }
                    
                    if (permissionState.nextRequestAllowedTime > System.currentTimeMillis()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val remainingMs = permissionState.nextRequestAllowedTime - System.currentTimeMillis()
                        val remainingSeconds = (remainingMs / 1000).toInt()
                        
                        Text(
                            text = "Try again in ${remainingSeconds}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun getPermissionIcon(appPermission: AppPermission): ImageVector {
    return when (appPermission) {
        AppPermission.RECORD_AUDIO -> Icons.Default.Mic
        AppPermission.SYSTEM_ALERT_WINDOW -> Icons.Default.OpenInNew
        AppPermission.ACCESSIBILITY_SERVICE -> Icons.Default.Accessibility
    }
}