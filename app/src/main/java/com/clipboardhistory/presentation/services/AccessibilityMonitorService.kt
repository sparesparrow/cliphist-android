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
}
