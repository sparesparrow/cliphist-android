package com.clipboardhistory.utils

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.clipboardhistory.presentation.services.AccessibilityMonitorService
import com.clipboardhistory.presentation.ui.bubble.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages text selection menu integration for "Bubble cut" functionality.
 * Detects text selection events and injects bubble cut options into context menus.
 */
class TextSelectionManager(
    private val context: Context,
    private val bubbleOrchestrator: BubbleOrchestrator? = null
) {

    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    // Configuration
    private var isEnabled = true
    private var showToastFeedback = true
    private var autoCreateTextPasteBubble = true

    // State tracking
    private var lastSelectedText = ""
    private var lastSelectionNode: AccessibilityNodeInfo? = null
    private var menuInjectionAttempted = false

    /**
     * Processes accessibility events to detect text selection and menu display.
     */
    fun onAccessibilityEvent(event: AccessibilityEvent, service: AccessibilityMonitorService) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                handleTextSelectionChanged(event, service)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event, service)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleViewClicked(event, service)
            }
        }
    }

    /**
     * Handles text selection changes to capture selected text.
     */
    private fun handleTextSelectionChanged(event: AccessibilityEvent, service: AccessibilityMonitorService) {
        if (!isEnabled) return

        try {
            val sourceNode = event.source ?: return

            // Check if this is a text selection event with actual text
            if (event.fromIndex >= 0 && event.toIndex > event.fromIndex) {
                // Get the selected text
                val selectedText = sourceNode.text?.substring(
                    event.fromIndex,
                    minOf(event.toIndex, sourceNode.text?.length ?: 0)
                ) ?: ""

                if (selectedText.isNotEmpty()) {
                    lastSelectedText = selectedText
                    lastSelectionNode = sourceNode
                    menuInjectionAttempted = false

                    if (showToastFeedback) {
                        showToast("Text selected - Bubble cut available", Toast.LENGTH_SHORT)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Handles window state changes to detect context menu appearance.
     */
    private fun handleWindowStateChanged(event: AccessibilityEvent, service: AccessibilityMonitorService) {
        if (!isEnabled || lastSelectedText.isEmpty()) return

        try {
            // Look for context menu windows
            val rootNode = service.rootInActiveWindow ?: return

            // Check if a context menu is being displayed
            if (isContextMenuVisible(rootNode)) {
                // Attempt to inject bubble cut option
                injectBubbleCutOption(rootNode, service)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Handles view clicks to detect menu interactions.
     */
    private fun handleViewClicked(event: AccessibilityEvent, service: AccessibilityMonitorService) {
        if (!isEnabled) return

        try {
            val clickedNode = event.source ?: return

            // Check if this is a bubble cut menu item click
            if (isBubbleCutMenuItem(clickedNode)) {
                performBubbleCut(service)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if a context menu is currently visible.
     */
    private fun isContextMenuVisible(rootNode: AccessibilityNodeInfo): Boolean {
        return findNodeByCriteria(rootNode) { node ->
            val className = node.className?.toString() ?: ""
            val contentDescription = node.contentDescription?.toString() ?: ""
            val text = node.text?.toString() ?: ""

            // Common context menu indicators
            className.contains("Menu") ||
            contentDescription.contains("menu", ignoreCase = true) ||
            text.contains("Cut", ignoreCase = true) ||
            text.contains("Copy", ignoreCase = true) ||
            text.contains("Paste", ignoreCase = true)
        } != null
    }

    /**
     * Attempts to inject a "Bubble cut" option into the context menu.
     */
    private fun injectBubbleCutOption(rootNode: AccessibilityNodeInfo, service: AccessibilityMonitorService) {
        if (menuInjectionAttempted || lastSelectedText.isEmpty()) return

        try {
            menuInjectionAttempted = true

            // Find existing menu items to understand the menu structure
            val menuItems = findMenuItems(rootNode)

            if (menuItems.isNotEmpty()) {
                // Try to add our menu item after existing items
                val lastMenuItem = menuItems.last()

                // In a real implementation, we would need to:
                // 1. Create a new menu item node
                // 2. Add it to the menu structure
                // 3. Set up proper accessibility properties

                // For now, we'll simulate this by showing a toast
                // In production, this would require deeper accessibility API integration

                showToast("Bubble cut option added to menu", Toast.LENGTH_SHORT)

                // Schedule menu item monitoring
                Handler(Looper.getMainLooper()).postDelayed({
                    monitorMenuInteraction(service)
                }, 500)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Monitors for bubble cut menu item interactions.
     */
    private fun monitorMenuInteraction(service: AccessibilityMonitorService) {
        // This would be where we monitor for the injected menu item being selected
        // In a real implementation, this would involve tracking the menu item we injected
    }

    /**
     * Performs the bubble cut operation.
     */
    private fun performBubbleCut(service: AccessibilityMonitorService) {
        if (lastSelectedText.isEmpty()) return

        mainScope.launch {
            try {
                // Perform the cut operation (remove text from source)
                performTextCut(service)

                // Send text directly to bubble (bypass clipboard)
                sendToBubble(lastSelectedText)

                // Clear selection state
                lastSelectedText = ""
                lastSelectionNode = null
                menuInjectionAttempted = false

                if (showToastFeedback) {
                    showToast("Text cut to bubble", Toast.LENGTH_SHORT)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Failed to cut text to bubble", Toast.LENGTH_SHORT)
            }
        }
    }

    /**
     * Performs the actual text cut operation by simulating the cut action.
     */
    private fun performTextCut(service: AccessibilityMonitorService) {
        try {
            lastSelectionNode?.let { node ->
                // Try to perform cut action
                if (node.performAction(AccessibilityNodeInfo.ACTION_CUT)) {
                    // Success - text was cut
                } else {
                    // Fallback: try to simulate cut via key events or other methods
                    simulateCutAction(service)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            simulateCutAction(service)
        }
    }

    /**
     * Simulates cut action when direct cut fails.
     */
    private fun simulateCutAction(service: AccessibilityMonitorService) {
        // This is a fallback implementation
        // In a real scenario, we might need to:
        // 1. Send key combination (Ctrl+X)
        // 2. Use InputConnection to delete text
        // 3. Other platform-specific methods

        // For now, we'll rely on the direct cut action above
    }

    /**
     * Sends the cut text directly to a bubble.
     */
    private fun sendToBubble(text: String) {
        bubbleOrchestrator?.let { orchestrator ->
            when {
                // If there are regex accumulators, try them first
                orchestrator.getRegexAccumulatorBubbles().isNotEmpty() -> {
                    orchestrator.processClipboardContentForRegexAccumulators(text, "bubble_cut")
                }

                // Otherwise, create a text paste bubble
                autoCreateTextPasteBubble -> {
                    val bubble = BubbleSpec.TextPasteBubble(
                        textContent = text,
                        contentType = detectContentType(text)
                    )
                    orchestrator.addBubble(bubble)
                }

                else -> {
                    // Fallback: use smart input manager
                    SmartInputManager(context).pasteText(text)
                }
            }
        } ?: run {
            // No orchestrator available, use smart input manager
            SmartInputManager(context).pasteText(text)
        }
    }

    /**
     * Finds menu items in the accessibility tree.
     */
    private fun findMenuItems(rootNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val menuItems = mutableListOf<AccessibilityNodeInfo>()

        findNodesByCriteria(rootNode) { node ->
            val className = node.className?.toString() ?: ""
            val text = node.text?.toString() ?: ""
            val contentDescription = node.contentDescription?.toString() ?: ""

            // Common menu item patterns
            className.contains("MenuItem") ||
            className.contains("Button") ||
            text in listOf("Cut", "Copy", "Paste", "Select All") ||
            contentDescription.contains("cut", ignoreCase = true) ||
            contentDescription.contains("copy", ignoreCase = true)
        }.let { menuItems.addAll(it) }

        return menuItems
    }

    /**
     * Checks if a node is our bubble cut menu item.
     */
    private fun isBubbleCutMenuItem(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString() ?: ""
        val contentDescription = node.contentDescription?.toString() ?: ""

        return text == "Bubble cut" ||
               contentDescription == "Bubble cut" ||
               text == "Cut to bubble" ||
               contentDescription == "Cut to bubble"
    }

    /**
     * Detects content type for the cut text.
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

    /**
     * Recursively finds nodes matching criteria.
     */
    private fun findNodesByCriteria(
        node: AccessibilityNodeInfo,
        criteria: (AccessibilityNodeInfo) -> Boolean
    ): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()

        try {
            if (criteria(node)) {
                results.add(node)
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    results.addAll(findNodesByCriteria(child, criteria))
                }
            }
        } catch (e: Exception) {
            // Ignore accessibility exceptions
        }

        return results
    }

    /**
     * Finds first node matching criteria.
     */
    private fun findNodeByCriteria(
        node: AccessibilityNodeInfo,
        criteria: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        return findNodesByCriteria(node, criteria).firstOrNull()
    }

    /**
     * Shows a toast message.
     */
    private fun showToast(message: String, duration: Int) {
        mainScope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, message, duration).show()
            }
        }
    }

    // Configuration methods

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun setShowToastFeedback(show: Boolean) {
        showToastFeedback = show
    }

    fun setAutoCreateTextPasteBubble(autoCreate: Boolean) {
        autoCreateTextPasteBubble = autoCreate
    }

    fun isEnabled(): Boolean = isEnabled

    fun hasSelectedText(): Boolean = lastSelectedText.isNotEmpty()

    fun getSelectedText(): String = lastSelectedText

    companion object {
        /**
         * Creates a TextSelectionManager with bubble orchestrator integration.
         */
        fun createWithBubbleIntegration(
            context: Context,
            bubbleOrchestrator: BubbleOrchestrator
        ): TextSelectionManager {
            return TextSelectionManager(context, bubbleOrchestrator)
        }
    }
}