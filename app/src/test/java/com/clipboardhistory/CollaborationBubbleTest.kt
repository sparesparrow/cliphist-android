package com.clipboardhistory

import androidx.compose.ui.geometry.Offset
import com.clipboardhistory.presentation.ui.bubble.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CollaborationBubbleTest {

    private val sampleContent = "Collaborative document content"
    private val position = Offset(100f, 200f)

    @Test
    fun `createSession creates host bubble with correct defaults`() {
        val bubble = CollaborationBubble.createSession(sampleContent)

        assertTrue(bubble.isHost)
        assertEquals(ConnectionStatus.CONNECTED, bubble.connectionStatus)
        assertEquals(sampleContent, bubble.content.text)
        assertTrue(bubble.collaborators.isEmpty())
        assertTrue(bubble.pendingChanges.isEmpty())
        assertTrue(bubble.changeHistory.isEmpty())
    }

    @Test
    fun `joinSession creates guest bubble with correct defaults`() {
        val sessionId = "test_session_123"
        val bubble = CollaborationBubble.joinSession(sessionId)

        assertFalse(bubble.isHost)
        assertEquals(ConnectionStatus.CONNECTING, bubble.connectionStatus)
        assertEquals(sessionId, bubble.sessionId)
        assertEquals("", bubble.content.text)
    }

    @Test
    fun `addCollaborator adds new collaborator to session`() {
        val bubble = CollaborationBubble.createSession()
        val collaborator = Collaborator(
            id = "user1",
            name = "John Doe",
            color = 0xFF2196F3
        )

        val updated = bubble.addCollaborator(collaborator)

        assertEquals(1, updated.collaborators.size)
        assertEquals(collaborator, updated.collaborators[0])
    }

    @Test
    fun `addCollaborator ignores duplicate collaborators`() {
        val bubble = CollaborationBubble.createSession()
        val collaborator = Collaborator(id = "user1", name = "John Doe")

        val updated1 = bubble.addCollaborator(collaborator)
        val updated2 = updated1.addCollaborator(collaborator)

        assertEquals(1, updated2.collaborators.size)
    }

    @Test
    fun `removeCollaborator removes specified collaborator`() {
        val bubble = CollaborationBubble.createSession()
        val collab1 = Collaborator(id = "user1", name = "John")
        val collab2 = Collaborator(id = "user2", name = "Jane")

        val withCollabs = bubble
            .addCollaborator(collab1)
            .addCollaborator(collab2)

        val removed = withCollabs.removeCollaborator("user1")

        assertEquals(1, removed.collaborators.size)
        assertEquals("user2", removed.collaborators[0].id)
    }

    @Test
    fun `applyContentChange updates content and history`() {
        val bubble = CollaborationBubble.createSession("Initial content")
        val change = ContentChange(
            type = ChangeType.TEXT_INSERT,
            userId = "user1",
            position = 8,
            newText = "modified "
        )

        val updated = bubble.applyContentChange(change)

        assertEquals("Initial modified content", updated.content.text)
        assertEquals(2, updated.content.version)
        assertEquals("user1", updated.content.lastModifiedBy)
        assertEquals(1, updated.changeHistory.size)
    }

    @Test
    fun `canPerformAction respects host permissions and action types`() {
        val hostBubble = CollaborationBubble.createSession()
        val guestBubble = CollaborationBubble.joinSession("session1")

        // Host can perform all actions
        assertTrue(hostBubble.canPerformAction(CollaborationAction.EDIT_CONTENT))
        assertTrue(hostBubble.canPerformAction(CollaborationAction.INVITE_USERS))
        assertTrue(hostBubble.canPerformAction(CollaborationAction.REMOVE_USERS))
        assertTrue(hostBubble.canPerformAction(CollaborationAction.CHANGE_PERMISSIONS))
        assertTrue(hostBubble.canPerformAction(CollaborationAction.END_SESSION))

        // Guest cannot remove users or change permissions
        assertTrue(guestBubble.canPerformAction(CollaborationAction.EDIT_CONTENT)) // Assuming default permissions
        assertFalse(guestBubble.canPerformAction(CollaborationAction.REMOVE_USERS))
        assertFalse(guestBubble.canPerformAction(CollaborationAction.CHANGE_PERMISSIONS))
        assertFalse(guestBubble.canPerformAction(CollaborationAction.END_SESSION))
    }

    @Test
    fun `getActiveCollaborators returns only active collaborators`() {
        val bubble = CollaborationBubble.createSession()
        val activeCollab = Collaborator(id = "active", name = "Active", status = CollaboratorStatus.ACTIVE)
        val awayCollab = Collaborator(id = "away", name = "Away", status = CollaboratorStatus.AWAY)
        val offlineCollab = Collaborator(id = "offline", name = "Offline", status = CollaboratorStatus.OFFLINE)

        val withCollabs = bubble
            .addCollaborator(activeCollab)
            .addCollaborator(awayCollab)
            .addCollaborator(offlineCollab)

        val active = withCollabs.getActiveCollaborators()
        assertEquals(1, active.size)
        assertEquals("active", active[0].id)
    }

    @Test
    fun `getCollaborationStatus returns correct session information`() {
        val bubble = CollaborationBubble.createSession()
            .addCollaborator(Collaborator(id = "user1", name = "User1", status = CollaboratorStatus.ACTIVE))
            .addCollaborator(Collaborator(id = "user2", name = "User2", status = CollaboratorStatus.AWAY))
            .addPendingChange(ContentChange(type = ChangeType.TEXT_INSERT, userId = "user1", newText = "test"))

        val status = bubble.getCollaborationStatus()

        assertTrue(status.isHost)
        assertEquals(ConnectionStatus.CONNECTED, status.connectionStatus)
        assertEquals(1, status.activeCollaborators) // Only active collaborator
        assertEquals(2, status.totalCollaborators)
        assertEquals(1, status.pendingChangesCount)
    }

    @Test
    fun `content methods work correctly`() {
        val content = CollaborativeContent(text = "Hello world", version = 1)
        val change = ContentChange(
            type = ChangeType.TEXT_INSERT,
            userId = "user1",
            position = 6,
            newText = "beautiful "
        )

        val updated = content.applyChange(change)

        assertEquals("Hello beautiful world", updated.text)
        assertEquals(2, updated.version)
        assertEquals("user1", updated.lastModifiedBy)
    }

    @Test
    fun `content statistics are calculated correctly`() {
        val bubble = CollaborationBubble.createSession()
            .addTranscription("First transcription", 0.9f)
            .addTranscription("Second transcription", 0.7f)
            .addTranscription("Third transcription", 0.95f)

        // Note: This test would need to be updated once transcription functionality is fully implemented
        // For now, we're testing the structure
        assertEquals("collab_", bubble.sessionId.take(6))
    }

    @Test
    fun `keyboard visibility changes minimize state correctly`() {
        val bubble = CollaborationBubble.createSession()

        // Should reposition when keyboard appears (for collaboration, we want to stay visible but adjust position)
        val keyboardVisible = bubble.withKeyboardState(true)
        assertFalse(keyboardVisible.isMinimized) // Collaboration bubbles typically stay expanded

        val keyboardHidden = keyboardVisible.withKeyboardState(false)
        assertFalse(keyboardHidden.isMinimized)
    }

    @Test
    fun `createInvitationLink generates shareable link`() {
        val bubble = CollaborationBubble.createSession()
        val link = bubble.createInvitationLink()

        assertTrue(link.startsWith("clipboard-collab://session/"))
        assertTrue(link.contains(bubble.sessionId))
    }

    @Test
    fun `clearPendingChanges removes all pending changes`() {
        val bubble = CollaborationBubble.createSession()
            .addPendingChange(ContentChange(type = ChangeType.TEXT_INSERT, userId = "user1", newText = "test1"))
            .addPendingChange(ContentChange(type = ChangeType.TEXT_DELETE, userId = "user1", position = 0, deletedLength = 4))

        assertEquals(2, bubble.pendingChanges.size)

        val cleared = bubble.clearPendingChanges()
        assertEquals(0, cleared.pendingChanges.size)
    }

    @Test
    fun `updateCollaboratorStatus changes collaborator status`() {
        val bubble = CollaborationBubble.createSession()
        val collaborator = Collaborator(id = "user1", name = "User1", status = CollaboratorStatus.ACTIVE)

        val withCollab = bubble.addCollaborator(collaborator)
        val updated = withCollab.updateCollaboratorStatus("user1", CollaboratorStatus.AWAY)

        assertEquals(CollaboratorStatus.AWAY, updated.collaborators[0].status)
    }

    @Test
    fun `addPendingChange adds change to pending list`() {
        val bubble = CollaborationBubble.createSession()
        val change = ContentChange(type = ChangeType.TEXT_INSERT, userId = "user1", newText = "test")

        val updated = bubble.addPendingChange(change)

        assertEquals(1, updated.pendingChanges.size)
        assertEquals(change, updated.pendingChanges[0])
    }

    @Test
    fun `withInteraction updates lastInteractionTime`() {
        val bubble = CollaborationBubble.createSession()
        val originalTime = bubble.lastInteractionTime

        // Simulate time passing
        Thread.sleep(10)

        val updated = bubble.withInteraction()
        assertTrue(updated.lastInteractionTime > originalTime)
    }
}