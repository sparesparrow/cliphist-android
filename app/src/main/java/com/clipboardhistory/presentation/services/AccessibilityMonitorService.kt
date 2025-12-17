package com.clipboardhistory.presentation.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Minimal accessibility service used to provide advanced automation capabilities
 * similar to Tasker/AutoApps when the user explicitly enables it.
 */
class AccessibilityMonitorService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op for now. We only need the service enabled to grant capabilities.
    }

    override fun onInterrupt() {
        // No-op
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