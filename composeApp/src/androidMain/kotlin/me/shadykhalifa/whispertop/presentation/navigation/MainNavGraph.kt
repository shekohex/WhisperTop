package me.shadykhalifa.whispertop.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import me.shadykhalifa.whispertop.presentation.ui.components.BottomNavigationComponent
import me.shadykhalifa.whispertop.presentation.ui.screens.AndroidHomeScreen
import me.shadykhalifa.whispertop.presentation.ui.screens.HistoryScreen
import me.shadykhalifa.whispertop.presentation.ui.screens.PermissionsDashboardScreen
import me.shadykhalifa.whispertop.presentation.ui.screens.SettingsScreen
import me.shadykhalifa.whispertop.presentation.viewmodels.MainNavigationViewModel
import org.koin.compose.koinInject

@Composable
fun MainNavGraph(
    navController: NavHostController = rememberNavController(),
    requestPermissions: Boolean = false,
    showSettings: Boolean = false,
    mainNavViewModel: MainNavigationViewModel = koinInject()
) {
    val navigationState by mainNavViewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    
    val startDestination = when {
        requestPermissions -> NavigationTab.Permissions.route
        showSettings -> NavigationTab.Settings.route
        else -> NavigationTab.Home.route
    }
    
    LaunchedEffect(navBackStackEntry) {
        navBackStackEntry?.destination?.route?.let { route ->
            NavigationTab.fromRoute(route)?.let { tab ->
                mainNavViewModel.selectTab(tab)
            }
        }
        
        val canNavigateBack = navController.previousBackStackEntry != null
        // Note: backQueue is private, so we use currentBackStackEntry depth as approximation
        val backStackCount = if (canNavigateBack) 1 else 0
        mainNavViewModel.updateBackStackState(canNavigateBack, backStackCount)
    }
    
    Scaffold(
        bottomBar = {
            if (mainNavViewModel.shouldShowBottomNavigation()) {
                BottomNavigationComponent(
                    selectedTab = navigationState.selectedTab,
                    onTabSelected = { tab ->
                        if (tab != navigationState.selectedTab) {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable(
                route = NavigationTab.Home.route,
                deepLinks = listOf(navDeepLink { uriPattern = "whispertop://dashboard" })
            ) {
                AndroidHomeScreen(
                    onNavigateToSettings = {
                        navController.navigate(NavigationTab.Settings.route)
                    },
                    onNavigateToPermissions = {
                        navController.navigate(NavigationTab.Permissions.route)
                    }
                )
            }
            
            composable(
                route = NavigationTab.History.route,
                deepLinks = listOf(navDeepLink { uriPattern = "whispertop://history" })
            ) {
                HistoryScreen(
                    onNavigateBack = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(NavigationTab.Home.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            
            composable(
                route = NavigationTab.Settings.route,
                deepLinks = listOf(navDeepLink { uriPattern = "whispertop://settings" })
            ) {
                SettingsScreen(
                    onNavigateBack = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(NavigationTab.Home.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            
            composable(
                route = NavigationTab.Permissions.route,
                deepLinks = listOf(navDeepLink { uriPattern = "whispertop://permissions" })
            ) {
                PermissionsDashboardScreen(
                    onNavigateBack = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(NavigationTab.Home.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    }
}