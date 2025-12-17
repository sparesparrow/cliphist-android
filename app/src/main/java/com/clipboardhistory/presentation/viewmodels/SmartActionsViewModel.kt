package com.clipboardhistory.presentation.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.SmartAction
import com.clipboardhistory.domain.repository.ClipboardRepository
import com.clipboardhistory.domain.usecase.ExecuteSmartActionUseCase
import com.clipboardhistory.domain.usecase.ManageFavoritesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for smart actions screen functionality.
 *
 * Manages clipboard item display and smart action execution.
 */
@HiltViewModel
class SmartActionsViewModel @Inject constructor(
    private val repository: ClipboardRepository,
    private val executeSmartActionUseCase: ExecuteSmartActionUseCase,
    private val manageFavoritesUseCase: ManageFavoritesUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _clipboardItem = MutableStateFlow<ClipboardItem?>(null)
    val clipboardItem: StateFlow<ClipboardItem?> = _clipboardItem.asStateFlow()

    private val _availableActions = MutableStateFlow<List<SmartAction>>(emptyList())
    val availableActions: StateFlow<List<SmartAction>> = _availableActions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Load clipboard item by ID and determine available actions.
     */
    fun loadItem(itemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val item = repository.getItemById(itemId)
                _clipboardItem.value = item

                if (item != null) {
                    val actions = executeSmartActionUseCase.getAvailableActions(
                        content = item.content,
                        contentType = item.contentType.name,
                    )
                    _availableActions.value = actions
                } else {
                    _availableActions.value = emptyList()
                }
            } catch (e: Exception) {
                _clipboardItem.value = null
                _availableActions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Execute a smart action.
     */
    suspend fun executeSmartAction(
        content: String,
        contentType: String,
        action: SmartAction,
    ): ExecuteSmartActionUseCase.SmartActionResult {
        return executeSmartActionUseCase.invoke(content, contentType, action)
    }

    /**
     * Toggle favorite status for the current item.
     */
    suspend fun toggleFavorite(itemId: Long): Boolean {
        val currentItem = _clipboardItem.value ?: return false

        val success = manageFavoritesUseCase.toggleFavorite(itemId.toString())

        if (success) {
            // Update the local item state
            _clipboardItem.value = currentItem.copy(isFavorite = !currentItem.isFavorite)
        }

        return success
    }

    /**
     * Copy content to clipboard.
     */
    fun copyToClipboard(content: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("clipboard", content)
            clipboard.setPrimaryClip(clip)
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    /**
     * Share content using Android's share system.
     */
    fun shareContent(content: String, contentType: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when {
                    contentType.startsWith("text/") -> "text/plain"
                    contentType.startsWith("image/") -> "image/*"
                    else -> "*/*"
                }
                putExtra(Intent.EXTRA_TEXT, content)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooserIntent = Intent.createChooser(intent, "Share via")
            chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    /**
     * Get suggested primary action for the current item.
     */
    fun getSuggestedPrimaryAction(): SmartAction? {
        val item = _clipboardItem.value ?: return null
        return executeSmartActionUseCase.suggestPrimaryAction(
            content = item.content,
            contentType = item.contentType.name,
        )
    }

    /**
     * Check if the current item is favorited.
     */
    fun isItemFavorited(): Boolean {
        return _clipboardItem.value?.isFavorite ?: false
    }

    /**
     * Get item metadata for display.
     */
    fun getItemMetadata(): ItemMetadata? {
        val item = _clipboardItem.value ?: return null

        return ItemMetadata(
            contentLength = item.content.length,
            contentType = item.contentType.name,
            timestamp = item.timestamp,
            isFavorite = item.isFavorite,
            sourceApp = item.sourceApp,
            encryptionKey = item.encryptionKey,
        )
    }

    /**
     * Refresh the current item data.
     */
    fun refreshItem() {
        val itemId = _clipboardItem.value?.id ?: return
        loadItem(itemId)
    }
}

/**
 * Data class for item metadata display.
 */
data class ItemMetadata(
    val contentLength: Int,
    val contentType: String,
    val timestamp: Long,
    val isFavorite: Boolean,
    val sourceApp: String?,
    val encryptionKey: String?,
)