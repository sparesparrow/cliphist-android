package com.clipboardhistory.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
                Manifest.permission.POST_NOTIFICATIONS
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
        return hasOverlayPermission(context) && hasNotificationPermission(context)
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
        
        return missingPermissions
    }
}