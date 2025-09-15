package com.clipboardhistory.domain.repository

import com.clipboardhistory.domain.model.ClipboardSettings

/**
 * Repository interface responsible solely for reading and writing settings.
 */
interface SettingsRepository {
    suspend fun getSettings(): ClipboardSettings
    suspend fun updateSettings(settings: ClipboardSettings)
}

