package com.clipboardhistory.presentation.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.clipboardhistory.domain.model.BubbleState

/**
 * Minimal stub of HighlightedAreaView to satisfy runtime and compile-time references.
 * Provides basic API used by FloatingBubbleService without visual implementation.
 */
class HighlightedAreaView @JvmOverloads constructor(
    context: Context,
    private val themeName: String,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    enum class ActivationEdge { NONE, LEFT, RIGHT, TOP, BOTTOM }

    fun show() {
        // no-op for stub
        visibility = VISIBLE
    }

    fun showWithSmartActions(actions: List<com.clipboardhistory.domain.model.SmartAction>, edge: ActivationEdge) {
        // no-op for stub; could render actions in a real implementation
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }

    fun updateEdgeGlow(alpha: Float) {
        // no-op for stub
    }

    fun getActionForPosition(x: Float, y: Float): BubbleState? {
        // Return null by default; a real implementation would map positions to actions
        return null
    }

    fun getSmartActionForPosition(x: Float, y: Float): com.clipboardhistory.domain.model.SmartAction? {
        // No smart action in stub
        return null
    }
}
