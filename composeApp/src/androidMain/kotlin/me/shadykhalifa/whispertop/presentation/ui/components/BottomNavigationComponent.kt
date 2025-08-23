package me.shadykhalifa.whispertop.presentation.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.shadykhalifa.whispertop.presentation.navigation.NavigationTab

@Composable
fun BottomNavigationComponent(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier,
    showBadges: Map<NavigationTab, Int> = emptyMap()
) {
    NavigationBar(
        modifier = modifier.semantics {
            contentDescription = "Bottom navigation bar"
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        NavigationTab.bottomNavigationTabs.forEach { tab ->
            val isSelected = selectedTab == tab
            val badgeCount = showBadges[tab] ?: 0
            
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    TabIcon(
                        tab = tab,
                        isSelected = isSelected,
                        badgeCount = badgeCount
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}

@Composable
private fun TabIcon(
    tab: NavigationTab,
    isSelected: Boolean,
    badgeCount: Int,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1.0f,
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "TabIconScale"
    )
    
    val icon: ImageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon
    
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    if (badgeCount <= 99) {
                        Text(text = badgeCount.toString())
                    } else {
                        Text(text = "99+")
                    }
                }
            },
            modifier = modifier.scale(scale)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "${tab.title} tab",
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = "${tab.title} tab",
            modifier = modifier
                .scale(scale)
                .size(24.dp)
        )
    }
}