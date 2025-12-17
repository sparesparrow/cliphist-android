package com.clipboardhistory.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.clipboardhistory.domain.model.ContentAnalyzer
import com.clipboardhistory.domain.model.SmartAction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Use case for executing smart actions based on clipboard content.
 *
 * This use case analyzes clipboard content and provides appropriate actions
 * that can be executed, such as opening URLs, making calls, sending emails, etc.
 */
class ExecuteSmartActionUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Execute a smart action for the given clipboard content.
     *
     * @param content The clipboard content
     * @param contentType The MIME type of the content
     * @param action The action to execute
     * @return SmartActionResult indicating success or failure
     */
    suspend operator fun invoke(
        content: String,
        contentType: String,
        action: SmartAction,
    ): SmartActionResult {
        return try {
            when (action.type) {
                SmartAction.ActionType.OPEN_LINK -> executeOpenLink(content)
                SmartAction.ActionType.CALL_NUMBER -> executeCallNumber(content)
                SmartAction.ActionType.SEND_EMAIL -> executeSendEmail(content)
                SmartAction.ActionType.OPEN_MAPS -> executeOpenMaps(content)
                SmartAction.ActionType.SEARCH_WEB -> executeSearchWeb(content)
                SmartAction.ActionType.SEND_SMS -> executeSendSms(content)
                SmartAction.ActionType.ADD_CONTACT -> executeAddContact(content)
                SmartAction.ActionType.OPEN_APP -> executeOpenApp(content)
                SmartAction.ActionType.COPY_TO_CLIPBOARD -> executeCopyToClipboard(content)
                SmartAction.ActionType.SHARE_CONTENT -> executeShareContent(content, contentType)
                SmartAction.ActionType.FORMAT_JSON -> executeFormatJson(content)
                SmartAction.ActionType.VALIDATE_JSON -> executeValidateJson(content)
                SmartAction.ActionType.CONNECT_WIFI -> executeConnectWifi(content)
                SmartAction.ActionType.MASK_CARD -> executeMaskCard(content)
                SmartAction.ActionType.OPEN_CALENDAR -> executeOpenCalendar(content)
                SmartAction.ActionType.OPEN_CONTACT -> executeOpenContact(content)
                else -> SmartActionResult.Failure("Unknown action type: ${action.type}")
            }
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to execute action: ${e.message}")
        }
    }

    /**
     * Get available smart actions for the given content.
     *
     * @param content The clipboard content
     * @param contentType The MIME type of the content
     * @return List of available smart actions
     */
    fun getAvailableActions(content: String, contentType: String): List<SmartAction> {
        val type = ContentAnalyzer.analyzeContentType(content)
        return ContentAnalyzer.getSmartActions(type, content)
    }

    /**
     * Suggest the most relevant action for the given content.
     *
     * @param content The clipboard content
     * @param contentType The MIME type of the content
     * @return The most relevant smart action, or null if none available
     */
    fun suggestPrimaryAction(content: String, contentType: String): SmartAction? {
        val actions = getAvailableActions(content, contentType)
        return actions.firstOrNull()
    }

    // Private action execution methods

    private fun executeOpenLink(url: String): SmartActionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            SmartActionResult.Success("Opened link in browser")
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to open link: ${e.message}")
        }
    }

    private fun executeCallNumber(phoneNumber: String): SmartActionResult {
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            SmartActionResult.Success("Opened dialer with number")
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to call number: ${e.message}")
        }
    }

    private fun executeSendEmail(email: String): SmartActionResult {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            SmartActionResult.Success("Opened email client")
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to send email: ${e.message}")
        }
    }

    private fun executeOpenMaps(address: String): SmartActionResult {
        return try {
            val encodedAddress = Uri.encode(address)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encodedAddress")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            SmartActionResult.Success("Opened maps with address")
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to open maps: ${e.message}")
        }
    }

    private fun executeSearchWeb(query: String): SmartActionResult {
        return try {
            val encodedQuery = Uri.encode(query)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encodedQuery")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            SmartActionResult.Success("Searched web for content")
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to search web: ${e.message}")
        }
    }

    private fun executeSendSms(phoneNumber: String): SmartActionResult {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            SmartActionResult.Success("Opened SMS app")
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to send SMS: ${e.message}")
        }
    }

    private fun executeAddContact(phoneNumber: String): SmartActionResult {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                type = "vnd.android.cursor.dir/contact"
                putExtra("phone", phoneNumber)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            SmartActionResult.Success("Opened contacts to add number")
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to add contact: ${e.message}")
        }
    }

    private fun executeOpenApp(packageName: String?): SmartActionResult {
        return try {
            if (packageName.isNullOrBlank()) {
                return SmartActionResult.Failure("No package name available")
            }

            val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (intent != null) {
                context.startActivity(intent)
                SmartActionResult.Success("Opened application")
            } else {
                SmartActionResult.Failure("Application not found")
            }
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to open app: ${e.message}")
        }
    }

    private fun executeCopyToClipboard(content: String): SmartActionResult {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("clipboard", content)
            clipboard.setPrimaryClip(clip)
            SmartActionResult.Success("Copied to clipboard")
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to copy: ${e.message}")
        }
    }

    private fun executeShareContent(content: String, contentType: String): SmartActionResult {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when {
                    contentType.startsWith("text/") -> "text/plain"
                    contentType.startsWith("image/") -> "image/*"
                    else -> "*/*"
                }
                putExtra(Intent.EXTRA_TEXT, content)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
            SmartActionResult.Success("Opened share dialog")
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to share: ${e.message}")
        }
    }

    private fun executeFormatJson(json: String): SmartActionResult {
        return try {
            val formattedJson = formatJsonString(json)
            executeCopyToClipboard(formattedJson)
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to format JSON: ${e.message}")
        }
    }

    private fun executeValidateJson(json: String): SmartActionResult {
        return try {
            // Simple JSON validation
            if (isValidJson(json)) {
                SmartActionResult.Success("JSON is valid")
            } else {
                SmartActionResult.Failure("JSON is invalid")
            }
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to validate JSON: ${e.message}")
        }
    }

    private fun executeConnectWifi(wifiCredentials: String): SmartActionResult {
        return try {
            // This would require system-level permissions and WiFi management
            // For now, just show the WiFi settings
            val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            SmartActionResult.Success("Opened WiFi settings")
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to open WiFi settings: ${e.message}")
        }
    }

    private fun executeMaskCard(cardNumber: String): SmartActionResult {
        return try {
            val masked = maskCreditCard(cardNumber)
            executeCopyToClipboard(masked)
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to mask card: ${e.message}")
        }
    }

    private fun executeOpenCalendar(eventInfo: String): SmartActionResult {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                type = "vnd.android.cursor.item/event"
                putExtra("title", "Clipboard Event")
                putExtra("description", eventInfo)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            SmartActionResult.Success("Opened calendar")
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to open calendar: ${e.message}")
        }
    }

    private fun executeOpenContact(contactInfo: String): SmartActionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "vnd.android.cursor.dir/contact"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            SmartActionResult.Success("Opened contacts")
        } catch (e: Exception) {
            SmartActionResult.Failure("Failed to open contacts: ${e.message}")
        }
    }

    // Utility methods

    private fun formatJsonString(json: String): String {
        // Simple JSON formatting - could be enhanced with proper JSON library
        return json.replace(",", ",\n")
            .replace("{", "{\n")
            .replace("}", "\n}")
            .replace("[", "[\n")
            .replace("]", "\n]")
    }

    private fun isValidJson(json: String): Boolean {
        return try {
            // Basic JSON validation - could be enhanced
            val trimmed = json.trim()
            (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))
        } catch (e: Exception) {
            false
        }
    }

    private fun maskCreditCard(cardNumber: String): String {
        val cleaned = cardNumber.replace(Regex("[^0-9]"), "")
        return if (cleaned.length >= 4) {
            "**** **** **** ${cleaned.takeLast(4)}"
        } else {
            cardNumber
        }
    }

    /**
     * Result of smart action execution.
     */
    sealed class SmartActionResult {
        data class Success(val message: String) : SmartActionResult()
        data class Failure(val error: String) : SmartActionResult()
    }
}