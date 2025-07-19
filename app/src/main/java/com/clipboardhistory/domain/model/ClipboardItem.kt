package com.clipboardhistory.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data model representing a clipboard item.
 * 
 * @property id Unique identifier for the clipboard item
 * @property content The text content of the clipboard item
 * @property timestamp When the item was created (in milliseconds)
 * @property contentType Type of the content (TEXT, IMAGE, etc.)
 * @property isEncrypted Whether the content is encrypted in storage
 * @property size Size of the content in bytes
 */
@Parcelize
data class ClipboardItem(
    val id: String,
    val content: String,
    val timestamp: Long,
    val contentType: ContentType,
    val isEncrypted: Boolean = true,
    val size: Int
) : Parcelable

/**
 * Enumeration of supported content types for clipboard items.
 */
enum class ContentType {
    TEXT,
    IMAGE,
    URL,
    FILE,
    OTHER
}

/**
 * Settings model for the clipboard history application.
 * 
 * @property maxHistorySize Maximum number of items to keep in history
 * @property autoDeleteAfterHours Automatically delete items after this many hours
 * @property enableEncryption Whether to encrypt clipboard data
 * @property bubbleSize Size of the floating bubbles (1-5 scale)
 * @property bubbleOpacity Opacity of the floating bubbles (0.1-1.0)
 * @property clipboardMode Current clipboard mode (REPLACE or EXTEND)
 */
data class ClipboardSettings(
    val maxHistorySize: Int = 100,
    val autoDeleteAfterHours: Int = 24,
    val enableEncryption: Boolean = true,
    val bubbleSize: Int = 3,
    val bubbleOpacity: Float = 0.8f,
    val clipboardMode: ClipboardMode = ClipboardMode.REPLACE
)

/**
 * Enumeration of clipboard operation modes.
 */
enum class ClipboardMode {
    REPLACE,
    EXTEND
}