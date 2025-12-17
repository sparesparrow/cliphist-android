package com.clipboardhistory.presentation.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ContentType
import com.clipboardhistory.domain.repository.ClipboardRepository
import com.clipboardhistory.domain.usecase.ManageFavoritesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for item detail screen functionality.
 *
 * Manages clipboard item display, editing, and actions.
 */
@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val repository: ClipboardRepository,
    private val manageFavoritesUseCase: ManageFavoritesUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _clipboardItem = MutableStateFlow<ClipboardItem?>(null)
    val clipboardItem: StateFlow<ClipboardItem?> = _clipboardItem.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _editedContent = MutableStateFlow("")
    val editedContent: StateFlow<String> = _editedContent.asStateFlow()

    /**
     * Load clipboard item by ID.
     */
    fun loadItem(itemId: String) {
        viewModelScope.launch {
            try {
                val item = repository.getItemById(itemId)
                _clipboardItem.value = item
                _editedContent.value = item?.content ?: ""
            } catch (e: Exception) {
                _clipboardItem.value = null
            }
        }
    }

    /**
     * Start editing the item content.
     */
    fun startEditing() {
        _editedContent.value = _clipboardItem.value?.content ?: ""
        _isEditing.value = true
    }

    /**
     * Cancel editing and revert changes.
     */
    fun cancelEditing() {
        _editedContent.value = _clipboardItem.value?.content ?: ""
        _isEditing.value = false
    }

    /**
     * Update the edited content.
     */
    fun updateEditedContent(content: String) {
        _editedContent.value = content
    }

    /**
     * Save the edited content.
     */
    suspend fun saveEditedContent(): Boolean {
        val currentItem = _clipboardItem.value ?: return false
        val newContent = _editedContent.value

        if (newContent == currentItem.content) {
            _isEditing.value = false
            return true // No changes to save
        }

        return try {
            val updatedItem = currentItem.copy(content = newContent)
            repository.updateItem(updatedItem)

            _clipboardItem.value = updatedItem
            _isEditing.value = false

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Toggle favorite status for the current item.
     */
    suspend fun toggleFavorite(): Boolean {
        val currentItem = _clipboardItem.value ?: return false
        val itemId = currentItem.id

        val success = manageFavoritesUseCase.toggleFavorite(itemId)

        if (success) {
            // Update local state
            val newFavoriteStatus = !currentItem.isFavorite
            _clipboardItem.value = currentItem.copy(isFavorite = newFavoriteStatus)
        }

        return success
    }

    /**
     * Delete the current item.
     */
    suspend fun deleteItem(): Boolean {
        val currentItem = _clipboardItem.value ?: return false

        return try {
            val success = repository.softDeleteItem(currentItem.id)

            if (success) {
                // Mark as deleted locally
                _clipboardItem.value = currentItem.copy(isDeleted = true)
            }

            success
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Copy the item content to clipboard.
     */
    fun copyToClipboard(context: Context) {
        val content = _clipboardItem.value?.content ?: return

        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("clipboard_item", content)
            clipboard.setPrimaryClip(clip)
        } catch (e: Exception) {
            // Handle silently
        }
    }

    /**
     * Share the item content.
     */
    fun shareContent(context: Context) {
        val item = _clipboardItem.value ?: return

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when (item.contentType) {
                    ContentType.TEXT -> "text/plain"
                    ContentType.URL -> "text/plain"
                    ContentType.IMAGE -> "image/*"
                    ContentType.FILE -> "*/*"
                    ContentType.OTHER -> "*/*"
                }
                putExtra(Intent.EXTRA_TEXT, item.content)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooserIntent = Intent.createChooser(intent, "Share clipboard item")
            chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            // Handle silently
        }
    }

    /**
     * Get item statistics for display.
     */
    fun getItemStats(): ItemStats? {
        val item = _clipboardItem.value ?: return null

        val wordCount = item.content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        val lineCount = item.content.lines().size
        val hasSpecialChars = item.content.any { !it.isLetterOrDigit() && !it.isWhitespace() }

        return ItemStats(
            characterCount = item.content.length,
            wordCount = wordCount,
            lineCount = lineCount,
            hasSpecialChars = hasSpecialChars,
            contentType = item.contentType.name,
            isFavorite = item.isFavorite,
            isEncrypted = item.encryptionKey != null,
            sourceApp = item.sourceApp,
        )
    }

    /**
     * Check if the item can be edited.
     */
    fun canEditItem(): Boolean {
        val item = _clipboardItem.value ?: return false
        // Allow editing if not deleted and content is reasonable length
        return !item.isDeleted && item.content.length < 10000
    }

    /**
     * Refresh the item data from repository.
     */
    fun refreshItem() {
        val itemId = _clipboardItem.value?.id ?: return
        loadItem(itemId)
    }
}

/**
 * Data class for item statistics display.
 */
data class ItemStats(
    val characterCount: Int,
    val wordCount: Int,
    val lineCount: Int,
    val hasSpecialChars: Boolean,
    val contentType: String,
    val isFavorite: Boolean,
    val isEncrypted: Boolean,
    val sourceApp: String?,
)