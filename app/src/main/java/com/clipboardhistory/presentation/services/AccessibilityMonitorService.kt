package com.clipboardhistory.presentation.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.clipboardhistory.utils.TextSelectionManager

/**
 * Enhanced accessibility service providing advanced automation capabilities
 * including text selection monitoring and bubble cut integration.
 */
class AccessibilityMonitorService : AccessibilityService() {

    private lateinit var textSelectionManager: TextSelectionManager

    override fun onCreate() {
        super.onCreate()
        textSelectionManager = TextSelectionManager(this, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let { accessibilityEvent ->
            textSelectionManager.onAccessibilityEvent(accessibilityEvent, this)
        }
    }

    override fun onInterrupt() {
        // Service interrupted - cleanup if needed
    }

    /**
     * Gets the text selection manager for external access.
     */
    fun getTextSelectionManager(): TextSelectionManager = textSelectionManager

    /**
     * Sets the bubble orchestrator for text selection integration.
     * This allows the text selection manager to send cut text directly to bubbles.
     */
    fun setBubbleOrchestrator(orchestrator: com.clipboardhistory.presentation.ui.bubble.BubbleOrchestrator?) {
        textSelectionManager = TextSelectionManager(this, orchestrator)
    }

    companion object {
        private var instance: AccessibilityMonitorService? = null

        /**
         * Gets the current instance of the accessibility service.
         * Returns null if the service is not running.
         */
        fun getInstance(): AccessibilityMonitorService? = instance

        /**
         * Sets the instance for testing purposes.
         * This method should only be used in unit tests.
         */
        internal fun setInstanceForTesting(service: AccessibilityMonitorService?) {
            instance = service
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
