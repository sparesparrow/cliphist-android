package com.clipboardhistory.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for clipboard items.
 *
 * This interface defines the database operations for clipboard items
 * using Room's annotation-based query system.
 */
@Dao
interface ClipboardItemDao {
    /**
     * Get all clipboard items ordered by timestamp (newest first).
     *
     * @return Flow of list of clipboard items
     */
    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<ClipboardItemEntity>>

    /**
     * Get a specific clipboard item by ID.
     *
     * @param id The ID of the clipboard item
     * @return The clipboard item entity or null if not found
     */
    @Query("SELECT * FROM clipboard_items WHERE id = :id")
    suspend fun getItemById(id: String): ClipboardItemEntity?

    /**
     * Insert a new clipboard item.
     *
     * @param item The clipboard item to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ClipboardItemEntity)

    /**
     * Insert multiple clipboard items.
     *
     * @param items The clipboard items to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ClipboardItemEntity>)

    /**
     * Update an existing clipboard item.
     *
     * @param item The clipboard item to update
     */
    @Update
    suspend fun updateItem(item: ClipboardItemEntity)

    /**
     * Delete a clipboard item.
     *
     * @param item The clipboard item to delete
     */
    @Delete
    suspend fun deleteItem(item: ClipboardItemEntity)

    /**
     * Delete a clipboard item by ID.
     *
     * @param id The ID of the clipboard item to delete
     */
    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    /**
     * Delete all clipboard items.
     */
    @Query("DELETE FROM clipboard_items")
    suspend fun deleteAllItems()

    /**
     * Delete items older than the specified timestamp.
     *
     * @param timestamp The timestamp threshold
     */
    @Query("DELETE FROM clipboard_items WHERE timestamp < :timestamp")
    suspend fun deleteItemsOlderThan(timestamp: Long)

    /**
     * Get the count of clipboard items.
     *
     * @return The total number of clipboard items
     */
    @Query("SELECT COUNT(*) FROM clipboard_items")
    suspend fun getItemCount(): Int

    /**
     * Get clipboard items with a limit (for pagination).
     *
     * @param limit The maximum number of items to return
     * @param offset The offset for pagination
     * @return List of clipboard items
     */
    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getItemsWithPagination(
        limit: Int,
        offset: Int,
    ): List<ClipboardItemEntity>
}
