package com.clipboardhistory.utils

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
}