# Create clipboard service
clipboard_service = '''package com.clipboardhistory.presentation.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.clipboardhistory.R
import com.clipboardhistory.domain.model.ContentType
import com.clipboardhistory.domain.usecase.AddClipboardItemUseCase
import com.clipboardhistory.domain.usecase.GetClipboardSettingsUseCase
import com.clipboardhistory.domain.usecase.CleanupOldItemsUseCase
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
    
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
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
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
                        
                        // Add to database
                        addClipboardItemUseCase(clipText, contentType)
                        
                        // Update notification
                        updateNotification(clipText)
                        
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
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
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
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val contentText = if (lastContent.isBlank()) {
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
}'''

# Create floating bubble service
floating_bubble_service = '''package com.clipboardhistory.presentation.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.clipboardhistory.R
import com.clipboardhistory.domain.model.ClipboardMode
import com.clipboardhistory.domain.usecase.GetAllClipboardItemsUseCase
import com.clipboardhistory.domain.usecase.GetClipboardSettingsUseCase
import com.clipboardhistory.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for managing floating clipboard bubbles.
 * 
 * This service creates and manages floating bubbles that provide
 * quick access to clipboard functionality.
 */
@AndroidEntryPoint
class FloatingBubbleService : Service() {
    
    @Inject
    lateinit var getAllClipboardItemsUseCase: GetAllClipboardItemsUseCase
    
    @Inject
    lateinit var getClipboardSettingsUseCase: GetClipboardSettingsUseCase
    
    private lateinit var windowManager: WindowManager
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var notificationManager: NotificationManager
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private val bubbles = mutableListOf<BubbleView>()
    private var emptyBubble: BubbleView? = null
    private var modeBubble: BubbleView? = null
    private var currentMode = ClipboardMode.REPLACE
    
    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "floating_bubble_channel"
        private const val CHANNEL_NAME = "Floating Bubbles"
        private const val BUBBLE_SIZE_DP = 60
        private const val BUBBLE_MARGIN_DP = 16
    }
    
    /**
     * Data class representing a floating bubble view.
     * 
     * @property view The actual view
     * @property params The window layout parameters
     * @property content The clipboard content (null for empty bubble)
     * @property type The type of bubble
     */
    data class BubbleView(
        val view: View,
        val params: WindowManager.LayoutParams,
        val content: String? = null,
        val type: BubbleType
    )
    
    /**
     * Enumeration of bubble types.
     */
    enum class BubbleType {
        EMPTY,
        FULL,
        MODE
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Load settings and initialize bubbles
        serviceScope.launch {
            val settings = getClipboardSettingsUseCase()
            currentMode = settings.clipboardMode
            initializeBubbles()
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        removeAllBubbles()
        serviceJob.cancel()
    }
    
    /**
     * Initializes the floating bubbles.
     */
    private fun initializeBubbles() {
        // Create empty bubble
        createEmptyBubble()
        
        // Create mode bubble
        createModeBubble()
        
        // Load existing clipboard items and create bubbles
        serviceScope.launch {
            val items = getAllClipboardItemsUseCase().first()
            items.take(5).forEachIndexed { index, item ->
                createFullBubble(item.content, index)
            }
        }
    }
    
    /**
     * Creates the empty bubble.
     */
    private fun createEmptyBubble() {
        val bubbleView = createBubbleView(BubbleType.EMPTY)
        val bubble = BubbleView(bubbleView, createLayoutParams(), null, BubbleType.EMPTY)
        
        // Position at top-right
        bubble.params.x = 100
        bubble.params.y = 100
        
        // Set click listener
        bubbleView.setOnClickListener {
            handleEmptyBubbleClick()
        }
        
        setupDragBehavior(bubble)
        
        try {
            windowManager.addView(bubbleView, bubble.params)
            emptyBubble = bubble
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Creates a full bubble with content.
     * 
     * @param content The clipboard content
     * @param index The index for positioning
     */
    private fun createFullBubble(content: String, index: Int) {
        val bubbleView = createBubbleView(BubbleType.FULL)
        val bubble = BubbleView(bubbleView, createLayoutParams(), content, BubbleType.FULL)
        
        // Position relative to empty bubble
        bubble.params.x = 100 + (index + 1) * (BUBBLE_SIZE_DP + BUBBLE_MARGIN_DP)
        bubble.params.y = 100
        
        // Set click listener
        bubbleView.setOnClickListener {
            handleFullBubbleClick(content)
        }
        
        setupDragBehavior(bubble)
        
        try {
            windowManager.addView(bubbleView, bubble.params)
            bubbles.add(bubble)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Creates the mode toggle bubble.
     */
    private fun createModeBubble() {
        val bubbleView = createBubbleView(BubbleType.MODE)
        val bubble = BubbleView(bubbleView, createLayoutParams(), null, BubbleType.MODE)
        
        // Position at bottom-right
        bubble.params.x = 100
        bubble.params.y = 200
        
        // Set click listener
        bubbleView.setOnClickListener {
            handleModeBubbleClick()
        }
        
        setupDragBehavior(bubble)
        
        try {
            windowManager.addView(bubbleView, bubble.params)
            modeBubble = bubble
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Creates a bubble view based on the type.
     * 
     * @param type The bubble type
     * @return The created view
     */
    private fun createBubbleView(type: BubbleType): View {
        val imageView = ImageView(this)
        
        val resourceId = when (type) {
            BubbleType.EMPTY -> R.drawable.ic_bubble_empty
            BubbleType.FULL -> R.drawable.ic_bubble_full
            BubbleType.MODE -> when (currentMode) {
                ClipboardMode.REPLACE -> R.drawable.ic_bubble_replace
                ClipboardMode.EXTEND -> R.drawable.ic_bubble_extend
            }
        }
        
        imageView.setImageResource(resourceId)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        
        return imageView
    }
    
    /**
     * Creates layout parameters for a bubble.
     * 
     * @return The layout parameters
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        val params = WindowManager.LayoutParams(
            dpToPx(BUBBLE_SIZE_DP),
            dpToPx(BUBBLE_SIZE_DP),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.TOP or Gravity.START
        return params
    }
    
    /**
     * Sets up drag behavior for a bubble.
     * 
     * @param bubble The bubble to setup
     */
    private fun setupDragBehavior(bubble: BubbleView) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        bubble.view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubble.params.x
                    initialY = bubble.params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    bubble.params.x = initialX + (event.rawX - initialTouchX).toInt()
                    bubble.params.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(bubble.view, bubble.params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Snap to edge
                    snapToEdge(bubble)
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * Snaps a bubble to the nearest screen edge.
     * 
     * @param bubble The bubble to snap
     */
    private fun snapToEdge(bubble: BubbleView) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        val centerX = bubble.params.x + dpToPx(BUBBLE_SIZE_DP) / 2
        val centerY = bubble.params.y + dpToPx(BUBBLE_SIZE_DP) / 2
        
        // Snap to left or right edge
        bubble.params.x = if (centerX < screenWidth / 2) {
            0
        } else {
            screenWidth - dpToPx(BUBBLE_SIZE_DP)
        }
        
        // Ensure bubble stays within screen bounds
        bubble.params.y = bubble.params.y.coerceIn(0, screenHeight - dpToPx(BUBBLE_SIZE_DP))
        
        try {
            windowManager.updateViewLayout(bubble.view, bubble.params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Handles empty bubble click.
     */
    private fun handleEmptyBubbleClick() {
        try {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val clipText = clipData.getItemAt(0).text?.toString() ?: ""
                if (clipText.isNotBlank()) {
                    // Convert empty bubble to full bubble
                    emptyBubble?.let { bubble ->
                        windowManager.removeView(bubble.view)
                        createFullBubble(clipText, bubbles.size)
                        createEmptyBubble() // Create new empty bubble
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Handles full bubble click.
     * 
     * @param content The clipboard content
     */
    private fun handleFullBubbleClick(content: String) {
        try {
            when (currentMode) {
                ClipboardMode.REPLACE -> {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("clipboard", content))
                }
                ClipboardMode.EXTEND -> {
                    val currentClip = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                    val newContent = if (currentClip.isBlank()) content else "$currentClip\\n$content"
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("clipboard", newContent))
                }
            }
            
            Toast.makeText(this, "Content copied to clipboard", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Handles mode bubble click.
     */
    private fun handleModeBubbleClick() {
        currentMode = when (currentMode) {
            ClipboardMode.REPLACE -> ClipboardMode.EXTEND
            ClipboardMode.EXTEND -> ClipboardMode.REPLACE
        }
        
        // Update mode bubble icon
        modeBubble?.let { bubble ->
            val imageView = bubble.view as ImageView
            val resourceId = when (currentMode) {
                ClipboardMode.REPLACE -> R.drawable.ic_bubble_replace
                ClipboardMode.EXTEND -> R.drawable.ic_bubble_extend
            }
            imageView.setImageResource(resourceId)
        }
        
        val modeText = when (currentMode) {
            ClipboardMode.REPLACE -> "Replace mode"
            ClipboardMode.EXTEND -> "Extend mode"
        }
        
        Toast.makeText(this, modeText, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Removes all bubbles from the window.
     */
    private fun removeAllBubbles() {
        try {
            emptyBubble?.let { windowManager.removeView(it.view) }
            modeBubble?.let { windowManager.removeView(it.view) }
            bubbles.forEach { windowManager.removeView(it.view) }
            
            emptyBubble = null
            modeBubble = null
            bubbles.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Converts dp to pixels.
     * 
     * @param dp The dp value
     * @return The pixel value
     */
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    
    /**
     * Creates the notification channel for the service.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for floating bubble service"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Creates the notification for the foreground service.
     * 
     * @return The notification
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Bubbles")
            .setContentText("Clipboard bubbles active")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}'''

# Write the service files
with open('app/src/main/java/com/clipboardhistory/presentation/services/ClipboardService.kt', 'w') as f:
    f.write(clipboard_service)

with open('app/src/main/java/com/clipboardhistory/presentation/services/FloatingBubbleService.kt', 'w') as f:
    f.write(floating_bubble_service)

print("Service classes for clipboard monitoring and floating bubbles created!")