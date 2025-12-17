package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Defines the different types of bubbles available in the system.
 * Each type has specific behaviors, policies, and UI characteristics.
 */
enum class BubbleType(
    val displayName: String,
    val keyboardPolicy: KeyboardPolicy,
    val maxInstances: Int = Int.MAX_VALUE,
    val defaultSize: Dp = 56.dp,
    val supportsDragging: Boolean = true,
    val autoHideDelay: Long = 0L, // 0 = never auto-hide
    val zIndexPriority: Int = 0 // Higher = appears on top
) {

    // Text input bubbles - appear when keyboard is shown, hide when keyboard is hidden
    TEXT_PASTE(
        displayName = "Text Paste",
        keyboardPolicy = KeyboardPolicy.SHOW_WHEN_KEYBOARD_VISIBLE,
        maxInstances = 10,
        defaultSize = 48.dp,
        supportsDragging = true,
        autoHideDelay = 30000L, // Auto-hide after 30 seconds
        zIndexPriority = 1
    ),

    // Toolbelt bubbles - minimize when keyboard is shown, can be expanded
    TOOLBELT(
        displayName = "Toolbelt",
        keyboardPolicy = KeyboardPolicy.MINIMIZE_WHEN_KEYBOARD_VISIBLE,
        maxInstances = 1,
        defaultSize = 280.dp, // Wider for controls
        supportsDragging = true,
        autoHideDelay = 0L, // Never auto-hide
        zIndexPriority = 2
    ),

    // Pinned items - always visible, reposition when keyboard shown
    PINNED_ITEM(
        displayName = "Pinned Item",
        keyboardPolicy = KeyboardPolicy.REPOSITION_WHEN_KEYBOARD_VISIBLE,
        maxInstances = 5,
        defaultSize = 44.dp,
        supportsDragging = true,
        autoHideDelay = 0L, // Never auto-hide
        zIndexPriority = 3
    ),

    // System notification bubbles - use Android's Bubble API
    SYSTEM_NOTIFICATION(
        displayName = "System Notification",
        keyboardPolicy = KeyboardPolicy.IGNORE_KEYBOARD,
        maxInstances = 3,
        defaultSize = 56.dp,
        supportsDragging = false, // System handles positioning
        autoHideDelay = 10000L, // Auto-hide after 10 seconds
        zIndexPriority = 0
    ),

    // Quick action bubbles - temporary, high priority
    QUICK_ACTION(
        displayName = "Quick Action",
        keyboardPolicy = KeyboardPolicy.SHOW_WHEN_KEYBOARD_VISIBLE,
        maxInstances = 3,
        defaultSize = 52.dp,
        supportsDragging = true,
        autoHideDelay = 15000L, // Auto-hide after 15 seconds
        zIndexPriority = 4
    );

    /**
     * Whether this bubble type should be visible based on keyboard state.
     */
    fun shouldBeVisible(isKeyboardVisible: Boolean): Boolean {
        return when (keyboardPolicy) {
            KeyboardPolicy.SHOW_WHEN_KEYBOARD_VISIBLE -> isKeyboardVisible
            KeyboardPolicy.HIDE_WHEN_KEYBOARD_VISIBLE -> !isKeyboardVisible
            KeyboardPolicy.MINIMIZE_WHEN_KEYBOARD_VISIBLE -> true // Always visible, just changes size
            KeyboardPolicy.REPOSITION_WHEN_KEYBOARD_VISIBLE -> true // Always visible, just repositions
            KeyboardPolicy.IGNORE_KEYBOARD -> true // Always visible
        }
    }

    /**
     * Whether this bubble type should be minimized based on keyboard state.
     */
    fun shouldBeMinimized(isKeyboardVisible: Boolean): Boolean {
        return keyboardPolicy == KeyboardPolicy.MINIMIZE_WHEN_KEYBOARD_VISIBLE && isKeyboardVisible
    }

    /**
     * Whether this bubble type should be repositioned based on keyboard state.
     */
    fun shouldBeRepositioned(isKeyboardVisible: Boolean): Boolean {
        return keyboardPolicy == KeyboardPolicy.REPOSITION_WHEN_KEYBOARD_VISIBLE && isKeyboardVisible
    }

    /**
     * Get the appropriate size for this bubble type based on keyboard state.
     */
    fun getSize(isKeyboardVisible: Boolean, isMinimized: Boolean = false): Dp {
        return when {
            isMinimized -> 32.dp // Minimized size
            shouldBeMinimized(isKeyboardVisible) -> 40.dp // Semi-minimized
            else -> defaultSize
        }
    }
}

/**
 * Defines how bubbles behave when the keyboard is shown/hidden.
 */
enum class KeyboardPolicy {
    /** Show only when keyboard is visible */
    SHOW_WHEN_KEYBOARD_VISIBLE,

    /** Hide when keyboard is visible */
    HIDE_WHEN_KEYBOARD_VISIBLE,

    /** Always visible but minimize when keyboard is shown */
    MINIMIZE_WHEN_KEYBOARD_VISIBLE,

    /** Always visible but reposition when keyboard is shown */
    REPOSITION_WHEN_KEYBOARD_VISIBLE,

    /** Ignore keyboard state, always behave the same */
    IGNORE_KEYBOARD
}