package com.clipboardhistory.utils

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Detects soft keyboard visibility from a Service context.
 *
 * The standard [KeyboardVisibilityDetector] requires an Activity window.
 * This version adds a zero-size overlay window via [WindowManager] so the
 * same ViewTreeObserver-based approach works from foreground services.
 *
 * Requires the SYSTEM_ALERT_WINDOW permission (already held by FloatingBubbleService).
 */
class ServiceKeyboardDetector(private val context: Context) {

    private val _keyboardState = MutableStateFlow(KeyboardState(isVisible = false, height = 0))
    val keyboardState: StateFlow<KeyboardState> = _keyboardState
    val isKeyboardVisible = _keyboardState.map { it.isVisible }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var anchorView: View? = null
    private var isStarted = false

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        detectKeyboard()
    }

    data class KeyboardState(val isVisible: Boolean, val height: Int = 0)

    /**
     * Adds a 1×1 pixel invisible overlay window and attaches a layout listener.
     * Safe to call multiple times; starts only once.
     */
    fun startMonitoring() {
        if (isStarted) return
        try {
            val params = WindowManager.LayoutParams(
                1, 1,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                // Enable software input mode so the window gets resize events
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }

            val view = View(context).also { anchorView = it }
            windowManager.addView(view, params)
            view.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
            isStarted = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Removes the overlay window and stops monitoring.
     */
    fun stopMonitoring() {
        if (!isStarted) return
        try {
            anchorView?.let { view ->
                view.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
                windowManager.removeView(view)
            }
            anchorView = null
            isStarted = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCurrentKeyboardState(): KeyboardState = _keyboardState.value

    private fun detectKeyboard() {
        val view = anchorView ?: return
        val rect = Rect()
        view.getWindowVisibleDisplayFrame(rect)
        val screenHeight = context.resources.displayMetrics.heightPixels
        val keyboardHeight = screenHeight - rect.bottom
        val isVisible = keyboardHeight > screenHeight * 0.15
        _keyboardState.value = KeyboardState(
            isVisible = isVisible,
            height = if (isVisible) keyboardHeight else 0
        )
    }
}
