package com.clipboardhistory.domain.model

/**
 * Content analyzer to provide smart actions based on content type heuristics.
 *
 * Analyzes clipboard content and determines the appropriate type and smart actions.
 */
object ContentAnalyzer {
    /** Content type enumeration */
    enum class Type { URL, PHONE, EMAIL, MAPS, TEXT }

    // Phone number regex pattern
    // Matches: +1234567890, 123-456-7890, +1 (555) 123-4567, +44 20 7946 0958, 555.123.4567
    // Requires at least 7 digits to be considered a phone number
    private val PHONE_PATTERN = Regex("^\\+?[\\d\\s()\\-.]+$")

    /**
     * Analyzes content and returns its type.
     *
     * @param content The content to analyze
     * @return The detected content type
     */
    fun analyzeContentType(content: String): Type {
        val trimmed = content.trim()

        // Empty or whitespace-only content is TEXT
        if (trimmed.isEmpty()) return Type.TEXT

        return when {
            // URL detection: starts with http:// or https://
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> Type.URL

            // Email detection: standard email format
            Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matches(trimmed) -> Type.EMAIL

            // Phone detection: matches phone pattern AND has enough digits
            isPhoneNumber(trimmed) -> Type.PHONE

            // Maps detection: short-medium text with spaces (likely address)
            trimmed.length in 3..100 && trimmed.contains(" ") -> Type.MAPS

            // Default to TEXT
            else -> Type.TEXT
        }
    }

    /**
     * Checks if the content is a phone number.
     *
     * A phone number must:
     * - Match the phone pattern (optional +, digits, spaces, parentheses, dashes)
     * - Contain at least 7 actual digits
     *
     * @param content The content to check
     * @return true if the content is a phone number
     */
    private fun isPhoneNumber(content: String): Boolean {
        // First check if it matches the general phone pattern
        if (!PHONE_PATTERN.matches(content)) return false

        // Count actual digits - must have at least 7
        val digitCount = content.count { it.isDigit() }
        return digitCount >= 7
    }

    fun getSmartActions(
        type: Type,
        content: String,
    ): List<SmartAction> {
        return when (type) {
            Type.URL -> listOf(SmartAction("Open Link", BubbleState.REPLACE))
            Type.PHONE -> listOf(SmartAction("Call Number", BubbleState.REPLACE))
            Type.EMAIL -> listOf(SmartAction("Send Email", BubbleState.REPLACE))
            Type.MAPS -> listOf(SmartAction("Open Maps", BubbleState.REPLACE))
            Type.TEXT -> listOf(SmartAction("Search Text", BubbleState.REPLACE))
        }
    }
}
