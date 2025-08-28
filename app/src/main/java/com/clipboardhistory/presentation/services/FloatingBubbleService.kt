package com.clipboardhistory.presentation.services

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
import android.provider.Settings
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
import kotlinx.coroutines.withContext
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
    private val mainScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private val bubbles = mutableListOf<BubbleView>()
    private var emptyBubble: BubbleView? = null
    private var modeBubble: BubbleView? = null
    private var currentMode = ClipboardMode.REPLACE
    private var currentBubbleOpacity: Float = 0.8f
    private var currentBubbleSizeDp: Int = DEFAULT_BUBBLE_SIZE_DP
    
    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "floating_bubble_channel"
        private const val CHANNEL_NAME = "Floating Bubbles"
        private const val DEFAULT_BUBBLE_SIZE_DP = 60
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

        // Guard: require overlay permission to draw bubbles
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, getString(R.string.permission_overlay_description), Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }
        
        // Load settings and initialize bubbles
        serviceScope.launch {
            val settings = getClipboardSettingsUseCase()
            currentMode = settings.clipboardMode
            currentBubbleOpacity = settings.bubbleOpacity.coerceIn(0.1f, 1.0f)
            currentBubbleSizeDp = mapSizeToDp(settings.bubbleSize)
            withContext(Dispatchers.Main) {
                initializeBubbles()
            }
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
            withContext(Dispatchers.Main) {
                items.take(5).forEachIndexed { index, item ->
                    createFullBubble(item.content, index)
                }
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
        bubble.params.x = 100 + (index + 1) * (currentBubbleSizeDp + BUBBLE_MARGIN_DP)
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
        imageView.alpha = currentBubbleOpacity
        
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
        val bubbleSizePx = dpToPx(currentBubbleSizeDp)
        val params = WindowManager.LayoutParams(
            bubbleSizePx,
            bubbleSizePx,
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
        
        val bubbleSizePx = dpToPx(currentBubbleSizeDp)
        val centerX = bubble.params.x + bubbleSizePx / 2
        val centerY = bubble.params.y + bubbleSizePx / 2
        
        // Snap to left or right edge
        bubble.params.x = if (centerX < screenWidth / 2) {
            0
        } else {
            screenWidth - bubbleSizePx
        }
        
        // Ensure bubble stays within screen bounds
        bubble.params.y = bubble.params.y.coerceIn(0, screenHeight - bubbleSizePx)
        
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
                    val newContent = if (currentClip.isBlank()) content else "$currentClip\n$content"
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
     * Returns bubble size in dp based on settings.bubbleSize (1..5).
     */
    private fun mapSizeToDp(sizeIndex: Int): Int {
        // Accepted values 1..5
        return when (sizeIndex.coerceIn(1, 5)) {
            1 -> 40
            2 -> 50
            3 -> 60
            4 -> 70
            else -> 80
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
}