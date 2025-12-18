package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import com.clipboardhistory.utils.TTSManager
import com.clipboardhistory.utils.VoiceRecognitionManager

/**
 * Voice-enabled bubble that combines TTS and voice recognition capabilities.
 * Provides speech synthesis for existing content and voice transcription for new content.
 */
data class VoiceBubble(
    override val id: String,
    override val type: BubbleType = AdvancedBubbleType.VOICE_BUBBLE,
    override val position: Offset = Offset.Zero,
    override val size: Dp = type.defaultSize,
    override val isVisible: Boolean = true,
    override val isMinimized: Boolean = false,
    override val lastInteractionTime: Long = System.currentTimeMillis(),
    val textContent: String = "",
    val isTTSEnabled: Boolean = true,
    val isVoiceRecognitionEnabled: Boolean = true,
    val ttsSettings: TTSSettings = TTSSettings(),
    val voiceSettings: VoiceSettings = VoiceSettings(),
    val transcriptionHistory: List<TranscriptionEntry> = emptyList(),
    override val relevanceScore: Float = 0.9f,
    override val contextualActions: List<String> = listOf("speak", "listen", "transcribe", "settings")
) : AdvancedBubbleSpec() {

    override val content: @Composable (BubbleSpec) -> Unit = { spec ->
        val voiceSpec = spec as VoiceBubble
        VoiceBubbleContent(voiceSpec)
    }

    override fun withKeyboardState(isKeyboardVisible: Boolean): VoiceBubble =
        copy(isMinimized = isKeyboardVisible)

    override fun withPosition(newPosition: Offset): VoiceBubble = copy(position = newPosition)
    override fun withMinimized(isMinimized: Boolean): VoiceBubble = copy(isMinimized = isMinimized)
    override fun withSize(newSize: Dp): VoiceBubble = copy(size = newSize)
    override fun withInteraction(): VoiceBubble = copy(lastInteractionTime = System.currentTimeMillis())

    /**
     * Add new transcription to the history.
     */
    fun addTranscription(text: String, confidence: Float, source: String = "voice"): VoiceBubble {
        val newEntry = TranscriptionEntry(
            text = text,
            confidence = confidence,
            timestamp = System.currentTimeMillis(),
            source = source
        )
        return copy(
            transcriptionHistory = transcriptionHistory + newEntry,
            lastInteractionTime = System.currentTimeMillis()
        )
    }

    /**
     * Update TTS settings.
     */
    fun withTTSSettings(settings: TTSSettings): VoiceBubble =
        copy(ttsSettings = settings)

    /**
     * Update voice recognition settings.
     */
    fun withVoiceSettings(settings: VoiceSettings): VoiceBubble =
        copy(voiceSettings = settings)

    /**
     * Get all text content (original + transcriptions).
     */
    fun getAllContent(): String {
        val originalText = if (textContent.isNotEmpty()) textContent else ""
        val transcriptionTexts = transcriptionHistory.map { it.text }

        return (listOf(originalText) + transcriptionTexts)
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
    }

    /**
     * Check if the bubble has any content to speak.
     */
    fun hasContentToSpeak(): Boolean = getAllContent().isNotEmpty()

    /**
     * Get recent transcription entries (last N entries).
     */
    fun getRecentTranscriptions(limit: Int = 5): List<TranscriptionEntry> =
        transcriptionHistory.takeLast(limit)

    /**
     * Clear transcription history.
     */
    fun clearTranscriptionHistory(): VoiceBubble =
        copy(transcriptionHistory = emptyList())

    /**
     * Get transcription statistics.
     */
    fun getTranscriptionStats(): TranscriptionStats {
        val totalTranscriptions = transcriptionHistory.size
        val averageConfidence = if (totalTranscriptions > 0) {
            transcriptionHistory.map { it.confidence }.average().toFloat()
        } else 0f

        val highConfidenceCount = transcriptionHistory.count { it.confidence >= 0.8f }
        val lowConfidenceCount = transcriptionHistory.count { it.confidence < 0.5f }

        return TranscriptionStats(
            totalTranscriptions = totalTranscriptions,
            averageConfidence = averageConfidence,
            highConfidenceCount = highConfidenceCount,
            lowConfidenceCount = lowConfidenceCount
        )
    }

    companion object {
        /**
         * Create a new voice bubble with initial text content.
         */
        fun createWithText(text: String): VoiceBubble {
            return VoiceBubble(
                id = "voice_${System.currentTimeMillis()}_${text.hashCode()}",
                textContent = text
            )
        }

        /**
         * Create a voice bubble for voice recognition only.
         */
        fun createForVoiceRecognition(): VoiceBubble {
            return VoiceBubble(
                id = "voice_recognition_${System.currentTimeMillis()}",
                textContent = "",
                isTTSEnabled = false
            )
        }

        /**
         * Create a voice bubble for TTS only.
         */
        fun createForTTS(text: String): VoiceBubble {
            return VoiceBubble(
                id = "tts_${System.currentTimeMillis()}_${text.hashCode()}",
                textContent = text,
                isVoiceRecognitionEnabled = false
            )
        }
    }
}

/**
 * TTS settings for voice bubble.
 */
data class TTSSettings(
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val language: String = "en",
    val autoSpeakOnLongPress: Boolean = true,
    val speakTranscriptionsOnly: Boolean = false,
    val volume: Float = 1.0f
)

/**
 * Voice recognition settings for voice bubble.
 */
data class VoiceSettings(
    val language: String = "en",
    val maxResults: Int = 5,
    val enablePartialResults: Boolean = true,
    val autoCreateNewBubble: Boolean = true,
    val appendToExisting: Boolean = false,
    val confidenceThreshold: Float = 0.6f
)

/**
 * Individual transcription entry.
 */
data class TranscriptionEntry(
    val text: String,
    val confidence: Float,
    val timestamp: Long,
    val source: String = "voice"
)

/**
 * Statistics about transcription history.
 */
data class TranscriptionStats(
    val totalTranscriptions: Int,
    val averageConfidence: Float,
    val highConfidenceCount: Int,
    val lowConfidenceCount: Int
)