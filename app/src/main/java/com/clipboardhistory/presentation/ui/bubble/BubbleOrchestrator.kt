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
import com.clipboardhistory.utils.TextSelectionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Orchestrates the display and management of different bubble types.
 * Handles keyboard-aware positioning, collision avoidance, and lifecycle management.
 */
class BubbleOrchestrator(
    private val keyboardDetector: KeyboardVisibilityDetector,
    private val smartInputManager: com.clipboardhistory.utils.SmartInputManager? = null,
    private val textSelectionManager: TextSelectionManager? = null
) : ViewModel() {

    private val _bubbles = MutableStateFlow<List<BubbleSpec>>(emptyList())
    val bubbles: StateFlow<List<BubbleSpec>> = _bubbles

    private val _containerSize = MutableStateFlow(IntSize.Zero)
    val containerSize: StateFlow<IntSize> = _containerSize

    private val _keyboardVisible = MutableStateFlow(false)
    val keyboardVisible: StateFlow<Boolean> = _keyboardVisible

    // Bubble cut menu management
    val bubbleCutMenuManager = BubbleCutMenuManager(
        textSelectionManager ?: TextSelectionManager(
            keyboardDetector.getApplicationContext() ?: android.app.Application(),
            null
        )
    ) { selectedText ->
        // Handle bubble cut action
        performBubbleCut(selectedText)
    }

    init {
        // Observe keyboard visibility changes
        viewModelScope.launch {
            keyboardDetector.isKeyboardVisible.collect { isVisible ->
                _keyboardVisible.value = isVisible
                updateBubblesForKeyboardState(isVisible)
            }
        }

        // Auto-hide bubbles based on their policies
        startAutoHideTimer()
    }

    /**
     * Adds a new bubble to the orchestrator.
     */
    fun addBubble(bubble: BubbleSpec) {
        _bubbles.update { currentBubbles ->
            // Check type limits
            val typeCount = currentBubbles.count { it.type == bubble.type }
            if (typeCount >= bubble.type.maxInstances) {
                // Remove oldest bubble of this type
                val filtered = currentBubbles.filter { it.type != bubble.type }.dropLast(1)
                filtered + bubble
            } else {
                currentBubbles + bubble
            }
        }

        // Position the new bubble to avoid collisions
        repositionBubble(bubble.id)
    }

    /**
     * Removes a bubble from the orchestrator.
     */
    fun removeBubble(bubbleId: String) {
        _bubbles.update { bubbles ->
            bubbles.filter { it.id != bubbleId }
        }
    }

    /**
     * Updates the position of a bubble.
     */
    fun updateBubblePosition(bubbleId: String, position: Offset) {
        _bubbles.update { bubbles ->
            bubbles.map { bubble ->
                if (bubble.id == bubbleId) {
                    bubble.withPosition(position).withInteraction()
                } else {
                    bubble
                }
            }
        }
    }

    /**
     * Handles bubble interaction (tap/click).
     */
    fun onBubbleInteraction(bubbleId: String) {
        val bubble = _bubbles.value.find { it.id == bubbleId } ?: return

        when (bubble) {
            is BubbleSpec.TextPasteBubble -> {
                handleTextPasteBubbleInteraction(bubble)
            }
            is BubbleSpec.ToolbeltBubble -> {
                handleToolbeltBubbleInteraction(bubble)
            }
            is BubbleSpec.OverlayBubble -> {
                handleGenericBubbleInteraction(bubble)
            }
            is BubbleSpec.SystemBubble -> {
                handleSystemBubbleInteraction(bubble)
            }
        }

        // Update interaction time
        _bubbles.update { bubbles ->
            bubbles.map {
                if (it.id == bubbleId) it.withInteraction() else it
            }
        }
    }

    /**
     * Toggles minimization state of a bubble.
     */
    fun toggleBubbleMinimized(bubbleId: String) {
        _bubbles.update { bubbles ->
            bubbles.map { bubble ->
                if (bubble.id == bubbleId) {
                    when (bubble) {
                        is BubbleSpec.ToolbeltBubble -> {
                            bubble.withMinimized(!bubble.isMinimized)
                        }
                        else -> bubble
                    }
                } else {
                    bubble
                }
            }
        }
    }

    /**
     * Updates container size (screen size or overlay area).
     */
    fun updateContainerSize(size: IntSize) {
        _containerSize.value = size
        repositionAllBubbles()
    }

    /**
     * Updates multiple bubbles at once (used for batch operations like regex accumulation).
     */
    fun updateBubbles(newBubbles: List<BubbleSpec>) {
        _bubbles.value = newBubbles
    }

    /**
     * Clears all bubbles of a specific type.
     */
    fun clearBubblesByType(type: BubbleType) {
        _bubbles.update { bubbles ->
            bubbles.filter { it.type != type }
        }
    }

    /**
     * Clears all bubbles.
     */
    fun clearAllBubbles() {
        _bubbles.value = emptyList()
    }

    /**
     * Gets bubbles that should be visible based on current state.
     */
    fun getVisibleBubbles(): List<BubbleSpec> {
        return _bubbles.value.filter { bubble ->
            bubble.isVisible && bubble.type.shouldBeVisible(_keyboardVisible.value)
        }
    }

    // Bubble cut functionality

    /**
     * Shows the bubble cut menu when text is selected.
     */
    fun showBubbleCutMenu(position: androidx.compose.ui.geometry.Offset) {
        val selectedText = bubbleCutMenuManager.getSelectedTextForMenu()
        if (selectedText.isNotEmpty()) {
            bubbleCutMenuManager.showBubbleCutMenu(selectedText, position)
        }
    }

    /**
     * Hides the bubble cut menu.
     */
    fun hideBubbleCutMenu() {
        bubbleCutMenuManager.hideBubbleCutMenu()
    }

    /**
     * Performs the bubble cut operation.
     */
    private fun performBubbleCut(text: String) {
        // Create appropriate bubble based on content type
        val bubble = createBubbleForText(text)
        addBubble(bubble)

        // Hide the bubble cut menu
        hideBubbleCutMenu()
    }

    /**
     * Creates the appropriate bubble type for the given text.
     */
    private fun createBubbleForText(text: String): BubbleSpec {
        // First check if any regex accumulators want this text
        processClipboardContentForRegexAccumulators(text, "bubble_cut")

        // Then create a text paste bubble as fallback
        return BubbleSpec.TextPasteBubble(
            textContent = text,
            contentType = detectContentType(text)
        )
    }

    /**
     * Detects content type for automatic bubble creation.
     */
    private fun detectContentType(text: String): BubbleSpec.TextPasteBubble.ContentType {
        return when {
            text.matches(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b")) -> {
                BubbleSpec.TextPasteBubble.ContentType.EMAIL
            }
            text.matches(Regex("https?://\\S+")) -> {
                BubbleSpec.TextPasteBubble.ContentType.URL
            }
            text.matches(Regex("\\b\\d{3}-\\d{3}-\\d{4}\\b")) -> {
                BubbleSpec.TextPasteBubble.ContentType.PHONE_NUMBER
            }
            text.matches(Regex("\\d+(\\.\\d+)?")) -> {
                BubbleSpec.TextPasteBubble.ContentType.NUMBER
            }
            text.contains("{") && text.contains("}") -> {
                BubbleSpec.TextPasteBubble.ContentType.JSON
            }
            text.contains("<") && text.contains(">") -> {
                BubbleSpec.TextPasteBubble.ContentType.XML
            }
            text.contains("function") || text.contains("def ") || text.contains("class ") -> {
                BubbleSpec.TextPasteBubble.ContentType.CODE
            }
            else -> BubbleSpec.TextPasteBubble.ContentType.TEXT
        }
    }

    // Private helper methods

    private fun updateBubblesForKeyboardState(isKeyboardVisible: Boolean) {
        _bubbles.update { bubbles ->
            bubbles.map { it.withKeyboardState(isKeyboardVisible) }
        }
        repositionAllBubbles()
    }

    private fun repositionBubble(bubbleId: String) {
        val bubbles = _bubbles.value
        val targetBubble = bubbles.find { it.id == bubbleId } ?: return
        val otherBubbles = bubbles.filter { it.id != bubbleId }

        val newPosition = calculateNonOverlappingPosition(targetBubble, otherBubbles)
        updateBubblePosition(bubbleId, newPosition)
    }

    private fun repositionAllBubbles() {
        _bubbles.update { bubbles ->
            bubbles.mapIndexed { index, bubble ->
                val otherBubbles = bubbles.filterIndexed { i, _ -> i != index }
                val newPosition = calculateNonOverlappingPosition(bubble, otherBubbles)
                bubble.withPosition(newPosition)
            }
        }
    }

    private fun calculateNonOverlappingPosition(
        bubble: BubbleSpec,
        otherBubbles: List<BubbleSpec>
    ): Offset {
        val containerSize = _containerSize.value
        if (containerSize == IntSize.Zero) return bubble.position

        val bubbleSizePx = bubble.size.value * 2 // Rough conversion to pixels
        var position = bubble.position

        // Simple collision avoidance - move bubble if it overlaps with others
        val maxAttempts = 10
        var attempts = 0

        while (attempts < maxAttempts) {
            var hasCollision = false

            for (other in otherBubbles) {
                val distance = calculateDistance(position, other.position)
                val minDistance = (bubbleSizePx + other.size.value * 2) * 0.8f // 80% of combined size

                if (distance < minDistance) {
                    hasCollision = true

                    // Move in a direction away from the collision
                    val dx = position.x - other.position.x
                    val dy = position.y - other.position.y
                    val length = kotlin.math.sqrt(dx * dx + dy * dy)

                    if (length > 0) {
                        val moveX = (dx / length) * minDistance * 0.6f
                        val moveY = (dy / length) * minDistance * 0.6f

                        position = Offset(
                            x = (position.x + moveX).coerceIn(
                                bubbleSizePx,
                                containerSize.width - bubbleSizePx
                            ),
                            y = (position.y + moveY).coerceIn(
                                bubbleSizePx,
                                containerSize.height - bubbleSizePx
                            )
                        )
                    }
                    break
                }
            }

            if (!hasCollision) break
            attempts++
        }

        return position
    }

    private fun calculateDistance(pos1: Offset, pos2: Offset): Float {
        val dx = pos1.x - pos2.x
        val dy = pos1.y - pos2.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun startAutoHideTimer() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000) // Check every 5 seconds

                val currentTime = System.currentTimeMillis()
                _bubbles.update { bubbles ->
                    bubbles.filter { bubble ->
                        val autoHideDelay = bubble.type.autoHideDelay
                        if (autoHideDelay > 0) {
                            currentTime - bubble.lastInteractionTime < autoHideDelay
                        } else {
                            true // Never auto-hide
                        }
                    }
                }
            }
        }
    }

    private fun handleTextPasteBubbleInteraction(bubble: BubbleSpec.TextPasteBubble) {
        smartInputManager?.pasteText(bubble.textContent) ?: run {
            // Fallback: copy to clipboard
            copyToClipboard(bubble.textContent)
        }
        removeBubble(bubble.id)
    }

    private fun handleToolbeltBubbleInteraction(bubble: BubbleSpec.ToolbeltBubble) {
        toggleBubbleMinimized(bubble.id)
    }

    private fun handleGenericBubbleInteraction(bubble: BubbleSpec.OverlayBubble) {
        // Generic bubble interaction - could be customized per bubble
        removeBubble(bubble.id)
    }

    private fun handleSystemBubbleInteraction(bubble: BubbleSpec.SystemBubble) {
        // System bubbles typically launch their associated activity
        // This would be handled by the system notification manager
    }

    private fun copyToClipboard(text: String) {
        // This would use the Android clipboard manager
        // Implementation would go here
    }
}

/**
 * Composable wrapper for the BubbleOrchestrator.
 */
@Composable
fun BubbleOrchestrator(
    orchestrator: BubbleOrchestrator,
    modifier: Modifier = Modifier
) {
    val bubbles by orchestrator.bubbles.collectAsState()
    val visibleBubbles = orchestrator.getVisibleBubbles()

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                orchestrator.updateContainerSize(size)
            }
    ) {
        // Render regular bubbles
        visibleBubbles.forEach { bubble ->
            key(bubble.id) {
                BubbleContainer(
                    bubble = bubble,
                    onInteraction = { orchestrator.onBubbleInteraction(bubble.id) },
                    onPositionChange = { newPosition ->
                        orchestrator.updateBubblePosition(bubble.id, newPosition)
                    },
                    onDismiss = { orchestrator.removeBubble(bubble.id) }
                )
            }
        }

        // Render bubble cut menu if visible
        BubbleCutMenu(
            position = orchestrator.bubbleCutMenuManager.position,
            selectedText = orchestrator.bubbleCutMenuManager.selectedText,
            isVisible = orchestrator.bubbleCutMenuManager.isVisible,
            onCutToBubble = { orchestrator.bubbleCutMenuManager.performBubbleCut() },
            onDismiss = { orchestrator.hideBubbleCutMenu() }
        )
    }
}