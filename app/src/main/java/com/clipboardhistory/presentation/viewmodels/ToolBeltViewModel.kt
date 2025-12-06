package com.clipboardhistory.presentation.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.clipboardhistory.domain.model.BubbleType
import com.clipboardhistory.domain.model.OperationMode
import com.clipboardhistory.domain.model.ToolBeltBubble
import com.clipboardhistory.domain.model.ToolBeltBubbleType

/**
 * ViewModel for managing ToolBelt state and operations.
 *
 * Responsibilities:
 * - Manage ToolBelt bubbles (add, remove, update)
 * - Track visibility states (private bubbles, history bubbles)
 * - Manage opacity settings
 * - Handle operation mode changes
 * - Persist ToolBelt configuration
 */
class ToolBeltViewModel : ViewModel() {

    private val _toolBubbles = MutableLiveData<List<ToolBeltBubble>>(emptyList())
    val toolBubbles: LiveData<List<ToolBeltBubble>> = _toolBubbles

    private val _opacityLevel = MutableLiveData<Float>(1.0f)
    val opacityLevel: LiveData<Float> = _opacityLevel

    private val _showPrivateBubbles = MutableLiveData<Boolean>(true)
    val showPrivateBubbles: LiveData<Boolean> = _showPrivateBubbles

    private val _showHistoryBubbles = MutableLiveData<Boolean>(true)
    val showHistoryBubbles: LiveData<Boolean> = _showHistoryBubbles

    private val _currentOperationMode = MutableLiveData<OperationMode>(OperationMode.OVERWRITE)
    val currentOperationMode: LiveData<OperationMode> = _currentOperationMode

    private val _selectedBubbleType = MutableLiveData<BubbleType>(BubbleType.CIRCLE)
    val selectedBubbleType: LiveData<BubbleType> = _selectedBubbleType

    private val _toolBeltEvent = MutableLiveData<ToolBeltEvent>()
    val toolBeltEvent: LiveData<ToolBeltEvent> = _toolBeltEvent

    /**
     * Initializes the ToolBelt with default bubbles.
     */
    fun initializeDefaultToolBelt() {
        val defaultBubbles =
            listOf(
                ToolBeltBubble(
                    type = ToolBeltBubbleType.OPACITY_SLIDER,
                    content = "Opacity",
                    bubbleType = BubbleType.CIRCLE,
                    position = 0,
                ),
                ToolBeltBubble(
                    type = ToolBeltBubbleType.PRIVATE_VISIBILITY_TOGGLE,
                    content = "Private",
                    bubbleType = BubbleType.SQUARE,
                    position = 1,
                ),
                ToolBeltBubble(
                    type = ToolBeltBubbleType.HISTORY_VISIBILITY_TOGGLE,
                    content = "History",
                    bubbleType = BubbleType.SQUARE,
                    position = 2,
                ),
                ToolBeltBubble(
                    type = ToolBeltBubbleType.BUBBLE_TYPE_CHANGER,
                    content = "Type",
                    bubbleType = BubbleType.HEXAGON,
                    position = 3,
                ),
                ToolBeltBubble(
                    type = ToolBeltBubbleType.OPERATION_MODE_SWITCHER,
                    content = "Mode",
                    bubbleType = BubbleType.CUBE,
                    position = 4,
                ),
                ToolBeltBubble(
                    type = ToolBeltBubbleType.SETTINGS,
                    content = "Settings",
                    bubbleType = BubbleType.CIRCLE,
                    position = 5,
                ),
            )
        _toolBubbles.value = defaultBubbles
        _toolBeltEvent.value = ToolBeltEvent.ToolBeltInitialized(defaultBubbles)
    }

    /**
     * Adds a new bubble to the ToolBelt.
     *
     * @param bubble The bubble to add
     */
    fun addBubble(bubble: ToolBeltBubble) {
        val currentBubbles = _toolBubbles.value ?: emptyList()
        val newBubbles = currentBubbles.toMutableList().apply { add(bubble) }
        _toolBubbles.value = newBubbles
        _toolBeltEvent.value = ToolBeltEvent.BubbleAdded(bubble)
    }

    /**
     * Removes a bubble from the ToolBelt.
     *
     * @param bubbleId The ID of the bubble to remove
     */
    fun removeBubble(bubbleId: String) {
        val currentBubbles = _toolBubbles.value ?: emptyList()
        val updatedBubbles = currentBubbles.filterNot { it.id == bubbleId }
        _toolBubbles.value = updatedBubbles
        _toolBeltEvent.value = ToolBeltEvent.BubbleRemoved(bubbleId)
    }

    /**
     * Updates an existing bubble in the ToolBelt.
     *
     * @param bubble The updated bubble
     */
    fun updateBubble(bubble: ToolBeltBubble) {
        val currentBubbles = _toolBubbles.value ?: emptyList()
        val updatedBubbles =
            currentBubbles.map { if (it.id == bubble.id) bubble else it }
        _toolBubbles.value = updatedBubbles
        _toolBeltEvent.value = ToolBeltEvent.BubbleUpdated(bubble)
    }

    /**
     * Changes the content of a bubble.
     *
     * @param bubbleId The ID of the bubble
     * @param newContent The new content
     */
    fun changeBubbleContent(bubbleId: String, newContent: String) {
        val currentBubbles = _toolBubbles.value ?: emptyList()
        val bubble = currentBubbles.find { it.id == bubbleId }
        if (bubble != null) {
            val updated = bubble.copy(content = newContent)
            updateBubble(updated)
        }
    }

    /**
     * Changes the type of a bubble.
     *
     * @param bubbleId The ID of the bubble
     * @param newType The new bubble type (shape)
     */
    fun changeBubbleType(bubbleId: String, newType: BubbleType) {
        val currentBubbles = _toolBubbles.value ?: emptyList()
        val bubble = currentBubbles.find { it.id == bubbleId }
        if (bubble != null) {
            val updated = bubble.copy(bubbleType = newType)
            updateBubble(updated)
        }
    }

    /**
     * Sets the opacity level.
     *
     * @param opacity The opacity level (0.0 to 1.0)
     */
    fun setOpacityLevel(opacity: Float) {
        val clampedOpacity = opacity.coerceIn(0f, 1f)
        _opacityLevel.value = clampedOpacity
        _toolBeltEvent.value = ToolBeltEvent.OpacityChanged(clampedOpacity)
    }

    /**
     * Sets the visibility of private bubbles.
     *
     * @param isVisible Whether private bubbles should be visible
     */
    fun setPrivateBubblesVisibility(isVisible: Boolean) {
        _showPrivateBubbles.value = isVisible
        _toolBeltEvent.value = ToolBeltEvent.PrivateBubblesToggled(isVisible)
    }

    /**
     * Sets the visibility of history bubbles.
     *
     * @param isVisible Whether history bubbles should be visible
     */
    fun setHistoryBubblesVisibility(isVisible: Boolean) {
        _showHistoryBubbles.value = isVisible
        _toolBeltEvent.value = ToolBeltEvent.HistoryBubblesToggled(isVisible)
    }

    /**
     * Changes the operation mode.
     *
     * @param mode The new operation mode
     */
    fun setOperationMode(mode: OperationMode) {
        _currentOperationMode.value = mode
        _toolBeltEvent.value = ToolBeltEvent.OperationModeChanged(mode)
    }

    /**
     * Cycles through operation modes.
     */
    fun cycleOperationMode() {
        val current = _currentOperationMode.value ?: OperationMode.OVERWRITE
        val next =
            when (current) {
                OperationMode.OVERWRITE -> OperationMode.APPEND
                OperationMode.APPEND -> OperationMode.PREPEND
                OperationMode.PREPEND -> OperationMode.OVERWRITE
            }
        setOperationMode(next)
    }

    /**
     * Sets the selected bubble type for creating new bubbles.
     *
     * @param type The bubble type
     */
    fun setSelectedBubbleType(type: BubbleType) {
        _selectedBubbleType.value = type
    }

    /**
     * Clears all bubbles from the ToolBelt.
     */
    fun clearAllBubbles() {
        _toolBubbles.value = emptyList()
        _toolBeltEvent.value = ToolBeltEvent.AllBubblesCleared
    }

    /**
     * Resets the ToolBelt to its default state.
     */
    fun resetToDefaults() {
        _opacityLevel.value = 1.0f
        _showPrivateBubbles.value = true
        _showHistoryBubbles.value = true
        _currentOperationMode.value = OperationMode.OVERWRITE
        initializeDefaultToolBelt()
    }

    /**
     * Gets a bubble by its ID.
     *
     * @param bubbleId The bubble ID
     * @return The bubble or null if not found
     */
    fun getBubbleById(bubbleId: String): ToolBeltBubble? =
        _toolBubbles.value?.find { it.id == bubbleId }

    /**
     * Gets bubbles of a specific type.
     *
     * @param type The ToolBelt bubble type
     * @return List of matching bubbles
     */
    fun getBubblesByType(type: ToolBeltBubbleType): List<ToolBeltBubble> =
        _toolBubbles.value?.filter { it.type == type } ?: emptyList()

    /**
     * Moves a bubble to a new position.
     *
     * @param bubbleId The ID of the bubble to move
     * @param newPosition The new position (0-indexed)
     */
    fun moveBubble(bubbleId: String, newPosition: Int) {
        val currentBubbles = _toolBubbles.value?.toMutableList() ?: return
        val bubble = currentBubbles.find { it.id == bubbleId } ?: return

        val validPosition = newPosition.coerceIn(0, currentBubbles.size - 1)
        currentBubbles.remove(bubble)
        currentBubbles.add(validPosition, bubble.copy(position = validPosition))

        _toolBubbles.value = currentBubbles
        _toolBeltEvent.value = ToolBeltEvent.BubbleMoved(bubbleId, validPosition)
    }
}

/**
 * Sealed class representing various ToolBelt events.
 */
sealed class ToolBeltEvent {
    data class ToolBeltInitialized(val bubbles: List<ToolBeltBubble>) : ToolBeltEvent()

    data class BubbleAdded(val bubble: ToolBeltBubble) : ToolBeltEvent()

    data class BubbleRemoved(val bubbleId: String) : ToolBeltEvent()

    data class BubbleUpdated(val bubble: ToolBeltBubble) : ToolBeltEvent()

    data class BubbleMoved(val bubbleId: String, val newPosition: Int) : ToolBeltEvent()

    data class OpacityChanged(val opacity: Float) : ToolBeltEvent()

    data class PrivateBubblesToggled(val isVisible: Boolean) : ToolBeltEvent()

    data class HistoryBubblesToggled(val isVisible: Boolean) : ToolBeltEvent()

    data class OperationModeChanged(val mode: OperationMode) : ToolBeltEvent()

    object AllBubblesCleared : ToolBeltEvent()
}
