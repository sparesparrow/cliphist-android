package com.clipboardhistory.presentation.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.clipboardhistory.domain.model.BubbleType
import com.clipboardhistory.domain.model.OperationMode
import com.clipboardhistory.domain.model.ToolBeltBubble
import com.clipboardhistory.domain.model.ToolBeltBubbleType
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ToolBeltViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: ToolBeltViewModel

    @Before
    fun setUp() {
        viewModel = ToolBeltViewModel()
    }

    @Test
    fun testInitializeDefaultToolBelt() {
        viewModel.initializeDefaultToolBelt()
        val bubbles = viewModel.toolBubbles.value
        assertNotNull(bubbles)
        assertTrue(bubbles!!.isNotEmpty())
        assertTrue(bubbles.size > 0)
    }

    @Test
    fun testDefaultBubbleTypes() {
        viewModel.initializeDefaultToolBelt()
        val bubbles = viewModel.toolBubbles.value!!

        // Check that default bubbles are present
        val bubbleTypes = bubbles.map { it.type }
        assertTrue(bubbleTypes.contains(ToolBeltBubbleType.OPACITY_SLIDER))
        assertTrue(bubbleTypes.contains(ToolBeltBubbleType.PRIVATE_VISIBILITY_TOGGLE))
        assertTrue(bubbleTypes.contains(ToolBeltBubbleType.HISTORY_VISIBILITY_TOGGLE))
        assertTrue(bubbleTypes.contains(ToolBeltBubbleType.OPERATION_MODE_SWITCHER))
    }

    @Test
    fun testAddBubble() {
        val bubble = ToolBeltBubble(
            type = ToolBeltBubbleType.GENERIC,
            content = "Test Bubble",
        )
        viewModel.addBubble(bubble)
        val bubbles = viewModel.toolBubbles.value
        assertNotNull(bubbles)
        assertTrue(bubbles!!.contains(bubble))
    }

    @Test
    fun testRemoveBubble() {
        val bubble = ToolBeltBubble(
            type = ToolBeltBubbleType.GENERIC,
            content = "Test Bubble",
        )
        viewModel.addBubble(bubble)
        viewModel.removeBubble(bubble.id)
        val bubbles = viewModel.toolBubbles.value
        assertFalse(bubbles!!.contains(bubble))
    }

    @Test
    fun testUpdateBubble() {
        val bubble = ToolBeltBubble(
            type = ToolBeltBubbleType.GENERIC,
            content = "Original",
        )
        viewModel.addBubble(bubble)

        val updated = bubble.copy(content = "Updated")
        viewModel.updateBubble(updated)

        val bubbles = viewModel.toolBubbles.value
        val foundBubble = bubbles!!.find { it.id == bubble.id }
        assertNotNull(foundBubble)
        assertEquals("Updated", foundBubble?.content)
    }

    @Test
    fun testChangeBubbleContent() {
        val bubble = ToolBeltBubble(
            type = ToolBeltBubbleType.GENERIC,
            content = "Original",
        )
        viewModel.addBubble(bubble)
        viewModel.changeBubbleContent(bubble.id, "Changed")

        val updated = viewModel.getBubbleById(bubble.id)
        assertNotNull(updated)
        assertEquals("Changed", updated?.content)
    }

    @Test
    fun testChangeBubbleType() {
        val bubble = ToolBeltBubble(
            type = ToolBeltBubbleType.GENERIC,
            bubbleType = BubbleType.CIRCLE,
        )
        viewModel.addBubble(bubble)
        viewModel.changeBubbleType(bubble.id, BubbleType.SQUARE)

        val updated = viewModel.getBubbleById(bubble.id)
        assertNotNull(updated)
        assertEquals(BubbleType.SQUARE, updated?.bubbleType)
    }

    @Test
    fun testSetOpacityLevel() {
        viewModel.setOpacityLevel(0.5f)
        assertEquals(0.5f, viewModel.opacityLevel.value)
    }

    @Test
    fun testOpacityLevelClamping() {
        viewModel.setOpacityLevel(1.5f)
        assertEquals(1.0f, viewModel.opacityLevel.value)

        viewModel.setOpacityLevel(-0.5f)
        assertEquals(0.0f, viewModel.opacityLevel.value)
    }

    @Test
    fun testSetPrivateBubblesVisibility() {
        viewModel.setPrivateBubblesVisibility(false)
        assertFalse(viewModel.showPrivateBubbles.value!!)

        viewModel.setPrivateBubblesVisibility(true)
        assertTrue(viewModel.showPrivateBubbles.value!!)
    }

    @Test
    fun testSetHistoryBubblesVisibility() {
        viewModel.setHistoryBubblesVisibility(false)
        assertFalse(viewModel.showHistoryBubbles.value!!)

        viewModel.setHistoryBubblesVisibility(true)
        assertTrue(viewModel.showHistoryBubbles.value!!)
    }

    @Test
    fun testSetOperationMode() {
        viewModel.setOperationMode(OperationMode.APPEND)
        assertEquals(OperationMode.APPEND, viewModel.currentOperationMode.value)

        viewModel.setOperationMode(OperationMode.PREPEND)
        assertEquals(OperationMode.PREPEND, viewModel.currentOperationMode.value)
    }

    @Test
    fun testCycleOperationMode() {
        // Start with OVERWRITE
        viewModel.setOperationMode(OperationMode.OVERWRITE)
        viewModel.cycleOperationMode()
        assertEquals(OperationMode.APPEND, viewModel.currentOperationMode.value)

        viewModel.cycleOperationMode()
        assertEquals(OperationMode.PREPEND, viewModel.currentOperationMode.value)

        viewModel.cycleOperationMode()
        assertEquals(OperationMode.OVERWRITE, viewModel.currentOperationMode.value)
    }

    @Test
    fun testClearAllBubbles() {
        viewModel.initializeDefaultToolBelt()
        assertTrue(viewModel.toolBubbles.value!!.isNotEmpty())

        viewModel.clearAllBubbles()
        assertTrue(viewModel.toolBubbles.value!!.isEmpty())
    }

    @Test
    fun testResetToDefaults() {
        // Modify state
        viewModel.setOpacityLevel(0.5f)
        viewModel.setOperationMode(OperationMode.APPEND)
        viewModel.clearAllBubbles()

        // Reset
        viewModel.resetToDefaults()

        // Verify reset
        assertEquals(1.0f, viewModel.opacityLevel.value)
        assertTrue(viewModel.showPrivateBubbles.value!!)
        assertTrue(viewModel.showHistoryBubbles.value!!)
        assertEquals(OperationMode.OVERWRITE, viewModel.currentOperationMode.value)
        assertTrue(viewModel.toolBubbles.value!!.isNotEmpty())
    }

    @Test
    fun testGetBubbleById() {
        val bubble = ToolBeltBubble(
            type = ToolBeltBubbleType.GENERIC,
            content = "Test",
        )
        viewModel.addBubble(bubble)

        val retrieved = viewModel.getBubbleById(bubble.id)
        assertNotNull(retrieved)
        assertEquals(bubble.id, retrieved?.id)
    }

    @Test
    fun testGetBubblesByType() {
        val bubble1 = ToolBeltBubble(type = ToolBeltBubbleType.GENERIC)
        val bubble2 = ToolBeltBubble(type = ToolBeltBubbleType.GENERIC)
        val bubble3 = ToolBeltBubble(type = ToolBeltBubbleType.SETTINGS)

        viewModel.addBubble(bubble1)
        viewModel.addBubble(bubble2)
        viewModel.addBubble(bubble3)

        val genericBubbles = viewModel.getBubblesByType(ToolBeltBubbleType.GENERIC)
        assertEquals(2, genericBubbles.size)

        val settingsBubbles = viewModel.getBubblesByType(ToolBeltBubbleType.SETTINGS)
        assertEquals(1, settingsBubbles.size)
    }

    @Test
    fun testMoveBubble() {
        val bubble1 = ToolBeltBubble(content = "Bubble 1")
        val bubble2 = ToolBeltBubble(content = "Bubble 2")
        val bubble3 = ToolBeltBubble(content = "Bubble 3")

        viewModel.addBubble(bubble1)
        viewModel.addBubble(bubble2)
        viewModel.addBubble(bubble3)

        viewModel.moveBubble(bubble1.id, 2)

        val bubbles = viewModel.toolBubbles.value!!
        assertEquals(bubble1.id, bubbles[2].id)
    }

    @Test
    fun testSetSelectedBubbleType() {
        viewModel.setSelectedBubbleType(BubbleType.HEXAGON)
        assertEquals(BubbleType.HEXAGON, viewModel.selectedBubbleType.value)
    }

    @Test
    fun testMultipleBubbleOperations() {
        val bubble1 = ToolBeltBubble(content = "Test 1")
        val bubble2 = ToolBeltBubble(content = "Test 2")

        viewModel.addBubble(bubble1)
        viewModel.addBubble(bubble2)
        assertEquals(2, viewModel.toolBubbles.value?.size)

        viewModel.changeBubbleContent(bubble1.id, "Modified")
        val modified = viewModel.getBubbleById(bubble1.id)
        assertEquals("Modified", modified?.content)

        viewModel.removeBubble(bubble2.id)
        assertEquals(1, viewModel.toolBubbles.value?.size)
    }
}
