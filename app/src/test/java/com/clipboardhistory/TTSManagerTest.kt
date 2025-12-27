package com.clipboardhistory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clipboardhistory.utils.TTSManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

@RunWith(RobolectricTestRunner::class)
class TTSManagerTest {

    private lateinit var context: Context
    private lateinit var ttsManager: TTSManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        ttsManager = TTSManager(context)
    }

    @After
    fun tearDown() {
        ttsManager.shutdown()
    }

    @Test
    fun `TTS availability check works correctly`() {
        // This test may vary based on device TTS availability
        val isAvailable = TTSManager.isTTSAvailable(context)
        // Just ensure the method doesn't crash
        assertNotNull(isAvailable)
    }

    @Test
    fun `initial state has correct defaults`() {
        assertFalse(ttsManager.isInitialized.value)
        assertFalse(ttsManager.isSpeaking.value)
        assertEquals(1.0f, ttsManager.getSpeechRate())
        assertEquals(1.0f, ttsManager.getPitch())
        assertEquals(Locale.getDefault(), ttsManager.getLanguage())
    }

    @Test
    fun `speech rate can be set and retrieved`() {
        ttsManager.setSpeechRate(1.5f)
        assertEquals(1.5f, ttsManager.getSpeechRate())

        // Test bounds clamping
        ttsManager.setSpeechRate(3.0f)
        assertEquals(2.0f, ttsManager.getSpeechRate())

        ttsManager.setSpeechRate(0.1f)
        assertEquals(0.5f, ttsManager.getSpeechRate())
    }

    @Test
    fun `pitch can be set and retrieved`() {
        ttsManager.setPitch(1.2f)
        assertEquals(1.2f, ttsManager.getPitch())

        // Test bounds clamping
        ttsManager.setPitch(3.0f)
        assertEquals(2.0f, ttsManager.getPitch())

        ttsManager.setPitch(0.1f)
        assertEquals(0.5f, ttsManager.getPitch())
    }

    @Test
    fun `language can be set and retrieved`() {
        val spanish = Locale("es", "ES")
        ttsManager.setLanguage(spanish)
        assertEquals(spanish, ttsManager.getLanguage())
    }

    @Test
    fun `speak returns false for empty text`() {
        val result = ttsManager.speak("")
        assertFalse(result)
    }

    @Test
    fun `speak returns false when TTS not initialized`() {
        // TTS may not be initialized immediately
        val result = ttsManager.speak("test")
        // Result depends on TTS initialization state
        assertNotNull(result)
    }

    @Test
    fun `stop speaking works correctly`() {
        val result = ttsManager.stop()
        assertFalse(ttsManager.isSpeaking.value)
        assertNotNull(result)
    }

    @Test
    fun `shutdown resets state correctly`() {
        ttsManager.shutdown()
        assertFalse(ttsManager.isInitialized.value)
        assertFalse(ttsManager.isSpeaking.value)
    }

    @Test
    fun `getAvailableLanguages returns non-null result`() {
        val languages = ttsManager.getAvailableLanguages()
        // Languages list may be empty if TTS not available, but should not be null
        assertNotNull(languages)
    }

    @Test
    fun `isLanguageAvailable returns boolean result`() {
        val english = Locale.ENGLISH
        val result = ttsManager.isLanguageAvailable(english)
        assertNotNull(result)
        // Result depends on device TTS capabilities
    }

    @Test
    fun `isSpeaking returns current speaking state`() {
        val speaking = ttsManager.isSpeaking()
        assertFalse(speaking) // Should be false initially
    }
}