package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipboardhistory.utils.KeyboardVisibilityDetector
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Orchestrates the display and management of different bubble types.
 * Handles keyboard-aware positioning, collision avoidance, and lifecycle management.
 */
class BubbleOrchestrator(
    private val keyboardDetector: KeyboardVisibilityDetector,
    private val smartInputManager: com.clipboardhistory.utils.SmartInputManager? = null
) : ViewModel() {

    private val _bubbles = MutableStateFlow<List<BubbleSpec>>(emptyList())
    val bubbles: StateFlow<List<BubbleSpec>> = _bubbles

    private val _keyboardVisible = MutableStateFlow(false)
    val keyboardVisible: StateFlow<Boolean> = _keyboardVisible

    init {
        // Observe keyboard visibility changes
        viewModelScope.launch {
            keyboardDetector.keyboardState.collect { state ->
                _keyboardVisible.value = state.isVisible
                updateBubblePositionsForKeyboard(state.isVisible, state.height)
            }
        }
    }

    /**
     * Adds a bubble to the orchestrator.
     */
    fun addBubble(bubble: BubbleSpec) {
        _bubbles.value = _bubbles.value + bubble
    }

    /**
     * Removes a bubble by ID.
     */
    fun removeBubble(bubbleId: String) {
        _bubbles.value = _bubbles.value.filter { it.id != bubbleId }
    }

    /**
     * Updates bubble position.
     */
    fun updateBubblePosition(bubbleId: String, position: Offset) {
        _bubbles.value = _bubbles.value.map { bubble ->
            if (bubble.id == bubbleId) {
                bubble.withPosition(position)
            } else {
                bubble
            }
        }
    }

    /**
     * Toggles bubble minimization.
     */
    fun toggleBubbleMinimized(bubbleId: String) {
        _bubbles.value = _bubbles.value.map { bubble ->
            if (bubble.id == bubbleId) {
                bubble.withMinimized(!bubble.isMinimized)
            } else {
                bubble
            }
        }
    }

    /**
     * Updates a bubble.
     */
    fun updateBubble(updatedBubble: BubbleSpec) {
        _bubbles.value = _bubbles.value.map { bubble ->
            if (bubble.id == updatedBubble.id) updatedBubble else bubble
        }
    }

    /**
     * Clears all bubbles of a specific type.
     */
    fun clearBubblesByType(type: BubbleType) {
        _bubbles.value = _bubbles.value.filter { it.type != type }
    }

    /**
     * Clears all bubbles.
     */
    fun clearAllBubbles() {
        _bubbles.value = emptyList()
    }

    /**
     * Updates bubble positions when keyboard visibility changes.
     */
    private fun updateBubblePositionsForKeyboard(isVisible: Boolean, keyboardHeight: Int) {
        _bubbles.value = _bubbles.value.map { bubble ->
            bubble.withKeyboardState(isVisible)
        }
    }

    // Private helper methods

    /**
     * Calculates collision-free positions for bubbles.
     */
    private fun calculateCollisionFreePosition(
        bubbles: List<BubbleSpec>,
        newBubble: BubbleSpec,
        screenSize: IntSize
    ): Offset {
        // Simple collision avoidance - place bubbles in a grid pattern
        val existingPositions = bubbles.map { it.position }
        val bubbleSize = 120f // Approximate bubble size

        for (row in 0..10) {
            for (col in 0..5) {
                val candidatePosition = Offset(
                    x = col * (bubbleSize + 16f) + 16f,
                    y = screenSize.height - (row + 1) * (bubbleSize + 16f) - 16f
                )

                // Check if this position conflicts with existing bubbles
                val hasConflict = existingPositions.any { existingPos ->
                    val distance = (candidatePosition - existingPos).getDistance()
                    distance < bubbleSize
                }

                if (!hasConflict) {
                    return candidatePosition
                }
            }
        }

        // Fallback position
        return Offset(16f, screenSize.height - bubbleSize - 16f)
    }

    /**
     * Gets the screen size for positioning calculations.
     */
    private fun getScreenSize(): IntSize {
        // This would be provided by the UI layer
        return IntSize(1080, 1920) // Default HD resolution
    }
}