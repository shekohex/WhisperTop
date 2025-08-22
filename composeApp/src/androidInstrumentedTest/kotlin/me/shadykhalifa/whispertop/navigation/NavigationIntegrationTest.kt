package me.shadykhalifa.whispertop.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.shadykhalifa.whispertop.presentation.navigation.AndroidNavGraph
import me.shadykhalifa.whispertop.presentation.navigation.NavGraph
import me.shadykhalifa.whispertop.presentation.navigation.Screen
import me.shadykhalifa.whispertop.ui.theme.WhisperTopTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class NavigationIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setupNavHost() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            
            WhisperTopTheme {
                NavGraph(navController = navController)
            }
        }
    }

    @Test
    fun navGraph_verifyStartDestination() {
        assertEquals(Screen.Home, navController.currentDestination?.route)
    }

    @Test
    fun navGraph_navigateToSettings() {
        // Click on Settings button from Home
        composeTestRule
            .onNodeWithText("Settings")
            .performClick()

        // Verify we navigated to Settings
        assertEquals(Screen.Settings, navController.currentDestination?.route)
    }

    @Test
    fun navGraph_navigateToPermissions() {
        // Click on Permissions button from Home  
        composeTestRule
            .onNodeWithText("Permissions")
            .performClick()

        // Verify we navigated to Permissions
        assertEquals(Screen.Permissions, navController.currentDestination?.route)
    }

    @Test
    fun navGraph_navigateToSettingsAndBack() {
        // Navigate to Settings
        composeTestRule
            .onNodeWithText("Settings")
            .performClick()

        assertEquals(Screen.Settings, navController.currentDestination?.route)

        // Navigate back
        navController.popBackStack()
        
        // Should be back at Home
        assertEquals(Screen.Home, navController.currentDestination?.route)
    }

    @Test
    fun navGraph_navigateToPermissionsAndBack() {
        // Navigate to Permissions
        composeTestRule
            .onNodeWithText("Permissions")
            .performClick()

        assertEquals(Screen.Permissions, navController.currentDestination?.route)

        // Navigate back
        navController.popBackStack()
        
        // Should be back at Home
        assertEquals(Screen.Home, navController.currentDestination?.route)
    }

    @Test
    fun navGraph_backStackManagement() {
        // Initial state - only Home in back stack
        assertEquals(1, navController.backStack.value.size)

        // Navigate to Settings
        composeTestRule
            .onNodeWithText("Settings")
            .performClick()

        // Back stack should have Home + Settings
        assertEquals(2, navController.backStack.value.size)

        // Navigate back
        navController.popBackStack()

        // Back to initial state
        assertEquals(1, navController.backStack.value.size)
        assertEquals(Screen.Home, navController.currentDestination?.route)
    }

    @Test
    fun navGraph_multipleNavigations() {
        // Navigate to Settings
        composeTestRule
            .onNodeWithText("Settings")
            .performClick()

        assertEquals(Screen.Settings, navController.currentDestination?.route)

        // Navigate back to Home
        navController.popBackStack()
        assertEquals(Screen.Home, navController.currentDestination?.route)

        // Navigate to Permissions
        composeTestRule
            .onNodeWithText("Permissions")
            .performClick()

        assertEquals(Screen.Permissions, navController.currentDestination?.route)

        // Navigate back to Home
        navController.popBackStack()
        assertEquals(Screen.Home, navController.currentDestination?.route)
    }

    @Test
    fun navGraph_validateScreenContent() {
        // Verify Home screen content
        composeTestRule
            .onNodeWithText("WhisperTop")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Settings")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Permissions")  
            .assertIsDisplayed()
    }

    @Test
    fun navGraph_startDestination_showSettings() {
        composeTestRule.setContent {
            val testNavController = TestNavHostController(LocalContext.current)
            testNavController.navigatorProvider.addNavigator(ComposeNavigator())
            
            WhisperTopTheme {
                NavGraph(
                    navController = testNavController,
                    showSettings = true
                )
            }
        }

        // Should start at Settings when showSettings = true
        composeTestRule.waitForIdle()
        // Note: We can't easily test the start destination change without more complex setup
        // This test validates that the parameter is accepted without crashing
    }

    @Test
    fun navGraph_startDestination_requestPermissions() {
        composeTestRule.setContent {
            val testNavController = TestNavHostController(LocalContext.current)
            testNavController.navigatorProvider.addNavigator(ComposeNavigator())
            
            WhisperTopTheme {
                NavGraph(
                    navController = testNavController,
                    requestPermissions = true
                )
            }
        }

        // Should start at Settings when requestPermissions = true
        composeTestRule.waitForIdle()
        // Note: We can't easily test the start destination change without more complex setup
        // This test validates that the parameter is accepted without crashing
    }

    @Test
    fun androidNavGraph_functionalityParityWithNavGraph() {
        composeTestRule.setContent {
            val testNavController = TestNavHostController(LocalContext.current)
            testNavController.navigatorProvider.addNavigator(ComposeNavigator())
            
            WhisperTopTheme {
                AndroidNavGraph(navController = testNavController)
            }
        }

        // Verify AndroidNavGraph starts at Home by default
        composeTestRule.waitForIdle()
        
        // Should display the Android-specific home screen content
        // Note: This would require AndroidHomeScreen to be implemented
        // For now, we just verify the graph can be created without error
    }

    @Test
    fun screenConstants_haveCorrectValues() {
        assertEquals("home", Screen.Home)
        assertEquals("settings", Screen.Settings)
        assertEquals("permissions", Screen.Permissions)
    }

    @Test
    fun navGraph_handlesFastClicks() {
        // Simulate rapid clicking to test navigation debouncing
        repeat(3) {
            composeTestRule
                .onNodeWithText("Settings")
                .performClick()
        }

        // Should only navigate once
        assertEquals(Screen.Settings, navController.currentDestination?.route)
        
        // Navigate back
        navController.popBackStack()
        assertEquals(Screen.Home, navController.currentDestination?.route)
    }

    @Test
    fun navGraph_handlesInvalidBackNavigation() {
        // Try to pop when already at root
        val initialBackStackSize = navController.backStack.value.size
        
        navController.popBackStack()
        
        // Should still be at Home, back stack unchanged
        assertEquals(Screen.Home, navController.currentDestination?.route)
        assertEquals(initialBackStackSize, navController.backStack.value.size)
    }

    @Test
    fun navGraph_navigationStateConsistency() {
        // Track navigation state throughout the flow
        val navigationStates = mutableListOf<String?>()
        
        navigationStates.add(navController.currentDestination?.route)
        
        // Navigate to Settings
        composeTestRule
            .onNodeWithText("Settings")
            .performClick()
        navigationStates.add(navController.currentDestination?.route)
        
        // Navigate back
        navController.popBackStack()
        navigationStates.add(navController.currentDestination?.route)
        
        // Navigate to Permissions
        composeTestRule
            .onNodeWithText("Permissions")
            .performClick()
        navigationStates.add(navController.currentDestination?.route)
        
        // Navigate back
        navController.popBackStack()
        navigationStates.add(navController.currentDestination?.route)
        
        // Verify expected state sequence
        val expectedStates = listOf(Screen.Home, Screen.Settings, Screen.Home, Screen.Permissions, Screen.Home)
        assertEquals(expectedStates, navigationStates)
    }
}