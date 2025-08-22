package me.shadykhalifa.whispertop.presentation.ui.components.permissions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.shadykhalifa.whispertop.domain.models.AppPermission
import me.shadykhalifa.whispertop.domain.models.PermissionState

enum class PermissionCategory(
    val displayName: String,
    val description: String
) {
    Audio("Audio", "Microphone and recording permissions"),
    System("System", "Overlay and accessibility permissions"),
    Accessibility("Accessibility", "Text insertion and automation")
}

@Composable
fun PermissionCategorySection(
    category: PermissionCategory,
    permissions: List<Pair<AppPermission, PermissionState>>,
    onRequestPermission: (AppPermission) -> Unit,
    onOpenSettings: (AppPermission) -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "ExpandIconRotation"
    )
    
    val grantedCount = permissions.count { it.second.isGranted }
    val totalCount = permissions.size
    
    Column(
        modifier = modifier.animateContentSize()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .semantics {
                    contentDescription = if (isExpanded) {
                        "Collapse ${category.displayName} section"
                    } else {
                        "Expand ${category.displayName} section"
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "$grantedCount of $totalCount granted",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (grantedCount == totalCount) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rotationAngle)
                )
            }
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            val configuration = LocalConfiguration.current
            val screenWidthDp = configuration.screenWidthDp.dp
            val columns = when {
                screenWidthDp >= 900.dp -> 3
                screenWidthDp >= 600.dp -> 2
                else -> 1
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(permissions) { (appPermission, permissionState) ->
                    PermissionCard(
                        appPermission = appPermission,
                        permissionState = permissionState,
                        onRequestPermission = onRequestPermission,
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

fun categorizePermissions(
    permissions: Map<AppPermission, PermissionState>
): Map<PermissionCategory, List<Pair<AppPermission, PermissionState>>> {
    return permissions.map { (permission, state) ->
        val category = when (permission) {
            AppPermission.RECORD_AUDIO -> PermissionCategory.Audio
            AppPermission.SYSTEM_ALERT_WINDOW -> PermissionCategory.System
            AppPermission.ACCESSIBILITY_SERVICE -> PermissionCategory.Accessibility
        }
        category to (permission to state)
    }.groupBy({ it.first }, { it.second })
}