package com.clipboardhistory.utils

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Detects soft keyboard visibility changes and provides reactive state updates.
 *
 * Uses ViewTreeObserver to monitor global layout changes and determine
 * when the soft keyboard is shown or hidden based on screen real estate.
 */
class KeyboardVisibilityDetector(
    private val context: android.content.Context
) : DefaultLifecycleObserver {

    private val _isKeyboardVisible = MutableStateFlow(false)
    val isKeyboardVisible: StateFlow<Boolean> = _isKeyboardVisible

    private val _keyboardHeight = MutableStateFlow(0)
    val keyboardHeight: StateFlow<Int> = _keyboardHeight

    private var lastKeyboardHeight = 0
    private var isRegistered = false

    // Threshold for considering keyboard visible (minimum height in pixels)
    private val KEYBOARD_HEIGHT_THRESHOLD = 150

    // Debounce time to avoid rapid show/hide events (in milliseconds)
    private val DEBOUNCE_DELAY_MS = 100L
    private var lastChangeTime = 0L

    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        detectKeyboardVisibility()
    }

    /**
     * Gets the root view for keyboard monitoring.
     * Returns null if not available (e.g., in services without UI).
     */
    private fun getRootView(): android.view.View? {
        return try {
            when (context) {
                is android.app.Activity -> context.window.decorView.rootView
                is android.app.Service -> {
                    // Services don't have direct access to window decor view
                    // This functionality may not work in services
                    null
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Starts monitoring keyboard visibility.
     * Should be called when the activity is resumed.
     */
    fun startMonitoring() {
        if (isRegistered) return

        try {
            val rootView = getRootView()
            if (rootView != null) {
                rootView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
                isRegistered = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Stops monitoring keyboard visibility.
     * Should be called when the activity is paused.
     */
    fun stopMonitoring() {
        if (!isRegistered) return

        try {
            val rootView = getRootView()
            if (rootView != null) {
                rootView.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
                isRegistered = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Detects keyboard visibility by comparing screen real estate.
     */
    private fun detectKeyboardVisibility() {
        try {
            val rootView = getRootView()
            if (rootView == null) {
                // Can't detect keyboard in services without UI
                return
            }

            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)

            val screenHeight = rootView.height
            val visibleHeight = rect.bottom - rect.top
            val heightDiff = screenHeight - visibleHeight

            val currentTime = System.currentTimeMillis()

            // Debounce rapid changes
            if (currentTime - lastChangeTime < DEBOUNCE_DELAY_MS) {
                return
            }

            val isVisible = heightDiff > KEYBOARD_HEIGHT_THRESHOLD
            val keyboardHeight = if (isVisible) heightDiff else 0

            // Only emit change if state actually changed
            if (isVisible != _isKeyboardVisible.value || keyboardHeight != lastKeyboardHeight) {
                _isKeyboardVisible.value = isVisible
                _keyboardHeight.value = keyboardHeight
                lastKeyboardHeight = keyboardHeight
                lastChangeTime = currentTime
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Gets current keyboard visibility state.
     */
    fun getCurrentKeyboardState(): KeyboardState {
        return KeyboardState(
            isVisible = _isKeyboardVisible.value,
            height = _keyboardHeight.value
        )
    }

    /**
     * Lifecycle observer implementation.
     */
    override fun onResume(owner: LifecycleOwner) {
        startMonitoring()
    }

    override fun onPause(owner: LifecycleOwner) {
        stopMonitoring()
    }

    /**
     * Data class representing keyboard state.
     */
    data class KeyboardState(
        val isVisible: Boolean,
        val height: Int
    )

    /**
     * Companion object with utility methods.
     */
    companion object {
        /**
         * Creates a KeyboardVisibilityDetector for the given context.
         * For Activities, consider using attachToLifecycle for automatic lifecycle management.
         */
        fun create(context: android.content.Context): KeyboardVisibilityDetector {
            return KeyboardVisibilityDetector(context)
        }

        /**
         * Creates a KeyboardVisibilityDetector attached to an Activity's lifecycle.
         */
        fun attachToLifecycle(activity: android.app.Activity): KeyboardVisibilityDetector {
            return KeyboardVisibilityDetector(activity).apply {
                if (activity is ComponentActivity) {
                    activity.lifecycle.addObserver(this)
                }
            }
        }
    }
}