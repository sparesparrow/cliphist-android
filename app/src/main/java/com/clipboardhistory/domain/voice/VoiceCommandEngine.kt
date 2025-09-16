package com.clipboardhistory.domain.voice

/**
 * Orchestrates intent proposals and safe execution for voice commands.
 * Implementations should remain UI-agnostic and side-effect minimal.
 */
interface VoiceCommandEngine {
    suspend fun proposeCommands(transcript: String, context: VoiceContext): List<VoiceCommand>
    suspend fun execute(command: VoiceCommand, context: VoiceContext): VoiceCommandResult
}

