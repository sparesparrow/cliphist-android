package com.clipboardhistory

import androidx.compose.ui.unit.dp
import com.clipboardhistory.presentation.ui.bubble.BubbleType
import com.clipboardhistory.presentation.ui.bubble.KeyboardPolicy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BubbleTypeTest {

    @Test
    fun `TEXT_PASTE bubble should show when keyboard visible, hide when keyboard hidden`() {
        assertTrue(BubbleType.TEXT_PASTE.shouldBeVisible(true))
        assertFalse(BubbleType.TEXT_PASTE.shouldBeVisible(false))
    }

    @Test
    fun `TOOLBELT bubble should always be visible but minimize when keyboard shown`() {
        assertTrue(BubbleType.TOOLBELT.shouldBeVisible(true))
        assertTrue(BubbleType.TOOLBELT.shouldBeVisible(false))
        assertTrue(BubbleType.TOOLBELT.shouldBeMinimized(true))
        assertFalse(BubbleType.TOOLBELT.shouldBeMinimized(false))
    }

    @Test
    fun `PINNED_ITEM bubble should always be visible and reposition when keyboard shown`() {
        assertTrue(BubbleType.PINNED_ITEM.shouldBeVisible(true))
        assertTrue(BubbleType.PINNED_ITEM.shouldBeVisible(false))
        assertTrue(BubbleType.PINNED_ITEM.shouldBeRepositioned(true))
        assertFalse(BubbleType.PINNED_ITEM.shouldBeRepositioned(false))
    }

    @Test
    fun `SYSTEM_NOTIFICATION bubble should ignore keyboard state`() {
        assertTrue(BubbleType.SYSTEM_NOTIFICATION.shouldBeVisible(true))
        assertTrue(BubbleType.SYSTEM_NOTIFICATION.shouldBeVisible(false))
        assertFalse(BubbleType.SYSTEM_NOTIFICATION.shouldBeMinimized(true))
        assertFalse(BubbleType.SYSTEM_NOTIFICATION.shouldBeRepositioned(true))
    }

    @Test
    fun `QUICK_ACTION bubble should show when keyboard visible and support dragging`() {
        assertTrue(BubbleType.QUICK_ACTION.shouldBeVisible(true))
        assertFalse(BubbleType.QUICK_ACTION.shouldBeVisible(false))
        assertTrue(BubbleType.QUICK_ACTION.supportsDragging)
        assertEquals(15000L, BubbleType.QUICK_ACTION.autoHideDelay)
    }

    @Test
    fun `bubble size should adjust based on keyboard state and minimization`() {
        // Normal state
        assertEquals(BubbleType.TEXT_PASTE.defaultSize, BubbleType.TEXT_PASTE.getSize(false, false))

        // Minimized state
        assertEquals(32.dp, BubbleType.TEXT_PASTE.getSize(true, true))

        // Toolbelt when minimized due to keyboard
        assertEquals(40.dp, BubbleType.TOOLBELT.getSize(true, true))
    }

    @Test
    fun `bubble types should have appropriate instance limits and priorities`() {
        assertEquals(Int.MAX_VALUE, BubbleType.TEXT_PASTE.maxInstances)
        assertEquals(1, BubbleType.TOOLBELT.maxInstances)
        assertEquals(5, BubbleType.PINNED_ITEM.maxInstances)
        assertEquals(3, BubbleType.SYSTEM_NOTIFICATION.maxInstances)

        // Z-index priorities
        assertEquals(1, BubbleType.TEXT_PASTE.zIndexPriority)
        assertEquals(2, BubbleType.TOOLBELT.zIndexPriority)
        assertEquals(3, BubbleType.PINNED_ITEM.zIndexPriority)
        assertEquals(4, BubbleType.QUICK_ACTION.zIndexPriority)
    }

    @Test
    fun `bubble types should have appropriate auto-hide behavior`() {
        assertEquals(30000L, BubbleType.TEXT_PASTE.autoHideDelay) // 30 seconds
        assertEquals(0L, BubbleType.TOOLBELT.autoHideDelay) // Never auto-hide
        assertEquals(0L, BubbleType.PINNED_ITEM.autoHideDelay) // Never auto-hide
        assertEquals(10000L, BubbleType.SYSTEM_NOTIFICATION.autoHideDelay) // 10 seconds
        assertEquals(15000L, BubbleType.QUICK_ACTION.autoHideDelay) // 15 seconds
    }

    @Test
    fun `bubble types should have appropriate default sizes`() {
        assertEquals(48.dp, BubbleType.TEXT_PASTE.defaultSize)
        assertEquals(280.dp, BubbleType.TOOLBELT.defaultSize) // Wider for tools
        assertEquals(44.dp, BubbleType.PINNED_ITEM.defaultSize)
        assertEquals(56.dp, BubbleType.SYSTEM_NOTIFICATION.defaultSize)
        assertEquals(52.dp, BubbleType.QUICK_ACTION.defaultSize)
    }
}