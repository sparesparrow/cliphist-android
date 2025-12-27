package com.clipboardhistory.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipboardhistory.presentation.ui.bubble.*
import com.clipboardhistory.utils.KeyboardVisibilityDetector
import com.clipboardhistory.utils.SmartInputManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing bubble state and interactions.
 * Coordinates between keyboard visibility, bubble orchestration, and user interactions.
 */
class BubbleViewModel(
    private val keyboardDetector: KeyboardVisibilityDetector,
    private val smartInputManager: SmartInputManager? = null
) : ViewModel() {

    private val _orchestrator = BubbleOrchestrator(keyboardDetector, smartInputManager)
    val orchestrator: BubbleOrchestrator = _orchestrator

    // Exposed flows for UI consumption
    val bubbles = _orchestrator.bubbles
    val keyboardVisible = _orchestrator.keyboardVisible
    val visibleBubbles = _orchestrator.bubbles.map { bubbles ->
        bubbles.filter { it.isVisible }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Initialize keyboard monitoring
        keyboardDetector.startMonitoring()

        // Setup text selection integration if accessibility service is available
        setupTextSelectionIntegration()
    }

    override fun onCleared() {
        super.onCleared()
        keyboardDetector.stopMonitoring()
    }

    /**
     * Sets up integration with text selection for bubble cut functionality.
     */
    private fun setupTextSelectionIntegration() {
        try {
            val accessibilityService = com.clipboardhistory.presentation.services.AccessibilityMonitorService.getInstance()
            accessibilityService?.setBubbleOrchestrator(_orchestrator)
        } catch (e: Exception) {
            // Accessibility service not available or not properly initialized
            // This is expected and not an error
        }
    }

    // Bubble management methods

    /**
     * Adds a text paste bubble with clipboard content.
     */
    fun addTextPasteBubble(
        content: String,
        contentType: BubbleSpec.TextPasteBubble.ContentType = BubbleSpec.TextPasteBubble.ContentType.TEXT,
        isFavorite: Boolean = false
    ) {
        val bubble = BubbleSpec.TextPasteBubble(
            textContent = content,
            contentType = contentType,
            isFavorite = isFavorite
        )
        _orchestrator.addBubble(bubble)
    }

    /**
     * Adds a toolbelt bubble with available tools.
     */
    fun addToolbeltBubble(tools: List<ToolbeltTool>) {
        val bubble = BubbleSpec.ToolbeltBubble(tools = tools)
        _orchestrator.addBubble(bubble)
    }

    /**
     * Adds a pinned item bubble that stays visible.
     */
    fun addPinnedItemBubble(content: String) {
        val bubble = BubbleSpec.TextPasteBubble(
            type = BubbleType.PINNED_ITEM,
            textContent = content,
            isFavorite = true
        )
        _orchestrator.addBubble(bubble)
    }

    /**
     * Adds a system notification bubble.
     */
    fun addSystemNotificationBubble(
        notificationId: Int,
        title: String,
        content: String,
        iconResId: Int
    ) {
        val bubble = BubbleSpec.SystemBubble(
            notificationId = notificationId,
            title = title,
            content = content,
            iconResId = iconResId
        )
        _orchestrator.addBubble(bubble)
    }

    /**
     * Adds a quick action bubble for temporary actions.
     */
    fun addQuickActionBubble(content: String) {
        val bubble = BubbleSpec.TextPasteBubble(
            type = BubbleType.QUICK_ACTION,
            textContent = content
        )
        _orchestrator.addBubble(bubble)
    }

    /**
     * Creates default toolbelt tools.
     */
    fun createDefaultToolbeltTools(): List<ToolbeltTool> {
        return listOf(
            ToolbeltTool(
                id = "opacity",
                name = "Opacity",
                icon = 1, // Placeholder - would be actual icon resource
                action = { /* Toggle opacity slider */ },
                priority = 1
            ),
            ToolbeltTool(
                id = "private_toggle",
                name = "Private",
                icon = 2,
                action = { /* Toggle private bubbles */ },
                priority = 2
            ),
            ToolbeltTool(
                id = "history_toggle",
                name = "History",
                icon = 3,
                action = { /* Toggle history bubbles */ },
                priority = 3
            ),
            ToolbeltTool(
                id = "type_changer",
                name = "Type",
                icon = 4,
                action = { /* Change bubble type */ },
                priority = 4
            ),
            ToolbeltTool(
                id = "content_editor",
                name = "Edit",
                icon = 5,
                action = { /* Open content editor */ },
                priority = 5
            ),
            ToolbeltTool(
                id = "operation_mode",
                name = "Mode",
                icon = 6,
                action = { /* Change operation mode */ },
                priority = 6
            ),
            ToolbeltTool(
                id = "clear_all",
                name = "Clear",
                icon = 7,
                action = { /* Clear all bubbles */ },
                priority = 7
            ),
            ToolbeltTool(
                id = "settings",
                name = "Settings",
                icon = 8,
                action = { /* Open settings */ },
                priority = 8
            )
        )
    }

    // Bubble interaction methods

    /**
     * Handles bubble tap/interaction.
     */
    fun onBubbleTap(bubbleId: String) {
        _orchestrator.onBubbleInteraction(bubbleId)
    }

    /**
     * Updates bubble position after dragging.
     */
    fun updateBubblePosition(bubbleId: String, position: androidx.compose.ui.geometry.Offset) {
        _orchestrator.updateBubblePosition(bubbleId, position)
    }

    /**
     * Toggles minimization state of a bubble.
     */
    fun toggleBubbleMinimized(bubbleId: String) {
        _orchestrator.toggleBubbleMinimized(bubbleId)
    }

    /**
     * Removes a specific bubble.
     */
    fun removeBubble(bubbleId: String) {
        _orchestrator.removeBubble(bubbleId)
    }

    // Bulk operations

    /**
     * Clears all bubbles of a specific type.
     */
    fun clearBubblesByType(type: BubbleType) {
        _orchestrator.clearBubblesByType(type)
    }

    /**
     * Clears all bubbles.
     */
    fun clearAllBubbles() {
        _orchestrator.clearAllBubbles()
    }

    /**
     * Gets bubbles that should be visible based on current state.
     */
    fun getVisibleBubbles(): List<BubbleSpec> {
        return _orchestrator.getVisibleBubbles()
    }

    // Utility methods

    /**
     * Checks if direct input pasting is available.
     */
    fun isDirectInputAvailable(): Boolean {
        return smartInputManager?.isDirectInputAvailable() ?: false
    }

    /**
     * Gets current input context information.
     */
    fun getInputContextInfo() = smartInputManager?.getInputContextInfo()

    /**
     * Gets keyboard state information.
     */
    fun getKeyboardState() = keyboardDetector.getCurrentKeyboardState()
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> b04bf72 (feat: Implement Regex Accumulator Bubble with pattern-based collection)

    // Regex accumulator operations

    /**
     * Processes clipboard content against active regex accumulator bubbles.
     */
    fun processClipboardContentForRegexAccumulators(content: String, source: String? = null) {
        val updatedBubbles = bubbles.value.map { bubble ->
            when (bubble) {
                is AdvancedBubbleSpec.RegexAccumulator -> {
                    if (bubble.isCollecting) {
                        bubble.tryAccumulate(content, source)
                    } else {
                        bubble
                    }
                }
                else -> bubble
            }
        }
        _orchestrator.updateBubbles(updatedBubbles)
    }

    /**
     * Gets all regex accumulator bubbles.
     */
    fun getRegexAccumulatorBubbles(): List<AdvancedBubbleSpec.RegexAccumulator> {
        return bubbles.value.filterIsInstance<AdvancedBubbleSpec.RegexAccumulator>()
    }

    /**
     * Creates a new regex accumulator bubble with the given pattern.
     */
    fun createRegexAccumulator(
        pattern: AdvancedBubbleSpec.RegexPattern,
        position: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero
    ): AdvancedBubbleSpec.RegexAccumulator {
        return AdvancedBubbleSpec.RegexAccumulator(
            pattern = pattern,
            position = position
        )
    }

<<<<<<< HEAD
    // Bubble cut functionality

    /**
     * Shows the bubble cut menu when text is selected.
     */
    fun showBubbleCutMenu(position: androidx.compose.ui.geometry.Offset) {
        _orchestrator.showBubbleCutMenu(position)
    }

    /**
     * Hides the bubble cut menu.
     */
    fun hideBubbleCutMenu() {
        _orchestrator.hideBubbleCutMenu()
    }

    /**
     * Checks if bubble cut menu should be shown for current text selection.
     */
    fun shouldShowBubbleCutMenu(): Boolean {
        return _orchestrator.bubbleCutMenuManager.shouldShowBubbleCutMenu()
    }

=======
>>>>>>> b04bf72 (feat: Implement Regex Accumulator Bubble with pattern-based collection)
    /**
     * Adds a regex accumulator bubble to the orchestrator.
     */
    fun addRegexAccumulator(pattern: AdvancedBubbleSpec.RegexPattern) {
        val bubble = createRegexAccumulator(pattern)
        _orchestrator.addBubble(bubble)
    }
<<<<<<< HEAD

    // Voice bubble functionality

    /**
     * Creates a new voice bubble with the given text content.
     */
    fun createVoiceBubble(
        textContent: String = "",
        isTTSEnabled: Boolean = true,
        isVoiceRecognitionEnabled: Boolean = true,
        position: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero
    ): VoiceBubble {
        return VoiceBubble(
            id = "voice_${System.currentTimeMillis()}_${textContent.hashCode()}",
            position = position,
            textContent = textContent,
            isTTSEnabled = isTTSEnabled,
            isVoiceRecognitionEnabled = isVoiceRecognitionEnabled
        )
    }

    /**
     * Adds a voice bubble to the orchestrator.
     */
    fun addVoiceBubble(
        textContent: String = "",
        isTTSEnabled: Boolean = true,
        isVoiceRecognitionEnabled: Boolean = true
    ) {
        val bubble = createVoiceBubble(textContent, isTTSEnabled, isVoiceRecognitionEnabled)
        _orchestrator.addBubble(bubble)
    }

    /**
     * Adds a voice transcription to an existing voice bubble.
     */
    fun addVoiceTranscriptionToBubble(
        bubbleId: String,
        transcription: String,
        confidence: Float = 1.0f,
        source: String = "voice"
    ) {
        val updatedBubble = bubbles.value.find { it.id == bubbleId } as? VoiceBubble
        updatedBubble?.let { voiceBubble ->
            val updated = voiceBubble.addTranscription(transcription, confidence, source)
            updateBubble(updated)
        }
    }

    /**
     * Gets all voice bubbles.
     */
    fun getVoiceBubbles(): List<VoiceBubble> {
        return bubbles.value.filterIsInstance<VoiceBubble>()
    }

    /**
     * Updates voice bubble TTS settings.
     */
    fun updateVoiceBubbleTTSSettings(
        bubbleId: String,
        speechRate: Float? = null,
        pitch: Float? = null,
        language: String? = null,
        autoSpeakOnLongPress: Boolean? = null
    ) {
        val bubble = bubbles.value.find { it.id == bubbleId } as? VoiceBubble
        bubble?.let { voiceBubble ->
            val currentSettings = voiceBubble.ttsSettings
            val newSettings = currentSettings.copy(
                speechRate = speechRate ?: currentSettings.speechRate,
                pitch = pitch ?: currentSettings.pitch,
                language = language ?: currentSettings.language,
                autoSpeakOnLongPress = autoSpeakOnLongPress ?: currentSettings.autoSpeakOnLongPress
            )
            val updated = voiceBubble.withTTSSettings(newSettings)
            updateBubble(updated)
        }
    }

    /**
     * Updates voice bubble voice recognition settings.
     */
    fun updateVoiceBubbleVoiceSettings(
        bubbleId: String,
        language: String? = null,
        maxResults: Int? = null,
        enablePartialResults: Boolean? = null,
        confidenceThreshold: Float? = null
    ) {
        val bubble = bubbles.value.find { it.id == bubbleId } as? VoiceBubble
        bubble?.let { voiceBubble ->
            val currentSettings = voiceBubble.voiceSettings
            val newSettings = currentSettings.copy(
                language = language ?: currentSettings.language,
                maxResults = maxResults ?: currentSettings.maxResults,
                enablePartialResults = enablePartialResults ?: currentSettings.enablePartialResults,
                confidenceThreshold = confidenceThreshold ?: currentSettings.confidenceThreshold
            )
            val updated = voiceBubble.withVoiceSettings(newSettings)
            updateBubble(updated)
        }
    }

    // Collaboration bubble functionality

    /**
     * Creates a new collaboration session as host.
     */
    fun createCollaborationSession(initialContent: String = ""): CollaborationBubble {
        return CollaborationBubble.createSession(initialContent)
    }

    /**
     * Joins an existing collaboration session.
     */
    fun joinCollaborationSession(sessionId: String): CollaborationBubble {
        return CollaborationBubble.joinSession(sessionId)
    }

    /**
     * Adds a collaboration bubble to the orchestrator.
     */
    fun addCollaborationBubble(initialContent: String = "", asHost: Boolean = true) {
        val bubble = if (asHost) {
            createCollaborationSession(initialContent)
        } else {
            // In a real app, this would use a provided session ID
            joinCollaborationSession("demo_session")
        }
        _orchestrator.addBubble(bubble)
    }

    /**
     * Adds a collaborator to a collaboration session.
     */
    fun addCollaboratorToSession(bubbleId: String, collaborator: Collaborator) {
        val bubble = bubbles.value.find { it.id == bubbleId } as? CollaborationBubble
        bubble?.let { collabBubble ->
            val updated = collabBubble.addCollaborator(collaborator)
            updateBubble(updated)
        }
    }

    /**
     * Removes a collaborator from a collaboration session.
     */
    fun removeCollaboratorFromSession(bubbleId: String, collaboratorId: String) {
        val bubble = bubbles.value.find { it.id == bubbleId } as? CollaborationBubble
        bubble?.let { collabBubble ->
            val updated = collabBubble.removeCollaborator(collaboratorId)
            updateBubble(updated)
        }
    }

    /**
     * Applies a content change to a collaboration session.
     */
    fun applyContentChangeToSession(bubbleId: String, change: ContentChange) {
        val bubble = bubbles.value.find { it.id == bubbleId } as? CollaborationBubble
        bubble?.let { collabBubble ->
            val updated = collabBubble.applyContentChange(change)
            updateBubble(updated)
        }
    }

    /**
     * Gets all collaboration bubbles.
     */
    fun getCollaborationBubbles(): List<CollaborationBubble> {
        return bubbles.value.filterIsInstance<CollaborationBubble>()
    }

    /**
     * Gets active collaboration sessions.
     */
    fun getActiveCollaborationSessions(): List<CollaborationBubble> {
        return getCollaborationBubbles().filter {
            it.connectionStatus == ConnectionStatus.CONNECTED
        }
    }
}