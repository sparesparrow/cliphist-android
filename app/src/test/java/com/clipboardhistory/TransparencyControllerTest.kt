package com.clipboardhistory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clipboardhistory.presentation.ui.toolbelt.TransparencyController
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransparencyControllerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        // Reset transparency to default
        TransparencyController.setGlobalOpacity(context, TransparencyController.getGlobalOpacity(context))
    }

    @Test
    fun `opacity should be clamped to valid range`() {
        // Test setting opacity below 0
        TransparencyController.setGlobalOpacity(context, -0.5f)
        assertEquals(0.0f, TransparencyController.getGlobalOpacity(context))

        // Test setting opacity above 1
        TransparencyController.setGlobalOpacity(context, 1.5f)
        assertEquals(1.0f, TransparencyController.getGlobalOpacity(context))

        // Test setting valid opacity
        TransparencyController.setGlobalOpacity(context, 0.7f)
        assertEquals(0.7f, TransparencyController.getGlobalOpacity(context))
    }

    @Test
    fun `opacity should persist across app restarts`() {
        val testOpacity = 0.6f

        // Set opacity
        TransparencyController.setGlobalOpacity(context, testOpacity)

        // Verify it's saved
        assertEquals(testOpacity, TransparencyController.getGlobalOpacity(context))

        // Simulate app restart by creating new context
        val newContext = ApplicationProvider.getApplicationContext()

        // Verify opacity is loaded from preferences
        assertEquals(testOpacity, TransparencyController.getGlobalOpacity(newContext))
    }

    @Test
    fun `callbacks should be notified when opacity changes`() {
        var callbackCalled = false
        var receivedOpacity = 0.0f

        val callback: (Float) -> Unit = { opacity ->
            callbackCalled = true
            receivedOpacity = opacity
        }

        // Register callback
        TransparencyController.registerBubble(callback)

        // Change opacity
        val testOpacity = 0.8f
        TransparencyController.setGlobalOpacity(context, testOpacity)

        // Verify callback was called with correct opacity
        assertTrue(callbackCalled)
        assertEquals(testOpacity, receivedOpacity)

        // Cleanup
        TransparencyController.unregisterBubble(callback)
    }

    @Test
    fun `multiple callbacks should all be notified`() {
        var callback1Called = false
        var callback2Called = false

        val callback1: (Float) -> Unit = { callback1Called = true }
        val callback2: (Float) -> Unit = { callback2Called = true }

        // Register callbacks
        TransparencyController.registerBubble(callback1)
        TransparencyController.registerBubble(callback2)

        // Change opacity
        TransparencyController.setGlobalOpacity(context, 0.5f)

        // Verify both callbacks were called
        assertTrue(callback1Called)
        assertTrue(callback2Called)

        // Cleanup
        TransparencyController.unregisterBubble(callback1)
        TransparencyController.unregisterBubble(callback2)
    }

    @Test
    fun `unregistering callback should stop notifications`() {
        var callbackCalled = false

        val callback: (Float) -> Unit = { callbackCalled = true }

        // Register and then unregister callback
        TransparencyController.registerBubble(callback)
        TransparencyController.unregisterBubble(callback)

        // Change opacity
        TransparencyController.setGlobalOpacity(context, 0.3f)

        // Verify callback was not called
        assertTrue(!callbackCalled)
    }

    @Test
    fun `default opacity should be returned when no preference is set`() {
        // Clear any existing preferences
        context.getSharedPreferences("cliphist_transparency", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        // Verify default opacity is returned
        assertEquals(0.8f, TransparencyController.getGlobalOpacity(context))
    }
}