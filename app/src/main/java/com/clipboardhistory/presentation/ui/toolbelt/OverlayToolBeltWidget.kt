package com.clipboardhistory.presentation.ui.toolbelt

import android.content.Context
import android.view.WindowManager

/**
 * Container widget managing all floating toolbelt bubbles
 * Implements Issue #12 - ToolBelt overlay widget
 */
class OverlayToolBeltWidget(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private val toolBubbles = mutableListOf<OverlayToolBeltBubble>()
    private var isVisible = false
    private var baseX = 50
    private var baseY = 150

    init {
        // Initialize default tool bubbles
        OverlayToolBeltBubble.ToolType.values().forEach { type ->
            toolBubbles.add(OverlayToolBeltBubble(context, windowManager, type))
        }

        // Load saved state
        BubbleStateManager.loadFromPreferences(context)
    }

    fun show() {
        if (isVisible) return
        isVisible = true

        // Show all tool bubbles in a horizontal belt formation
        toolBubbles.forEachIndexed { index, bubble ->
            bubble.show()
            // Position bubbles in a horizontal belt
            positionBubble(bubble, index)
        }
    }

    fun hide() {
        if (!isVisible) return
        isVisible = false

        toolBubbles.forEach { it.hide() }
    }

    fun toggle() {
        if (isVisible) hide() else show()
    }

    fun updateBasePosition(x: Int, y: Int) {
        baseX = x
        baseY = y
        if (isVisible) {
            toolBubbles.forEachIndexed { index, bubble ->
                positionBubble(bubble, index)
            }
        }
    }

    private fun positionBubble(bubble: OverlayToolBeltBubble, index: Int) {
        // Position bubbles horizontally at the base position
        // Each bubble offset by 70dp (converted to pixels)
        val offsetX = baseX + (index * 70 * context.resources.displayMetrics.density).toInt()
        bubble.updatePosition(offsetX, baseY)
    }

    fun addCustomTool(toolType: OverlayToolBeltBubble.ToolType) {
        val bubble = OverlayToolBeltBubble(context, windowManager, toolType)
        toolBubbles.add(bubble)
        if (isVisible) {
            bubble.show()
            positionBubble(bubble, toolBubbles.size - 1)
        }
    }

    fun destroy() {
        toolBubbles.forEach { it.destroy() }
        toolBubbles.clear()
    }

    fun isVisible(): Boolean = isVisible
}