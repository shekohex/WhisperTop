package me.shadykhalifa.whispertop.presentation.ui.components.permissions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import me.shadykhalifa.whispertop.domain.models.PermissionState

@Composable
fun ActionButton(
    permissionState: PermissionState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonData = when {
        permissionState.isGranted -> null
        permissionState.requiresSettings -> {
            Tuple4(
                "Settings",
                Icons.Default.Settings,
                onOpenSettings,
                "Open system settings for ${permissionState.permission.displayName}"
            )
        }
        permissionState.nextRequestAllowedTime > System.currentTimeMillis() -> null
        else -> {
            Tuple4(
                "Grant",
                Icons.Default.PlayArrow,
                onRequestPermission,
                "Request ${permissionState.permission.displayName} permission"
            )
        }
    }

    buttonData?.let { (text, icon, onClick, contentDesc) ->
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.semantics {
                contentDescription = contentDesc
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

