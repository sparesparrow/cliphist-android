package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clipboardhistory.presentation.ui.bubble.CollaborationBubble
import com.clipboardhistory.presentation.ui.bubble.Collaborator
import com.clipboardhistory.presentation.ui.bubble.ConnectionStatus

/**
 * Composable content for collaboration bubbles.
 * Enables real-time collaborative editing with multiple users.
 */
@Composable
fun CollaborationBubbleContent(spec: CollaborationBubble) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(CollabTab.EDITOR) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
            },
            label = "collaboration_content"
        ) { expanded ->
            if (expanded) {
                ExpandedCollaborationView(spec, selectedTab) { selectedTab = it }
            } else {
                CollapsedCollaborationView(spec) { isExpanded = true }
            }
        }
    }
}

/**
 * Collapsed collaboration view showing session status and active collaborators.
 */
@Composable
private fun CollapsedCollaborationView(
    spec: CollaborationBubble,
    onExpand: () -> Unit
) {
    val status = spec.getCollaborationStatus()
    val activeCount = status.activeCollaborators
    val connectionColor = when (spec.connectionStatus) {
        ConnectionStatus.CONNECTED -> Color(0xFF4CAF50) // Green
        ConnectionStatus.CONNECTING -> Color(0xFFFF9800) // Orange
        ConnectionStatus.RECONNECTING -> Color(0xFFFF9800) // Orange
        ConnectionStatus.ERROR -> Color(0xFFF44336) // Red
        ConnectionStatus.DISCONNECTED -> Color(0xFF9E9E9E) // Gray
    }

    Surface(
        modifier = Modifier
            .size(if (activeCount > 0) 90.dp else 70.dp)
            .clickable(onClick = onExpand),
        shape = CircleShape,
        color = connectionColor,
        shadowElevation = if (activeCount > 0) 8.dp else 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                activeCount > 0 -> {
                    // Show active collaborators
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = "Collaborating",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = activeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                spec.isHost -> {
                    // Host indicator
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Host - tap to invite collaborators",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    // Guest indicator
                    Icon(
                        Icons.Default.PersonOutline,
                        contentDescription = "Collaborator",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Connection status indicator
            if (spec.connectionStatus != ConnectionStatus.CONNECTED) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(12.dp),
                    shape = CircleShape,
                    color = connectionColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when (spec.connectionStatus) {
                                ConnectionStatus.CONNECTING -> "⟳"
                                ConnectionStatus.RECONNECTING -> "⟲"
                                ConnectionStatus.ERROR -> "!"
                                else -> "✗"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 8.sp
                        )
                    }
                }
            }

            // Pending changes indicator
            if (spec.pendingChanges.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(12.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = spec.pendingChanges.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Expanded collaboration view with full editing interface.
 */
@Composable
private fun ExpandedCollaborationView(
    spec: CollaborationBubble,
    selectedTab: CollabTab,
    onTabSelected: (CollabTab) -> Unit
) {
    Column(
        modifier = Modifier
            .width(340.dp)
            .heightIn(min = 400.dp, max = 600.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with session info and controls
        CollaborationHeader(spec)

        // Tab selector
        CollaborationTabSelector(selectedTab, onTabSelected)

        // Tab content
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith
                    fadeOut(animationSpec = tween(200))
            },
            label = "collab_tab_content"
        ) { tab ->
            when (tab) {
                CollabTab.EDITOR -> EditorTab(spec)
                CollabTab.COLLABORATORS -> CollaboratorsTab(spec)
                CollabTab.HISTORY -> HistoryTab(spec)
                CollabTab.SETTINGS -> SettingsTab(spec)
            }
        }
    }
}

/**
 * Header with session information and quick actions.
 */
@Composable
private fun CollaborationHeader(spec: CollaborationBubble) {
    val status = spec.getCollaborationStatus()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = if (spec.isHost) "Collaboration Session" else "Collaborating",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Session: ${spec.sessionId.take(8)}...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Connection status
            ConnectionStatusIndicator(spec.connectionStatus)
        }

        // Session stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            StatItem("Active", status.activeCollaborators.toString())
            StatItem("Total", status.totalCollaborators.toString())
            StatItem("Changes", status.pendingChangesCount.toString())
        }
    }
}

/**
 * Connection status indicator.
 */
@Composable
private fun ConnectionStatusIndicator(status: ConnectionStatus) {
    val (icon, color, description) = when (status) {
        ConnectionStatus.CONNECTED -> Triple(Icons.Default.Wifi, Color(0xFF4CAF50), "Connected")
        ConnectionStatus.CONNECTING -> Triple(Icons.Default.Wifi, Color(0xFFFF9800), "Connecting")
        ConnectionStatus.RECONNECTING -> Triple(Icons.Default.Refresh, Color(0xFFFF9800), "Reconnecting")
        ConnectionStatus.ERROR -> Triple(Icons.Default.Error, Color(0xFFF44336), "Connection Error")
        ConnectionStatus.DISCONNECTED -> Triple(Icons.Default.WifiOff, Color(0xFF9E9E9E), "Disconnected")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = description,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

/**
 * Tab selector for different collaboration views.
 */
@Composable
private fun CollaborationTabSelector(selectedTab: CollabTab, onTabSelected: (CollabTab) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(CollabTab.values()) { tab ->
            TabButton(tab, selectedTab == tab) { onTabSelected(tab) }
        }
    }
}

/**
 * Individual tab button.
 */
@Composable
private fun TabButton(tab: CollabTab, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                tab.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = tab.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Editor tab with collaborative text editing.
 */
@Composable
private fun EditorTab(spec: CollaborationBubble) {
    var textContent by remember(spec.content.text) { mutableStateOf(spec.content.text) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Collaborative editor
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 200.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                BasicTextField(
                    value = textContent,
                    onValueChange = {
                        textContent = it
                        // TODO: Send change to collaborators
                    },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.fillMaxSize(),
                    decorationBox = { innerTextField ->
                        if (textContent.isEmpty()) {
                            Text(
                                text = "Start collaborating...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        // Editor stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            StatItem("Words", spec.content.getWordCount().toString())
            StatItem("Chars", spec.content.getCharacterCount().toString())
            StatItem("Version", spec.content.version.toString())
        }
    }
}

/**
 * Collaborators tab showing session participants.
 */
@Composable
private fun CollaboratorsTab(spec: CollaborationBubble) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Host indicator
        if (spec.isHost) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Host",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "You are the session host",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Collaborator list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            items(spec.collaborators) { collaborator ->
                CollaboratorItem(collaborator, spec.isHost)
            }
        }

        // Invite button (only for host)
        if (spec.isHost && spec.collaborators.size < spec.permissions.maxCollaborators) {
            OutlinedButton(
                onClick = { /* Generate invitation */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Invite Collaborators")
            }
        }
    }
}

/**
 * History tab showing collaboration history.
 */
@Composable
private fun HistoryTab(spec: CollaborationBubble) {
    val recentChanges = spec.changeHistory.takeLast(10).reversed()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (recentChanges.isEmpty()) {
            EmptyHistoryView()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.heightIn(max = 250.dp)
            ) {
                items(recentChanges) { change ->
                    ChangeHistoryItem(change)
                }
            }
        }
    }
}

/**
 * Settings tab for collaboration configuration.
 */
@Composable
private fun SettingsTab(spec: CollaborationBubble) {
    if (!spec.isHost) {
        // Guests have limited settings
        GuestSettingsView()
    } else {
        // Host settings
        HostSettingsView(spec)
    }
}

/**
 * Individual collaborator item.
 */
@Composable
private fun CollaboratorItem(collaborator: Collaborator, isHost: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Avatar with status indicator
            Box {
                Surface(
                    shape = CircleShape,
                    color = Color(collaborator.color),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = collaborator.name.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Status indicator
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp),
                    shape = CircleShape,
                    color = when (collaborator.status) {
                        CollaboratorStatus.ACTIVE -> Color(0xFF4CAF50)
                        CollaboratorStatus.AWAY -> Color(0xFFFF9800)
                        CollaboratorStatus.OFFLINE -> Color(0xFFF44336)
                        CollaboratorStatus.VIEWING -> Color(0xFF2196F3)
                    }
                ) {}
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = collaborator.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (collaborator.id == "host") {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Host",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Text(
                    text = when (collaborator.status) {
                        CollaboratorStatus.ACTIVE -> "Active now"
                        CollaboratorStatus.AWAY -> "Away"
                        CollaboratorStatus.OFFLINE -> "Offline"
                        CollaboratorStatus.VIEWING -> "Viewing"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action menu (only for host)
            if (isHost && collaborator.id != "current_user") {
                IconButton(onClick = { /* Show actions */ }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More actions"
                    )
                }
            }
        }
    }
}

/**
 * Individual change history item.
 */
@Composable
private fun ChangeHistoryItem(change: ContentChange) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = when (change.type) {
                        ChangeType.TEXT_INSERT -> "Added text"
                        ChangeType.TEXT_DELETE -> "Deleted text"
                        ChangeType.CURSOR_MOVE -> "Moved cursor"
                        ChangeType.SELECTION_CHANGE -> "Changed selection"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatTimestamp(change.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (change.newText.isNotEmpty() && change.newText.length < 50) {
                Text(
                    text = "\"${change.newText}\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Empty states for different views.
 */
@Composable
private fun EmptyHistoryView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "No collaboration history yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Guest settings view (limited options).
 */
@Composable
private fun GuestSettingsView() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Guest Settings",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "You have limited settings as a guest. The host controls most session options.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { /* Leave session */ },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Leave Session")
                }
            }
        }
    }
}

/**
 * Host settings view (full control).
 */
@Composable
private fun HostSettingsView(spec: CollaborationBubble) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Session Settings",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Permission toggles
        PermissionSetting("Allow Editing", spec.permissions.allowEditing) { /* Update */ }
        PermissionSetting("Allow Inviting", spec.permissions.allowInviting) { /* Update */ }
        PermissionSetting("Allow Exporting", spec.permissions.allowExporting) { /* Update */ }

        Spacer(modifier = Modifier.height(8.dp))

        // Danger zone
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Danger Zone",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { /* End session */ },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("End Collaboration Session")
                }
            }
        }
    }
}

/**
 * Permission setting toggle.
 */
@Composable
private fun PermissionSetting(label: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Switch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}

/**
 * Statistic item for displaying counts.
 */
@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Collaboration tabs.
 */
enum class CollabTab(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    EDITOR("Editor", Icons.Default.Edit),
    COLLABORATORS("People", Icons.Default.People),
    HISTORY("History", Icons.Default.History),
    SETTINGS("Settings", Icons.Default.Settings)
}

// Helper function
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}