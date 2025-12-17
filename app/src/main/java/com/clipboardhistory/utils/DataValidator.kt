package com.clipboardhistory.utils

import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ContentType
import java.net.URL
import java.util.regex.Pattern

/**
 * Utility class for validating and sanitizing clipboard data.
 *
 * This class provides comprehensive validation and sanitization
 * for clipboard content to ensure data integrity and security.
 */
object DataValidator {

    private const val MAX_CONTENT_LENGTH = 100000 // 100KB limit
    private const val MAX_CONTENT_TYPE_LENGTH = 100
    private const val MAX_SOURCE_APP_LENGTH = 200

    // Common patterns for validation
    private val URL_PATTERN = Pattern.compile(
        "^https?://[\\w.-]+(?:\\.[\\w.-]+)+[\\w.,@?^=%&:/~+#-]*$",
        Pattern.CASE_INSENSITIVE
    )

    private val EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    )

    private val PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$|^\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}$"
    )

    private val JSON_PATTERN = Pattern.compile(
        "^\\s*[{\\[]"
    )

    /**
     * Validate and sanitize a clipboard item.
     *
     * @param item The clipboard item to validate
     * @return ValidationResult containing sanitized item or validation errors
     */
    fun validateAndSanitize(item: ClipboardItem): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Validate and sanitize ID
        val sanitizedId = validateId(item.id)
        if (sanitizedId == null) {
            errors.add(ValidationError("id", "Invalid ID format"))
        }

        // Validate and sanitize content
        val sanitizedContent = validateContent(item.content)
        if (sanitizedContent == null) {
            errors.add(ValidationError("content", "Content validation failed"))
        }

        // Validate content type
        val sanitizedContentType = validateContentType(item.contentType.name)
        if (sanitizedContentType == null) {
            errors.add(ValidationError("contentType", "Invalid content type"))
        }

        // Validate timestamp
        val sanitizedTimestamp = validateTimestamp(item.timestamp)
        if (sanitizedTimestamp == null) {
            errors.add(ValidationError("timestamp", "Invalid timestamp"))
        }

        // Sanitize optional fields
        val sanitizedSourceApp = item.sourceApp?.let { validateSourceApp(it) }
        val sanitizedEncryptionKey = item.encryptionKey?.let { validateEncryptionKey(it) }

        return if (errors.isEmpty()) {
            ValidationResult.Success(
                ClipboardItem(
                    id = sanitizedId!!,
                    content = sanitizedContent!!,
                    contentType = ContentType.valueOf(sanitizedContentType!!),
                    timestamp = sanitizedTimestamp!!,
                    size = sanitizedContent!!.length,
                    sourceApp = sanitizedSourceApp,
                    isFavorite = item.isFavorite,
                    isDeleted = item.isDeleted,
                    encryptionKey = sanitizedEncryptionKey,
                )
            )
        } else {
            ValidationResult.Failure(errors)
        }
    }

    /**
     * Validate clipboard content string.
     *
     * @param content The content to validate
     * @return Sanitized content or null if invalid
     */
    fun validateContent(content: String): String? {
        // Check length
        if (content.length > MAX_CONTENT_LENGTH) {
            return null
        }

        // Basic sanitization - remove null bytes and other problematic characters
        val sanitized = content
            .replace("\u0000", "") // Remove null bytes
            .replace("\r\n", "\n") // Normalize line endings
            .replace("\r", "\n")   // Normalize line endings

        // Check for potentially malicious content
        if (containsMaliciousPatterns(sanitized)) {
            return null
        }

        return sanitized
    }

    /**
     * Validate content type string.
     *
     * @param contentType The content type to validate
     * @return Sanitized content type or null if invalid
     */
    fun validateContentType(contentType: String): String? {
        if (contentType.length > MAX_CONTENT_TYPE_LENGTH) {
            return null
        }

        // Basic MIME type validation
        val mimePattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*/[a-zA-Z][a-zA-Z0-9+.-]*$")
        return if (mimePattern.matcher(contentType).matches()) {
            contentType.lowercase()
        } else {
            null
        }
    }

    /**
     * Validate ID (Long type).
     *
     * @param id The ID to validate
     * @return Validated ID or null if invalid
     */
    fun validateId(id: String): String? {
        return if (id.isNotBlank()) id else null
    }

    /**
     * Validate timestamp.
     *
     * @param timestamp The timestamp to validate
     * @return Validated timestamp or null if invalid
     */
    fun validateTimestamp(timestamp: Long): Long? {
        val currentTime = System.currentTimeMillis()
        val minTime = 0L // Unix epoch
        val maxTime = currentTime + (365 * 24 * 60 * 60 * 1000L) // 1 year in future

        return if (timestamp in minTime..maxTime) timestamp else null
    }

    /**
     * Validate source app string.
     *
     * @param sourceApp The source app to validate
     * @return Sanitized source app or null if invalid
     */
    fun validateSourceApp(sourceApp: String): String? {
        if (sourceApp.length > MAX_SOURCE_APP_LENGTH) {
            return null
        }

        // Basic package name validation
        val packagePattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9._-]*[a-zA-Z0-9]$")
        return if (packagePattern.matcher(sourceApp).matches()) {
            sourceApp
        } else {
            null
        }
    }

    /**
     * Validate encryption key.
     *
     * @param encryptionKey The encryption key to validate
     * @return Validated encryption key or null if invalid
     */
    fun validateEncryptionKey(encryptionKey: String): String? {
        // Encryption keys should be reasonable length and contain valid characters
        return if (encryptionKey.length in 8..256 && encryptionKey.all { it.isLetterOrDigit() || it in "._-" }) {
            encryptionKey
        } else {
            null
        }
    }

    /**
     * Validate URL format.
     *
     * @param url The URL string to validate
     * @return true if valid URL format
     */
    fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            URL_PATTERN.matcher(url).matches()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validate email format.
     *
     * @param email The email string to validate
     * @return true if valid email format
     */
    fun isValidEmail(email: String): Boolean {
        return EMAIL_PATTERN.matcher(email).matches()
    }

    /**
     * Validate phone number format.
     *
     * @param phone The phone number string to validate
     * @return true if valid phone format
     */
    fun isValidPhone(phone: String): Boolean {
        return PHONE_PATTERN.matcher(phone).matches()
    }

    /**
     * Validate JSON format.
     *
     * @param json The JSON string to validate
     * @return true if valid JSON format
     */
    fun isValidJson(json: String): Boolean {
        if (!JSON_PATTERN.matcher(json.trim()).find()) return false

        return try {
            // Basic JSON validation - could be enhanced with proper JSON parsing
            val trimmed = json.trim()
            var braceCount = 0
            var bracketCount = 0
            var inString = false
            var escaped = false

            for (char in trimmed) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    inString -> if (char == '"') inString = false
                    char == '"' -> inString = true
                    !inString -> {
                        when (char) {
                            '{' -> braceCount++
                            '}' -> braceCount--
                            '[' -> bracketCount++
                            ']' -> bracketCount--
                        }
                        if (braceCount < 0 || bracketCount < 0) return false
                    }
                }
            }

            braceCount == 0 && bracketCount == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check for potentially malicious patterns in content.
     *
     * @param content The content to check
     * @return true if malicious patterns detected
     */
    private fun containsMaliciousPatterns(content: String): Boolean {
        val maliciousPatterns = listOf(
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("data:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<iframe", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<object", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<embed", Pattern.CASE_INSENSITIVE),
        )

        return maliciousPatterns.any { it.matcher(content).find() }
    }

    /**
     * Sanitize text content by removing or escaping potentially dangerous characters.
     *
     * @param content The content to sanitize
     * @return Sanitized content
     */
    fun sanitizeTextContent(content: String): String {
        return content
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("&", "&amp;")
    }

    /**
     * Validate content size for storage constraints.
     *
     * @param content The content to check
     * @return true if content size is acceptable
     */
    fun isValidContentSize(content: String): Boolean {
        return content.length <= MAX_CONTENT_LENGTH
    }

    /**
     * Result of validation operation.
     */
    sealed class ValidationResult {
        data class Success(val sanitizedItem: ClipboardItem) : ValidationResult()
        data class Failure(val errors: List<ValidationError>) : ValidationResult()
    }

    /**
     * Represents a validation error.
     */
    data class ValidationError(
        val field: String,
        val message: String,
    )
}