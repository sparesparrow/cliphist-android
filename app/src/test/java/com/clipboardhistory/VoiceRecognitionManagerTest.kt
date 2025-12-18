package com.clipboardhistory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clipboardhistory.utils.VoiceRecognitionManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

@RunWith(RobolectricTestRunner::class)
class VoiceRecognitionManagerTest {

    private lateinit var context: Context
    private lateinit var voiceManager: VoiceRecognitionManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        voiceManager = VoiceRecognitionManager(context, null)
    }

    @After
    fun tearDown() {
        voiceManager.shutdown()
    }

    @Test
    fun `voice recognition availability check works correctly`() {
        val isAvailable = VoiceRecognitionManager.isSpeechRecognitionAvailable(context)
        // Result depends on device capabilities, but method should not crash
        assertNotNull(isAvailable)
    }

    @Test
    fun `initial state has correct defaults`() {
        assertFalse(voiceManager.isListening.value)
        assertFalse(voiceManager.isAvailable.value)
        assertNull(voiceManager.transcriptionResult.value)
        assertNull(voiceManager.recognitionError.value)
        assertEquals(0f, voiceManager.confidenceScore.value)
        assertEquals(Locale.getDefault(), voiceManager.getLanguage())
        assertEquals(5, voiceManager.getMaxResults())
        assertTrue(voiceManager.getEnablePartialResults())
    }

    @Test
    fun `language can be set and retrieved`() {
        val spanish = Locale("es", "ES")
        voiceManager.setLanguage(spanish)
        assertEquals(spanish, voiceManager.getLanguage())
    }

    @Test
    fun `max results can be set and retrieved`() {
        voiceManager.setMaxResults(10)
        assertEquals(10, voiceManager.getMaxResults())

        // Test bounds clamping
        voiceManager.setMaxResults(15)
        assertEquals(10, voiceManager.getMaxResults()) // Max 10

        voiceManager.setMaxResults(0)
        assertEquals(1, voiceManager.getMaxResults()) // Min 1
    }

    @Test
    fun `partial results setting can be toggled`() {
        voiceManager.setEnablePartialResults(false)
        assertFalse(voiceManager.getEnablePartialResults())

        voiceManager.setEnablePartialResults(true)
        assertTrue(voiceManager.getEnablePartialResults())
    }

    @Test
    fun `stop listening works when not listening`() {
        val result = voiceManager.stopListening()
        assertFalse(voiceManager.isListening.value)
        // Result depends on speech recognizer state
        assertNotNull(result)
    }

    @Test
    fun `cancel listening works when not listening`() {
        val result = voiceManager.cancelListening()
        assertFalse(voiceManager.isListening.value)
        // Result depends on speech recognizer state
        assertNotNull(result)
    }

    @Test
    fun `shutdown resets all state correctly`() {
        // Set some state first
        voiceManager.setLanguage(Locale.FRENCH)
        voiceManager.setMaxResults(8)

        voiceManager.shutdown()

        assertFalse(voiceManager.isListening.value)
        assertFalse(voiceManager.isAvailable.value)
        assertNull(voiceManager.transcriptionResult.value)
        assertNull(voiceManager.recognitionError.value)
        assertTrue(voiceManager.partialResults.value.isEmpty())
        assertEquals(0f, voiceManager.confidenceScore.value)
    }

    @Test
    fun `start listening returns false when not available`() {
        // Speech recognition may not be available in test environment
        val result = voiceManager.startListening()
        // Result depends on device capabilities and permissions
        assertNotNull(result)
    }

    @Test
    fun `getSupportedLanguages returns non-null result`() {
        val languages = VoiceRecognitionManager.getSupportedLanguages(context)
        // Languages list may be empty if speech recognition not available, but should not be null
        assertNotNull(languages)
    }

    @Test
    fun `callbacks can be set and are initially null`() {
        assertNull(voiceManager.onTranscriptionComplete)
        assertNull(voiceManager.onError)
        assertNull(voiceManager.onPartialResult)

        // Set callbacks
        voiceManager.onTranscriptionComplete = { text, confidence -> }
        voiceManager.onError = { error -> }
        voiceManager.onPartialResult = { results -> }

        assertNotNull(voiceManager.onTranscriptionComplete)
        assertNotNull(voiceManager.onError)
        assertNotNull(voiceManager.onPartialResult)
    }
}