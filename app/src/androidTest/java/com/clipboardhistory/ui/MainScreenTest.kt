package com.clipboardhistory.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.presentation.ui.screens.MainScreen
import com.clipboardhistory.presentation.viewmodels.MainViewModel
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/**
 * UI tests for MainScreen composable.
 *
 * Tests basic UI interactions and accessibility features.
 */
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockViewModel = mock(MainViewModel::class.java)

    @Test
    fun mainScreen_displaysCorrectTitle() {
        composeTestRule.setContent {
            MainScreen(
                viewModel = mockViewModel,
                onNavigateToSettings = {},
                onNavigateToStatistics = {},
                onNavigateToItemDetail = {},
                onNavigateToSmartActions = {},
                onStartServices = {},
                onStopServices = {},
            )
        }

        // Check if main screen title is displayed
        composeTestRule.onNodeWithText("Clipboard History").assertIsDisplayed()
    }

    @Test
    fun mainScreen_displaysServiceControls() {
        composeTestRule.setContent {
            MainScreen(
                viewModel = mockViewModel,
                onNavigateToSettings = {},
                onNavigateToStatistics = {},
                onNavigateToItemDetail = {},
                onNavigateToSmartActions = {},
                onStartServices = {},
                onStopServices = {},
            )
        }

        // Check for service control buttons
        composeTestRule.onNodeWithText("Start Services").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stop Services").assertIsDisplayed()
    }

    @Test
    fun mainScreen_startServicesButton_clickTriggersCallback() {
        var startServicesClicked = false

        composeTestRule.setContent {
            MainScreen(
                viewModel = mockViewModel,
                onNavigateToSettings = {},
                onNavigateToStatistics = {},
                onNavigateToItemDetail = {},
                onNavigateToSmartActions = {},
                onStartServices = { startServicesClicked = true },
                onStopServices = {},
            )
        }

        // Click the start services button
        composeTestRule.onNodeWithText("Start Services").performClick()

        // Verify callback was triggered
        assert(startServicesClicked)
    }

    @Test
    fun mainScreen_stopServicesButton_clickTriggersCallback() {
        var stopServicesClicked = false

        composeTestRule.setContent {
            MainScreen(
                viewModel = mockViewModel,
                onNavigateToSettings = {},
                onNavigateToStatistics = {},
                onNavigateToItemDetail = {},
                onNavigateToSmartActions = {},
                onStartServices = {},
                onStopServices = { stopServicesClicked = true },
            )
        }

        // Click the stop services button
        composeTestRule.onNodeWithText("Stop Services").performClick()

        // Verify callback was triggered
        assert(stopServicesClicked)
    }

    @Test
    fun mainScreen_navigationButtons_exist() {
        composeTestRule.setContent {
            MainScreen(
                viewModel = mockViewModel,
                onNavigateToSettings = {},
                onNavigateToStatistics = {},
                onNavigateToItemDetail = {},
                onNavigateToSmartActions = {},
                onStartServices = {},
                onStopServices = {},
            )
        }

        // Check for navigation buttons in the UI
        // These might be in a menu or bottom navigation
        composeTestRule.onNodeWithContentDescription("Settings").assertExists()
        composeTestRule.onNodeWithContentDescription("Statistics").assertExists()
    }

    @Test
    fun mainScreen_accessibility_contentDescriptions() {
        composeTestRule.setContent {
            MainScreen(
                viewModel = mockViewModel,
                onNavigateToSettings = {},
                onNavigateToStatistics = {},
                onNavigateToItemDetail = {},
                onNavigateToSmartActions = {},
                onStartServices = {},
                onStopServices = {},
            )
        }

        // Test accessibility content descriptions
        composeTestRule.onNodeWithContentDescription("Start clipboard monitoring services").assertExists()
        composeTestRule.onNodeWithContentDescription("Stop clipboard monitoring services").assertExists()
    }
}