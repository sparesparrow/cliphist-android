package com.clipboardhistory.presentation.services

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ContentAnalyzer
import com.clipboardhistory.domain.model.ContentType
import com.clipboardhistory.domain.usecase.AddClipboardItemUseCase
import com.clipboardhistory.domain.usecase.GetClipboardSettingsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Bound service for clipboard monitoring with real-time communication.
 *
 * This service provides binding capabilities for activities and other components
 * to receive real-time clipboard updates and control monitoring behavior.
 */
@AndroidEntryPoint
class ClipboardMonitorService : Service() {

    @Inject
    lateinit var addClipboardItemUseCase: AddClipboardItemUseCase

    @Inject
    lateinit var getClipboardSettingsUseCase: GetClipboardSettingsUseCase

    private lateinit var clipboardManager: ClipboardManager
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    // State flows for real-time communication
    private val _clipboardContent = MutableStateFlow<String?>(null)
    val clipboardContent: StateFlow<String?> = _clipboardContent.asStateFlow()

    private val _lastClipboardItem = MutableStateFlow<ClipboardItem?>(null)
    val lastClipboardItem: StateFlow<ClipboardItem?> = _lastClipboardItem.asStateFlow()

    private val _monitoringStatus = MutableStateFlow(MonitoringStatus.INACTIVE)
    val monitoringStatus: StateFlow<MonitoringStatus> = _monitoringStatus.asStateFlow()

    private var lastClipboardText = ""
    private var isMonitoringActive = false

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        handleClipboardChange()
    }

    // Binder for service binding
    private val binder = ClipboardMonitorBinder()

    inner class ClipboardMonitorBinder : Binder() {
        fun getService(): ClipboardMonitorService = this@ClipboardMonitorService
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If service is killed and restarted, start monitoring
        startMonitoring()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceJob.cancel()
    }

    /**
     * Starts clipboard monitoring.
     */
    fun startMonitoring() {
        if (isMonitoringActive) return

        isMonitoringActive = true
        _monitoringStatus.value = MonitoringStatus.ACTIVE

        clipboardManager.addPrimaryClipChangedListener(clipboardListener)

        // Check current clipboard content
        handleClipboardChange()
    }

    /**
     * Stops clipboard monitoring.
     */
    fun stopMonitoring() {
        if (!isMonitoringActive) return

        isMonitoringActive = false
        _monitoringStatus.value = MonitoringStatus.INACTIVE

        clipboardManager.removePrimaryClipChangedListener(clipboardListener)
    }

    /**
     * Gets the current monitoring status.
     */
    fun getMonitoringStatus(): MonitoringStatus = _monitoringStatus.value

    /**
     * Manually triggers clipboard content processing.
     * Useful for testing or manual refresh.
     */
    fun refreshClipboardContent() {
        handleClipboardChange()
    }

    /**
     * Gets clipboard statistics.
     */
    suspend fun getClipboardStatistics(): ClipboardStatistics {
        val settings = getClipboardSettingsUseCase()
        // This would typically query the repository for statistics
        // For now, returning basic info
        return ClipboardStatistics(
            totalItems = 0, // Would be fetched from repository
            itemsToday = 0,
            lastActivity = System.currentTimeMillis(),
            monitoringEnabled = settings.enableClipboardMonitoring
        )
    }

    private fun handleClipboardChange() {
        serviceScope.launch {
            try {
                val clipData = clipboardManager.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val clipText = clipData.getItemAt(0)?.text?.toString() ?: ""

                    // Update content flow
                    _clipboardContent.value = clipText

                    // Only process if the text has changed and is not empty
                    if (clipText.isNotBlank() && clipText != lastClipboardText) {
                        lastClipboardText = clipText

                        // Determine content type with enhanced analysis
                        val analyzerType = ContentAnalyzer.analyzeContentType(clipText)
                        val contentType = when (analyzerType) {
                            ContentAnalyzer.Type.URL -> ContentType.URL
                            ContentAnalyzer.Type.PHONE -> ContentType.TEXT
                            ContentAnalyzer.Type.EMAIL -> ContentType.TEXT
                            ContentAnalyzer.Type.MAPS -> ContentType.TEXT
                            ContentAnalyzer.Type.TEXT -> ContentType.TEXT
                        }

                        // Add to database
                        val result = addClipboardItemUseCase(clipText, contentType)

                        // Update last item flow if successfully added
                        if (result != null) {
                            _lastClipboardItem.value = result
                        }
                    }
                }
            } catch (e: Exception) {
                _monitoringStatus.value = MonitoringStatus.ERROR
                e.printStackTrace()
            }
        }
    }

    enum class MonitoringStatus {
        INACTIVE,
        ACTIVE,
        ERROR
    }

    data class ClipboardStatistics(
        val totalItems: Int,
        val itemsToday: Int,
        val lastActivity: Long,
        val monitoringEnabled: Boolean
    )
}