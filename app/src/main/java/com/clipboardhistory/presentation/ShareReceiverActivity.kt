package com.clipboardhistory.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ContentType
import com.clipboardhistory.domain.usecase.AddClipboardItemUseCase
import com.clipboardhistory.domain.usecase.GetAllClipboardItemsUseCase
import com.clipboardhistory.domain.usecase.UpdateClipboardItemUseCase
import com.clipboardhistory.presentation.ui.components.BubbleSelectionScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity to handle text shared from other apps (SEND) and the text selection context menu (PROCESS_TEXT).
 * Shows bubble selection screen for replace/append operations.
 */
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {
    @Inject
    lateinit var addClipboardItemUseCase: AddClipboardItemUseCase

    @Inject
    lateinit var getAllClipboardItemsUseCase: GetAllClipboardItemsUseCase

    @Inject
    lateinit var updateClipboardItemUseCase: UpdateClipboardItemUseCase

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var sharedText: String? = null
    private var contentType: ContentType = ContentType.TEXT
    private var clipboardItems: List<ClipboardItem> = emptyList()

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
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!text.isNullOrBlank()) {
            sharedText = text
            contentType = detectContentType(text)
            showBubbleSelectionScreen()
        } else {
            finishWithMessage(false)
        }
    }

    private fun handleProcessText(intent: Intent) {
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        if (!text.isNullOrBlank()) {
            sharedText = text
            contentType = detectContentType(text)
            showBubbleSelectionScreen()
        } else {
            finishWithMessage(false)
        }
    }

    private fun showBubbleSelectionScreen() {
        // Load clipboard items first
        scope.launch {
            try {
                val items = getAllClipboardItemsUseCase().first()
                runOnUiThread {
                    clipboardItems = items
                    setContent {
                        MaterialTheme {
                            BubbleSelectionScreen(
                                sharedText = sharedText ?: "",
                                onReplaceBubble = { bubbleItem ->
                                    handleReplaceBubble(bubbleItem)
                                },
                                onAppendBubble = { bubbleItem ->
                                    handleAppendBubble(bubbleItem)
                                },
                                onPrependBubble = { bubbleItem ->
                                    handlePrependBubble(bubbleItem)
                                },
                                onAddNewBubble = {
                                    handleAddNewBubble()
                                },
                                onCancel = {
                                    finishWithMessage(false)
                                },
                                clipboardItems = clipboardItems,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    finishWithMessage(false)
                }
            }
        }
    }

    private fun handleReplaceBubble(bubbleItem: ClipboardItem) {
        scope.launch {
            try {
                // Replace the content of the selected bubble
                val updatedItem =
                    bubbleItem.copy(
                        content = sharedText ?: "",
                        timestamp = System.currentTimeMillis(),
                    )
                updateClipboardItemUseCase(updatedItem)
                runOnUiThread { finishWithMessage(true) }
            } catch (e: Exception) {
                runOnUiThread { finishWithMessage(false) }
            }
        }
    }

    private fun handleAppendBubble(bubbleItem: ClipboardItem) {
        scope.launch {
            try {
                // Append the new content to the selected bubble
                val newContent = "${bubbleItem.content}\n${sharedText ?: ""}"
                val updatedItem =
                    bubbleItem.copy(
                        content = newContent,
                        timestamp = System.currentTimeMillis(),
                    )
                updateClipboardItemUseCase(updatedItem)
                runOnUiThread { finishWithMessage(true) }
            } catch (e: Exception) {
                runOnUiThread { finishWithMessage(false) }
            }
        }
    }

    private fun handlePrependBubble(bubbleItem: ClipboardItem) {
        scope.launch {
            try {
                // Prepend the new content to the selected bubble
                val newContent = "${sharedText ?: ""}\n${bubbleItem.content}"
                val updatedItem =
                    bubbleItem.copy(
                        content = newContent,
                        timestamp = System.currentTimeMillis(),
                    )
                updateClipboardItemUseCase(updatedItem)
                runOnUiThread { finishWithMessage(true) }
            } catch (e: Exception) {
                runOnUiThread { finishWithMessage(false) }
            }
        }
    }

    private fun handleAddNewBubble() {
        scope.launch {
            try {
                val result = addClipboardItemUseCase(sharedText ?: "", contentType)
                runOnUiThread {
                    if (result != null) {
                        finishWithMessage(true)
                    } else {
                        // Content already exists
                        Toast.makeText(this@ShareReceiverActivity, "Content already exists in clipboard history", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                }
            } catch (e: Exception) {
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
