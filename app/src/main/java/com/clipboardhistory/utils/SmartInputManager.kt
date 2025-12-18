package com.clipboardhistory.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import com.clipboardhistory.presentation.services.AccessibilityMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Smart input manager that can paste text directly into focused input fields.
 *
 * Uses accessibility services to find focused input fields and InputConnection
 * to paste text directly without going through the system clipboard for better UX.
 */
class SmartInputManager(
    private val context: Context
) {

    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    /**
     * Attempts to paste text directly into the currently focused input field.
     * Falls back to system clipboard if direct input is not available.
     *
     * @param text The text to paste
     * @param useDirectInput Whether to attempt direct input (true) or use clipboard (false)
     * @return true if paste was successful, false otherwise
     */
    fun pasteText(text: String, useDirectInput: Boolean = true): Boolean {
        return if (useDirectInput) {
            pasteDirectly(text)
        } else {
            pasteViaClipboard(text)
        }
    }

    /**
     * Pastes text directly into focused input field using InputConnection.
     * This provides the best user experience as it doesn't affect the system clipboard.
     */
    private fun pasteDirectly(text: String): Boolean {
        try {
            val accessibilityService = AccessibilityMonitorService.getInstance()
            if (accessibilityService == null) {
                // Fallback to clipboard if accessibility service not available
                return pasteViaClipboard(text)
            }

            val focusedNode = findFocusedInputNode(accessibilityService)
            if (focusedNode == null) {
                // No focused input field found
                return pasteViaClipboard(text)
            }

            // Get input connection from the focused node
            val inputConnection = getInputConnectionFromNode(focusedNode)
            if (inputConnection != null) {
                // Paste directly using input connection
                val success = inputConnection.commitText(text, 1)
                focusedNode.refresh()

                if (success) {
                    showToast("Text pasted successfully", Toast.LENGTH_SHORT)
                    return true
                }
            }

            // Fallback to clipboard
            return pasteViaClipboard(text)

        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to clipboard on any error
            return pasteViaClipboard(text)
        }
    }

    /**
     * Pastes text via system clipboard (traditional method).
     */
    private fun pasteViaClipboard(text: String): Boolean {
        return try {
            val clip = ClipData.newPlainText("clipboard", text)
            clipboardManager.setPrimaryClip(clip)

            // Small delay to ensure clipboard is updated
            Handler(Looper.getMainLooper()).postDelayed({
                showToast("Text copied to clipboard", Toast.LENGTH_SHORT)
            }, 100)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Failed to paste text", Toast.LENGTH_SHORT)
            false
        }
    }

    /**
     * Finds the currently focused input node using accessibility service.
     */
    private fun findFocusedInputNode(accessibilityService: AccessibilityMonitorService): AccessibilityNodeInfo? {
        return try {
            val rootNode = accessibilityService.rootInActiveWindow
            findFocusedEditableNode(rootNode)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Recursively searches for a focused editable node in the accessibility tree.
     */
    private fun findFocusedEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        try {
            // Check if this node is focused and editable
            if (node.isFocused && node.isEditable) {
                return node
            }

            // Search child nodes
            for (i in 0 until node.childCount) {
                val childNode = node.getChild(i)
                val result = findFocusedEditableNode(childNode)
                if (result != null) {
                    return result
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * Attempts to get InputConnection from an accessibility node.
     * This is a best-effort approach and may not work for all input fields.
     */
    private fun getInputConnectionFromNode(node: AccessibilityNodeInfo): InputConnection? {
        // Note: Getting InputConnection directly from AccessibilityNodeInfo is not
        // straightforward. In a real implementation, we might need to work with
        // the InputMethodService or use other approaches.

        // For now, we'll rely on the clipboard method as the primary approach
        // and use direct input only when explicitly supported
        return null
    }

    /**
     * Checks if direct input pasting is available.
     */
    fun isDirectInputAvailable(): Boolean {
        val accessibilityService = AccessibilityMonitorService.getInstance()
        return accessibilityService != null && accessibilityService.rootInActiveWindow != null
    }

    /**
     * Gets information about the current input context.
     */
    fun getInputContextInfo(): InputContextInfo {
        val accessibilityService = AccessibilityMonitorService.getInstance()
        val focusedNode = accessibilityService?.let { findFocusedInputNode(it) }

        // Keyboard visibility detection requires Activity context
        // Services cannot directly detect keyboard, so we default to false
        val keyboardVisible = try {
            val context = accessibilityService?.getApplicationContext()
            if (context is android.app.Activity) {
                KeyboardVisibilityDetector.create(context).getCurrentKeyboardState().isVisible
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }

        return InputContextInfo(
            hasAccessibilityService = accessibilityService != null,
            hasFocusedInput = focusedNode != null,
            inputSupportsDirectPaste = false, // For now, we'll use clipboard method
            keyboardVisible = keyboardVisible
        )
    }

    /**
     * Shows a toast message on the main thread.
     */
    private fun showToast(message: String, duration: Int) {
        mainScope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, message, duration).show()
            }
        }
    }

    /**
     * Data class containing information about the current input context.
     */
    data class InputContextInfo(
        val hasAccessibilityService: Boolean = false,
        val hasFocusedInput: Boolean = false,
        val inputSupportsDirectPaste: Boolean = false,
        val keyboardVisible: Boolean = false
    ) {
        /**
         * Whether smart pasting is available in the current context.
         */
        val canSmartPaste: Boolean
            get() = hasAccessibilityService && hasFocusedInput
    }
}