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
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.NavType
import me.shadykhalifa.whispertop.presentation.ui.components.BottomNavigationComponent
import me.shadykhalifa.whispertop.presentation.ui.screens.AndroidHomeScreen
import me.shadykhalifa.whispertop.presentation.ui.screens.HistoryScreen
import me.shadykhalifa.whispertop.presentation.ui.screens.PermissionsDashboardScreen
import me.shadykhalifa.whispertop.presentation.ui.screens.SettingsScreen
import me.shadykhalifa.whispertop.presentation.ui.screens.TranscriptionDetailScreen
import me.shadykhalifa.whispertop.presentation.ui.screens.OnboardingWpmScreen
import me.shadykhalifa.whispertop.presentation.viewmodels.MainNavigationViewModel
import me.shadykhalifa.whispertop.presentation.navigation.NavigationTransitions
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import androidx.lifecycle.SavedStateHandle

@Composable
fun MainNavGraph(
    navController: NavHostController = rememberNavController(),
    requestPermissions: Boolean = false,
    showSettings: Boolean = false,
    showWpmOnboarding: Boolean = false,
    mainNavViewModel: MainNavigationViewModel = koinInject { parametersOf(SavedStateHandle()) }
) {
    val navigationState by mainNavViewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    
    val startDestination = when {
        requestPermissions -> NavigationTab.Permissions.route
        showSettings -> NavigationTab.Settings.route
        showWpmOnboarding -> "wpm_onboarding"
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
            enterTransition = NavigationTransitions.forwardTransition(),
            exitTransition = NavigationTransitions.forwardExitTransition(),
            popEnterTransition = NavigationTransitions.backwardTransition(),
            popExitTransition = NavigationTransitions.backwardExitTransition()
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
                    },
                    onNavigateToDetail = { transcriptionId ->
                        navController.navigate("transcription_detail/$transcriptionId")
                    }
                )
            }
            
            composable(
                route = NavigationTab.Settings.route,
                deepLinks = listOf(navDeepLink { uriPattern = "whispertop://settings" }),
                enterTransition = NavigationTransitions.modalEnterTransition(),
                exitTransition = NavigationTransitions.modalExitTransition(),
                popEnterTransition = NavigationTransitions.backwardTransition(),
                popExitTransition = NavigationTransitions.backwardExitTransition()
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
                deepLinks = listOf(navDeepLink { uriPattern = "whispertop://permissions" }),
                enterTransition = NavigationTransitions.bottomSheetEnterTransition(),
                exitTransition = NavigationTransitions.bottomSheetExitTransition(),
                popEnterTransition = NavigationTransitions.backwardTransition(),
                popExitTransition = NavigationTransitions.backwardExitTransition()
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
            
            composable(
                route = "transcription_detail/{transcriptionId}",
                arguments = listOf(navArgument("transcriptionId") { type = NavType.StringType }),
                enterTransition = NavigationTransitions.sharedElementEnterTransition(),
                exitTransition = NavigationTransitions.sharedElementExitTransition(),
                popEnterTransition = NavigationTransitions.backwardTransition(),
                popExitTransition = NavigationTransitions.backwardExitTransition()
            ) { backStackEntry ->
                val transcriptionId = backStackEntry.arguments?.getString("transcriptionId") ?: ""
                TranscriptionDetailScreen(
                    transcriptionId = transcriptionId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(
                route = "wpm_onboarding",
                deepLinks = listOf(navDeepLink { uriPattern = "whispertop://wpm_onboarding" })
            ) {
                OnboardingWpmScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onCompleteOnboarding = {
                        navController.navigate(NavigationTab.Home.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}