package com.clipboardhistory.presentation.viewmodels

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ClipboardSettings
import com.clipboardhistory.domain.usecase.AddClipboardItemUseCase
import com.clipboardhistory.domain.usecase.CleanupOldItemsUseCase
import com.clipboardhistory.domain.usecase.DeleteClipboardItemUseCase
import com.clipboardhistory.domain.usecase.GetAllClipboardItemsUseCase
import com.clipboardhistory.domain.usecase.GetClipboardSettingsUseCase
import com.clipboardhistory.domain.usecase.UpdateClipboardSettingsUseCase
import com.clipboardhistory.presentation.services.ClipboardService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main ViewModel for the clipboard history application.
 *
 * This ViewModel manages the state and business logic for the main screen,
 * including clipboard items, settings, and user interactions.
 */
@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        application: Application,
        private val getAllClipboardItemsUseCase: GetAllClipboardItemsUseCase,
        private val addClipboardItemUseCase: AddClipboardItemUseCase,
        private val deleteClipboardItemUseCase: DeleteClipboardItemUseCase,
        private val getClipboardSettingsUseCase: GetClipboardSettingsUseCase,
        private val updateClipboardSettingsUseCase: UpdateClipboardSettingsUseCase,
        private val cleanupOldItemsUseCase: CleanupOldItemsUseCase,
    ) : AndroidViewModel(application) {
        private val _uiState = MutableStateFlow(MainUiState())
        val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

        /**
         * Data class representing the UI state for the main screen.
         *
         * @property clipboardItems List of clipboard items
         * @property settings Current clipboard settings
         * @property isLoading Whether the screen is loading
         * @property error Error message if any
         * @property isServiceRunning Whether the clipboard service is running
         */
        data class MainUiState(
            val clipboardItems: List<ClipboardItem> = emptyList(),
            val settings: ClipboardSettings = ClipboardSettings(),
            val isLoading: Boolean = false,
            val error: String? = null,
            val isServiceRunning: Boolean = false,
        )

        init {
            loadClipboardItems()
            loadSettings()
            refreshServiceState()
        }

        /**
         * Checks the actual running state of ClipboardService via ActivityManager
         * and syncs the UI state. Fixes the case where the service is still alive
         * after a process restart but the ViewModel starts with isServiceRunning = false.
         */
        fun refreshServiceState() {
            val am = getApplication<Application>()
                .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            val running = am.getRunningServices(Int.MAX_VALUE)
                .any { it.service.className == ClipboardService::class.java.name }
            _uiState.value = _uiState.value.copy(isServiceRunning = running)
        }

        /**
         * Loads clipboard items from the repository.
         */
        private fun loadClipboardItems() {
            viewModelScope.launch {
                getAllClipboardItemsUseCase().collect { items ->
                    _uiState.value =
                        _uiState.value.copy(
                            clipboardItems = items,
                            isLoading = false,
                        )
                }
            }
        }

        /**
         * Loads settings from the repository.
         */
        private fun loadSettings() {
            viewModelScope.launch {
                try {
                    val settings = getClipboardSettingsUseCase()
                    _uiState.value = _uiState.value.copy(settings = settings)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            }
        }

        /**
         * Adds a new clipboard item.
         *
         * @param content The content to add
         */
        fun addClipboardItem(content: String) {
            viewModelScope.launch {
                try {
                    val result = addClipboardItemUseCase(content)
                    if (result == null) {
                        // Content already exists, show a message
                        _uiState.value = _uiState.value.copy(error = "Content already exists in clipboard history")
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            }
        }

        /**
         * Deletes a clipboard item.
         *
         * @param item The item to delete
         */
        fun deleteClipboardItem(item: ClipboardItem) {
            viewModelScope.launch {
                try {
                    deleteClipboardItemUseCase(item)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            }
        }

        /**
         * Updates the clipboard settings.
         *
         * @param settings The new settings
         */
        fun updateSettings(settings: ClipboardSettings) {
            viewModelScope.launch {
                try {
                    updateClipboardSettingsUseCase(settings)
                    _uiState.value = _uiState.value.copy(settings = settings)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            }
        }

        /**
         * Updates the service running state.
         *
         * @param isRunning Whether the service is running
         */
        fun updateServiceRunningState(isRunning: Boolean) {
            _uiState.value = _uiState.value.copy(isServiceRunning = isRunning)
        }

        /**
         * Clears the current error.
         */
        fun clearError() {
            _uiState.value = _uiState.value.copy(error = null)
        }

        /**
         * Triggers cleanup of old clipboard items.
         */
        fun cleanupOldItems() {
            viewModelScope.launch {
                try {
                    cleanupOldItemsUseCase(_uiState.value.settings)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            }
        }
    }
