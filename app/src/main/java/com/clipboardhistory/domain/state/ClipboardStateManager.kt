package com.clipboardhistory.domain.state

import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ClipboardSettings
import com.clipboardhistory.domain.model.ContentType
import com.clipboardhistory.domain.repository.ClipboardRepository
import com.clipboardhistory.domain.repository.ClipboardStatistics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized state management for clipboard operations.
 *
 * This class manages the global application state for clipboard functionality,
 * providing reactive state flows and state mutation operations.
 */
@Singleton
class ClipboardStateManager @Inject constructor(
    private val repository: ClipboardRepository,
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // Core state flows
    private val _clipboardItems = MutableStateFlow<List<ClipboardItem>>(emptyList())
    val clipboardItems: StateFlow<List<ClipboardItem>> = _clipboardItems.asStateFlow()

    private val _favoriteItems = MutableStateFlow<List<ClipboardItem>>(emptyList())
    val favoriteItems: StateFlow<List<ClipboardItem>> = _favoriteItems.asStateFlow()

    private val _currentItem = MutableStateFlow<ClipboardItem?>(null)
    val currentItem: StateFlow<ClipboardItem?> = _currentItem.asStateFlow()

    private val _settings = MutableStateFlow(ClipboardSettings())
    val settings: StateFlow<ClipboardSettings> = _settings.asStateFlow()

    private val _statistics = MutableStateFlow<ClipboardStatistics?>(null)
    val statistics: StateFlow<ClipboardStatistics?> = _statistics.asStateFlow()

    // UI state flows
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        initializeState()
    }

    /**
     * Initialize state by loading data from repository.
     */
    private fun initializeState() {
        scope.launch {
            loadClipboardItems()
            loadFavoriteItems()
            loadSettings()
            loadStatistics()
        }
    }

    /**
     * Load all clipboard items from repository.
     */
    suspend fun loadClipboardItems() {
        try {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getAllItems().collectLatest { items ->
                _clipboardItems.value = items.filter { !it.isDeleted }
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message
            )
        }
    }

    /**
     * Load favorite items from repository.
     */
    suspend fun loadFavoriteItems() {
        try {
            _favoriteItems.value = repository.getFavoriteItems().first()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
        }
    }

    /**
     * Load application settings.
     */
    suspend fun loadSettings() {
        try {
            _settings.value = repository.getSettings()
        } catch (e: Exception) {
            // Use default settings if loading fails
            _settings.value = ClipboardSettings()
        }
    }

    /**
     * Load clipboard statistics.
     */
    suspend fun loadStatistics() {
        try {
            _statistics.value = repository.getStatistics()
        } catch (e: Exception) {
            _statistics.value = null
        }
    }

    /**
     * Add a new clipboard item.
     */
    suspend fun addItem(content: String, contentType: String = "text/plain"): Result<ClipboardItem> {
        return try {
            _syncState.value = SyncState.SYNCING

            val newItem = ClipboardItem(
                id = System.currentTimeMillis().toString(), // Use timestamp as ID
                content = content,
                contentType = ContentType.TEXT, // Map string to enum
                timestamp = System.currentTimeMillis(),
                size = content.length
            )

            repository.insertItem(newItem)

            // Reload items to get the new item
            loadClipboardItems()

            // Find and set as current item
            val foundItem = _clipboardItems.value.find { it.content == content && it.timestamp == newItem.timestamp }
            if (foundItem != null) {
                _currentItem.value = foundItem
                _syncState.value = SyncState.IDLE
                Result.success(foundItem)
            } else {
                _syncState.value = SyncState.ERROR
                Result.failure(Exception("Failed to retrieve created item"))
            }
        } catch (e: Exception) {
            _syncState.value = SyncState.ERROR
            _uiState.value = _uiState.value.copy(error = e.message)
            Result.failure(e)
        }
    }

    /**
     * Update an existing clipboard item.
     */
    suspend fun updateItem(item: ClipboardItem): Result<Unit> {
        return try {
            _syncState.value = SyncState.SYNCING
            repository.updateItem(item)
            loadClipboardItems()
            _syncState.value = SyncState.IDLE
            Result.success(Unit)
        } catch (e: Exception) {
            _syncState.value = SyncState.ERROR
            _uiState.value = _uiState.value.copy(error = e.message)
            Result.failure(e)
        }
    }

    /**
     * Delete an item (soft delete).
     */
    suspend fun deleteItem(itemId: String): Result<Unit> {
        return try {
            _syncState.value = SyncState.SYNCING
            val success = repository.softDeleteItem(itemId)
            if (success) {
                loadClipboardItems()
                _syncState.value = SyncState.IDLE
                Result.success(Unit)
            } else {
                _syncState.value = SyncState.ERROR
                Result.failure(Exception("Failed to delete item"))
            }
        } catch (e: Exception) {
            _syncState.value = SyncState.ERROR
            _uiState.value = _uiState.value.copy(error = e.message)
            Result.failure(e)
        }
    }

    /**
     * Toggle favorite status of an item.
     */
    suspend fun toggleFavorite(itemId: String): Result<Boolean> {
        return try {
            val item = _clipboardItems.value.find { it.id == itemId }
            if (item != null) {
                val success = repository.toggleFavoriteStatus(itemId)
                if (success) {
                    loadFavoriteItems()
                    loadClipboardItems() // Refresh main list
                    val newFavoriteStatus = !item.isFavorite
                    Result.success(newFavoriteStatus)
                } else {
                    Result.failure(Exception("Failed to toggle favorite"))
                }
            } else {
                Result.failure(Exception("Item not found"))
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
            Result.failure(e)
        }
    }

    /**
     * Set current active item.
     */
    fun setCurrentItem(item: ClipboardItem?) {
        _currentItem.value = item
    }

    /**
     * Update application settings.
     */
    suspend fun updateSettings(newSettings: ClipboardSettings): Result<Unit> {
        return try {
            repository.updateSettings(newSettings)
            _settings.value = newSettings
            Result.success(Unit)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
            Result.failure(e)
        }
    }

    /**
     * Perform search operation.
     */
    suspend fun searchItems(query: String): Result<List<ClipboardItem>> {
        return try {
            _searchState.value = _searchState.value.copy(isSearching = true)
            val results = repository.searchItems(query)
            _searchState.value = _searchState.value.copy(
                isSearching = false,
                searchResults = results,
                lastQuery = query
            )
            Result.success(results)
        } catch (e: Exception) {
            _searchState.value = _searchState.value.copy(
                isSearching = false,
                error = e.message
            )
            Result.failure(e)
        }
    }

    /**
     * Clear search results.
     */
    fun clearSearch() {
        _searchState.value = SearchState()
    }

    /**
     * Clear any error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Get items filtered by content type.
     */
    fun getItemsByType(contentType: String): List<ClipboardItem> {
        return _clipboardItems.value.filter { it.contentType.name == contentType }
    }


    /**
     * Refresh all data from repository.
     */
    suspend fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        try {
            loadClipboardItems()
            loadFavoriteItems()
            loadStatistics()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isRefreshing = false,
                error = e.message
            )
        }
    }

    /**
     * UI state data class.
     */
    data class UiState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val selectedItemId: Long? = null,
    )

    /**
     * Search state data class.
     */
    data class SearchState(
        val isSearching: Boolean = false,
        val searchResults: List<ClipboardItem> = emptyList(),
        val lastQuery: String = "",
        val error: String? = null,
    )

    /**
     * Synchronization state enum.
     */
    enum class SyncState {
        IDLE,
        SYNCING,
        ERROR,
    }
}