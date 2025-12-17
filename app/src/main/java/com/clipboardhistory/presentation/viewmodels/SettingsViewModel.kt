package com.clipboardhistory.presentation.viewmodels

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipboardhistory.domain.model.BubbleType
import com.clipboardhistory.domain.model.ClipboardSettings
import com.clipboardhistory.domain.repository.ClipboardRepository
import com.clipboardhistory.utils.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for settings screen functionality.
 *
 * Manages settings state, permissions, and configuration updates.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ClipboardRepository,
) : ViewModel() {

    private val _settings = MutableStateFlow(ClipboardSettings())
    val settings: StateFlow<ClipboardSettings> = _settings.asStateFlow()

    private val _permissionStatus = MutableStateFlow(PermissionStatus())
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    init {
        loadSettings()
    }

    /**
     * Load current settings from repository.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _settings.value = repository.getSettings()
            } catch (e: Exception) {
                // Use default settings if loading fails
                _settings.value = ClipboardSettings()
            }
        }
    }

    /**
     * Update clipboard monitoring setting.
     */
    suspend fun updateClipboardMonitoring(enabled: Boolean) {
        val updatedSettings = _settings.value.copy(enableClipboardMonitoring = enabled)
        updateSettings(updatedSettings)
    }

    /**
     * Update accessibility monitoring setting.
     */
    suspend fun updateAccessibilityMonitoring(enabled: Boolean) {
        val updatedSettings = _settings.value.copy(enableAccessibilityMonitoring = enabled)
        updateSettings(updatedSettings)
    }

    /**
     * Update encryption setting.
     */
    suspend fun updateEncryption(enabled: Boolean) {
        val updatedSettings = _settings.value.copy(enableEncryption = enabled)
        updateSettings(updatedSettings)
    }

    /**
     * Update bubble size setting.
     */
    suspend fun updateBubbleSize(size: Int) {
        val updatedSettings = _settings.value.copy(bubbleSize = size)
        updateSettings(updatedSettings)
    }

    /**
     * Update bubble opacity setting.
     */
    suspend fun updateBubbleOpacity(opacity: Float) {
        val updatedSettings = _settings.value.copy(bubbleOpacity = opacity)
        updateSettings(updatedSettings)
    }

    /**
     * Update bubble type setting.
     */
    suspend fun updateBubbleType(type: BubbleType) {
        val updatedSettings = _settings.value.copy(bubbleType = type)
        updateSettings(updatedSettings)
    }

    /**
     * Update settings in repository.
     */
    private suspend fun updateSettings(newSettings: ClipboardSettings) {
        try {
            repository.updateSettings(newSettings)
            _settings.value = newSettings
        } catch (e: Exception) {
            // Handle error - could emit error state
        }
    }

    /**
     * Clear all clipboard data.
     */
    suspend fun clearAllData() {
        // This would implement data clearing logic
        // For now, just reset settings to defaults
        resetSettings()
    }

    /**
     * Reset all settings to defaults.
     */
    suspend fun resetSettings() {
        val defaultSettings = ClipboardSettings()
        updateSettings(defaultSettings)
    }

    /**
     * Check and update permission status.
     */
    fun checkPermissionStatus(context: Context) {
        _permissionStatus.value = PermissionStatus(
            overlayGranted = PermissionUtils.hasOverlayPermission(context),
            notificationsGranted = PermissionUtils.hasNotificationPermission(context),
            usageAccessGranted = PermissionUtils.hasUsageAccess(context),
            batteryOptimizationGranted = PermissionUtils.isIgnoringBatteryOptimizations(context),
            accessibilityGranted = PermissionUtils.hasAccessibilityPermission(context),
        )
    }

    /**
     * Request overlay permission.
     */
    fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    }

    /**
     * Request notification permission.
     */
    fun requestNotificationPermission(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                checkPermissionStatus(activity)
            }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Request usage access permission.
     */
    fun requestUsageAccessPermission(context: Context) {
        val intent = PermissionUtils.usageAccessSettingsIntent()
        context.startActivity(intent)
    }

    /**
     * Request battery optimization permission.
     */
    fun requestBatteryOptimizationPermission(context: Context) {
        val intent = PermissionUtils.batteryOptimizationIntent(context)
        context.startActivity(intent)
    }

    /**
     * Request accessibility permission.
     */
    fun requestAccessibilityPermission(context: Context) {
        val intent = PermissionUtils.accessibilitySettingsIntent()
        context.startActivity(intent)
    }
}

/**
 * Data class representing the status of various permissions.
 */
data class PermissionStatus(
    val overlayGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val usageAccessGranted: Boolean = false,
    val batteryOptimizationGranted: Boolean = false,
    val accessibilityGranted: Boolean = false,
) {
    /**
     * Check if all required permissions are granted.
     */
    val allRequiredGranted: Boolean
        get() = overlayGranted && notificationsGranted &&
                usageAccessGranted && batteryOptimizationGranted

    /**
     * Check if advanced permissions are granted.
     */
    val advancedPermissionsGranted: Boolean
        get() = allRequiredGranted && accessibilityGranted
}