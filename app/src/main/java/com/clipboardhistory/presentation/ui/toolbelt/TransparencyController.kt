package com.clipboardhistory.presentation.ui.toolbelt

import android.content.Context
import android.content.SharedPreferences
import android.view.WindowManager

/**
 * Global transparency controller for all bubbles
 * Implements Issue #11 - Transparency slider
 */
object TransparencyController {
    private const val PREFS_NAME = "cliphist_transparency"
    private const val KEY_OPACITY = "global_opacity"
    private const val DEFAULT_OPACITY = 0.8f

    private val bubbleCallbacks = mutableListOf<(Float) -> Unit>()

    fun setGlobalOpacity(context: Context, opacity: Float) {
        // Save to preferences
        getPrefs(context).edit()
            .putFloat(KEY_OPACITY, opacity)
            .apply()

        // Notify all registered bubbles
        bubbleCallbacks.forEach { it(opacity) }
    }

    fun getGlobalOpacity(context: Context): Float {
        return getPrefs(context).getFloat(KEY_OPACITY, DEFAULT_OPACITY)
    }

    fun registerBubble(callback: (Float) -> Unit) {
        bubbleCallbacks.add(callback)
    }

    fun unregisterBubble(callback: (Float) -> Unit) {
        bubbleCallbacks.remove(callback)
    }

    fun applyOpacityToView(params: WindowManager.LayoutParams, opacity: Float) {
        params.alpha = opacity.coerceIn(0f, 1f)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}