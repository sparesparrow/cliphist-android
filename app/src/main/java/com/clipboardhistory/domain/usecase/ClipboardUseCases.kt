package com.clipboardhistory.domain.usecase

import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ClipboardSettings
import com.clipboardhistory.domain.model.ContentType
import com.clipboardhistory.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for adding a new clipboard item.
 * 
 * @property repository The clipboard repository
 */
class AddClipboardItemUseCase @Inject constructor(
    private val repository: ClipboardRepository
) {
    /**
     * Adds a new clipboard item to the repository.
     * 
     * @param content The content to add
     * @param contentType The type of content
     * @return The created clipboard item, or null if content already exists
     */
    suspend operator fun invoke(content: String, contentType: ContentType = ContentType.TEXT): ClipboardItem? {
        // Check if content already exists
        val existingItems = repository.getAllItems().first()
        val isDuplicate = existingItems.any { it.content == content }
        
        if (isDuplicate) {
            return null // Don't add duplicate content
        }
        
        // Respect current encryption setting from repository
        val settings = repository.getSettings()
        val item = ClipboardItem(
            id = UUID.randomUUID().toString(),
            content = content,
            timestamp = System.currentTimeMillis(),
            contentType = contentType,
            isEncrypted = settings.enableEncryption,
            size = content.toByteArray().size
        )
        
        repository.insertItem(item)
        return item
    }
}

/**
 * Use case for getting all clipboard items.
 * 
 * @property repository The clipboard repository
 */
class GetAllClipboardItemsUseCase @Inject constructor(
    private val repository: ClipboardRepository
) {
    /**
     * Gets all clipboard items from the repository.
     * 
     * @return Flow of list of clipboard items
     */
    operator fun invoke(): Flow<List<ClipboardItem>> {
        return repository.getAllItems()
    }
}

/**
 * Use case for deleting a clipboard item.
 * 
 * @property repository The clipboard repository
 */
class DeleteClipboardItemUseCase @Inject constructor(
    private val repository: ClipboardRepository
) {
    /**
     * Deletes a clipboard item from the repository.
     * 
     * @param item The clipboard item to delete
     */
    suspend operator fun invoke(item: ClipboardItem) {
        repository.deleteItem(item)
    }
}

/**
 * Use case for updating a clipboard item.
 * 
 * @property repository The clipboard repository
 */
class UpdateClipboardItemUseCase @Inject constructor(
    private val repository: ClipboardRepository
) {
    /**
     * Updates a clipboard item in the repository.
     * 
     * @param item The clipboard item to update
     */
    suspend operator fun invoke(item: ClipboardItem) {
        repository.updateItem(item)
    }
}

/**
 * Use case for getting clipboard settings.
 * 
 * @property repository The clipboard repository
 */
class GetClipboardSettingsUseCase @Inject constructor(
    private val repository: ClipboardRepository
) {
    /**
     * Gets the current clipboard settings.
     * 
     * @return The current clipboard settings
     */
    suspend operator fun invoke(): ClipboardSettings {
        return repository.getSettings()
    }
}

/**
 * Use case for updating clipboard settings.
 * 
 * @property repository The clipboard repository
 */
class UpdateClipboardSettingsUseCase @Inject constructor(
    private val repository: ClipboardRepository
) {
    /**
     * Updates the clipboard settings.
     * 
     * @param settings The new clipboard settings
     */
    suspend operator fun invoke(settings: ClipboardSettings) {
        repository.updateSettings(settings)
    }
}

/**
 * Use case for cleaning up old clipboard items.
 * 
 * @property repository The clipboard repository
 */
class CleanupOldItemsUseCase @Inject constructor(
    private val repository: ClipboardRepository
) {
    /**
     * Cleans up old clipboard items based on settings.
     * 
     * @param settings The clipboard settings
     */
    suspend operator fun invoke(settings: ClipboardSettings) {
        repository.deleteItemsOlderThan(settings.autoDeleteAfterHours)
    }
}