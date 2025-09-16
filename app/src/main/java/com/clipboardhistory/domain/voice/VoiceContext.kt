package com.clipboardhistory.domain.voice

import java.util.Locale

/**
 * Minimal runtime context used by voice components for policy and localization.
 */
data class VoiceContext(
    val isDriving: Boolean,
    val isUserAuthenticated: Boolean,
    val locale: Locale? = null
)

