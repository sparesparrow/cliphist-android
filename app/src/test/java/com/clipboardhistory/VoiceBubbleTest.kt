package com.clipboardhistory

import androidx.compose.ui.geometry.Offset
import com.clipboardhistory.presentation.ui.bubble.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceBubbleTest {

    private val defaultPosition = Offset(100f, 200f)
    private val sampleText = "Hello world"

    @Test
    fun `createWithText creates bubble with correct initial state`() {
        val bubble = VoiceBubble.createWithText(sampleText)

        assertEquals(sampleText, bubble.textContent)
        assertTrue(bubble.isTTSEnabled)
        assertTrue(bubble.isVoiceRecognitionEnabled)
        assertTrue(bubble.isVisible)
        assertFalse(bubble.isMinimized)
        assertTrue(bubble.transcriptionHistory.isEmpty())
        assertTrue(bubble.hasContentToSpeak())
    }

    @Test
    fun `createForVoiceRecognition creates bubble optimized for voice input`() {
        val bubble = VoiceBubble.createForVoiceRecognition()

        assertEquals("", bubble.textContent)
        assertFalse(bubble.isTTSEnabled)
        assertTrue(bubble.isVoiceRecognitionEnabled)
        assertFalse(bubble.hasContentToSpeak())
    }

    @Test
    fun `createForTTS creates bubble optimized for speech output`() {
        val bubble = VoiceBubble.createForTTS(sampleText)

        assertEquals(sampleText, bubble.textContent)
        assertTrue(bubble.isTTSEnabled)
        assertFalse(bubble.isVoiceRecognitionEnabled)
        assertTrue(bubble.hasContentToSpeak())
    }

    @Test
    fun `addTranscription adds entry to history`() {
        val bubble = VoiceBubble.createWithText(sampleText)
        val transcription = "This is a test transcription"
        val confidence = 0.85f

        val updated = bubble.addTranscription(transcription, confidence, "voice")

        assertEquals(1, updated.transcriptionHistory.size)
        assertEquals(transcription, updated.transcriptionHistory[0].text)
        assertEquals(confidence, updated.transcriptionHistory[0].confidence)
        assertEquals("voice", updated.transcriptionHistory[0].source)
    }

    @Test
    fun `getAllContent combines original text and transcriptions`() {
        val bubble = VoiceBubble.createWithText("Original text")
        val updated = bubble
            .addTranscription("First transcription", 0.9f)
            .addTranscription("Second transcription", 0.8f)

        val allContent = updated.getAllContent()
        assertTrue(allContent.contains("Original text"))
        assertTrue(allContent.contains("First transcription"))
        assertTrue(allContent.contains("Second transcription"))
    }

    @Test
    fun `getAllContent separates content with newlines`() {
        val bubble = VoiceBubble.createWithText("Line 1")
            .addTranscription("Line 2", 1.0f)

        val allContent = bubble.getAllContent()
        assertEquals("Line 1\n\nLine 2", allContent)
    }

    @Test
    fun `hasContentToSpeak returns true when original text exists`() {
        val bubble = VoiceBubble.createWithText(sampleText)
        assertTrue(bubble.hasContentToSpeak())
    }

    @Test
    fun `hasContentToSpeak returns true when transcriptions exist`() {
        val bubble = VoiceBubble.createForVoiceRecognition()
            .addTranscription(sampleText, 1.0f)

        assertTrue(bubble.hasContentToSpeak())
    }

    @Test
    fun `hasContentToSpeak returns false when no content exists`() {
        val bubble = VoiceBubble.createForVoiceRecognition()
        assertFalse(bubble.hasContentToSpeak())
    }

    @Test
    fun `getRecentTranscriptions returns limited results in reverse order`() {
        val bubble = VoiceBubble.createWithText("")
        val updated = bubble
            .addTranscription("First", 1.0f)
            .addTranscription("Second", 0.9f)
            .addTranscription("Third", 0.8f)
            .addTranscription("Fourth", 0.7f)
            .addTranscription("Fifth", 0.6f)

        val recent = updated.getRecentTranscriptions(3)
        assertEquals(3, recent.size)
        assertEquals("Fifth", recent[0].text) // Most recent first
        assertEquals("Fourth", recent[1].text)
        assertEquals("Third", recent[2].text)
    }

    @Test
    fun `clearTranscriptionHistory removes all transcriptions`() {
        val bubble = VoiceBubble.createWithText("")
            .addTranscription("Test 1", 1.0f)
            .addTranscription("Test 2", 0.9f)

        assertEquals(2, bubble.transcriptionHistory.size)

        val cleared = bubble.clearTranscriptionHistory()
        assertEquals(0, cleared.transcriptionHistory.size)
    }

    @Test
    fun `withTTSSettings updates TTS configuration`() {
        val bubble = VoiceBubble.createWithText(sampleText)
        val newSettings = TTSSettings(
            speechRate = 1.5f,
            pitch = 1.2f,
            language = "es",
            autoSpeakOnLongPress = false
        )

        val updated = bubble.withTTSSettings(newSettings)
        assertEquals(newSettings, updated.ttsSettings)
    }

    @Test
    fun `withVoiceSettings updates voice recognition configuration`() {
        val bubble = VoiceBubble.createWithText(sampleText)
        val newSettings = VoiceSettings(
            language = "fr",
            maxResults = 8,
            enablePartialResults = false,
            confidenceThreshold = 0.8f
        )

        val updated = bubble.withVoiceSettings(newSettings)
        assertEquals(newSettings, updated.voiceSettings)
    }

    @Test
    fun `keyboard visibility changes minimize state correctly`() {
        val bubble = VoiceBubble.createWithText(sampleText)

        // Should minimize when keyboard appears
        val minimized = bubble.withKeyboardState(true)
        assertTrue(minimized.isMinimized)

        // Should expand when keyboard disappears
        val expanded = minimized.withKeyboardState(false)
        assertFalse(expanded.isMinimized)
    }

    @Test
    fun `getTranscriptionStats calculates correct statistics`() {
        val bubble = VoiceBubble.createWithText("")
            .addTranscription("High confidence", 0.95f)
            .addTranscription("Medium confidence", 0.75f)
            .addTranscription("Low confidence", 0.45f)
            .addTranscription("Very low confidence", 0.25f)

        val stats = bubble.getTranscriptionStats()

        assertEquals(4, stats.totalTranscriptions)
        assertEquals(0.6f, stats.averageConfidence, 0.01f)
        assertEquals(2, stats.highConfidenceCount) // >= 0.8
        assertEquals(2, stats.lowConfidenceCount)  // < 0.5
    }

    @Test
    fun `getTranscriptionStats returns zeros for empty history`() {
        val bubble = VoiceBubble.createWithText("")
        val stats = bubble.getTranscriptionStats()

        assertEquals(0, stats.totalTranscriptions)
        assertEquals(0f, stats.averageConfidence)
        assertEquals(0, stats.highConfidenceCount)
        assertEquals(0, stats.lowConfidenceCount)
    }

    @Test
    fun `bubble properties are correctly copied in withPosition`() {
        val bubble = VoiceBubble.createWithText(sampleText)
        val newPosition = Offset(300f, 400f)

        val updated = bubble.withPosition(newPosition)
        assertEquals(newPosition, updated.position)
        assertEquals(sampleText, updated.textContent) // Other properties unchanged
    }

    @Test
    fun `bubble properties are correctly copied in withMinimized`() {
        val bubble = VoiceBubble.createWithText(sampleText)

        val minimized = bubble.withMinimized(true)
        assertTrue(minimized.isMinimized)

        val expanded = minimized.withMinimized(false)
        assertFalse(expanded.isMinimized)
    }

    @Test
    fun `withInteraction updates lastInteractionTime`() {
        val bubble = VoiceBubble.createWithText(sampleText)
        val originalTime = bubble.lastInteractionTime

        // Simulate time passing
        Thread.sleep(10)

        val updated = bubble.withInteraction()
        assertTrue(updated.lastInteractionTime > originalTime)
    }

    @Test
    fun `display text shows preview when transcription history exists`() {
        val bubble = VoiceBubble.createWithText("Short text")
            .addTranscription("This is a very long transcription that should be truncated", 1.0f)

        val allContent = bubble.getAllContent()
        assertTrue(allContent.contains("Short text"))
        assertTrue(allContent.contains("This is a very long transcription"))
    }
}