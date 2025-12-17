package com.clipboardhistory.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
     * Query by timestamp range.
     *
     * @param startTimestamp Start of the timestamp range
     * @param endTimestamp End of the timestamp range
     * @return List of clipboard items in the timestamp range
     */
    @Query("SELECT * FROM clipboard_items WHERE timestamp BETWEEN :startTimestamp AND :endTimestamp AND is_deleted = 0 ORDER BY timestamp DESC")
    suspend fun getItemsByTimestampRange(startTimestamp: Long, endTimestamp: Long): List<ClipboardItemEntity>

    /**
     * Query by content type.
     *
     * @param contentType The content type to filter by
     * @return List of clipboard items with the specified content type
     */
    @Query("SELECT * FROM clipboard_items WHERE content_type = :contentType AND is_deleted = 0 ORDER BY timestamp DESC")
    suspend fun getItemsByContentType(contentType: String): List<ClipboardItemEntity>

    /**
     * Search functionality with full-text search.
     *
     * @param query The search query
     * @return List of clipboard items matching the search query
     */
    @Query("SELECT * FROM clipboard_items WHERE content LIKE '%' || :query || '%' AND is_deleted = 0 ORDER BY timestamp DESC")
    suspend fun searchItems(query: String): List<ClipboardItemEntity>

    /**
     * Soft delete operations - mark item as deleted.
     *
     * @param id The ID of the clipboard item to soft delete
     */
    @Query("UPDATE clipboard_items SET is_deleted = 1 WHERE id = :id")
    suspend fun softDeleteItemById(id: String)

    /**
     * Soft delete operations - restore item from soft delete.
     *
     * @param id The ID of the clipboard item to restore
     */
    @Query("UPDATE clipboard_items SET is_deleted = 0 WHERE id = :id")
    suspend fun restoreItemById(id: String)

    /**
     * Get all non-deleted items ordered by timestamp (newest first).
     *
     * @return Flow of list of clipboard items
     */
    @Query("SELECT * FROM clipboard_items WHERE is_deleted = 0 ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<ClipboardItemEntity>>

    /**
     * Get favorite items.
     *
     * @return Flow of list of favorite clipboard items
     */
    @Query("SELECT * FROM clipboard_items WHERE is_favorite = 1 AND is_deleted = 0 ORDER BY timestamp DESC")
    fun getFavoriteItems(): Flow<List<ClipboardItemEntity>>

    /**
     * Toggle favorite status.
     *
     * @param id The ID of the clipboard item
     * @param isFavorite The new favorite status
     */
    @Query("UPDATE clipboard_items SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    /**
     * Cleanup old items (retention policy).
     *
     * @param timestamp The timestamp threshold - items older than this will be deleted
     */
    @Query("DELETE FROM clipboard_items WHERE timestamp < :timestamp")
    suspend fun cleanupOldItems(timestamp: Long)

    /**
     * Get items count (excluding soft deleted).
     *
     * @return The total number of non-deleted clipboard items
     */
    @Query("SELECT COUNT(*) FROM clipboard_items WHERE is_deleted = 0")
    suspend fun getItemCount(): Int

    /**
     * Get soft deleted items for potential restoration.
     *
     * @return List of soft deleted clipboard items
     */
    @Query("SELECT * FROM clipboard_items WHERE is_deleted = 1 ORDER BY timestamp DESC")
    suspend fun getSoftDeletedItems(): List<ClipboardItemEntity>

    /**
     * Permanently delete soft deleted items older than timestamp.
     *
     * @param timestamp The timestamp threshold
     * @return The number of deleted items
     */
    @Query("DELETE FROM clipboard_items WHERE is_deleted = 1 AND timestamp < :timestamp")
    suspend fun permanentlyDeleteOldSoftDeletedItems(timestamp: Long): Int

    /**
     * Delete all items (hard delete).
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
