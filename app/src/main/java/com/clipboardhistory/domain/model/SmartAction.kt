package com.clipboardhistory.domain.model

/**
 * Model representing a smart action suggestion for clipboard content.
 */
data class SmartAction(
    val label: String,
    val type: ActionType,
) {
    /**
     * Enumeration of supported smart action types.
     */
    enum class ActionType {
        OPEN_LINK,
        CALL_NUMBER,
        SEND_EMAIL,
        OPEN_MAPS,
        SEARCH_WEB,
        SEND_SMS,
        ADD_CONTACT,
        OPEN_APP,
        COPY_TO_CLIPBOARD,
        SHARE_CONTENT,
        FORMAT_JSON,
        VALIDATE_JSON,
        CONNECT_WIFI,
        MASK_CARD,
        OPEN_CALENDAR,
        OPEN_CONTACT,
        CUSTOM,
    }
}
