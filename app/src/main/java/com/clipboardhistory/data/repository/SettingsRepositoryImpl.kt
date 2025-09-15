package com.clipboardhistory.data.repository

import com.clipboardhistory.data.encryption.EncryptionManager
import com.clipboardhistory.domain.model.ClipboardMode
import com.clipboardhistory.domain.model.ClipboardSettings
import com.clipboardhistory.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val encryptionManager: EncryptionManager
) : SettingsRepository {

    override suspend fun getSettings(): ClipboardSettings {
        val maxHistorySize = encryptionManager.getSecureString("max_history_size", "100").toIntOrNull() ?: 100
        val autoDeleteAfterHours = encryptionManager.getSecureString("auto_delete_hours", "24").toIntOrNull() ?: 24
        val enableEncryption = encryptionManager.getSecureString("enable_encryption", "true").toBoolean()
        val bubbleSize = encryptionManager.getSecureString("bubble_size", "3").toIntOrNull() ?: 3
        val bubbleOpacity = encryptionManager.getSecureString("bubble_opacity", "0.8").toFloatOrNull() ?: 0.8f
        val clipboardMode = encryptionManager.getSecureString("clipboard_mode", ClipboardMode.REPLACE.name).let {
            runCatching { ClipboardMode.valueOf(it) }.getOrDefault(ClipboardMode.REPLACE)
        }

        return ClipboardSettings(
            maxHistorySize = maxHistorySize,
            autoDeleteAfterHours = autoDeleteAfterHours,
            enableEncryption = enableEncryption,
            bubbleSize = bubbleSize,
            bubbleOpacity = bubbleOpacity,
            clipboardMode = clipboardMode
        )
    }

    override suspend fun updateSettings(settings: ClipboardSettings) {
        encryptionManager.storeSecureString("max_history_size", settings.maxHistorySize.toString())
        encryptionManager.storeSecureString("auto_delete_hours", settings.autoDeleteAfterHours.toString())
        encryptionManager.storeSecureString("enable_encryption", settings.enableEncryption.toString())
        encryptionManager.storeSecureString("bubble_size", settings.bubbleSize.toString())
        encryptionManager.storeSecureString("bubble_opacity", settings.bubbleOpacity.toString())
        encryptionManager.storeSecureString("clipboard_mode", settings.clipboardMode.name)
    }
}

