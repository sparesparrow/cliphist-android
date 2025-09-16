package com.clipboardhistory.domain.voice

/**
 * Command contract for voice-controlled actions in the clipboard domain.
 */
sealed interface VoiceCommand {
    data object CopyLatest : VoiceCommand
    data class Search(val query: String) : VoiceCommand
    data class PinItem(val itemId: String) : VoiceCommand
    data object ClearHistory : VoiceCommand
    data class ToggleEncryption(val enable: Boolean) : VoiceCommand
    data object OpenBubble : VoiceCommand
    data object StartServices : VoiceCommand
    data object StopServices : VoiceCommand
}

sealed class VoiceCommandResult {
    data object Success : VoiceCommandResult()
    data class Rejected(val reason: String) : VoiceCommandResult()
    data class Error(val message: String, val cause: Throwable? = null) : VoiceCommandResult()
}

