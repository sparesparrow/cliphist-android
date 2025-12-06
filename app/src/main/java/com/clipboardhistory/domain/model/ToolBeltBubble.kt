package com.clipboardhistory.domain.model

import java.util.*

/**
 * Represents a special bubble type used in the ToolBelt widget.
 * ToolBelt bubbles provide quick access to common tools and operations.
 *
 * @param id Unique identifier for the bubble
 * @param type The type of tool this bubble represents
 * @param content The content/label for the bubble
 * @param bubbleType The visual shape of the bubble
 * @param isPrivate Whether this bubble is private
 * @param isVisible Whether this bubble is visible in the ToolBelt
 * @param operationMode The default operation when this bubble is used
 * @param position The position in the ToolBelt (0-indexed)
 * @param createdAt When this bubble was created
 * @param updatedAt When this bubble was last updated
 */
data class ToolBeltBubble(
    val id: String = UUID.randomUUID().toString(),
    val type: ToolBeltBubbleType = ToolBeltBubbleType.GENERIC,
    val content: String = "",
    val bubbleType: BubbleType = BubbleType.CIRCLE,
    val isPrivate: Boolean = false,
    val isVisible: Boolean = true,
    val operationMode: OperationMode = OperationMode.OVERWRITE,
    val position: Int = 0,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) {
    /**
     * Creates a copy of this bubble with updated properties.
     */
    fun copy(
        id: String = this.id,
        type: ToolBeltBubbleType = this.type,
        content: String = this.content,
        bubbleType: BubbleType = this.bubbleType,
        isPrivate: Boolean = this.isPrivate,
        isVisible: Boolean = this.isVisible,
        operationMode: OperationMode = this.operationMode,
        position: Int = this.position,
        createdAt: Date = this.createdAt,
        updatedAt: Date = Date(),
    ): ToolBeltBubble =
        ToolBeltBubble(
            id = id,
            type = type,
            content = content,
            bubbleType = bubbleType,
            isPrivate = isPrivate,
            isVisible = isVisible,
            operationMode = operationMode,
            position = position,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}

/**
 * Enumeration of different ToolBelt bubble types.
 */
enum class ToolBeltBubbleType {
    /** Generic/custom tool bubble */
    GENERIC,

    /** Opacity/transparency slider */
    OPACITY_SLIDER,

    /** Toggle for private bubbles visibility */
    PRIVATE_VISIBILITY_TOGGLE,

    /** Toggle for clipboard history visibility */
    HISTORY_VISIBILITY_TOGGLE,

    /** Bubble type changer */
    BUBBLE_TYPE_CHANGER,

    /** Bubble content editor */
    CONTENT_EDITOR,

    /** Operation mode switcher (overwrite, append, prepend) */
    OPERATION_MODE_SWITCHER,

    /** Clear all action */
    CLEAR_ALL,

    /** Settings access */
    SETTINGS,
}

/**
 * Enumeration of operation modes for clipboard operations.
 */
enum class OperationMode {
    /** Replace the clipboard content entirely */
    OVERWRITE,

    /** Append to the end of clipboard content */
    APPEND,

    /** Prepend to the beginning of clipboard content */
    PREPEND,
}
