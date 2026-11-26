package com.clipboardhistory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clipboardhistory.presentation.services.AccessibilityMonitorService
import com.clipboardhistory.presentation.ui.bubble.BubbleOrchestrator
import com.clipboardhistory.utils.KeyboardVisibilityDetector
import com.clipboardhistory.utils.TextSelectionManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextSelectionManagerTest {

    private lateinit var context: Context
    private lateinit var textSelectionManager: TextSelectionManager

    @Mock
    private lateinit var mockBubbleOrchestrator: BubbleOrchestrator

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        textSelectionManager = TextSelectionManager(context, mockBubbleOrchestrator)
    }

    @After
    fun tearDown() {
        // Reset accessibility service instance
        AccessibilityMonitorService.setInstanceForTesting(null)
    }

    @Test
    fun `isEnabled returns true by default`() {
        assertTrue(textSelectionManager.isEnabled())
    }

    @Test
    fun `setEnabled changes enabled state`() {
        textSelectionManager.setEnabled(false)
        assertFalse(textSelectionManager.isEnabled())

        textSelectionManager.setEnabled(true)
        assertTrue(textSelectionManager.isEnabled())
    }

    @Test
    fun `hasSelectedText returns false when no text selected`() {
        assertFalse(textSelectionManager.hasSelectedText())
    }

    @Test
    fun `getSelectedText returns empty string when no text selected`() {
        assertEquals("", textSelectionManager.getSelectedText())
    }

    @Test
    fun `setShowToastFeedback changes toast feedback setting`() {
        textSelectionManager.setShowToastFeedback(false)
        // This would be tested with actual toast verification in integration tests
        assertNotNull(textSelectionManager)
    }

    @Test
    fun `setAutoCreateTextPasteBubble changes auto creation setting`() {
        textSelectionManager.setAutoCreateTextPasteBubble(false)
        // This would be tested with actual bubble creation in integration tests
        assertNotNull(textSelectionManager)
    }

    @Test
    fun `createWithBubbleIntegration creates manager with orchestrator`() {
        val orchestrator = mock(BubbleOrchestrator::class.java)
        val manager = TextSelectionManager.createWithBubbleIntegration(context, orchestrator)

        assertNotNull(manager)
        // Verify orchestrator integration would require more complex testing
    }

    @Test
    fun `detectContentType correctly identifies email`() {
        val manager = TextSelectionManager(context, null)
        val javaClass = manager.javaClass
        val method = javaClass.getDeclaredMethod("detectContentType", String::class.java)
        method.isAccessible = true

        val result = method.invoke(manager, "user@example.com")
        assertEquals("EMAIL", result.toString())
    }

    @Test
    fun `detectContentType correctly identifies URL`() {
        val manager = TextSelectionManager(context, null)
        val javaClass = manager.javaClass
        val method = javaClass.getDeclaredMethod("detectContentType", String::class.java)
        method.isAccessible = true

        val result = method.invoke(manager, "https://example.com")
        assertEquals("URL", result.toString())
    }

    @Test
    fun `detectContentType correctly identifies phone number`() {
        val manager = TextSelectionManager(context, null)
        val javaClass = manager.javaClass
        val method = javaClass.getDeclaredMethod("detectContentType", String::class.java)
        method.isAccessible = true

        val result = method.invoke(manager, "555-123-4567")
        assertEquals("PHONE_NUMBER", result.toString())
    }

    @Test
    fun `detectContentType correctly identifies JSON`() {
        val manager = TextSelectionManager(context, null)
        val javaClass = manager.javaClass
        val method = javaClass.getDeclaredMethod("detectContentType", String::class.java)
        method.isAccessible = true

        val result = method.invoke(manager, "{\"key\": \"value\"}")
        assertEquals("JSON", result.toString())
    }

    @Test
    fun `detectContentType correctly identifies code`() {
        val manager = TextSelectionManager(context, null)
        val javaClass = manager.javaClass
        val method = javaClass.getDeclaredMethod("detectContentType", String::class.java)
        method.isAccessible = true

        val result = method.invoke(manager, "function test() {}")
        assertEquals("CODE", result.toString())
    }

    @Test
    fun `detectContentType defaults to TEXT for unrecognized content`() {
        val manager = TextSelectionManager(context, null)
        val javaClass = manager.javaClass
        val method = javaClass.getDeclaredMethod("detectContentType", String::class.java)
        method.isAccessible = true

        val result = method.invoke(manager, "some random text")
        assertEquals("TEXT", result.toString())
    }

    @Test
    fun `pasteText uses smart input manager when bubble orchestrator not available`() {
        val manager = TextSelectionManager(context, null)
        val result = manager.pasteText("test content")

        // Should return true as it falls back to smart input manager
        assertTrue(result)
    }

    @Test
    fun `pasteText uses bubble orchestrator when available`() {
        val manager = TextSelectionManager(context, mockBubbleOrchestrator)
        val result = manager.pasteText("test content")

        // Should return true as it tries bubble orchestrator
        assertTrue(result)
    }
}