package com.clipboardhistory.domain.usecase

import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for searching clipboard items by content.
 *
 * This use case provides advanced search functionality for clipboard history
 * with filtering, ranking, and result limiting capabilities.
 */
class SearchClipboardItemsUseCase @Inject constructor(
    private val repository: ClipboardRepository,
) {
    /**
     * Search clipboard items by query.
     *
     * @param query The search query (can be partial text)
     * @param limit Maximum number of results to return
     * @param caseSensitive Whether the search should be case sensitive
     * @return List of matching clipboard items, ordered by relevance
     */
    suspend operator fun invoke(
        query: String,
        limit: Int = 50,
        caseSensitive: Boolean = false,
    ): List<ClipboardItem> {
        if (query.isBlank()) return emptyList()

        val searchQuery = if (caseSensitive) query else query.lowercase()

        return repository.searchItems(searchQuery)
            .map { item ->
                // Calculate relevance score for better ordering
                val relevanceScore = calculateRelevanceScore(item, searchQuery, caseSensitive)
                item to relevanceScore
            }
            .sortedByDescending { it.second } // Sort by relevance score
            .map { it.first } // Extract items
            .take(limit)
    }

    /**
     * Advanced search with multiple criteria.
     *
     * @param query The search query
     * @param contentTypes Filter by specific content types
     * @param dateFrom Filter items from this timestamp
     * @param dateTo Filter items until this timestamp
     * @param onlyFavorites Only return favorite items
     * @param limit Maximum number of results
     * @return Filtered list of clipboard items
     */
    suspend fun advancedSearch(
        query: String = "",
        contentTypes: List<String>? = null,
        dateFrom: Long? = null,
        dateTo: Long? = null,
        onlyFavorites: Boolean = false,
        limit: Int = 100,
    ): List<ClipboardItem> {
        // Get base results
        val baseResults = if (query.isNotBlank()) {
            invoke(query, limit * 2) // Get more results for filtering
        } else {
            repository.getAllItems().first()
        }

        return baseResults
            .asSequence()
            .filter { item ->
                // Apply date range filter
                val inDateRange = when {
                    dateFrom != null && dateTo != null -> item.timestamp in dateFrom..dateTo
                    dateFrom != null -> item.timestamp >= dateFrom
                    dateTo != null -> item.timestamp <= dateTo
                    else -> true
                }
                inDateRange
            }
            .filter { item ->
                // Apply favorites filter
                if (onlyFavorites) item.isFavorite else true
            }
            .take(limit)
            .toList()
    }

    /**
     * Search suggestions based on partial query.
     *
     * @param partialQuery The partial search query
     * @param maxSuggestions Maximum number of suggestions to return
     * @return List of suggested search terms
     */
    suspend fun getSearchSuggestions(
        partialQuery: String,
        maxSuggestions: Int = 5,
    ): List<String> {
        if (partialQuery.isBlank()) return emptyList()

        val allItems = repository.getAllItems().first()
        val suggestions = mutableSetOf<String>()

        allItems.forEach { item ->
            // Extract words that start with the partial query
            val words = item.content.split(Regex("\\s+"))
            words.forEach { word ->
                if (word.startsWith(partialQuery, ignoreCase = true) &&
                    word.length > partialQuery.length) {
                    suggestions.add(word)
                }
            }

            // Also suggest complete phrases that contain the query
            if (item.content.contains(partialQuery, ignoreCase = true)) {
                val startIndex = item.content.indexOf(partialQuery, ignoreCase = true)
                val endIndex = minOf(startIndex + 50, item.content.length)
                val suggestion = item.content.substring(startIndex, endIndex)
                if (suggestion.length > partialQuery.length) {
                    suggestions.add(suggestion)
                }
            }
        }

        return suggestions.take(maxSuggestions)
    }

    /**
     * Calculate relevance score for search result ordering.
     *
     * Higher scores indicate better matches:
     * - Exact matches get highest score
     * - Starts with query gets higher score than contains
     * - Case-sensitive matches get slight boost
     * - Shorter content gets slight preference (more focused)
     */
    private fun calculateRelevanceScore(
        item: ClipboardItem,
        query: String,
        caseSensitive: Boolean,
    ): Double {
        val content = if (caseSensitive) item.content else item.content.lowercase()
        var score = 0.0

        // Exact match gets highest score
        if (content == query) {
            score += 100.0
        }
        // Starts with query gets high score
        else if (content.startsWith(query)) {
            score += 50.0
        }
        // Contains query gets medium score
        else if (content.contains(query)) {
            score += 25.0
        }

        // Case-sensitive match gets small boost
        if (caseSensitive && item.content.contains(query)) {
            score += 5.0
        }

        // Shorter content gets slight preference
        if (item.content.length < 100) {
            score += 2.0
        }

        // Recent items get slight boost
        val daysSinceCreation = (System.currentTimeMillis() - item.timestamp) / (1000 * 60 * 60 * 24.0)
        if (daysSinceCreation < 7) {
            score += 3.0
        }

        // Favorite items get boost
        if (item.isFavorite) {
            score += 1.0
        }

        return score
    }
}