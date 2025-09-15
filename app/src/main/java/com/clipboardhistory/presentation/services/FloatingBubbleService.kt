package com.clipboardhistory.presentation.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.SearchManager
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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.clipboardhistory.R
import com.clipboardhistory.domain.model.BubbleState
import com.clipboardhistory.domain.model.BubbleType
import com.clipboardhistory.domain.usecase.AddClipboardItemUseCase
import com.clipboardhistory.domain.usecase.GetAllClipboardItemsUseCase
import com.clipboardhistory.domain.usecase.GetClipboardSettingsUseCase
import com.clipboardhistory.presentation.MainActivity
import com.clipboardhistory.presentation.ui.components.BubbleView
import com.clipboardhistory.presentation.ui.components.BubbleViewFactory
import com.clipboardhistory.presentation.ui.components.HighlightedAreaView
import com.clipboardhistory.domain.model.ContentAnalyzer
import com.clipboardhistory.domain.model.SmartAction
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
 * quick access to clipboard functionality with theme support and state management.
 */
@AndroidEntryPoint
class FloatingBubbleService : Service() {
    
    @Inject
    lateinit var getAllClipboardItemsUseCase: GetAllClipboardItemsUseCase
    
    @Inject
    lateinit var getClipboardSettingsUseCase: GetClipboardSettingsUseCase
    
    @Inject
    lateinit var addClipboardItemUseCase: AddClipboardItemUseCase
    
    private lateinit var windowManager: WindowManager
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var notificationManager: NotificationManager
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val mainScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    // Service monitoring
    private var isServiceRunning = false
    private var lastActivityTime = System.currentTimeMillis()
    
    private val bubbles = mutableListOf<BubbleData>()
    private var emptyBubble: BubbleData? = null
    private var currentBubbleOpacity: Float = 0.8f
    private var currentBubbleSizeDp: Int = DEFAULT_BUBBLE_SIZE_DP
    private var currentThemeName: String = "Default"
    private var currentBubbleType: BubbleType = BubbleType.CIRCLE
    
    // Enhanced drag-and-drop system
    private var highlightedAreaView: HighlightedAreaView? = null
    private var isDragging = false
    private var draggedBubble: BubbleData? = null
    private var edgeThreshold = 100 // Distance from edge to trigger activation
    private var isEdgeActivated = false
    private var currentDragEdge: HighlightedAreaView.ActivationEdge = HighlightedAreaView.ActivationEdge.NONE
    
    // 2-second append window variables
    private var lastCopyTime: Long = 0
    private var appendWindowActive: Boolean = false
    
    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "floating_bubble_channel"
        private const val CHANNEL_NAME = "Floating Bubbles"
        private const val DEFAULT_BUBBLE_SIZE_DP = 60
        private const val BUBBLE_MARGIN_DP = 16
    }
    
    /**
     * Data class representing a floating bubble with its data and state.
     * 
     * @property view The bubble view
     * @property params The window layout parameters
     * @property content The clipboard content (null for empty bubble)
     * @property state The current bubble state
     * @property originalState The original state before edge changes
     * @property bubbleType The type/shape of the bubble
     */
    data class BubbleData(
        var view: BubbleView,
        val params: WindowManager.LayoutParams,
        var content: String? = null,
        var state: BubbleState,
        var originalState: BubbleState = state,
        val bubbleType: BubbleType = BubbleType.CIRCLE
    )
    
    override fun onCreate() {
        super.onCreate()
        
        try {
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
            
            // Load settings and initialize bubbles with retry mechanism
            initializeServiceWithRetry()
            
            // Start service monitoring
            startServiceMonitoring()
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Attempt to restart service after delay
            serviceScope.launch {
                kotlinx.coroutines.delay(5000)
                val intent = Intent(applicationContext, this@FloatingBubbleService::class.java)
                startService(intent)
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure service stays alive and restarts if killed
        return START_STICKY
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Restart service if app is removed from recent tasks
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // Handle low memory gracefully
        serviceScope.launch {
            // Reduce bubble count if needed
            if (bubbles.size > 3) {
                withContext(Dispatchers.Main) {
                    removeExcessBubbles()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        removeAllBubbles()
        serviceJob.cancel()
        
        // Attempt to restart service if it was killed unexpectedly
        if (System.currentTimeMillis() - lastActivityTime < 30000) { // 30 seconds
            val intent = Intent(applicationContext, this::class.java)
            startService(intent)
        }
    }
    
    /**
     * Starts service monitoring to ensure it stays alive.
     */
    private fun startServiceMonitoring() {
        isServiceRunning = true
        serviceScope.launch {
            while (isServiceRunning) {
                try {
                    lastActivityTime = System.currentTimeMillis()
                    
                    // Check if bubbles are still visible
                    if (emptyBubble == null && bubbles.isEmpty()) {
                        // Reinitialize if all bubbles are gone
                        withContext(Dispatchers.Main) {
                            initializeBubbles()
                        }
                    }
                    
                    // Update notification to keep service alive
                    notificationManager.notify(NOTIFICATION_ID, createNotification())
                    
                    kotlinx.coroutines.delay(30000) // Check every 30 seconds
                } catch (e: Exception) {
                    e.printStackTrace()
                    kotlinx.coroutines.delay(5000) // Shorter delay on error
                }
            }
        }
    }
    
    /**
     * Initializes the service with retry mechanism.
     */
    private fun initializeServiceWithRetry() {
        serviceScope.launch {
            try {
                val settings = getClipboardSettingsUseCase()
                currentBubbleOpacity = settings.bubbleOpacity.coerceIn(0.1f, 1.0f)
                currentBubbleSizeDp = mapSizeToDp(settings.bubbleSize)
                currentThemeName = settings.selectedTheme
                currentBubbleType = settings.bubbleType
                withContext(Dispatchers.Main) {
                    initializeBubbles()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Retry after 3 seconds
                kotlinx.coroutines.delay(3000)
                initializeServiceWithRetry()
            }
        }
    }
    
    /**
     * Initializes the floating bubbles.
     */
    private fun initializeBubbles() {
        try {
            // Create empty bubble
            createEmptyBubble()
            
            // Load existing clipboard items and create bubbles
            serviceScope.launch {
                try {
                    val items = getAllClipboardItemsUseCase().first()
                    withContext(Dispatchers.Main) {
                        items.take(5).forEachIndexed { index, item ->
                            createFullBubble(item.content, index)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Continue with just empty bubble if loading fails
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Creates the empty bubble.
     */
    private fun createEmptyBubble() {
        try {
            val bubbleView = BubbleViewFactory.createBubbleView(
                context = this,
                themeName = currentThemeName,
                state = BubbleState.EMPTY,
                bubbleType = currentBubbleType,
                content = null,
                opacity = currentBubbleOpacity
            )
            
            val bubble = BubbleData(
                view = bubbleView,
                params = createLayoutParams(),
                content = null,
                state = BubbleState.EMPTY,
                bubbleType = currentBubbleType
            )
            
            // Position at top-right
            bubble.params.x = 100
            bubble.params.y = 100
            
                    // Set click listener
        bubbleView.setOnClickListener {
            if (currentBubbleType == BubbleType.CUBE) {
                // For cube bubbles, flash the content if available
                val clipData = clipboardManager.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val clipText = clipData.getItemAt(0).text?.toString()
                    if (!clipText.isNullOrEmpty()) {
                        bubbleView.flashContent(clipText)
                        // Also copy to clipboard as normal behavior
                        handleEmptyBubbleClick()
                    } else {
                        handleEmptyBubbleClick()
                    }
                } else {
                    handleEmptyBubbleClick()
                }
            } else {
                handleEmptyBubbleClick()
            }
        }
            
            setupDragBehavior(bubble)
            
            try {
                windowManager.addView(bubbleView, bubble.params)
                emptyBubble = bubble
            } catch (e: Exception) {
                e.printStackTrace()
                // Try to recover by recreating the bubble
                serviceScope.launch {
                    kotlinx.coroutines.delay(1000)
                    withContext(Dispatchers.Main) {
                        createEmptyBubble()
                    }
                }
            }
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
        val bubbleView = BubbleViewFactory.createBubbleView(
            context = this,
            themeName = currentThemeName,
            state = BubbleState.STORING,
            bubbleType = currentBubbleType,
            content = content,
            opacity = currentBubbleOpacity
        )
        
        val bubble = BubbleData(
            view = bubbleView,
            params = createLayoutParams(),
            content = content,
            state = BubbleState.STORING,
            bubbleType = currentBubbleType
        )
        
        // Position relative to empty bubble
        bubble.params.x = 100 + (index + 1) * (currentBubbleSizeDp + BUBBLE_MARGIN_DP)
        bubble.params.y = 100
        
        // Set click listener
        bubbleView.setOnClickListener {
            if (currentBubbleType == BubbleType.CUBE) {
                // For cube bubbles, flash the content first
                bubbleView.flashContent(content ?: "")
                // Then handle the normal click behavior
                handleFullBubbleClick(content)
            } else {
                handleFullBubbleClick(content)
            }
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
     * Sets up enhanced drag behavior with edge-based activation and smart actions.
     * 
     * @param bubble The bubble to setup
     */
    private fun setupDragBehavior(bubble: BubbleData) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var dragStarted = false
        
        bubble.view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubble.params.x
                    initialY = bubble.params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    dragStarted = false
                    isEdgeActivated = false
                    currentDragEdge = HighlightedAreaView.ActivationEdge.NONE
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
                    
                    // Start drag if moved enough distance
                    if (!dragStarted && distance > 20) {
                        dragStarted = true
                        isDragging = true
                        draggedBubble = bubble
                    }
                    
                    if (dragStarted) {
                        bubble.params.x = initialX + deltaX.toInt()
                        bubble.params.y = initialY + deltaY.toInt()
                        
                        // Check for edge activation
                        checkEdgeActivation(event.rawX, event.rawY)
                        
                        try {
                            windowManager.updateViewLayout(bubble.view, bubble.params)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragStarted && bubble.content != null && isEdgeActivated) {
                        // Check if bubble was dropped on an action area
                        val smartAction = highlightedAreaView?.getSmartActionForPosition(
                            event.rawX,
                            event.rawY - (highlightedAreaView?.y?.toFloat() ?: 0f)
                        )
                        
                        if (smartAction != null) {
                            // Handle the smart action
                            handleSmartAction(bubble, smartAction)
                        } else {
                            // Fallback to basic action
                            val action = highlightedAreaView?.getActionForPosition(
                                event.rawX,
                                event.rawY - (highlightedAreaView?.y?.toFloat() ?: 0f)
                            )
                            if (action != null) {
                                handleDropAction(bubble, action)
                            }
                        }
                        
                        // Hide highlighted areas
                        hideHighlightedAreas()
                    }
                    
                    // Reset states
                    isDragging = false
                    draggedBubble = null
                    isEdgeActivated = false
                    currentDragEdge = HighlightedAreaView.ActivationEdge.NONE
                    
                    // Snap to edge
                    snapToEdge(bubble)
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * Checks for edge activation and shows action areas accordingly.
     */
    private fun checkEdgeActivation(x: Float, y: Float) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        val edge = when {
            x <= edgeThreshold -> HighlightedAreaView.ActivationEdge.LEFT
            x >= screenWidth - edgeThreshold -> HighlightedAreaView.ActivationEdge.RIGHT
            y <= edgeThreshold -> HighlightedAreaView.ActivationEdge.TOP
            y >= screenHeight - edgeThreshold -> HighlightedAreaView.ActivationEdge.BOTTOM
            else -> HighlightedAreaView.ActivationEdge.NONE
        }
        
        if (edge != HighlightedAreaView.ActivationEdge.NONE && !isEdgeActivated) {
            isEdgeActivated = true
            currentDragEdge = edge
            showHighlightedAreas()
            
            // Update edge glow
            val glowAlpha = when (edge) {
                HighlightedAreaView.ActivationEdge.LEFT -> 1f - (x / edgeThreshold)
                HighlightedAreaView.ActivationEdge.RIGHT -> 1f - ((screenWidth - x) / edgeThreshold)
                HighlightedAreaView.ActivationEdge.TOP -> 1f - (y / edgeThreshold)
                HighlightedAreaView.ActivationEdge.BOTTOM -> 1f - ((screenHeight - y) / edgeThreshold)
                else -> 0f
            }
            highlightedAreaView?.updateEdgeGlow(glowAlpha.coerceIn(0f, 1f))
        } else if (edge == HighlightedAreaView.ActivationEdge.NONE && isEdgeActivated) {
            // Hide action areas when moving away from edge
            hideHighlightedAreas()
            isEdgeActivated = false
            currentDragEdge = HighlightedAreaView.ActivationEdge.NONE
        } else if (isEdgeActivated) {
            // Update edge glow while at edge
            val glowAlpha = when (currentDragEdge) {
                HighlightedAreaView.ActivationEdge.LEFT -> 1f - (x / edgeThreshold)
                HighlightedAreaView.ActivationEdge.RIGHT -> 1f - ((screenWidth - x) / edgeThreshold)
                HighlightedAreaView.ActivationEdge.TOP -> 1f - (y / edgeThreshold)
                HighlightedAreaView.ActivationEdge.BOTTOM -> 1f - ((screenHeight - y) / edgeThreshold)
                else -> 0f
            }
            highlightedAreaView?.updateEdgeGlow(glowAlpha.coerceIn(0f, 1f))
        }
    }
    

    
    /**
     * Updates the bubble state and updates the view colors.
     * 
     * @param bubble The bubble to update
     * @param newState The new state
     */
    private fun updateBubbleState(bubble: BubbleData, newState: BubbleState) {
        bubble.state = newState
        bubble.view.updateState(newState)
    }
    
    /**
     * Snaps a bubble to the nearest screen edge.
     * 
     * @param bubble The bubble to snap
     */
    private fun snapToEdge(bubble: BubbleData) {
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
     * Creates and shows the highlighted action areas with smart positioning.
     */
    private fun showHighlightedAreas() {
        if (highlightedAreaView != null) return
        
        try {
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            
            // Create highlighted area view
            highlightedAreaView = HighlightedAreaView(this, currentThemeName)
            
            // Determine positioning based on activation edge
            val positionData = when (currentDragEdge) {
                HighlightedAreaView.ActivationEdge.LEFT, HighlightedAreaView.ActivationEdge.RIGHT -> {
                    // Horizontal layout for left/right edges
                    screenWidth to 200 to 0 to (screenHeight / 2 - 100)
                }
                HighlightedAreaView.ActivationEdge.TOP, HighlightedAreaView.ActivationEdge.BOTTOM -> {
                    // Vertical layout for top/bottom edges
                    300 to screenHeight to (screenWidth / 2 - 150) to 0
                }
                else -> screenWidth to 200 to 0 to (screenHeight / 2 - 100)
            }
            
            val viewWidth = positionData.first.first.first
            val viewHeight = positionData.first.first.second
            val x = positionData.first.second
            val y = positionData.second
            
            // Create layout parameters for the highlighted areas
            val params = WindowManager.LayoutParams(
                viewWidth,
                viewHeight,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            
            params.gravity = Gravity.TOP or Gravity.START
            params.x = x
            params.y = y
            
            try {
                windowManager.addView(highlightedAreaView, params)
                
                // Show with smart actions if available
                val bubble = draggedBubble
                val bubbleContent = bubble?.content
                if (bubbleContent != null) {
                    val contentType = ContentAnalyzer.analyzeContentType(bubbleContent)
                    val smartActions = ContentAnalyzer.getSmartActions(contentType, bubbleContent)
                    highlightedAreaView?.showWithSmartActions(smartActions, currentDragEdge)
                } else {
                    highlightedAreaView?.show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Hides and removes the highlighted action areas.
     */
    private fun hideHighlightedAreas() {
        try {
            highlightedAreaView?.let { view ->
                view.hide()
                // Remove view after animation completes
                view.postDelayed({
                    try {
                        windowManager.removeView(view)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 200)
            }
            highlightedAreaView = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Handles drop action when a bubble is dropped on an action area.
     * 
     * @param bubble The bubble being dropped
     * @param action The action to perform
     */
    private fun handleDropAction(bubble: BubbleData, action: BubbleState) {
        try {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val clipText = clipData.getItemAt(0).text?.toString()
                if (!clipText.isNullOrEmpty()) {
                    val currentContent = bubble.content ?: ""
                    val newContent = when (action) {
                        BubbleState.PREPEND -> clipText + currentContent
                        BubbleState.REPLACE -> clipText
                        BubbleState.APPEND -> currentContent + clipText
                        else -> currentContent
                    }
                    
                    // Update bubble content
                    bubble.content = newContent
                    
                    // Update the bubble view
                    bubble.view.updateState(BubbleState.STORING)
                    
                    // Add to clipboard history
                    serviceScope.launch {
                        addClipboardItemUseCase(newContent)
                    }
                    
                    // Show feedback
                    Toast.makeText(this, "Content updated", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Handles smart action when a bubble is dropped on a smart action area.
     * 
     * @param bubble The bubble being dropped
     * @param smartAction The smart action to perform
     */
    private fun handleSmartAction(bubble: BubbleData, smartAction: SmartAction) {
        try {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val clipText = clipData.getItemAt(0).text?.toString()
                if (!clipText.isNullOrEmpty()) {
                    val currentContent = bubble.content ?: ""
                    val newContent = when (smartAction.action) {
                        BubbleState.PREPEND -> clipText + currentContent
                        BubbleState.REPLACE -> clipText
                        BubbleState.APPEND -> currentContent + clipText
                        else -> currentContent
                    }
                    
                    // Update bubble content
                    bubble.content = newContent
                    
                    // Update the bubble view
                    bubble.view.updateState(BubbleState.STORING)
                    
                    // Add to clipboard history
                    serviceScope.launch {
                        addClipboardItemUseCase(newContent)
                    }
                    
                    // Show smart action feedback
                    Toast.makeText(this, smartAction.label, Toast.LENGTH_SHORT).show()
                    
                    // Handle specific smart actions
                    handleSpecificSmartAction(smartAction, clipText)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Handles specific smart actions based on content type.
     * 
     * @param smartAction The smart action
     * @param content The content to process
     */
    private fun handleSpecificSmartAction(smartAction: SmartAction, content: String) {
        when (smartAction.label) {
            "Open Link" -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(content))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
                }
            }
            "Call Number" -> {
                try {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = android.net.Uri.parse("tel:$content")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not dial number", Toast.LENGTH_SHORT).show()
                }
            }
            "Send Email" -> {
                try {
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = android.net.Uri.parse("mailto:$content")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open email", Toast.LENGTH_SHORT).show()
                }
            }
            "Open Maps" -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(content)}")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open maps", Toast.LENGTH_SHORT).show()
                }
            }
            "Search Text" -> {
                try {
                    val intent = Intent(Intent.ACTION_WEB_SEARCH)
                    intent.putExtra(SearchManager.QUERY, content)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not search", Toast.LENGTH_SHORT).show()
                }
            }
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
                    // Check if content already exists before creating bubble
                    serviceScope.launch {
                        val result = addClipboardItemUseCase(clipText)
                        if (result != null) {
                            // Content was added, create bubble
                            withContext(Dispatchers.Main) {
                                emptyBubble?.let { bubble ->
                                    windowManager.removeView(bubble.view)
                                    createFullBubble(clipText, bubbles.size)
                                    createEmptyBubble() // Create new empty bubble
                                }
                            }
                        } else {
                            // Content already exists, show message
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@FloatingBubbleService, "Content already exists in clipboard history", Toast.LENGTH_SHORT).show()
                            }
                        }
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
            val currentTime = System.currentTimeMillis()
            
            // Check if we're in the 2-second append window
            if (appendWindowActive && (currentTime - lastCopyTime) <= 2000) {
                // Append mode: append bubble content to current clipboard
                val currentClip = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                val newContent = if (currentClip.isBlank()) content else "$currentClip\n$content"
                clipboardManager.setPrimaryClip(ClipData.newPlainText("clipboard", newContent))
                Toast.makeText(this, "Content appended to clipboard", Toast.LENGTH_SHORT).show()
                
                // End append window
                appendWindowActive = false
            } else {
                // Normal mode: replace clipboard with bubble content
                clipboardManager.setPrimaryClip(ClipData.newPlainText("clipboard", content))
                Toast.makeText(this, "Content copied to clipboard", Toast.LENGTH_SHORT).show()
                
                // Start 2-second append window
                lastCopyTime = currentTime
                appendWindowActive = true
                
                // Auto-disable append window after 2 seconds
                mainScope.launch {
                    kotlinx.coroutines.delay(2000)
                    appendWindowActive = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    

    
    /**
     * Removes all bubbles from the window.
     */
    private fun removeAllBubbles() {
        try {
            emptyBubble?.let { windowManager.removeView(it.view) }
            bubbles.forEach { windowManager.removeView(it.view) }
            
            emptyBubble = null
            bubbles.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Removes excess bubbles to free memory.
     */
    private fun removeExcessBubbles() {
        try {
            // Keep only the 3 most recent bubbles
            val bubblesToRemove = bubbles.drop(3)
            bubblesToRemove.forEach { bubble ->
                try {
                    windowManager.removeView(bubble.view)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            bubbles.removeAll(bubblesToRemove.toSet())
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