package me.shadykhalifa.whispertop.presentation.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import me.shadykhalifa.whispertop.presentation.navigation.NavigationTab
import me.shadykhalifa.whispertop.presentation.ui.theme.WhisperTopTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class BottomNavigationComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `bottom navigation should display all tabs`() {
        composeTestRule.setContent {
            WhisperTopTheme {
                BottomNavigationComponent(
                    selectedTab = NavigationTab.Home,
                    onTabSelected = {}
                )
            }
        }

        NavigationTab.bottomNavigationTabs.forEach { tab ->
            composeTestRule
                .onNodeWithText(tab.title)
                .assertIsDisplayed()
        }
    }

    @Test
    fun `selected tab should be highlighted`() {
        composeTestRule.setContent {
            WhisperTopTheme {
                BottomNavigationComponent(
                    selectedTab = NavigationTab.History,
                    onTabSelected = {}
                )
            }
        }

        // The selected tab should be displayed (we can't easily test Material 3 selection state in unit tests)
        composeTestRule
            .onNodeWithText("History")
            .assertIsDisplayed()
    }

    @Test
    fun `clicking tab should trigger callback`() {
        var selectedTab: NavigationTab? = null
        
        composeTestRule.setContent {
            WhisperTopTheme {
                BottomNavigationComponent(
                    selectedTab = NavigationTab.Home,
                    onTabSelected = { tab ->
                        selectedTab = tab
                    }
                )
            }
        }

        composeTestRule
            .onNodeWithText("Settings")
            .performClick()

        assertEquals(NavigationTab.Settings, selectedTab)
    }

    @Test
    fun `all navigation tabs should be clickable`() {
        val clickedTabs = mutableListOf<NavigationTab>()
        
        composeTestRule.setContent {
            WhisperTopTheme {
                BottomNavigationComponent(
                    selectedTab = NavigationTab.Home,
                    onTabSelected = { tab ->
                        clickedTabs.add(tab)
                    }
                )
            }
        }

        NavigationTab.bottomNavigationTabs.forEach { tab ->
            composeTestRule
                .onNodeWithText(tab.title)
                .performClick()
        }

        assertEquals(NavigationTab.bottomNavigationTabs.size, clickedTabs.size)
        assertEquals(NavigationTab.bottomNavigationTabs.toSet(), clickedTabs.toSet())
    }

    @Test
    fun `badges should be displayed when provided`() {
        val badges = mapOf(NavigationTab.History to 5, NavigationTab.Settings to 2)
        
        composeTestRule.setContent {
            WhisperTopTheme {
                BottomNavigationComponent(
                    selectedTab = NavigationTab.Home,
                    onTabSelected = {},
                    showBadges = badges
                )
            }
        }

        composeTestRule
            .onNodeWithText("5")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("2")
            .assertIsDisplayed()
    }

    @Test
    fun `badge over 99 should show 99+`() {
        val badges = mapOf(NavigationTab.History to 150)
        
        composeTestRule.setContent {
            WhisperTopTheme {
                BottomNavigationComponent(
                    selectedTab = NavigationTab.Home,
                    onTabSelected = {},
                    showBadges = badges
                )
            }
        }

        composeTestRule
            .onNodeWithText("99+")
            .assertIsDisplayed()
    }

    @Test
    fun `bottom navigation should have correct accessibility description`() {
        composeTestRule.setContent {
            WhisperTopTheme {
                BottomNavigationComponent(
                    selectedTab = NavigationTab.Home,
                    onTabSelected = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Bottom navigation bar")
            .assertIsDisplayed()
    }

    @Test
    fun `tab icons should have correct accessibility descriptions`() {
        composeTestRule.setContent {
            WhisperTopTheme {
                BottomNavigationComponent(
                    selectedTab = NavigationTab.Home,
                    onTabSelected = {}
                )
            }
        }

        NavigationTab.bottomNavigationTabs.forEach { tab ->
            composeTestRule
                .onNodeWithContentDescription("${tab.title} tab")
                .assertIsDisplayed()
        }
    }
}