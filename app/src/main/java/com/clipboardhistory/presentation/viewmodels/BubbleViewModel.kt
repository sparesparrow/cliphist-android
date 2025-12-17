package com.clipboardhistory.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipboardhistory.presentation.ui.bubble.*
import com.clipboardhistory.utils.KeyboardVisibilityDetector
import com.clipboardhistory.utils.SmartInputManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing bubble state and interactions.
 * Coordinates between keyboard visibility, bubble orchestration, and user interactions.
 */
class BubbleViewModel(
    private val keyboardDetector: KeyboardVisibilityDetector,
    private val smartInputManager: SmartInputManager? = null
) : ViewModel() {

    private val _orchestrator = BubbleOrchestrator(keyboardDetector, smartInputManager)
    val orchestrator: BubbleOrchestrator = _orchestrator

    // Exposed flows for UI consumption
    val bubbles = _orchestrator.bubbles
    val keyboardVisible = _orchestrator.keyboardVisible
    val visibleBubbles = _orchestrator.bubbles.map { bubbles ->
        bubbles.filter { it.isVisible }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Initialize keyboard monitoring
        keyboardDetector.startMonitoring()
    }

    override fun onCleared() {
        super.onCleared()
        keyboardDetector.stopMonitoring()
    }

    // Bubble management methods

    /**
     * Adds a text paste bubble with clipboard content.
     */
    fun addTextPasteBubble(
        content: String,
        contentType: BubbleSpec.TextPasteBubble.ContentType = BubbleSpec.TextPasteBubble.ContentType.TEXT,
        isFavorite: Boolean = false
    ) {
        val bubble = BubbleSpec.TextPasteBubble(
            textContent = content,
            contentType = contentType,
            isFavorite = isFavorite
        )
        _orchestrator.addBubble(bubble)
    }

    /**
     * Adds a toolbelt bubble with available tools.
     */
    fun addToolbeltBubble(tools: List<ToolbeltTool>) {
        val bubble = BubbleSpec.ToolbeltBubble(tools = tools)
        _orchestrator.addBubble(bubble)
    }

    /**
     * Adds a pinned item bubble that stays visible.
     */
    fun addPinnedItemBubble(content: String) {
        val bubble = BubbleSpec.TextPasteBubble(
            type = BubbleType.PINNED_ITEM,
            textContent = content,
            isFavorite = true
        )
        _orchestrator.addBubble(bubble)
    }

    /**
     * Adds a system notification bubble.
     */
    fun addSystemNotificationBubble(
        notificationId: Int,
        title: String,
        content: String,
        iconResId: Int
    ) {
        val bubble = BubbleSpec.SystemBubble(
            notificationId = notificationId,
            title = title,
            content = content,
            iconResId = iconResId
        )
        _orchestrator.addBubble(bubble)
    }

    /**
     * Adds a quick action bubble for temporary actions.
     */
    fun addQuickActionBubble(content: String) {
        val bubble = BubbleSpec.TextPasteBubble(
            type = BubbleType.QUICK_ACTION,
            textContent = content
        )
        _orchestrator.addBubble(bubble)
    }

    /**
     * Creates default toolbelt tools.
     */
    fun createDefaultToolbeltTools(): List<ToolbeltTool> {
        return listOf(
            ToolbeltTool(
                id = "opacity",
                name = "Opacity",
                icon = 1, // Placeholder - would be actual icon resource
                action = { /* Toggle opacity slider */ },
                priority = 1
            ),
            ToolbeltTool(
                id = "private_toggle",
                name = "Private",
                icon = 2,
                action = { /* Toggle private bubbles */ },
                priority = 2
            ),
            ToolbeltTool(
                id = "history_toggle",
                name = "History",
                icon = 3,
                action = { /* Toggle history bubbles */ },
                priority = 3
            ),
            ToolbeltTool(
                id = "type_changer",
                name = "Type",
                icon = 4,
                action = { /* Change bubble type */ },
                priority = 4
            ),
            ToolbeltTool(
                id = "content_editor",
                name = "Edit",
                icon = 5,
                action = { /* Open content editor */ },
                priority = 5
            ),
            ToolbeltTool(
                id = "operation_mode",
                name = "Mode",
                icon = 6,
                action = { /* Change operation mode */ },
                priority = 6
            )
        )
    }

    /**
     * Removes a bubble by ID.
     */
    fun removeBubble(bubbleId: String) {
        _orchestrator.removeBubble(bubbleId)
    }

    /**
     * Updates bubble position.
     */
    fun updateBubblePosition(bubbleId: String, position: Offset) {
        _orchestrator.updateBubblePosition(bubbleId, position)
    }

    /**
     * Toggles bubble minimization.
     */
    fun toggleBubbleMinimized(bubbleId: String) {
        _orchestrator.toggleBubbleMinimized(bubbleId)
    }

    /**
     * Updates a bubble.
     */
    fun updateBubble(bubble: BubbleSpec) {
        _orchestrator.updateBubble(bubble)
    }

    /**
     * Gets bubbles of a specific type.
     */
    fun getBubblesByType(type: BubbleType): List<BubbleSpec> {
        return bubbles.value.filter { it.type == type }
    }

    /**
     * Clears all bubbles.
     */
    fun clearAllBubbles() {
        _orchestrator.clearAllBubbles()
    }

    /**
     * Gets the current keyboard state.
     */
    fun getKeyboardState() = keyboardDetector.getCurrentKeyboardState()
}