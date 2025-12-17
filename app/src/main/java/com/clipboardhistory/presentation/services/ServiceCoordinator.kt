package com.clipboardhistory.presentation.services

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import com.clipboardhistory.domain.usecase.GetClipboardSettingsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service coordinator for managing clipboard monitoring services.
 *
 * This class coordinates the lifecycle and operations of ClipboardService,
 * FloatingBubbleService, and AccessibilityMonitorService, ensuring proper
 * background monitoring and service dependencies.
 */
@Singleton
class ServiceCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getClipboardSettingsUseCase: GetClipboardSettingsUseCase,
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var isMonitoringActive = false

    /**
     * Starts clipboard monitoring based on current settings and permissions.
     */
    fun startClipboardMonitoring() {
        scope.launch {
            val settings = getClipboardSettingsUseCase()
            if (!settings.enableClipboardMonitoring) return@launch

            isMonitoringActive = true

            // Start ClipboardService for basic monitoring
            startClipboardService()

            // Start FloatingBubbleService for bubble functionality
            startFloatingBubbleService()

            // Start AccessibilityMonitorService if accessibility is enabled
            if (settings.enableAccessibilityMonitoring && isAccessibilityServiceEnabled()) {
                startAccessibilityService()
            }
        }
    }

    /**
     * Stops all clipboard monitoring services.
     */
    fun stopClipboardMonitoring() {
        isMonitoringActive = false

        // Stop ClipboardService
        stopClipboardService()

        // Stop FloatingBubbleService
        stopFloatingBubbleService()

        // Note: AccessibilityService cannot be programmatically stopped
        // It requires user interaction through system settings
    }

    /**
     * Checks if clipboard monitoring is currently active.
     */
    fun isMonitoringActive(): Boolean = isMonitoringActive

    /**
     * Updates monitoring configuration based on new settings.
     */
    fun updateMonitoringConfiguration() {
        scope.launch {
            val settings = getClipboardSettingsUseCase()

            if (settings.enableClipboardMonitoring && !isMonitoringActive) {
                startClipboardMonitoring()
            } else if (!settings.enableClipboardMonitoring && isMonitoringActive) {
                stopClipboardMonitoring()
            }
        }
    }

    /**
     * Handles system events that might affect service operation.
     */
    fun handleSystemEvent(event: SystemEvent) {
        when (event) {
            SystemEvent.BOOT_COMPLETED -> {
                // Restart monitoring after device boot
                startClipboardMonitoring()
            }
            SystemEvent.USER_UNLOCKED -> {
                // Resume monitoring after device unlock
                startClipboardMonitoring()
            }
            SystemEvent.LOW_MEMORY -> {
                // Pause intensive operations during low memory
                // Keep basic clipboard service running
            }
            SystemEvent.BATTERY_LOW -> {
                // Reduce monitoring frequency during low battery
                adjustMonitoringForBattery()
            }
        }
    }

    /**
     * Gets the current status of all monitoring services.
     */
    suspend fun getMonitoringStatus(): MonitoringStatus {
        val settings = getClipboardSettingsUseCase()

        return MonitoringStatus(
            isClipboardServiceRunning = isServiceRunning(ClipboardService::class.java),
            isFloatingBubbleServiceRunning = isServiceRunning(FloatingBubbleService::class.java),
            isAccessibilityServiceEnabled = isAccessibilityServiceEnabled(),
            isMonitoringEnabled = settings.enableClipboardMonitoring,
            isAccessibilityMonitoringEnabled = settings.enableAccessibilityMonitoring,
            activeServices = getActiveServices()
        )
    }

    private fun startClipboardService() {
        val intent = Intent(context, ClipboardService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopClipboardService() {
        val intent = Intent(context, ClipboardService::class.java)
        context.stopService(intent)
    }

    private fun startFloatingBubbleService() {
        val intent = Intent(context, FloatingBubbleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopFloatingBubbleService() {
        val intent = Intent(context, FloatingBubbleService::class.java)
        context.stopService(intent)
    }

    private fun startAccessibilityService() {
        // Accessibility services are started/stopped by the system
        // We can only check if they're enabled
        // Users need to manually enable through system settings
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = (context as Application).getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        // Simplified check - for now just return false as we don't have accessibility service implemented
        return false
    }

    private fun adjustMonitoringForBattery() {
        // Implementation for battery-aware monitoring adjustments
        // Could reduce polling frequency or pause non-essential features
    }

    private fun getActiveServices(): List<String> {
        val activeServices = mutableListOf<String>()

        if (isServiceRunning(ClipboardService::class.java)) {
            activeServices.add("ClipboardService")
        }

        if (isServiceRunning(FloatingBubbleService::class.java)) {
            activeServices.add("FloatingBubbleService")
        }

        if (isAccessibilityServiceEnabled()) {
            activeServices.add("AccessibilityMonitorService")
        }

        return activeServices
    }

    enum class SystemEvent {
        BOOT_COMPLETED,
        USER_UNLOCKED,
        LOW_MEMORY,
        BATTERY_LOW,
    }

    data class MonitoringStatus(
        val isClipboardServiceRunning: Boolean,
        val isFloatingBubbleServiceRunning: Boolean,
        val isAccessibilityServiceEnabled: Boolean,
        val isMonitoringEnabled: Boolean,
        val isAccessibilityMonitoringEnabled: Boolean,
        val activeServices: List<String>
    )
}