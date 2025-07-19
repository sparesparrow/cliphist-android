package com.clipboardhistory.presentation.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ClipboardSettings
import com.clipboardhistory.domain.model.ContentType
import com.clipboardhistory.presentation.ui.theme.ClipboardHistoryTheme
import com.clipboardhistory.presentation.viewmodels.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * UI tests for MainScreen composable.
 * 
 * This test class verifies the UI behavior and user interactions
 * of the main screen.
 */
@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `main screen displays correctly when service is stopped`() {
        val mockViewModel = mock(MainViewModel::class.java)
        val uiState = MainViewModel.MainUiState(
            clipboardItems = emptyList(),
            settings = ClipboardSettings(),
            isServiceRunning = false
        )
        
        whenever(mockViewModel.uiState).thenReturn(MutableStateFlow(uiState))
        
        composeTestRule.setContent {
            ClipboardHistoryTheme {
                MainScreen(
                    viewModel = mockViewModel,
                    onStartServices = {},
                    onStopServices = {}
                )
            }
        }
        
        // Verify UI elements are displayed
        composeTestRule.onNodeWithText("Clipboard History").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clipboard service is stopped").assertIsDisplayed()
        composeTestRule.onNodeWithText("No clipboard items yet").assertIsDisplayed()
    }
    
    @Test
    fun `main screen displays clipboard items correctly`() {
        val mockViewModel = mock(MainViewModel::class.java)
        val testItems = listOf(
            ClipboardItem(
                id = "1",
                content = "Test content 1",
                timestamp = System.currentTimeMillis(),
                contentType = ContentType.TEXT,
                isEncrypted = false,
                size = 14
            ),
            ClipboardItem(
                id = "2",
                content = "Test content 2",
                timestamp = System.currentTimeMillis(),
                contentType = ContentType.TEXT,
                isEncrypted = true,
                size = 14
            )
        )
        
        val uiState = MainViewModel.MainUiState(
            clipboardItems = testItems,
            settings = ClipboardSettings(),
            isServiceRunning = true
        )
        
        whenever(mockViewModel.uiState).thenReturn(MutableStateFlow(uiState))
        
        composeTestRule.setContent {
            ClipboardHistoryTheme {
                MainScreen(
                    viewModel = mockViewModel,
                    onStartServices = {},
                    onStopServices = {}
                )
            }
        }
        
        // Verify clipboard items are displayed
        composeTestRule.onNodeWithText("Test content 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test content 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("🔒 Encrypted").assertIsDisplayed()
    }
    
    @Test
    fun `floating action button toggles service state`() {
        val mockViewModel = mock(MainViewModel::class.java)
        var startServicesCalled = false
        var stopServicesCalled = false
        
        val uiState = MainViewModel.MainUiState(
            clipboardItems = emptyList(),
            settings = ClipboardSettings(),
            isServiceRunning = false
        )
        
        whenever(mockViewModel.uiState).thenReturn(MutableStateFlow(uiState))
        
        composeTestRule.setContent {
            ClipboardHistoryTheme {
                MainScreen(
                    viewModel = mockViewModel,
                    onStartServices = { startServicesCalled = true },
                    onStopServices = { stopServicesCalled = true }
                )
            }
        }
        
        // Click the FAB
        composeTestRule.onNodeWithContentDescription("Start Service").performClick()
        
        // Verify start services was called
        assert(startServicesCalled)
        verify(mockViewModel).updateServiceRunningState(true)
    }
    
    @Test
    fun `settings button opens settings dialog`() {
        val mockViewModel = mock(MainViewModel::class.java)
        val uiState = MainViewModel.MainUiState(
            clipboardItems = emptyList(),
            settings = ClipboardSettings(),
            isServiceRunning = false
        )
        
        whenever(mockViewModel.uiState).thenReturn(MutableStateFlow(uiState))
        
        composeTestRule.setContent {
            ClipboardHistoryTheme {
                MainScreen(
                    viewModel = mockViewModel,
                    onStartServices = {},
                    onStopServices = {}
                )
            }
        }
        
        // Click settings button
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        
        // Verify settings dialog is displayed
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Max History Size").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable Encryption").assertIsDisplayed()
    }
    
    @Test
    fun `add button opens add dialog`() {
        val mockViewModel = mock(MainViewModel::class.java)
        val uiState = MainViewModel.MainUiState(
            clipboardItems = emptyList(),
            settings = ClipboardSettings(),
            isServiceRunning = false
        )
        
        whenever(mockViewModel.uiState).thenReturn(MutableStateFlow(uiState))
        
        composeTestRule.setContent {
            ClipboardHistoryTheme {
                MainScreen(
                    viewModel = mockViewModel,
                    onStartServices = {},
                    onStopServices = {}
                )
            }
        }
        
        // Click add button
        composeTestRule.onNodeWithContentDescription("Add Item").performClick()
        
        // Verify add dialog is displayed
        composeTestRule.onNodeWithText("Add Clipboard Item").assertIsDisplayed()
        composeTestRule.onNodeWithText("Content").assertIsDisplayed()
    }
}