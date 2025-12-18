package com.clipboardhistory.utils

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Manages Text-to-Speech functionality for voice bubble interactions.
 * Provides speech synthesis with progress tracking and error handling.
 */
class TTSManager(
    private val context: Context
) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    // State flows for reactive UI updates
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _currentUtterance = MutableStateFlow<String?>(null)
    val currentUtterance: StateFlow<String?> = _currentUtterance

    private val _speechProgress = MutableStateFlow(0f)
    val speechProgress: StateFlow<Float> = _speechProgress

    // Configuration
    private var speechRate = 1.0f
    private var pitch = 1.0f
    private var volume = 1.0f
    private var selectedLanguage = Locale.getDefault()

    init {
        initializeTTS()
    }

    /**
     * Initialize Text-to-Speech engine.
     */
    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            _isInitialized.value = true
            configureTTS()
            setupUtteranceProgressListener()
        } else {
            _isInitialized.value = false
            showToast("TTS initialization failed", Toast.LENGTH_LONG)
        }
    }

    /**
     * Configure TTS with user preferences.
     */
    private fun configureTTS() {
        textToSpeech?.apply {
            // Set language
            val result = setLanguage(selectedLanguage)
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default language
                setLanguage(Locale.getDefault())
            }

            // Set speech parameters
            setSpeechRate(speechRate)
            setPitch(pitch)

            // Set audio attributes for better accessibility
            val audioManager = context.getSystemService(Context.AUDIO_MANAGER) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            this@TTSManager.volume = currentVolume.toFloat() / maxVolume.toFloat()
        }
    }

    /**
     * Setup utterance progress listener for tracking speech progress.
     */
    private fun setupUtteranceProgressListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                _speechProgress.value = 0f
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                _currentUtterance.value = null
                _speechProgress.value = 1f
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                _currentUtterance.value = null
                _speechProgress.value = 0f
                showToast("Speech synthesis failed", Toast.LENGTH_SHORT)
            }
        })
    }

    /**
     * Speak the given text.
     */
    fun speak(text: String, utteranceId: String = UUID.randomUUID().toString()): Boolean {
        if (!isInitialized.value) {
            showToast("Text-to-speech not available", Toast.LENGTH_SHORT)
            return false
        }

        if (text.isBlank()) {
            showToast("No text to speak", Toast.LENGTH_SHORT)
            return false
        }

        try {
            _currentUtterance.value = text

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
            }

            val result = textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            return result == TextToSpeech.SUCCESS

        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Speech failed: ${e.message}", Toast.LENGTH_SHORT)
            return false
        }
    }

    /**
     * Stop current speech.
     */
    fun stop(): Boolean {
        return try {
            val result = textToSpeech?.stop()
            _isSpeaking.value = false
            _currentUtterance.value = null
            _speechProgress.value = 0f
            result == TextToSpeech.SUCCESS
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Check if TTS is currently speaking.
     */
    fun isSpeaking(): Boolean = _isSpeaking.value

    /**
     * Get available languages for TTS.
     */
    fun getAvailableLanguages(): Set<Locale> {
        return textToSpeech?.availableLanguages ?: emptySet()
    }

    /**
     * Set speech rate (0.5f = slow, 2.0f = fast).
     */
    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        textToSpeech?.setSpeechRate(speechRate)
    }

    /**
     * Set speech pitch (0.5f = low, 2.0f = high).
     */
    fun setPitch(pitch: Float) {
        this.pitch = pitch.coerceIn(0.5f, 2.0f)
        textToSpeech?.setPitch(this.pitch)
    }

    /**
     * Set TTS language.
     */
    fun setLanguage(language: Locale): Boolean {
        selectedLanguage = language
        return textToSpeech?.setLanguage(language) == TextToSpeech.LANG_AVAILABLE
    }

    /**
     * Get current speech rate.
     */
    fun getSpeechRate(): Float = speechRate

    /**
     * Get current pitch.
     */
    fun getPitch(): Float = pitch

    /**
     * Get current language.
     */
    fun getLanguage(): Locale = selectedLanguage

    /**
     * Check if a language is available.
     */
    fun isLanguageAvailable(language: Locale): Boolean {
        return textToSpeech?.isLanguageAvailable(language) == TextToSpeech.LANG_AVAILABLE
    }

    /**
     * Show a toast message.
     */
    private fun showToast(message: String, duration: Int) {
        mainScope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, message, duration).show()
            }
        }
    }

    /**
     * Cleanup resources.
     */
    fun shutdown() {
        textToSpeech?.apply {
            stop()
            shutdown()
        }
        textToSpeech = null
        _isInitialized.value = false
        _isSpeaking.value = false
        _currentUtterance.value = null
        _speechProgress.value = 0f
    }

    companion object {
        /**
         * Check if TTS is available on this device.
         */
        fun isTTSAvailable(context: Context): Boolean {
            return try {
                val intent = android.content.Intent()
                intent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
                val packageManager = context.packageManager
                val resolveInfo = packageManager.queryIntentActivities(intent, 0)
                resolveInfo.isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
    }
}