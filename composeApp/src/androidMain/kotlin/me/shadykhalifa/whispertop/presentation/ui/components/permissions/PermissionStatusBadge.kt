package me.shadykhalifa.whispertop.presentation.ui.components.permissions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.shadykhalifa.whispertop.domain.models.PermissionState

@Composable
fun PermissionStatusBadge(
    permissionState: PermissionState,
    modifier: Modifier = Modifier
) {
    val (icon, color, backgroundColor, contentDesc) = when {
        permissionState.isGranted -> {
            Tuple4(
                Icons.Default.Check,
                MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.colorScheme.primary,
                "Permission granted"
            )
        }
        permissionState.requiresSettings -> {
            Tuple4(
                Icons.Default.Close,
                MaterialTheme.colorScheme.onError,
                MaterialTheme.colorScheme.error,
                "Permission denied, requires settings"
            )
        }
        permissionState.needsRationale -> {
            Tuple4(
                Icons.Default.Warning,
                MaterialTheme.colorScheme.onSecondary,
                MaterialTheme.colorScheme.secondary,
                "Permission needs explanation"
            )
        }
        permissionState.nextRequestAllowedTime > System.currentTimeMillis() -> {
            Tuple4(
                Icons.Default.Schedule,
                MaterialTheme.colorScheme.onTertiary,
                MaterialTheme.colorScheme.tertiary,
                "Permission request on cooldown"
            )
        }
        else -> {
            Tuple4(
                Icons.Default.Warning,
                MaterialTheme.colorScheme.onSurfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant,
                "Permission not granted"
            )
        }
    }

    AnimatedContent(
        targetState = permissionState,
        transitionSpec = {
            (scaleIn(animationSpec = spring()) + fadeIn()) togetherWith
                    (scaleOut(animationSpec = spring()) + fadeOut())
        },
        modifier = modifier.semantics {
            contentDescription = contentDesc
        },
        label = "PermissionStatusBadge"
    ) { state ->
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)