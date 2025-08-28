package com.clipboardhistory.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.clipboardhistory.domain.usecase.AddClipboardItemUseCase
import com.clipboardhistory.domain.model.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Activity to handle text shared from other apps (SEND) and the text selection context menu (PROCESS_TEXT).
 * Finishes immediately after saving the shared text to clipboard history.
 */
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    @Inject
    lateinit var addClipboardItemUseCase: AddClipboardItemUseCase

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent?.action
        val type = intent?.type

        if (Intent.ACTION_SEND == action && type == "text/plain") {
            handleSendText(intent)
        } else if (Intent.ACTION_PROCESS_TEXT == action && type == "text/plain") {
            handleProcessText(intent)
        } else {
            finishWithMessage(false)
        }
    }

    private fun handleSendText(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!sharedText.isNullOrBlank()) {
            saveToHistory(sharedText)
        } else {
            finishWithMessage(false)
        }
    }

    private fun handleProcessText(intent: Intent) {
        val processedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        if (!processedText.isNullOrBlank()) {
            saveToHistory(processedText)
        } else {
            finishWithMessage(false)
        }
    }

    private fun saveToHistory(text: String) {
        scope.launch {
            try {
                addClipboardItemUseCase(text, detectContentType(text))
                runOnUiThread { finishWithMessage(true) }
            } catch (_: Exception) {
                runOnUiThread { finishWithMessage(false) }
            }
        }
    }

    private fun detectContentType(text: String): ContentType {
        return when {
            text.startsWith("http://") || text.startsWith("https://") -> ContentType.URL
            text.startsWith("file://") -> ContentType.FILE
            text.matches(Regex(".*\\.(jpg|jpeg|png|gif|bmp|webp)$", RegexOption.IGNORE_CASE)) -> ContentType.IMAGE
            else -> ContentType.TEXT
        }
    }

    private fun finishWithMessage(success: Boolean) {
        if (success) {
            Toast.makeText(this, getString(com.clipboardhistory.R.string.saved_to_history), Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
        } else {
            Toast.makeText(this, getString(com.clipboardhistory.R.string.nothing_to_save), Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}


