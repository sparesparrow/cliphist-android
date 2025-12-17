package com.clipboardhistory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clipboardhistory.domain.model.OperationMode
import com.clipboardhistory.presentation.ui.toolbelt.BubbleStateManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BubbleStateManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Load initial state
        BubbleStateManager.loadFromPreferences(context)
    }

    @After
    fun tearDown() {
        // Reset to defaults
        context.getSharedPreferences("bubble_state", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @Test
    fun `toggleVisibility should update private visibility state`() = runBlocking {
        // Initially should be true
        assertTrue(BubbleStateManager.privateVisible.first())

        // Toggle to false
        BubbleStateManager.toggleVisibility(context, "Private", false)
        assertFalse(BubbleStateManager.privateVisible.first())

        // Toggle back to true
        BubbleStateManager.toggleVisibility(context, "Private", true)
        assertTrue(BubbleStateManager.privateVisible.first())
    }

    @Test
    fun `toggleVisibility should update history visibility state`() = runBlocking {
        // Initially should be true
        assertTrue(BubbleStateManager.historyVisible.first())

        // Toggle to false
        BubbleStateManager.toggleVisibility(context, "History", false)
        assertFalse(BubbleStateManager.historyVisible.first())

        // Toggle back to true
        BubbleStateManager.toggleVisibility(context, "History", true)
        assertTrue(BubbleStateManager.historyVisible.first())
    }

    @Test
    fun `setOperationMode should update operation mode state`() = runBlocking {
        // Initially should be OVERWRITE
        assertEquals(OperationMode.OVERWRITE, BubbleStateManager.operationMode.first())

        // Change to APPEND
        BubbleStateManager.setOperationMode(context, OperationMode.APPEND)
        assertEquals(OperationMode.APPEND, BubbleStateManager.operationMode.first())

        // Change to PREPEND
        BubbleStateManager.setOperationMode(context, OperationMode.PREPEND)
        assertEquals(OperationMode.PREPEND, BubbleStateManager.operationMode.first())
    }

    @Test
    fun `shouldShowBubble should return correct visibility for private bubbles`() = runBlocking {
        // Test private bubble when private visibility is enabled
        BubbleStateManager.toggleVisibility(context, "Private", true)
        assertTrue(BubbleStateManager.shouldShowBubble(isPrivate = true, isHistory = false))

        // Test private bubble when private visibility is disabled
        BubbleStateManager.toggleVisibility(context, "Private", false)
        assertFalse(BubbleStateManager.shouldShowBubble(isPrivate = true, isHistory = false))

        // Test non-private bubble should always be visible regardless of private setting
        assertTrue(BubbleStateManager.shouldShowBubble(isPrivate = false, isHistory = false))
    }

    @Test
    fun `shouldShowBubble should return correct visibility for history bubbles`() = runBlocking {
        // Test history bubble when history visibility is enabled
        BubbleStateManager.toggleVisibility(context, "History", true)
        assertTrue(BubbleStateManager.shouldShowBubble(isPrivate = false, isHistory = true))

        // Test history bubble when history visibility is disabled
        BubbleStateManager.toggleVisibility(context, "History", false)
        assertFalse(BubbleStateManager.shouldShowBubble(isPrivate = false, isHistory = true))

        // Test non-history bubble should always be visible regardless of history setting
        assertTrue(BubbleStateManager.shouldShowBubble(isPrivate = false, isHistory = false))
    }

    @Test
    fun `loadFromPreferences should restore saved states`() = runBlocking {
        // Set specific states
        BubbleStateManager.toggleVisibility(context, "Private", false)
        BubbleStateManager.toggleVisibility(context, "History", false)
        BubbleStateManager.setOperationMode(context, OperationMode.APPEND)

        // Create new instance (simulate app restart)
        val newContext = ApplicationProvider.getApplicationContext()

        // Load from preferences
        BubbleStateManager.loadFromPreferences(newContext)

        // Verify states are restored
        assertFalse(BubbleStateManager.privateVisible.first())
        assertFalse(BubbleStateManager.historyVisible.first())
        assertEquals(OperationMode.APPEND, BubbleStateManager.operationMode.first())
    }

    @Test
    fun `loadFromPreferences should handle invalid operation mode gracefully`() = runBlocking {
        // Save invalid operation mode string
        context.getSharedPreferences("bubble_state", Context.MODE_PRIVATE)
            .edit()
            .putString("operation_mode", "INVALID_MODE")
            .apply()

        // Load from preferences
        BubbleStateManager.loadFromPreferences(context)

        // Should default to OVERWRITE
        assertEquals(OperationMode.OVERWRITE, BubbleStateManager.operationMode.first())
    }

    @Test
    fun `states should persist across context changes`() = runBlocking {
        // Set states
        BubbleStateManager.toggleVisibility(context, "Private", false)
        BubbleStateManager.setOperationMode(context, OperationMode.PREPEND)

        // Simulate context change (app restart)
        val newContext = ApplicationProvider.getApplicationContext()

        // Load states
        BubbleStateManager.loadFromPreferences(newContext)

        // Verify persistence
        assertFalse(BubbleStateManager.privateVisible.first())
        assertEquals(OperationMode.PREPEND, BubbleStateManager.operationMode.first())
    }
}