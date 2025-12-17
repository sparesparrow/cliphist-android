package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.UUID

/**
 * Unified specification for all bubble types in the system.
 * Defines behavior, appearance, and lifecycle for different bubble categories.
 */
sealed class BubbleSpec {
    abstract val id: String
    abstract val type: BubbleType
    abstract val position: Offset
    abstract val size: Dp
    abstract val isVisible: Boolean
    abstract val isMinimized: Boolean
    abstract val lastInteractionTime: Long
    abstract val content: @Composable (BubbleSpec) -> Unit

    /**
     * Creates a copy with updated visibility based on keyboard state.
     */
    abstract fun withKeyboardState(isKeyboardVisible: Boolean): BubbleSpec

    /**
     * Creates a copy with updated position.
     */
    abstract fun withPosition(newPosition: Offset): BubbleSpec

    /**
     * Creates a copy with updated minimization state.
     */
    abstract fun withMinimized(isMinimized: Boolean): BubbleSpec

    /**
     * Creates a copy with updated size.
     */
    abstract fun withSize(newSize: Dp): BubbleSpec

    /**
     * Creates a copy with updated interaction time.
     */
    abstract fun withInteraction(): BubbleSpec

    /**
     * Overlay bubble - rendered via WindowManager
     */
    data class OverlayBubble(
        override val id: String = UUID.randomUUID().toString(),
        override val type: BubbleType,
        override val position: Offset = Offset.Zero,
        override val size: Dp = type.defaultSize,
        override val isVisible: Boolean = true,
        override val isMinimized: Boolean = false,
        override val lastInteractionTime: Long = System.currentTimeMillis(),
        val isDraggable: Boolean = type.supportsDragging,
        val customContent: @Composable (OverlayBubble) -> Unit
    ) : BubbleSpec() {

        override val content: @Composable (BubbleSpec) -> Unit = { spec ->
            customContent(spec as OverlayBubble)
        }

        override fun withKeyboardState(isKeyboardVisible: Boolean): OverlayBubble {
            val shouldBeVisible = type.shouldBeVisible(isKeyboardVisible)
            val shouldBeMinimized = type.shouldBeMinimized(isKeyboardVisible)
            val newSize = type.getSize(isKeyboardVisible, shouldBeMinimized)

            return copy(
                isVisible = shouldBeVisible,
                isMinimized = shouldBeMinimized,
                size = newSize
            )
        }

        override fun withPosition(newPosition: Offset): OverlayBubble =
            copy(position = newPosition)

        override fun withMinimized(isMinimized: Boolean): OverlayBubble =
            copy(isMinimized = isMinimized)

        override fun withSize(newSize: Dp): OverlayBubble =
            copy(size = newSize)

        override fun withInteraction(): OverlayBubble =
            copy(lastInteractionTime = System.currentTimeMillis())
    }

    /**
     * System bubble - rendered via Android's Bubble API
     */
    data class SystemBubble(
        override val id: String = UUID.randomUUID().toString(),
        override val type: BubbleType,
        override val position: Offset = Offset.Zero, // Not used for system bubbles
        override val size: Dp = type.defaultSize,
        override val isVisible: Boolean = true,
        override val isMinimized: Boolean = false,
        override val lastInteractionTime: Long = System.currentTimeMillis(),
        val notificationId: Int,
        val title: String,
        val content: String,
        val iconResId: Int,
        val bubbleActivity: Class<*>? = null,
        val customContent: @Composable (SystemBubble) -> Unit
    ) : BubbleSpec() {

        override val content: @Composable (BubbleSpec) -> Unit = { spec ->
            customContent(spec as SystemBubble)
        }

        override fun withKeyboardState(isKeyboardVisible: Boolean): SystemBubble {
            // System bubbles generally ignore keyboard state
            return this
        }

        override fun withPosition(newPosition: Offset): SystemBubble =
            copy(position = newPosition)

        override fun withMinimized(isMinimized: Boolean): SystemBubble =
            copy(isMinimized = isMinimized)

        override fun withSize(newSize: Dp): SystemBubble =
            copy(size = newSize)

        override fun withInteraction(): SystemBubble =
            copy(lastInteractionTime = System.currentTimeMillis())
    }

    /**
     * Toolbelt bubble - specialized overlay for control tools
     */
    data class ToolbeltBubble(
        override val id: String = UUID.randomUUID().toString(),
        override val type: BubbleType = BubbleType.TOOLBELT,
        override val position: Offset = Offset.Zero,
        override val size: Dp = 280.dp, // Wider for tool layout
        override val isVisible: Boolean = true,
        override val isMinimized: Boolean = false,
        override val lastInteractionTime: Long = System.currentTimeMillis(),
        val tools: List<ToolbeltTool> = emptyList(),
        val isExpanded: Boolean = !isMinimized,
        val orientation: ToolbeltOrientation = ToolbeltOrientation.HORIZONTAL
    ) : BubbleSpec() {

        override val content: @Composable (BubbleSpec) -> Unit = { spec ->
            val toolbeltSpec = spec as ToolbeltBubble
            ToolbeltContent(toolbeltSpec)
        }

        override fun withKeyboardState(isKeyboardVisible: Boolean): ToolbeltBubble {
            val shouldBeMinimized = type.shouldBeMinimized(isKeyboardVisible)
            val newSize = if (shouldBeMinimized) 48.dp else 280.dp

            return copy(
                isMinimized = shouldBeMinimized,
                isExpanded = !shouldBeMinimized,
                size = newSize
            )
        }

        override fun withPosition(newPosition: Offset): ToolbeltBubble =
            copy(position = newPosition)

        override fun withMinimized(isMinimized: Boolean): ToolbeltBubble =
            copy(isMinimized = isMinimized, isExpanded = !isMinimized)

        override fun withSize(newSize: Dp): ToolbeltBubble =
            copy(size = newSize)

        override fun withInteraction(): ToolbeltBubble =
            copy(lastInteractionTime = System.currentTimeMillis())

        enum class ToolbeltOrientation {
            HORIZONTAL, VERTICAL
        }
    }

    /**
     * Text paste bubble - for clipboard content
     */
    data class TextPasteBubble(
        override val id: String = UUID.randomUUID().toString(),
        override val type: BubbleType = BubbleType.TEXT_PASTE,
        override val position: Offset = Offset.Zero,
        override val size: Dp = type.defaultSize,
        override val isVisible: Boolean = true,
        override val isMinimized: Boolean = false,
        override val lastInteractionTime: Long = System.currentTimeMillis(),
        val textContent: String,
        val contentType: ContentType = ContentType.TEXT,
        val previewLength: Int = 50,
        val isFavorite: Boolean = false
    ) : BubbleSpec() {

        override val content: @Composable (BubbleSpec) -> Unit = { spec ->
            val textSpec = spec as TextPasteBubble
            TextPasteContent(textSpec)
        }

        override fun withKeyboardState(isKeyboardVisible: Boolean): TextPasteBubble {
            val shouldBeVisible = type.shouldBeVisible(isKeyboardVisible)
            return copy(isVisible = shouldBeVisible)
        }

        override fun withPosition(newPosition: Offset): TextPasteBubble =
            copy(position = newPosition)

        override fun withMinimized(isMinimized: Boolean): TextPasteBubble =
            copy(isMinimized = isMinimized)

        override fun withSize(newSize: Dp): TextPasteBubble =
            copy(size = newSize)

        override fun withInteraction(): TextPasteBubble =
            copy(lastInteractionTime = System.currentTimeMillis())

        enum class ContentType {
            TEXT, URL, EMAIL, PHONE_NUMBER, JSON, CODE
        }

        val displayText: String
            get() = if (textContent.length <= previewLength) {
                textContent
            } else {
                textContent.take(previewLength) + "..."
            }
    }

    companion object {
        /**
         * Factory method to create appropriate bubble spec based on type.
         */
        fun create(
            type: BubbleType,
            id: String = UUID.randomUUID().toString(),
            position: Offset = Offset.Zero,
            content: Any? = null
        ): BubbleSpec {
            return when (type) {
                BubbleType.TEXT_PASTE -> {
                    val text = content as? String ?: ""
                    TextPasteBubble(id = id, position = position, textContent = text)
                }
                BubbleType.TOOLBELT -> {
                    val tools = content as? List<*> ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    ToolbeltBubble(id = id, position = position, tools = tools as List<ToolbeltTool>)
                }
                BubbleType.PINNED_ITEM -> {
                    val text = content as? String ?: ""
                    TextPasteBubble(
                        id = id,
                        type = BubbleType.PINNED_ITEM,
                        position = position,
                        textContent = text
                    )
                }
                BubbleType.SYSTEM_NOTIFICATION -> {
                    val notificationData = content as? Map<String, Any> ?: emptyMap()
                    SystemBubble(
                        id = id,
                        position = position,
                        notificationId = notificationData["notificationId"] as? Int ?: 0,
                        title = notificationData["title"] as? String ?: "",
                        content = notificationData["content"] as? String ?: "",
                        iconResId = notificationData["iconResId"] as? Int ?: 0
                    )
                }
                BubbleType.QUICK_ACTION -> {
                    val actionData = content as? Map<String, Any> ?: emptyMap()
                    TextPasteBubble(
                        id = id,
                        type = BubbleType.QUICK_ACTION,
                        position = position,
                        textContent = actionData["text"] as? String ?: ""
                    )
                }
            }
        }
    }
}

/**
 * Represents a tool in the toolbelt.
 */
data class ToolbeltTool(
    val id: String,
    val name: String,
    val icon: Int, // Resource ID
    val action: () -> Unit,
    val isEnabled: Boolean = true,
    val priority: Int = 0
)