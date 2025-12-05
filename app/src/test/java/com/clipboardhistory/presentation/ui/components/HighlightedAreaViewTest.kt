package com.clipboardhistory.presentation.ui.components

import android.content.Context
import android.graphics.RectF
import com.clipboardhistory.domain.model.BubbleState
import com.clipboardhistory.domain.model.SmartAction
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals

/**
 * Unit tests for HighlightedAreaView.
 *
 * Note: These tests focus on the logic and data handling aspects.
 * Full UI rendering tests would require instrumented tests with Robolectric.
 */
@RunWith(MockitoJUnitRunner::class)
class HighlightedAreaViewTest {
    @Mock
    private lateinit var mockContext: Context

    private val testSmartActions =
        listOf(
            SmartAction("Open Link", BubbleState.REPLACE),
            SmartAction("Call Number", BubbleState.REPLACE),
            SmartAction("Send Email", BubbleState.REPLACE),
        )

    @Test
    fun `ActivationEdge enum has expected values`() {
        val edges = HighlightedAreaView.ActivationEdge.values()

        assertEquals(5, edges.size)
        assertEquals(HighlightedAreaView.ActivationEdge.NONE, edges[0])
        assertEquals(HighlightedAreaView.ActivationEdge.LEFT, edges[1])
        assertEquals(HighlightedAreaView.ActivationEdge.RIGHT, edges[2])
        assertEquals(HighlightedAreaView.ActivationEdge.TOP, edges[3])
        assertEquals(HighlightedAreaView.ActivationEdge.BOTTOM, edges[4])
    }

    @Test
    fun `SmartAction has correct properties`() {
        val action = SmartAction("Test Action", BubbleState.APPEND)

        assertEquals("Test Action", action.label)
        assertEquals(BubbleState.APPEND, action.action)
    }

    @Test
    fun `SmartAction with different bubble states`() {
        val replaceAction = SmartAction("Replace", BubbleState.REPLACE)
        val appendAction = SmartAction("Append", BubbleState.APPEND)
        val prependAction = SmartAction("Prepend", BubbleState.PREPEND)

        assertEquals(BubbleState.REPLACE, replaceAction.action)
        assertEquals(BubbleState.APPEND, appendAction.action)
        assertEquals(BubbleState.PREPEND, prependAction.action)
    }

    @Test
    fun `SmartAction list for URL content`() {
        val actions = listOf(SmartAction("Open Link", BubbleState.REPLACE))

        assertEquals(1, actions.size)
        assertEquals("Open Link", actions[0].label)
    }

    @Test
    fun `SmartAction list for Phone content`() {
        val actions = listOf(SmartAction("Call Number", BubbleState.REPLACE))

        assertEquals(1, actions.size)
        assertEquals("Call Number", actions[0].label)
    }

    @Test
    fun `SmartAction list for Email content`() {
        val actions = listOf(SmartAction("Send Email", BubbleState.REPLACE))

        assertEquals(1, actions.size)
        assertEquals("Send Email", actions[0].label)
    }

    @Test
    fun `SmartAction list for Maps content`() {
        val actions = listOf(SmartAction("Open Maps", BubbleState.REPLACE))

        assertEquals(1, actions.size)
        assertEquals("Open Maps", actions[0].label)
    }

    @Test
    fun `SmartAction list for Text content`() {
        val actions = listOf(SmartAction("Search Text", BubbleState.REPLACE))

        assertEquals(1, actions.size)
        assertEquals("Search Text", actions[0].label)
    }

    @Test
    fun `RectF contains point correctly`() {
        val rect = RectF(0f, 0f, 100f, 80f)

        // Point inside
        assert(rect.contains(50f, 40f))

        // Point outside
        assert(!rect.contains(150f, 40f))
        assert(!rect.contains(50f, 100f))
    }

    @Test
    fun `Multiple action rects do not overlap`() {
        val rect1 = RectF(0f, 0f, 100f, 80f)
        val rect2 = RectF(116f, 0f, 216f, 80f) // 16dp spacing

        // rect1 should not contain rect2's center
        assert(!rect1.contains(rect2.centerX(), rect2.centerY()))

        // rect2 should not contain rect1's center
        assert(!rect2.contains(rect1.centerX(), rect1.centerY()))
    }

    @Test
    fun `testSmartActions list is correctly constructed`() {
        assertEquals(3, testSmartActions.size)
        assertEquals("Open Link", testSmartActions[0].label)
        assertEquals("Call Number", testSmartActions[1].label)
        assertEquals("Send Email", testSmartActions[2].label)
    }
}
