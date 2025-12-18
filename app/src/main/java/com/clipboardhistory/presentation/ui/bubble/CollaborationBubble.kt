package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import com.clipboardhistory.utils.ContentAnalyzer

/**
 * Collaboration Bubble for real-time collaborative editing and sharing.
 * Enables multiple users to collaboratively work on clipboard content.
 */
data class CollaborationBubble(
    override val id: String,
    override val type: BubbleType = AdvancedBubbleType.COLLABORATION,
    override val position: Offset = Offset.Zero,
    override val size: Dp = type.defaultSize,
    override val isVisible: Boolean = true,
    override val isMinimized: Boolean = false,
    override val lastInteractionTime: Long = System.currentTimeMillis(),
    val sessionId: String = generateSessionId(),
    val content: CollaborativeContent = CollaborativeContent(),
    val collaborators: List<Collaborator> = emptyList(),
    val isHost: Boolean = true,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val pendingChanges: List<ContentChange> = emptyList(),
    val changeHistory: List<ContentChange> = emptyList(),
    val permissions: CollaborationPermissions = CollaborationPermissions(),
    override val relevanceScore: Float = 0.8f,
    override val contextualActions: List<String> = listOf("invite", "share", "sync", "export")
) : AdvancedBubbleSpec() {

    override val content: @Composable (BubbleSpec) -> Unit = { spec ->
        val collabSpec = spec as CollaborationBubble
        CollaborationBubbleContent(collabSpec)
    }

    override fun withKeyboardState(isKeyboardVisible: Boolean): CollaborationBubble =
        copy(isMinimized = !isKeyboardVisible) // Show when keyboard hidden for collaboration

    override fun withPosition(newPosition: Offset): CollaborationBubble = copy(position = newPosition)
    override fun withMinimized(isMinimized: Boolean): CollaborationBubble = copy(isMinimized = isMinimized)
    override fun withSize(newSize: Dp): CollaborationBubble = copy(size = newSize)
    override fun withInteraction(): CollaborationBubble = copy(lastInteractionTime = System.currentTimeMillis())

    /**
     * Adds a new collaborator to the session.
     */
    fun addCollaborator(collaborator: Collaborator): CollaborationBubble {
        if (collaborators.none { it.id == collaborator.id }) {
            return copy(collaborators = collaborators + collaborator)
        }
        return this
    }

    /**
     * Removes a collaborator from the session.
     */
    fun removeCollaborator(collaboratorId: String): CollaborationBubble {
        return copy(collaborators = collaborators.filter { it.id != collaboratorId })
    }

    /**
     * Updates a collaborator's status.
     */
    fun updateCollaboratorStatus(collaboratorId: String, status: CollaboratorStatus): CollaborationBubble {
        return copy(collaborators = collaborators.map { collaborator ->
            if (collaborator.id == collaboratorId) {
                collaborator.copy(status = status)
            } else {
                collaborator
            }
        })
    }

    /**
     * Applies a content change from a collaborator.
     */
    fun applyContentChange(change: ContentChange): CollaborationBubble {
        val updatedContent = content.applyChange(change)
        return copy(
            content = updatedContent,
            changeHistory = changeHistory + change,
            lastInteractionTime = System.currentTimeMillis()
        )
    }

    /**
     * Adds a pending change that needs to be synced.
     */
    fun addPendingChange(change: ContentChange): CollaborationBubble {
        return copy(pendingChanges = pendingChanges + change)
    }

    /**
     * Clears pending changes after successful sync.
     */
    fun clearPendingChanges(): CollaborationBubble {
        return copy(pendingChanges = emptyList())
    }

    /**
     * Gets the current collaboration status.
     */
    fun getCollaborationStatus(): CollaborationStatus {
        return CollaborationStatus(
            sessionId = sessionId,
            isHost = isHost,
            connectionStatus = connectionStatus,
            activeCollaborators = collaborators.count { it.status == CollaboratorStatus.ACTIVE },
            totalCollaborators = collaborators.size,
            pendingChangesCount = pendingChanges.size,
            lastActivity = lastInteractionTime
        )
    }

    /**
     * Checks if the current user can perform an action.
     */
    fun canPerformAction(action: CollaborationAction, userId: String? = null): Boolean {
        return when (action) {
            CollaborationAction.EDIT_CONTENT -> permissions.allowEditing
            CollaborationAction.INVITE_USERS -> permissions.allowInviting
            CollaborationAction.REMOVE_USERS -> isHost && permissions.allowRemoving
            CollaborationAction.CHANGE_PERMISSIONS -> isHost
            CollaborationAction.END_SESSION -> isHost
            CollaborationAction.EXPORT_CONTENT -> permissions.allowExporting
        }
    }

    /**
     * Gets collaborators currently typing or active.
     */
    fun getActiveCollaborators(): List<Collaborator> {
        return collaborators.filter { it.status == CollaboratorStatus.ACTIVE }
    }

    /**
     * Creates a shareable invitation link for the session.
     */
    fun createInvitationLink(): String {
        // In a real implementation, this would generate a secure invitation link
        return "clipboard-collab://session/$sessionId"
    }

    companion object {
        private fun generateSessionId(): String {
            return "collab_${System.currentTimeMillis()}_${(1000..9999).random()}"
        }

        /**
         * Creates a new collaboration session as host.
         */
        fun createSession(initialContent: String = ""): CollaborationBubble {
            return CollaborationBubble(
                isHost = true,
                connectionStatus = ConnectionStatus.CONNECTED,
                content = CollaborativeContent(text = initialContent)
            )
        }

        /**
         * Joins an existing collaboration session.
         */
        fun joinSession(sessionId: String): CollaborationBubble {
            return CollaborationBubble(
                sessionId = sessionId,
                isHost = false,
                connectionStatus = ConnectionStatus.CONNECTING
            )
        }
    }
}

/**
 * Represents a collaborator in the session.
 */
data class Collaborator(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val color: Long = generateColor(),
    val status: CollaboratorStatus = CollaboratorStatus.ACTIVE,
    val joinedAt: Long = System.currentTimeMillis(),
    val lastActivity: Long = System.currentTimeMillis(),
    val permissions: CollaboratorPermissions = CollaboratorPermissions()
) {
    companion object {
        private fun generateColor(): Long {
            val colors = listOf(
                0xFF2196F3, // Blue
                0xFF4CAF50, // Green
                0xFFFF9800, // Orange
                0xFFE91E63, // Pink
                0xFF9C27B0, // Purple
                0xFF00BCD4, // Cyan
                0xFF8BC34A, // Light Green
                0xFFFF5722  // Deep Orange
            )
            return colors.random()
        }
    }
}

/**
 * Collaborator status indicators.
 */
enum class CollaboratorStatus {
    ACTIVE,      // Currently active/typing
    AWAY,        // Temporarily away
    OFFLINE,     // Disconnected
    VIEWING      // Just viewing, not editing
}

/**
 * Permissions for individual collaborators.
 */
data class CollaboratorPermissions(
    val canEdit: Boolean = true,
    val canInvite: Boolean = false,
    val canExport: Boolean = true,
    val canDelete: Boolean = false
)

/**
 * Collaborative content that supports real-time editing.
 */
data class CollaborativeContent(
    val text: String = "",
    val version: Int = 1,
    val lastModified: Long = System.currentTimeMillis(),
    val lastModifiedBy: String? = null,
    val cursors: Map<String, CursorPosition> = emptyMap(), // User ID -> cursor position
    val selections: Map<String, TextSelection> = emptyMap() // User ID -> selection
) {
    /**
     * Applies a content change and returns updated content.
     */
    fun applyChange(change: ContentChange): CollaborativeContent {
        return when (change.type) {
            ChangeType.TEXT_INSERT -> {
                val newText = text.substring(0, change.position) +
                             change.newText +
                             text.substring(change.position)
                copy(
                    text = newText,
                    version = version + 1,
                    lastModified = System.currentTimeMillis(),
                    lastModifiedBy = change.userId
                )
            }
            ChangeType.TEXT_DELETE -> {
                val newText = text.substring(0, change.position) +
                             text.substring(change.position + change.deletedLength)
                copy(
                    text = newText,
                    version = version + 1,
                    lastModified = System.currentTimeMillis(),
                    lastModifiedBy = change.userId
                )
            }
            ChangeType.CURSOR_MOVE -> {
                copy(cursors = cursors + (change.userId to CursorPosition(change.position)))
            }
            ChangeType.SELECTION_CHANGE -> {
                val selection = TextSelection(change.position, change.position + change.newText.length)
                copy(selections = selections + (change.userId to selection))
            }
        }
    }

    /**
     * Gets the word count of the content.
     */
    fun getWordCount(): Int = text.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size

    /**
     * Gets the character count of the content.
     */
    fun getCharacterCount(): Int = text.length
}

/**
 * Represents a cursor position in the collaborative content.
 */
data class CursorPosition(
    val offset: Int,
    val line: Int = 0,
    val column: Int = 0
)

/**
 * Represents a text selection in the collaborative content.
 */
data class TextSelection(
    val start: Int,
    val end: Int
)

/**
 * Types of content changes that can occur.
 */
enum class ChangeType {
    TEXT_INSERT, TEXT_DELETE, CURSOR_MOVE, SELECTION_CHANGE
}

/**
 * Represents a change made to collaborative content.
 */
data class ContentChange(
    val id: String = generateId(),
    val type: ChangeType,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val position: Int,
    val newText: String = "",
    val deletedLength: Int = 0,
    val version: Int = 1
) {
    companion object {
        private fun generateId(): String = "change_${System.currentTimeMillis()}_${(0..999).random()}"
    }
}

/**
 * Overall collaboration permissions for the session.
 */
data class CollaborationPermissions(
    val allowEditing: Boolean = true,
    val allowInviting: Boolean = true,
    val allowRemoving: Boolean = true, // Only host can remove
    val allowExporting: Boolean = true,
    val requireApproval: Boolean = false, // For sensitive content
    val maxCollaborators: Int = 10
)

/**
 * Actions that can be performed in collaboration.
 */
enum class CollaborationAction {
    EDIT_CONTENT, INVITE_USERS, REMOVE_USERS, CHANGE_PERMISSIONS, END_SESSION, EXPORT_CONTENT
}

/**
 * Current connection status of the collaboration session.
 */
enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
}

/**
 * Overall collaboration session status.
 */
data class CollaborationStatus(
    val sessionId: String,
    val isHost: Boolean,
    val connectionStatus: ConnectionStatus,
    val activeCollaborators: Int,
    val totalCollaborators: Int,
    val pendingChangesCount: Int,
    val lastActivity: Long
)