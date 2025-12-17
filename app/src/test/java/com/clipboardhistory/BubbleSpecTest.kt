package com.clipboardhistory

import androidx.compose.ui.geometry.Offset
import com.clipboardhistory.presentation.ui.bubble.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BubbleSpecTest {

    @Test
    fun `TextPasteBubble should update visibility based on keyboard state`() {
        val bubble = BubbleSpec.TextPasteBubble(textContent = "test content")

        // Initially visible
        assertTrue(bubble.isVisible)

        // When keyboard hidden, should become invisible
        val updatedBubble = bubble.withKeyboardState(false)
        assertFalse(updatedBubble.isVisible)

        // When keyboard shown, should become visible
        val visibleAgain = updatedBubble.withKeyboardState(true)
        assertTrue(visibleAgain.isVisible)
    }

    @Test
    fun `ToolbeltBubble should minimize when keyboard is shown`() {
        val bubble = BubbleSpec.ToolbeltBubble(tools = emptyList())

        // Initially expanded and visible
        assertTrue(bubble.isVisible)
        assertFalse(bubble.isMinimized)
        assertTrue(bubble.isExpanded)
        assertEquals(280.dp, bubble.size)

        // When keyboard shown, should minimize
        val minimizedBubble = bubble.withKeyboardState(true)
        assertTrue(minimizedBubble.isVisible) // Still visible
        assertTrue(minimizedBubble.isMinimized)
        assertFalse(minimizedBubble.isExpanded)
        assertEquals(48.dp, minimizedBubble.size)
    }

    @Test
    fun `PinnedItem bubble should reposition when keyboard is shown but stay visible`() {
        val bubble = BubbleSpec.TextPasteBubble(
            type = BubbleType.PINNED_ITEM,
            textContent = "pinned content"
        )

        // Should always be visible
        assertTrue(bubble.withKeyboardState(true).isVisible)
        assertTrue(bubble.withKeyboardState(false).isVisible)
    }

    @Test
    fun `SystemBubble should ignore keyboard state changes`() {
        val bubble = BubbleSpec.SystemBubble(
            notificationId = 1,
            title = "Test",
            content = "Content",
            iconResId = 1
        )

        // Should always be visible regardless of keyboard state
        assertTrue(bubble.withKeyboardState(true).isVisible)
        assertTrue(bubble.withKeyboardState(false).isVisible)
    }

    @Test
    fun `bubbles should update position correctly`() {
        val bubble = BubbleSpec.TextPasteBubble(textContent = "test")
        val newPosition = Offset(100f, 200f)

        val updatedBubble = bubble.withPosition(newPosition)
        assertEquals(newPosition, updatedBubble.position)
    }

    @Test
    fun `bubbles should update size correctly`() {
        val bubble = BubbleSpec.TextPasteBubble(textContent = "test")

        val updatedBubble = bubble.withSize(100.dp)
        assertEquals(100.dp, updatedBubble.size)
    }

    @Test
    fun `bubbles should update minimization state correctly`() {
        val toolbeltBubble = BubbleSpec.ToolbeltBubble(tools = emptyList())

        val minimized = toolbeltBubble.withMinimized(true)
        assertTrue(minimized.isMinimized)
        assertFalse(minimized.isExpanded)

        val expanded = minimized.withMinimized(false)
        assertFalse(expanded.isMinimized)
        assertTrue(expanded.isExpanded)
    }

    @Test
    fun `TextPasteBubble should generate correct display text`() {
        // Short text should show in full
        val shortBubble = BubbleSpec.TextPasteBubble(textContent = "short")
        assertEquals("short", shortBubble.displayText)

        // Long text should be truncated
        val longText = "This is a very long text that should be truncated to fit the preview"
        val longBubble = BubbleSpec.TextPasteBubble(textContent = longText)
        assertEquals("This is a very long text that should be trun...", longBubble.displayText)
    }

    @Test
    fun `BubbleSpec create method should create correct bubble types`() {
        // Text paste bubble
        val textBubble = BubbleSpec.create(BubbleType.TEXT_PASTE, content = "test content")
        assertTrue(textBubble is BubbleSpec.TextPasteBubble)
        assertEquals("test content", (textBubble as BubbleSpec.TextPasteBubble).textContent)

        // Toolbelt bubble
        val tools = listOf(ToolbeltTool("test", "Test", 1) {})
        val toolbeltBubble = BubbleSpec.create(BubbleType.TOOLBELT, content = tools)
        assertTrue(toolbeltBubble is BubbleSpec.ToolbeltBubble)
        assertEquals(tools, (toolbeltBubble as BubbleSpec.ToolbeltBubble).tools)

        // Pinned item bubble
        val pinnedBubble = BubbleSpec.create(BubbleType.PINNED_ITEM, content = "pinned")
        assertTrue(pinnedBubble is BubbleSpec.TextPasteBubble)
        assertEquals(BubbleType.PINNED_ITEM, pinnedBubble.type)
        assertEquals("pinned", (pinnedBubble as BubbleSpec.TextPasteBubble).textContent)
    }

    @Test
    fun `withInteraction should update lastInteractionTime`() {
        val bubble = BubbleSpec.TextPasteBubble(textContent = "test")
        val originalTime = bubble.lastInteractionTime

        // Simulate some time passing
        Thread.sleep(10)

        val updatedBubble = bubble.withInteraction()
        assertTrue(updatedBubble.lastInteractionTime > originalTime)
    }

    @Test
    fun `bubbles should respect their type constraints`() {
        // Test max instances
        assertEquals(Int.MAX_VALUE, BubbleType.TEXT_PASTE.maxInstances)
        assertEquals(1, BubbleType.TOOLBELT.maxInstances)
        assertEquals(5, BubbleType.PINNED_ITEM.maxInstances)

        // Test dragging support
        assertTrue(BubbleType.TEXT_PASTE.supportsDragging)
        assertTrue(BubbleType.TOOLBELT.supportsDragging)
        assertFalse(BubbleType.SYSTEM_NOTIFICATION.supportsDragging)
    }
}