package com.clipboardhistory.presentation.ui.toolbelt

import android.content.Context
import com.clipboardhistory.domain.model.OperationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Centralized state manager for bubble visibility and operations
 * Implements Issue #12 - ToolBelt state coordination
 */
object BubbleStateManager {
    private val _privateVisible = MutableStateFlow(true)
    val privateVisible: StateFlow<Boolean> = _privateVisible

    private val _historyVisible = MutableStateFlow(true)
    val historyVisible: StateFlow<Boolean> = _historyVisible

    private val _operationMode = MutableStateFlow(OperationMode.OVERWRITE)
    val operationMode: StateFlow<OperationMode> = _operationMode

    fun toggleVisibility(context: Context, type: String, visible: Boolean) {
        when(type) {
            "Private" -> _privateVisible.value = visible
            "History" -> _historyVisible.value = visible
        }

        // Save to preferences
        context.getSharedPreferences("bubble_state", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("${type.lowercase()}_visible", visible)
            .apply()
    }

    fun setOperationMode(context: Context, mode: OperationMode) {
        _operationMode.value = mode

        context.getSharedPreferences("bubble_state", Context.MODE_PRIVATE)
            .edit()
            .putString("operation_mode", mode.name)
            .apply()
    }

    fun shouldShowBubble(isPrivate: Boolean, isHistory: Boolean): Boolean {
        if (isPrivate && !_privateVisible.value) return false
        if (isHistory && !_historyVisible.value) return false
        return true
    }

    fun loadFromPreferences(context: Context) {
        val prefs = context.getSharedPreferences("bubble_state", Context.MODE_PRIVATE)
        _privateVisible.value = prefs.getBoolean("private_visible", true)
        _historyVisible.value = prefs.getBoolean("history_visible", true)

        val modeString = prefs.getString("operation_mode", OperationMode.OVERWRITE.name)
        _operationMode.value = try {
            OperationMode.valueOf(modeString ?: OperationMode.OVERWRITE.name)
        } catch (e: IllegalArgumentException) {
            OperationMode.OVERWRITE
        }
    }
}