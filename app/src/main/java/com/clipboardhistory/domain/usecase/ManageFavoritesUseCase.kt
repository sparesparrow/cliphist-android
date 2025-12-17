package com.clipboardhistory.domain.usecase

import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for managing favorite clipboard items.
 *
 * This use case provides operations for adding/removing favorites,
 * retrieving favorite items, and managing favorite-related operations.
 */
class ManageFavoritesUseCase @Inject constructor(
    private val repository: ClipboardRepository,
) {
    /**
     * Get all favorite clipboard items.
     *
     * @return List of favorite clipboard items, ordered by timestamp (newest first)
     */
    suspend fun getFavoriteItems(): List<ClipboardItem> {
        return repository.getFavoriteItems().first()
    }

    /**
     * Add an item to favorites.
     *
     * @param itemId The ID of the item to favorite
     * @return true if the operation was successful, false otherwise
     */
    suspend fun addToFavorites(itemId: String): Boolean {
        return repository.toggleFavoriteStatus(itemId)
    }

    /**
     * Remove an item from favorites.
     *
     * @param itemId The ID of the item to unfavorite
     * @return true if the operation was successful, false otherwise
     */
    suspend fun removeFromFavorites(itemId: String): Boolean {
        return repository.toggleFavoriteStatus(itemId)
    }

    /**
     * Toggle favorite status of an item.
     *
     * @param itemId The ID of the item to toggle
     * @return true if the operation was successful, false otherwise
     */
    suspend fun toggleFavorite(itemId: String): Boolean {
        return repository.toggleFavoriteStatus(itemId)
    }

    /**
     * Check if an item is favorited.
     *
     * @param itemId The ID of the item to check
     * @return true if the item is favorited, false otherwise
     */
    suspend fun isFavorite(itemId: Long): Boolean {
        val item = repository.getItemById(itemId.toString())
        return item?.isFavorite ?: false
    }

    /**
     * Get favorite items filtered by content type.
     *
     * @param contentType The content type to filter by
     * @return List of favorite items with the specified content type
     */
    suspend fun getFavoriteItemsByType(contentType: String): List<ClipboardItem> {
        return getFavoriteItems().filter { it.contentType.name == contentType }
    }

    /**
     * Get favorite items within a time range.
     *
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return List of favorite items within the time range
     */
    suspend fun getFavoriteItemsInTimeRange(
        startTime: Long,
        endTime: Long,
    ): List<ClipboardItem> {
        return repository.getItemsByTimestampRange(startTime, endTime)
            .filter { it.isFavorite }
    }

    /**
     * Get the most recently favorited items.
     *
     * @param count Number of recent favorites to return
     * @return List of most recently favorited items
     */
    suspend fun getRecentFavorites(count: Int = 10): List<ClipboardItem> {
        return getFavoriteItems()
            .sortedByDescending { it.timestamp }
            .take(count)
    }

    /**
     * Get favorite statistics.
     *
     * @return Map containing favorite-related statistics
     */
    suspend fun getFavoriteStatistics(): Map<String, Any> {
        val favorites = getFavoriteItems()

        return mapOf(
            "totalFavorites" to favorites.size,
            "favoriteContentTypes" to favorites.groupBy { it.contentType }
                .mapValues { it.value.size },
            "oldestFavorite" to (favorites.minByOrNull { it.timestamp }?.timestamp ?: 0L),
            "newestFavorite" to (favorites.maxByOrNull { it.timestamp }?.timestamp ?: 0L),
            "averageFavoriteLength" to if (favorites.isNotEmpty()) {
                favorites.sumOf { it.content.length } / favorites.size
            } else 0,
        )
    }

    /**
     * Bulk operations on favorites.
     */
    suspend fun addMultipleToFavorites(itemIds: List<String>): Map<String, Boolean> {
        return itemIds.associateWith { itemId ->
            addToFavorites(itemId)
        }
    }

    suspend fun removeMultipleFromFavorites(itemIds: List<String>): Map<String, Boolean> {
        return itemIds.associateWith { itemId ->
            removeFromFavorites(itemId)
        }
    }

    /**
     * Clear all favorites.
     *
     * @return true if all favorites were successfully removed, false otherwise
     */
    suspend fun clearAllFavorites(): Boolean {
        val favorites = getFavoriteItems()
        var allSuccessful = true

        favorites.forEach { favorite ->
            if (!removeFromFavorites(favorite.id)) {
                allSuccessful = false
            }
        }

        return allSuccessful
    }
}