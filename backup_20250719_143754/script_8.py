# Create utility classes
clipboard_utils = '''package com.clipboardhistory.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

/**
 * Utility class for clipboard operations.
 * 
 * This class provides helper methods for common clipboard operations
 * and abstracts platform-specific clipboard management.
 */
object ClipboardUtils {
    
    /**
     * Copies text to the system clipboard.
     * 
     * @param context The application context
     * @param text The text to copy
     * @param label Optional label for the clip
     */
    fun copyToClipboard(context: Context, text: String, label: String = "clipboard") {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clipData)
    }
    
    /**
     * Gets text from the system clipboard.
     * 
     * @param context The application context
     * @return The clipboard text or null if empty
     */
    fun getClipboardText(context: Context): String? {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboardManager.primaryClip
        
        return if (clipData != null && clipData.itemCount > 0) {
            clipData.getItemAt(0).text?.toString()
        } else {
            null
        }
    }
    
    /**
     * Checks if the clipboard has text content.
     * 
     * @param context The application context
     * @return True if clipboard has text, false otherwise
     */
    fun hasClipboardText(context: Context): Boolean {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboardManager.primaryClip
        
        return clipData != null && clipData.itemCount > 0 && 
               clipData.getItemAt(0).text != null
    }
    
    /**
     * Clears the system clipboard.
     * 
     * @param context The application context
     */
    fun clearClipboard(context: Context) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("", "")
        clipboardManager.setPrimaryClip(clipData)
    }
    
    /**
     * Shows a toast with clipboard operation result.
     * 
     * @param context The application context
     * @param message The message to show
     */
    fun showClipboardToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}'''

# Create permission utils
permission_utils = '''package com.clipboardhistory.utils

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
}'''

# Create constants
constants = '''package com.clipboardhistory.utils

/**
 * Constants used throughout the application.
 * 
 * This object contains all the constant values used in the application
 * to avoid magic numbers and strings.
 */
object Constants {
    
    // Database constants
    const val DATABASE_NAME = "clipboard_history.db"
    const val DATABASE_VERSION = 1
    
    // Notification constants
    const val CLIPBOARD_SERVICE_NOTIFICATION_ID = 1001
    const val FLOATING_BUBBLE_SERVICE_NOTIFICATION_ID = 1002
    const val CLIPBOARD_SERVICE_CHANNEL_ID = "clipboard_service_channel"
    const val FLOATING_BUBBLE_SERVICE_CHANNEL_ID = "floating_bubble_channel"
    
    // SharedPreferences constants
    const val PREFERENCES_NAME = "clipboard_history_prefs"
    const val ENCRYPTED_PREFERENCES_NAME = "encrypted_clipboard_prefs"
    
    // Settings constants
    const val DEFAULT_MAX_HISTORY_SIZE = 100
    const val DEFAULT_AUTO_DELETE_HOURS = 24
    const val DEFAULT_BUBBLE_SIZE = 3
    const val DEFAULT_BUBBLE_OPACITY = 0.8f
    const val MIN_BUBBLE_SIZE = 1
    const val MAX_BUBBLE_SIZE = 5
    const val MIN_BUBBLE_OPACITY = 0.1f
    const val MAX_BUBBLE_OPACITY = 1.0f
    
    // Bubble constants
    const val BUBBLE_SIZE_DP = 60
    const val BUBBLE_MARGIN_DP = 16
    const val MAX_VISIBLE_BUBBLES = 5
    
    // Clipboard constants
    const val MAX_CLIPBOARD_SIZE = 1024 * 1024 // 1MB
    const val MAX_CLIPBOARD_PREVIEW_LENGTH = 100
    
    // Encryption constants
    const val ENCRYPTION_KEY_ALIAS = "clipboard_encryption_key"
    const val ENCRYPTION_TRANSFORMATION = "AES/CBC/PKCS7Padding"
    
    // Service constants
    const val SERVICE_START_DELAY_MS = 1000L
    const val CLEANUP_INTERVAL_MS = 60 * 60 * 1000L // 1 hour
    
    // UI constants
    const val ANIMATION_DURATION_MS = 300L
    const val DEBOUNCE_DELAY_MS = 500L
    
    // Intent extras
    const val EXTRA_CLIPBOARD_CONTENT = "extra_clipboard_content"
    const val EXTRA_CLIPBOARD_ITEM_ID = "extra_clipboard_item_id"
    const val EXTRA_SERVICE_ACTION = "extra_service_action"
    
    // Service actions
    const val ACTION_START_SERVICE = "action_start_service"
    const val ACTION_STOP_SERVICE = "action_stop_service"
    const val ACTION_TOGGLE_SERVICE = "action_toggle_service"
    
    // File extensions
    const val EXPORT_FILE_EXTENSION = ".json"
    const val BACKUP_FILE_EXTENSION = ".backup"
    
    // Error messages
    const val ERROR_PERMISSION_DENIED = "Permission denied"
    const val ERROR_SERVICE_NOT_AVAILABLE = "Service not available"
    const val ERROR_CLIPBOARD_EMPTY = "Clipboard is empty"
    const val ERROR_ENCRYPTION_FAILED = "Encryption failed"
    const val ERROR_DECRYPTION_FAILED = "Decryption failed"
}'''

# Create resource files
strings_xml = '''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Clipboard History</string>
    
    <!-- Service notifications -->
    <string name="clipboard_service_title">Clipboard History</string>
    <string name="clipboard_service_description">Monitoring clipboard changes</string>
    <string name="floating_bubble_service_title">Floating Bubbles</string>
    <string name="floating_bubble_service_description">Clipboard bubbles active</string>
    
    <!-- Permissions -->
    <string name="permission_overlay_title">Overlay Permission Required</string>
    <string name="permission_overlay_description">This app needs permission to display floating bubbles over other apps.</string>
    <string name="permission_notification_title">Notification Permission Required</string>
    <string name="permission_notification_description">This app needs permission to show notifications for the clipboard service.</string>
    
    <!-- Main screen -->
    <string name="main_screen_title">Clipboard History</string>
    <string name="start_service">Start Service</string>
    <string name="stop_service">Stop Service</string>
    <string name="settings">Settings</string>
    <string name="add_item">Add Item</string>
    <string name="empty_state_title">No clipboard items yet</string>
    <string name="empty_state_description">Start the clipboard service to begin capturing clipboard history</string>
    <string name="service_running">Clipboard service is running</string>
    <string name="service_stopped">Clipboard service is stopped</string>
    
    <!-- Clipboard items -->
    <string name="copy_to_clipboard">Copy to Clipboard</string>
    <string name="delete_item">Delete Item</string>
    <string name="content_copied">Content copied to clipboard</string>
    <string name="item_deleted">Item deleted</string>
    <string name="just_now">Just now</string>
    <string name="minutes_ago">%dm ago</string>
    <string name="hours_ago">%dh ago</string>
    
    <!-- Settings -->
    <string name="settings_title">Settings</string>
    <string name="max_history_size">Max History Size</string>
    <string name="auto_delete_hours">Auto-delete After</string>
    <string name="enable_encryption">Enable Encryption</string>
    <string name="bubble_size">Bubble Size</string>
    <string name="bubble_opacity">Bubble Opacity</string>
    <string name="clipboard_mode">Clipboard Mode</string>
    <string name="mode_replace">Replace</string>
    <string name="mode_extend">Extend</string>
    <string name="save">Save</string>
    <string name="cancel">Cancel</string>
    
    <!-- Dialogs -->
    <string name="add_clipboard_item">Add Clipboard Item</string>
    <string name="content_label">Content</string>
    <string name="add">Add</string>
    <string name="delete_confirmation">Are you sure you want to delete this item?</string>
    <string name="delete">Delete</string>
    <string name="clear_all_confirmation">Are you sure you want to clear all clipboard items?</string>
    <string name="clear_all">Clear All</string>
    
    <!-- Errors -->
    <string name="error_permission_denied">Permission denied</string>
    <string name="error_service_not_available">Service not available</string>
    <string name="error_clipboard_empty">Clipboard is empty</string>
    <string name="error_encryption_failed">Encryption failed</string>
    <string name="error_decryption_failed">Decryption failed</string>
    <string name="error_unknown">An unknown error occurred</string>
    
    <!-- Bubble modes -->
    <string name="replace_mode">Replace mode</string>
    <string name="extend_mode">Extend mode</string>
    
    <!-- Content types -->
    <string name="content_type_text">TEXT</string>
    <string name="content_type_image">IMAGE</string>
    <string name="content_type_url">URL</string>
    <string name="content_type_file">FILE</string>
    <string name="content_type_other">OTHER</string>
    
    <!-- Size formats -->
    <string name="size_bytes">%dB</string>
    <string name="size_kilobytes">%dKB</string>
    <string name="size_megabytes">%dMB</string>
    
    <!-- Accessibility -->
    <string name="accessibility_copy_button">Copy button</string>
    <string name="accessibility_delete_button">Delete button</string>
    <string name="accessibility_floating_bubble">Floating clipboard bubble</string>
    <string name="accessibility_empty_bubble">Empty clipboard bubble</string>
    <string name="accessibility_mode_bubble">Mode toggle bubble</string>
</resources>'''

# Create colors XML
colors_xml = '''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
    
    <!-- Custom colors -->
    <color name="bubble_background">#80000000</color>
    <color name="bubble_border">#FFFFFF</color>
    <color name="service_running">#4CAF50</color>
    <color name="service_stopped">#F44336</color>
    <color name="encrypted_indicator">#2196F3</color>
</resources>'''

# Create themes XML
themes_xml = '''<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <!-- Base application theme. -->
    <style name="Theme.ClipboardHistory" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- Customize your theme here. -->
        <item name="colorPrimary">@color/purple_500</item>
        <item name="colorPrimaryVariant">@color/purple_700</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/teal_200</item>
        <item name="colorSecondaryVariant">@color/teal_700</item>
        <item name="colorOnSecondary">@color/black</item>
        <item name="android:statusBarColor">?attr/colorPrimaryVariant</item>
    </style>
</resources>'''

# Create drawable resources (as XML since we can't create actual images)
bubble_empty_drawable = '''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/bubble_background" />
    <stroke
        android:width="2dp"
        android:color="@color/bubble_border" />
    <size
        android:width="60dp"
        android:height="60dp" />
</shape>'''

bubble_full_drawable = '''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/purple_500" />
    <stroke
        android:width="2dp"
        android:color="@color/bubble_border" />
    <size
        android:width="60dp"
        android:height="60dp" />
</shape>'''

# Create backup and data extraction rules
backup_rules = '''<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <exclude domain="sharedpref" path="clipboard_history_prefs" />
    <exclude domain="sharedpref" path="encrypted_clipboard_prefs" />
    <exclude domain="database" path="clipboard_history.db" />
</full-backup-content>'''

data_extraction_rules = '''<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="sharedpref" path="clipboard_history_prefs" />
        <exclude domain="sharedpref" path="encrypted_clipboard_prefs" />
        <exclude domain="database" path="clipboard_history.db" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="sharedpref" path="clipboard_history_prefs" />
        <exclude domain="sharedpref" path="encrypted_clipboard_prefs" />
        <exclude domain="database" path="clipboard_history.db" />
    </device-transfer>
</data-extraction-rules>'''

# Write utility files
with open('app/src/main/java/com/clipboardhistory/utils/ClipboardUtils.kt', 'w') as f:
    f.write(clipboard_utils)

with open('app/src/main/java/com/clipboardhistory/utils/PermissionUtils.kt', 'w') as f:
    f.write(permission_utils)

with open('app/src/main/java/com/clipboardhistory/utils/Constants.kt', 'w') as f:
    f.write(constants)

# Write resource files
with open('app/src/main/res/values/strings.xml', 'w') as f:
    f.write(strings_xml)

with open('app/src/main/res/values/colors.xml', 'w') as f:
    f.write(colors_xml)

with open('app/src/main/res/values/themes.xml', 'w') as f:
    f.write(themes_xml)

with open('app/src/main/res/drawable/ic_bubble_empty.xml', 'w') as f:
    f.write(bubble_empty_drawable)

with open('app/src/main/res/drawable/ic_bubble_full.xml', 'w') as f:
    f.write(bubble_full_drawable)

with open('app/src/main/res/xml/backup_rules.xml', 'w') as f:
    f.write(backup_rules)

with open('app/src/main/res/xml/data_extraction_rules.xml', 'w') as f:
    f.write(data_extraction_rules)

print("Utility classes and resource files created!")