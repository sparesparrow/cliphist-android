package com.clipboardhistory.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Manages voice recognition (speech-to-text) functionality for voice bubble creation.
 * Handles microphone permissions, speech recognition, and transcription results.
 */
class VoiceRecognitionManager(
    private val context: Context,
    private val activity: Activity? = null
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionIntent: Intent? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    // State flows for reactive UI updates
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable

    private val _transcriptionResult = MutableStateFlow<String?>(null)
    val transcriptionResult: StateFlow<String?> = _transcriptionResult

    private val _recognitionError = MutableStateFlow<String?>(null)
    val recognitionError: StateFlow<String?> = _recognitionError

    private val _confidenceScore = MutableStateFlow(0f)
    val confidenceScore: StateFlow<Float> = _recognitionError

    private val _partialResults = MutableStateFlow<List<String>>(emptyList())
    val partialResults: StateFlow<List<String>> = _partialResults

    // Configuration
    private var selectedLanguage = Locale.getDefault()
    private var maxResults = 5
    private var enablePartialResults = true

    // Callbacks
    var onTranscriptionComplete: ((String, Float) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onPartialResult: ((List<String>) -> Unit)? = null

    init {
        initializeSpeechRecognizer()
        checkAvailability()
    }

    /**
     * Initialize speech recognizer and intent.
     */
    private fun initializeSpeechRecognizer() {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())

            recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguage.toString())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, enablePartialResults)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isAvailable.value = false
        }
    }

    /**
     * Create recognition listener for handling speech events.
     */
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                _recognitionError.value = null
                showToast("Listening...", Toast.LENGTH_SHORT)
            }

            override fun onBeginningOfSpeech() {
                // Speech input has started
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Volume level changed - could be used for visual feedback
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }

            override fun onEndOfSpeech() {
                _isListening.value = false
                showToast("Processing speech...", Toast.LENGTH_SHORT)
            }

            override fun onError(error: Int) {
                _isListening.value = false
                val errorMessage = getErrorMessage(error)
                _recognitionError.value = errorMessage
                onError?.invoke(errorMessage)
                showToast("Recognition error: $errorMessage", Toast.LENGTH_SHORT)
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false

                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                    if (matches.isNotEmpty()) {
                        val bestMatch = matches[0]
                        val confidenceScores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                        val confidence = confidenceScores?.getOrNull(0) ?: 0f

                        _transcriptionResult.value = bestMatch
                        _confidenceScore.value = confidence
                        _partialResults.value = emptyList()

                        onTranscriptionComplete?.invoke(bestMatch, confidence)
                        showToast("Transcribed: ${bestMatch.take(50)}${if (bestMatch.length > 50) "..." else ""}", Toast.LENGTH_SHORT)
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (enablePartialResults) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                        _partialResults.value = matches
                        onPartialResult?.invoke(matches)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Handle other speech events
            }
        }
    }

    /**
     * Check if speech recognition is available on this device.
     */
    private fun checkAvailability() {
        _isAvailable.value = try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Start voice recognition.
     */
    fun startListening(): Boolean {
        if (!isAvailable.value) {
            showToast("Speech recognition not available", Toast.LENGTH_LONG)
            return false
        }

        if (!hasRecordAudioPermission()) {
            requestRecordAudioPermission()
            return false
        }

        if (_isListening.value) {
            showToast("Already listening", Toast.LENGTH_SHORT)
            return false
        }

        return try {
            speechRecognizer?.startListening(recognitionIntent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Failed to start listening", Toast.LENGTH_SHORT)
            false
        }
    }

    /**
     * Stop voice recognition.
     */
    fun stopListening(): Boolean {
        return try {
            speechRecognizer?.stopListening()
            _isListening.value = false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Cancel voice recognition.
     */
    fun cancelListening(): Boolean {
        return try {
            speechRecognizer?.cancel()
            _isListening.value = false
            _partialResults.value = emptyList()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Check if RECORD_AUDIO permission is granted.
     */
    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request RECORD_AUDIO permission.
     */
    private fun requestRecordAudioPermission() {
        activity?.let {
            ActivityCompat.requestPermissions(
                it,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
        showToast("Microphone permission required for voice recognition", Toast.LENGTH_LONG)
    }

    /**
     * Set recognition language.
     */
    fun setLanguage(language: Locale) {
        selectedLanguage = language
        recognitionIntent?.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.toString())
    }

    /**
     * Set maximum number of recognition results.
     */
    fun setMaxResults(max: Int) {
        maxResults = max.coerceIn(1, 10)
        recognitionIntent?.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults)
    }

    /**
     * Enable or disable partial results.
     */
    fun setEnablePartialResults(enabled: Boolean) {
        enablePartialResults = enabled
        recognitionIntent?.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, enablePartialResults)
    }

    /**
     * Get current language.
     */
    fun getLanguage(): Locale = selectedLanguage

    /**
     * Get error message for recognition error code.
     */
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Unknown error ($errorCode)"
        }
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
        cancelListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _isAvailable.value = false
        _isListening.value = false
        _transcriptionResult.value = null
        _recognitionError.value = null
        _partialResults.value = emptyList()
    }

    companion object {
        const val REQUEST_RECORD_AUDIO_PERMISSION = 1001

        /**
         * Check if speech recognition is available on this device.
         */
        fun isSpeechRecognitionAvailable(context: Context): Boolean {
            return try {
                SpeechRecognizer.isRecognitionAvailable(context)
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Get list of supported languages for speech recognition.
         */
        fun getSupportedLanguages(context: Context): List<Locale> {
            return try {
                val intent = Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS)
                val languages = mutableListOf<Locale>()

                // This is a simplified approach - in production, you'd query the
                // speech recognition service for supported languages
                listOf(
                    Locale.ENGLISH,
                    Locale.FRENCH,
                    Locale.GERMAN,
                    Locale.ITALIAN,
                    Locale.SPANISH,
                    Locale.JAPANESE,
                    Locale.KOREAN,
                    Locale.CHINESE
                )
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}