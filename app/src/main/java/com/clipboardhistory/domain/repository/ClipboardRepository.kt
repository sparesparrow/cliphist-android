package com.clipboardhistory.domain.repository

import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ClipboardSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for clipboard operations.
 *
 * This interface defines the contract for clipboard data operations,
 * abstracting the data source implementation details.
 */
interface ClipboardRepository {

    /**
     * Get all clipboard items as a Flow.
     *
     * @return Flow of list of clipboard items
     */
    fun getAllItems(): Flow<List<ClipboardItem>>

    /**
     * Get a clipboard item by ID.
     *
     * @param id The ID of the clipboard item
     * @return The clipboard item or null if not found
     */
    suspend fun getItemById(id: String): ClipboardItem?

    /**
     * Insert a new clipboard item.
     *
     * @param item The clipboard item to insert
     */
    suspend fun insertItem(item: ClipboardItem)

    /**
     * Update an existing clipboard item.
     *
     * @param item The clipboard item to update
     */
    suspend fun updateItem(item: ClipboardItem)

    /**
     * Delete a clipboard item.
     *
     * @param item The clipboard item to delete
     */
    suspend fun deleteItem(item: ClipboardItem)

    /**
     * Delete a clipboard item by ID.
     *
     * @param id The ID of the clipboard item to delete
     */
    suspend fun deleteItemById(id: String)

    /**
     * Delete all clipboard items.
     */
    suspend fun deleteAllItems()

    /**
     * Delete items older than the specified number of hours.
     *
     * @param hours The number of hours threshold
     */
    suspend fun deleteItemsOlderThan(hours: Int)

    /**
     * Get the current clipboard settings.
     *
     * @return The current clipboard settings
     */
    suspend fun getSettings(): ClipboardSettings

    /**
     * Update the clipboard settings.
     *
     * @param settings The new clipboard settings
     */
    suspend fun updateSettings(settings: ClipboardSettings)

    /**
     * Get clipboard items with pagination.
     *
     * @param limit The maximum number of items to return
     * @param offset The offset for pagination
     * @return List of clipboard items
     */
    suspend fun getItemsWithPagination(limit: Int, offset: Int): List<ClipboardItem>
}
