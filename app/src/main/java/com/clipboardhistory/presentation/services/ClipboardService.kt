package com.clipboardhistory.presentation.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.clipboardhistory.R
import com.clipboardhistory.domain.model.ContentType
import com.clipboardhistory.domain.usecase.AddClipboardItemUseCase
import com.clipboardhistory.domain.usecase.CleanupOldItemsUseCase
import com.clipboardhistory.domain.usecase.GetClipboardSettingsUseCase
import com.clipboardhistory.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for monitoring clipboard changes.
 *
 * This service runs in the background and monitors clipboard changes,
 * automatically saving new clipboard items to the database.
 */
@AndroidEntryPoint
class ClipboardService : Service() {
    @Inject
    lateinit var addClipboardItemUseCase: AddClipboardItemUseCase

    @Inject
    lateinit var getClipboardSettingsUseCase: GetClipboardSettingsUseCase

    @Inject
    lateinit var cleanupOldItemsUseCase: CleanupOldItemsUseCase

    private lateinit var clipboardManager: ClipboardManager
    private lateinit var notificationManager: NotificationManager
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var lastClipboardText = ""

    private val clipboardListener =
        ClipboardManager.OnPrimaryClipChangedListener {
            handleClipboardChange()
        }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "clipboard_service_channel"
        private const val CHANNEL_NAME = "Clipboard Service"
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Add clipboard listener
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)

        // Initialize with current clipboard content
        handleClipboardChange()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardManager.removePrimaryClipChangedListener(clipboardListener)
        serviceJob.cancel()
    }

    /**
     * Handles clipboard changes and saves new items.
     */
    private fun handleClipboardChange() {
        serviceScope.launch {
            try {
                val clipData = clipboardManager.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val clipText = clipData.getItemAt(0).text?.toString() ?: ""

                    // Only process if the text has changed and is not empty
                    if (clipText.isNotBlank() && clipText != lastClipboardText) {
                        lastClipboardText = clipText

                        // Determine content type
                        val contentType = determineContentType(clipText)

                        // Add to database (only if not duplicate)
                        val result = addClipboardItemUseCase(clipText, contentType)

                        // Update notification only if content was added
                        if (result != null) {
                            updateNotification(clipText)
                        }

                        // Cleanup old items periodically
                        val settings = getClipboardSettingsUseCase()
                        cleanupOldItemsUseCase(settings)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Determines the content type of the clipboard text.
     *
     * @param text The clipboard text
     * @return The determined content type
     */
    private fun determineContentType(text: String): ContentType {
        return when {
            text.startsWith("http://") || text.startsWith("https://") -> ContentType.URL
            text.startsWith("file://") -> ContentType.FILE
            text.matches(Regex(".*\\.(jpg|jpeg|png|gif|bmp|webp)$", RegexOption.IGNORE_CASE)) -> ContentType.IMAGE
            else -> ContentType.TEXT
        }
    }

    /**
     * Creates the notification channel for the service.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Channel for clipboard monitoring service"
                    setShowBadge(false)
                }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates the notification for the foreground service.
     *
     * @param lastContent The last clipboard content (optional)
     * @return The notification
     */
    private fun createNotification(lastContent: String = ""): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val contentText =
            if (lastContent.isBlank()) {
                "Monitoring clipboard changes"
            } else {
                "Last: ${lastContent.take(30)}${if (lastContent.length > 30) "..." else ""}"
            }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Clipboard History")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * Updates the notification with new clipboard content.
     *
     * @param content The new clipboard content
     */
    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
