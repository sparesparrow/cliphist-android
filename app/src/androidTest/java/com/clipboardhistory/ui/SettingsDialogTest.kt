package com.clipboardhistory.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.clipboardhistory.domain.model.ClipboardSettings
import com.clipboardhistory.presentation.services.LocalApiControl
import com.clipboardhistory.presentation.ui.components.SettingsDialog
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The settings dialog is what the gear on the main screen opens, so the local API switch for
 * agents has to be in it (the separate SettingsScreen is not reachable from the app).
 */
class SettingsDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun apiOff() {
        LocalApiControl.disable(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @Test
    fun settingsDialog_showsLocalApiSwitch_offByDefault() {
        composeTestRule.setContent {
            SettingsDialog(settings = ClipboardSettings(), onDismiss = {}, onSave = {})
        }

        composeTestRule.onNodeWithText("Local API for agents").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Copy token").assertDoesNotExist()
    }
}
