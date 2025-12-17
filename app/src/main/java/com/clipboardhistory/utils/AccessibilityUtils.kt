package com.clipboardhistory.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * Accessibility utilities for ClipHist UI components.
 *
 * Provides semantic properties and accessibility helpers to ensure
 * the app is usable by people with disabilities.
 */
object AccessibilityUtils {

    /**
     * Creates accessible content description for clipboard items.
     */
    fun getClipboardItemDescription(
        content: String,
        contentType: String,
        timestamp: Long,
        isFavorite: Boolean = false,
    ): String {
        val typeDescription = when (contentType) {
            "text/plain" -> "plain text"
            "text/url" -> "web link"
            "text/email" -> "email address"
            "text/phone" -> "phone number"
            "text/address" -> "address"
            "application/json" -> "JSON data"
            "text/credit-card" -> "credit card"
            else -> "content"
        }

        val timeAgo = formatTimeAgo(timestamp)
        val favoriteText = if (isFavorite) ", favorited" else ""

        val preview = content.take(50).let {
            if (it.length < content.length) "$it..." else it
        }

        return "$typeDescription: $preview, copied $timeAgo$favoriteText"
    }

    /**
     * Creates accessible description for action buttons.
     */
    fun getActionButtonDescription(
        actionName: String,
        itemContent: String,
        contentType: String,
    ): String {
        val typeDescription = when (contentType) {
            "text/url" -> "web link"
            "text/email" -> "email address"
            "text/phone" -> "phone number"
            else -> "content"
        }

        return "$actionName for $typeDescription: ${itemContent.take(30)}"
    }

    /**
     * Creates accessible description for permission settings.
     */
    fun getPermissionDescription(
        permissionName: String,
        isGranted: Boolean,
        description: String,
    ): String {
        val status = if (isGranted) "granted" else "not granted"
        return "$permissionName permission, $status. $description"
    }

    /**
     * Creates accessible description for statistics cards.
     */
    fun getStatisticsDescription(
        title: String,
        value: String,
        additionalInfo: String? = null,
    ): String {
        val baseDescription = "$title: $value"
        return if (additionalInfo != null) {
            "$baseDescription. $additionalInfo"
        } else {
            baseDescription
        }
    }

    /**
     * Creates accessible description for navigation buttons.
     */
    fun getNavigationDescription(
        destination: String,
        context: String? = null,
    ): String {
        return if (context != null) {
            "Navigate to $destination screen from $context"
        } else {
            "Navigate to $destination screen"
        }
    }

    /**
     * Creates accessible state description for toggleable items.
     */
    fun getToggleStateDescription(
        itemName: String,
        isEnabled: Boolean,
    ): String {
        val state = if (isEnabled) "enabled" else "disabled"
        return "$itemName is $state"
    }

    /**
     * Creates accessible progress description for loading states.
     */
    fun getLoadingDescription(
        operation: String,
    ): String {
        return "Loading $operation, please wait"
    }

    /**
     * Creates accessible error description.
     */
    fun getErrorDescription(
        error: String,
        suggestion: String? = null,
    ): String {
        val baseDescription = "Error: $error"
        return if (suggestion != null) {
            "$baseDescription. Suggestion: $suggestion"
        } else {
            baseDescription
        }
    }

    /**
     * Creates accessible success description.
     */
    fun getSuccessDescription(
        operation: String,
        result: String? = null,
    ): String {
        val baseDescription = "$operation completed successfully"
        return if (result != null) {
            "$baseDescription: $result"
        } else {
            baseDescription
        }
    }

    /**
     * Creates accessible search result description.
     */
    fun getSearchResultDescription(
        query: String,
        resultCount: Int,
        totalCount: Int,
    ): String {
        return when {
            resultCount == 0 -> "No results found for search: $query"
            resultCount == 1 -> "1 result found for search: $query"
            resultCount < totalCount -> "$resultCount of $totalCount results shown for search: $query"
            else -> "$resultCount results found for search: $query"
        }
    }

    /**
     * Semantic properties for interactive elements.
     */
    object Semantics {
        val Button: androidx.compose.ui.semantics.SemanticsPropertyReceiver.() -> Unit = {
            role = Role.Button
        }

        val ToggleButton: androidx.compose.ui.semantics.SemanticsPropertyReceiver.() -> Unit = {
            role = Role.Switch
        }

        val Card: androidx.compose.ui.semantics.SemanticsPropertyReceiver.() -> Unit = {
            role = Role.Button
        }
    }

    /**
     * Accessibility roles for different UI elements.
     */
    enum class AccessibilityRole {
        BUTTON,
        TOGGLE,
        CARD,
        LIST_ITEM,
        HEADER,
        TEXT_FIELD,
        IMAGE,
        PROGRESS_BAR,
    }

    /**
     * Creates semantic properties for clipboard items in lists.
     */
    fun clipboardItemSemantics(
        content: String,
        contentType: String,
        timestamp: Long,
        isFavorite: Boolean = false,
        isSelected: Boolean = false,
    ): androidx.compose.ui.semantics.SemanticsPropertyReceiver.() -> Unit = {
        contentDescription = getClipboardItemDescription(content, contentType, timestamp, isFavorite)
        if (isSelected) {
            stateDescription = "selected"
        }
        role = Role.Button
    }

    /**
     * Creates semantic properties for action buttons.
     */
    fun actionButtonSemantics(
        actionName: String,
        itemContent: String,
        contentType: String,
    ): androidx.compose.ui.semantics.SemanticsPropertyReceiver.() -> Unit = {
        contentDescription = getActionButtonDescription(actionName, itemContent, contentType)
        role = Role.Button
    }

    /**
     * Creates semantic properties for navigation elements.
     */
    fun navigationSemantics(
        destination: String,
        context: String? = null,
    ): androidx.compose.ui.semantics.SemanticsPropertyReceiver.() -> Unit = {
        contentDescription = getNavigationDescription(destination, context)
        role = Role.Button
    }

    /**
     * Creates semantic properties for toggle elements.
     */
    fun toggleSemantics(
        itemName: String,
        isEnabled: Boolean,
    ): androidx.compose.ui.semantics.SemanticsPropertyReceiver.() -> Unit = {
        contentDescription = "$itemName toggle"
        stateDescription = getToggleStateDescription(itemName, isEnabled)
        role = Role.Switch
    }

    /**
     * Creates semantic properties for loading states.
     */
    fun loadingSemantics(
        operation: String,
    ): androidx.compose.ui.semantics.SemanticsPropertyReceiver.() -> Unit = {
        contentDescription = getLoadingDescription(operation)
        // Note: Progress bars would have additional live region properties
    }

    /**
     * Creates semantic properties for error states.
     */
    fun errorSemantics(
        error: String,
        suggestion: String? = null,
    ): androidx.compose.ui.semantics.SemanticsPropertyReceiver.() -> Unit = {
        contentDescription = getErrorDescription(error, suggestion)
        // Error messages should be marked as live regions for screen readers
    }
}

/**
 * Extension functions for easier accessibility usage.
 */
fun Modifier.clipboardItemAccessibility(
    content: String,
    contentType: String,
    timestamp: Long,
    isFavorite: Boolean = false,
    isSelected: Boolean = false,
) = semantics {
    with(AccessibilityUtils) {
        clipboardItemSemantics(content, contentType, timestamp, isFavorite, isSelected)()
    }
}

fun Modifier.actionButtonAccessibility(
    actionName: String,
    itemContent: String,
    contentType: String,
) = semantics {
    with(AccessibilityUtils) {
        actionButtonSemantics(actionName, itemContent, contentType)()
    }
}

fun Modifier.navigationAccessibility(
    destination: String,
    context: String? = null,
) = semantics {
    with(AccessibilityUtils) {
        navigationSemantics(destination, context)()
    }
}

fun Modifier.toggleAccessibility(
    itemName: String,
    isEnabled: Boolean,
) = semantics {
    with(AccessibilityUtils) {
        toggleSemantics(itemName, isEnabled)()
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
        else -> "${diff / (24 * 60 * 60 * 1000)} days ago"
    }
}