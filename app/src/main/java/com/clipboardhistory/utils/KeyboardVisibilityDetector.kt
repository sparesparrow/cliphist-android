package com.clipboardhistory.utils

import android.app.Activity
import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Detects soft keyboard visibility changes and provides reactive state updates.
 * Designed to work with Activities and provides lifecycle-aware monitoring.
 */
class KeyboardVisibilityDetector(
    private val activity: Activity
) : DefaultLifecycleObserver {

    private val _keyboardState = MutableStateFlow(KeyboardState(isVisible = false, height = 0))
    val keyboardState: StateFlow<KeyboardState> = _keyboardState

    // Legacy flow for backward compatibility
    val isKeyboardVisible = _keyboardState.map { it.isVisible }

    private var isRegistered = false
    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        detectKeyboardVisibility()
    }

    /**
     * Data class representing keyboard state.
     */
    data class KeyboardState(
        val isVisible: Boolean,
        val height: Int = 0
    )

    /**
     * Starts monitoring keyboard visibility.
     * Should be called when the activity is resumed.
     */
    fun startMonitoring() {
        try {
            val rootView = activity.window.decorView.rootView
            rootView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
            isRegistered = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Stops monitoring keyboard visibility.
     * Should be called when the activity is paused.
     */
    fun stopMonitoring() {
        try {
            val rootView = activity.window.decorView.rootView
            rootView.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
            isRegistered = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Detects current keyboard visibility state.
     */
    private fun detectKeyboardVisibility() {
        try {
            val rootView = activity.window.decorView.rootView
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)

            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom

            // Consider keyboard visible if it's taller than 15% of screen height
            val isVisible = keypadHeight > screenHeight * 0.15

            _keyboardState.value = KeyboardState(
                isVisible = isVisible,
                height = if (isVisible) keypadHeight else 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Gets the current keyboard state synchronously.
     */
    fun getCurrentKeyboardState(): KeyboardState = _keyboardState.value

    override fun onResume(owner: LifecycleOwner) {
        startMonitoring()
    }

    override fun onPause(owner: LifecycleOwner) {
        stopMonitoring()
    }

    companion object {
        /**
         * Creates a KeyboardVisibilityDetector attached to an Activity's lifecycle.
         */
        fun attachToLifecycle(activity: Activity): KeyboardVisibilityDetector {
            return KeyboardVisibilityDetector(activity).apply {
                activity.lifecycle.addObserver(this)
            }
        }
    }
}