package me.shadykhalifa.whispertop.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.navigation.NavBackStackEntry

/**
 * Material Motion-based navigation transitions for WhisperTop
 */
object NavigationTransitions {
    
    // Material Motion Duration Constants
    private const val DURATION_SHORT = 250
    private const val DURATION_MEDIUM = 300
    private const val DURATION_LONG = 400
    
    // Material Motion Easing Curves
    private val EMPHASIZED_EASING = CubicBezierEasing(0.2f, 0.0f, 0f, 1.0f)
    private val EMPHASIZED_ACCELERATE = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    private val EMPHASIZED_DECELERATE = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    private val STANDARD_EASING = CubicBezierEasing(0.2f, 0.0f, 0f, 1.0f)
    
    /**
     * Forward navigation transition (push) with Material Motion
     */
    fun forwardTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EMPHASIZED_DECELERATE
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                delayMillis = 50,
                easing = LinearEasing
            )
        ) + scaleIn(
            initialScale = 0.95f,
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EMPHASIZED_DECELERATE
            )
        )
    }
    
    /**
     * Forward exit transition with Material Motion
     */
    fun forwardExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EMPHASIZED_ACCELERATE
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = DURATION_SHORT,
                easing = LinearEasing
            )
        ) + scaleOut(
            targetScale = 0.95f,
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EMPHASIZED_ACCELERATE
            )
        )
    }
    
    /**
     * Backward navigation transition (pop) with Material Motion
     */
    fun backwardTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EMPHASIZED_DECELERATE
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                delayMillis = 50,
                easing = LinearEasing
            )
        ) + scaleIn(
            initialScale = 1.05f,
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EMPHASIZED_DECELERATE
            )
        )
    }
    
    /**
     * Backward exit transition with Material Motion
     */
    fun backwardExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EMPHASIZED_ACCELERATE
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = DURATION_SHORT,
                easing = LinearEasing
            )
        ) + scaleOut(
            targetScale = 1.05f,
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EMPHASIZED_ACCELERATE
            )
        )
    }
    
    /**
     * Modal/dialog-style enter transition
     */
    fun modalEnterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EMPHASIZED_DECELERATE
            )
        ) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EMPHASIZED_DECELERATE
            )
        ) + slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Up,
            initialOffset = { it / 4 },
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EMPHASIZED_DECELERATE
            )
        )
    }
    
    /**
     * Modal/dialog-style exit transition
     */
    fun modalExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(
            animationSpec = tween(
                durationMillis = DURATION_SHORT,
                easing = EMPHASIZED_ACCELERATE
            )
        ) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(
                durationMillis = DURATION_SHORT,
                easing = EMPHASIZED_ACCELERATE
            )
        ) + slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Down,
            targetOffset = { it / 4 },
            animationSpec = tween(
                durationMillis = DURATION_SHORT,
                easing = EMPHASIZED_ACCELERATE
            )
        )
    }
    
    /**
     * Bottom sheet-style enter transition
     */
    fun bottomSheetEnterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Up,
            animationSpec = tween(
                durationMillis = DURATION_LONG,
                easing = EMPHASIZED_DECELERATE
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                delayMillis = 100,
                easing = LinearEasing
            )
        )
    }
    
    /**
     * Bottom sheet-style exit transition
     */
    fun bottomSheetExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Down,
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EMPHASIZED_ACCELERATE
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = DURATION_SHORT,
                easing = LinearEasing
            )
        )
    }
    
    /**
     * Tab switching transition with fade
     */
    fun tabSwitchTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(
            animationSpec = tween(
                durationMillis = DURATION_SHORT,
                easing = STANDARD_EASING
            )
        ) + scaleIn(
            initialScale = 0.98f,
            animationSpec = tween(
                durationMillis = DURATION_SHORT,
                easing = STANDARD_EASING
            )
        )
    }
    
    /**
     * Tab switching exit transition
     */
    fun tabSwitchExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(
            animationSpec = tween(
                durationMillis = DURATION_SHORT,
                easing = STANDARD_EASING
            )
        ) + scaleOut(
            targetScale = 1.02f,
            animationSpec = tween(
                durationMillis = DURATION_SHORT,
                easing = STANDARD_EASING
            )
        )
    }
    
    /**
     * Shared element-style transition for detail screens
     */
    fun sharedElementEnterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(
            animationSpec = tween(
                durationMillis = DURATION_LONG,
                easing = EMPHASIZED_DECELERATE
            )
        ) + slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            initialOffset = { it / 8 },
            animationSpec = tween(
                durationMillis = DURATION_LONG,
                easing = EMPHASIZED_DECELERATE
            )
        ) + scaleIn(
            initialScale = 0.9f,
            animationSpec = tween(
                durationMillis = DURATION_LONG,
                easing = EMPHASIZED_DECELERATE
            )
        )
    }
    
    /**
     * Shared element-style exit transition
     */
    fun sharedElementExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EMPHASIZED_ACCELERATE
            )
        ) + slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            targetOffset = { it / 8 },
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EMPHASIZED_ACCELERATE
            )
        ) + scaleOut(
            targetScale = 0.9f,
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EMPHASIZED_ACCELERATE
            )
        )
    }
    
    /**
     * Cross-fade transition for content replacement
     */
    fun crossfadeTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = STANDARD_EASING
            )
        )
    }
    
    /**
     * Cross-fade exit transition
     */
    fun crossfadeExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = STANDARD_EASING
            )
        )
    }
    
    /**
     * Determine transition type based on navigation direction and screen type
     */
    fun getTransitionForRoute(
        route: String?,
        isPopTransition: Boolean = false
    ): Pair<
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition,
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
    > {
        return when {
            isPopTransition -> backwardTransition() to backwardExitTransition()
            route?.contains("detail") == true -> sharedElementEnterTransition() to sharedElementExitTransition()
            route?.contains("settings") == true -> modalEnterTransition() to modalExitTransition()
            route?.contains("permissions") == true -> bottomSheetEnterTransition() to bottomSheetExitTransition()
            isTabRoute(route) -> tabSwitchTransition() to tabSwitchExitTransition()
            else -> forwardTransition() to forwardExitTransition()
        }
    }
    
    private fun isTabRoute(route: String?): Boolean {
        return route in listOf(
            NavigationTab.Home.route,
            NavigationTab.History.route,
            NavigationTab.Settings.route,
            NavigationTab.Permissions.route
        )
    }
}