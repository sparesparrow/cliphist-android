package com.clipboardhistory.utils

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat

/**
 * Utility class for handling permissions.
 *
 * This class provides helper methods for checking and requesting
 * permissions required by the application.
 */
object PermissionUtils {
    /**
     * Checks if the app has overlay permission.
     *
     * @param context The application context
     * @return True if permission is granted, false otherwise
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Checks if the app has notification permission (Android 13+).
     *
     * @param context The application context
     * @return True if permission is granted, false otherwise
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required on older versions
        }
    }

    /**
     * Gets the URI for requesting overlay permission.
     *
     * @param context The application context
     * @return URI for Settings.ACTION_MANAGE_OVERLAY_PERMISSION
     */
    fun getOverlayPermissionUri(context: Context): Uri {
        return Uri.parse("package:${context.packageName}")
    }

    /**
     * Checks if all required permissions are granted.
     *
     * @param context The application context
     * @return True if all permissions are granted, false otherwise
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasOverlayPermission(context) &&
            hasNotificationPermission(context) &&
            hasUsageAccess(context) &&
            isIgnoringBatteryOptimizations(context)
    }

    /**
     * Gets a list of missing permissions.
     *
     * @param context The application context
     * @return List of missing permission names
     */
    fun getMissingPermissions(context: Context): List<String> {
        val missingPermissions = mutableListOf<String>()

        if (!hasOverlayPermission(context)) {
            missingPermissions.add("System Alert Window")
        }

        if (!hasNotificationPermission(context)) {
            missingPermissions.add("Notifications")
        }
        if (!hasUsageAccess(context)) {
            missingPermissions.add("Usage Access")
        }
        if (!isIgnoringBatteryOptimizations(context)) {
            missingPermissions.add("Ignore Battery Optimizations")
        }

        return missingPermissions
    }

    /** Checks if the app has Usage Access (PACKAGE_USAGE_STATS). */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val uid = android.os.Process.myUid()
        val packageName = context.packageName
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mode =
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    uid,
                    packageName,
                )
            mode == AppOpsManager.MODE_ALLOWED
        } else {
            @Suppress("DEPRECATION")
            val mode =
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    uid,
                    packageName,
                )
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    /** Intent for Usage Access settings screen. */
    fun usageAccessSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    /** Checks if app is ignoring battery optimizations. */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Intent for Ignore Battery Optimizations screen. */
    fun batteryOptimizationIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
}
