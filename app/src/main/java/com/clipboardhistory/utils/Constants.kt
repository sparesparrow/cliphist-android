package com.clipboardhistory.utils

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
}
