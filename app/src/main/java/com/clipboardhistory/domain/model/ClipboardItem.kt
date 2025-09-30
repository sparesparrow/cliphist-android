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
    val size: Int,
) : Parcelable

/**
 * Enumeration of supported content types for clipboard items.
 */
enum class ContentType {
    TEXT,
    IMAGE,
    URL,
    FILE,
    OTHER,
}

/**
 * Enumeration of bubble states.
 */
enum class BubbleState {
    EMPTY, // Bubble has no content
    STORING, // Bubble stores existing content (normal state)
    REPLACE, // Next copy will replace bubble content
    APPEND, // Next copy will append to bubble content
    PREPEND, // Next copy will prepend to bubble content
}

/**
 * Enumeration of bubble types/shapes.
 */
enum class BubbleType {
    CIRCLE, // Circular bubble (default)
    CUBE, // Cube-shaped bubble that flashes content
    HEXAGON, // Hexagonal bubble
    SQUARE, // Square bubble with rounded corners
}

/**
 * Data class representing a bubble theme with colors for different states.
 */
data class BubbleTheme(
    val name: String,
    val description: String,
    val colors: BubbleColors,
)

/**
 * Data class representing colors for different bubble states.
 */
data class BubbleColors(
    val empty: Int, // Color for empty bubbles
    val storing: Int, // Color for bubbles storing content
    val replace: Int, // Color for replace state
    val append: Int, // Color for append state
    val prepend: Int, // Color for prepend state
)

/**
 * Predefined bubble themes.
 */
object BubbleThemes {
    val DEFAULT =
        BubbleTheme(
            name = "Default",
            description = "Material Design colors",
            colors =
                BubbleColors(
                    empty = 0xFFE0E0E0.toInt(), // Light gray
                    storing = 0xFF2196F3.toInt(), // Blue
                    replace = 0xFFFF5722.toInt(), // Red-orange
                    append = 0xFF4CAF50.toInt(), // Green
                    prepend = 0xFF9C27B0.toInt(), // Purple
                ),
        )

    val DARK =
        BubbleTheme(
            name = "Dark",
            description = "Dark theme with vibrant colors",
            colors =
                BubbleColors(
                    empty = 0xFF424242.toInt(), // Dark gray
                    storing = 0xFF1976D2.toInt(), // Dark blue
                    replace = 0xFFD32F2F.toInt(), // Dark red
                    append = 0xFF388E3C.toInt(), // Dark green
                    prepend = 0xFF7B1FA2.toInt(), // Dark purple
                ),
        )

    val PASTEL =
        BubbleTheme(
            name = "Pastel",
            description = "Soft pastel colors",
            colors =
                BubbleColors(
                    empty = 0xFFF5F5F5.toInt(), // Very light gray
                    storing = 0xFFBBDEFB.toInt(), // Light blue
                    replace = 0xFFFFCDD2.toInt(), // Light red
                    append = 0xFFC8E6C9.toInt(), // Light green
                    prepend = 0xFFE1BEE7.toInt(), // Light purple
                ),
        )

    val NEON =
        BubbleTheme(
            name = "Neon",
            description = "Bright neon colors",
            colors =
                BubbleColors(
                    empty = 0xFF2C2C2C.toInt(), // Dark background
                    storing = 0xFF00BCD4.toInt(), // Cyan
                    replace = 0xFFFF4081.toInt(), // Pink
                    append = 0xFF8BC34A.toInt(), // Light green
                    prepend = 0xFFE040FB.toInt(), // Neon purple
                ),
        )

    val ALL_THEMES = listOf(DEFAULT, DARK, PASTEL, NEON)
}

/**
 * Settings model for the clipboard history application.
 *
 * @property maxHistorySize Maximum number of items to keep in history
 * @property autoDeleteAfterHours Automatically delete items after this many hours
 * @property enableEncryption Whether to encrypt clipboard data
 * @property bubbleSize Size of the floating bubbles (1-5 scale)
 * @property bubbleOpacity Opacity of the floating bubbles (0.1-1.0)
 * @property selectedTheme The selected bubble theme
 * @property bubbleType The type/shape of bubbles to display
 */
data class ClipboardSettings(
    val maxHistorySize: Int = 100,
    val autoDeleteAfterHours: Int = 24,
    val enableEncryption: Boolean = true,
    val bubbleSize: Int = 3,
    val bubbleOpacity: Float = 0.8f,
    val selectedTheme: String = "Default",
    val bubbleType: BubbleType = BubbleType.CIRCLE,
)
