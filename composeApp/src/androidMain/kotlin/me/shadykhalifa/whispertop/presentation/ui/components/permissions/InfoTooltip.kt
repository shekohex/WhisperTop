package me.shadykhalifa.whispertop.presentation.ui.components.permissions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoTooltip(
    tooltip: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(
                    text = tooltip,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        state = rememberTooltipState(),
        modifier = modifier
    ) {
        IconButton(
            onClick = {},
            enabled = enabled,
            modifier = Modifier.semantics {
                contentDescription = "Information about permission"
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}