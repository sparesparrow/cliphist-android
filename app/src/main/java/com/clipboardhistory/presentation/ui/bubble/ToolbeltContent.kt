package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clipboardhistory.presentation.ui.bubble.BubbleSpec.ToolbeltBubble

/**
 * Composable content for toolbelt bubbles.
 * Displays a horizontal or vertical arrangement of tool buttons.
 */
@Composable
fun ToolbeltContent(spec: ToolbeltBubble) {
    val transition = updateTransition(targetState = spec.isMinimized, label = "toolbelt")

    transition.AnimatedContent(
        transitionSpec = {
            if (targetState) {
                // Minimizing: scale down and fade
                scaleIn(animationSpec = tween(300)) togetherWith
                scaleOut(animationSpec = tween(200))
            } else {
                // Expanding: scale up and fade
                scaleIn(animationSpec = tween(300)) togetherWith
                scaleOut(animationSpec = tween(200))
            }
        }
    ) { isMinimized ->
        if (isMinimized) {
            MinimizedToolbelt(spec)
        } else {
            ExpandedToolbelt(spec)
        }
    }
}

/**
 * Minimized toolbelt - shows just an expand handle.
 */
@Composable
private fun MinimizedToolbelt(spec: ToolbeltBubble) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .clickable {
                // This will be handled by the parent bubble orchestrator
                // to expand the toolbelt
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Expand toolbelt",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Expanded toolbelt - shows all available tools.
 */
@Composable
private fun ExpandedToolbelt(spec: ToolbeltBubble) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        when (spec.orientation) {
            ToolbeltBubble.ToolbeltOrientation.HORIZONTAL -> HorizontalToolbelt(spec)
            ToolbeltBubble.ToolbeltOrientation.VERTICAL -> VerticalToolbelt(spec)
        }
    }
}

/**
 * Horizontal toolbelt layout.
 */
@Composable
private fun HorizontalToolbelt(spec: ToolbeltBubble) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with minimize button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tools",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(
                onClick = { /* Handled by parent */ },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Minimize,
                    contentDescription = "Minimize toolbelt",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tool buttons in a scrollable row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(spec.tools.sortedBy { it.priority }) { tool ->
                ToolButton(tool)
            }
        }
    }
}

/**
 * Vertical toolbelt layout.
 */
@Composable
private fun VerticalToolbelt(spec: ToolbeltBubble) {
    Row(
        modifier = Modifier
            .height(200.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tools in a column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            spec.tools.sortedBy { it.priority }.forEach { tool ->
                VerticalToolButton(tool)
            }
        }

        // Minimize button on the side
        IconButton(
            onClick = { /* Handled by parent */ },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Minimize,
                contentDescription = "Minimize toolbelt",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Individual tool button for horizontal layout.
 */
@Composable
private fun ToolButton(tool: ToolbeltTool) {
    val icon = getToolIcon(tool.icon)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable(enabled = tool.isEnabled) {
                    tool.action()
                },
            shape = CircleShape,
            color = if (tool.isEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = tool.name,
                    tint = if (tool.isEnabled) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = tool.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Individual tool button for vertical layout.
 */
@Composable
private fun VerticalToolButton(tool: ToolbeltTool) {
    val icon = getToolIcon(tool.icon)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .clickable(enabled = tool.isEnabled) {
                    tool.action()
                },
            shape = CircleShape,
            color = if (tool.isEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = tool.name,
                    tint = if (tool.isEnabled) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = tool.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Maps tool icon resource IDs to Compose Icons.
 * In a real implementation, this would use actual icon resources.
 */
private fun getToolIcon(iconResId: Int): ImageVector {
    return when (iconResId) {
        // Placeholder mappings - in real app these would be actual icon resources
        1 -> Icons.Default.Opacity // Transparency slider
        2 -> Icons.Default.VisibilityOff // Private toggle
        3 -> Icons.Default.History // History toggle
        4 -> Icons.Default.SwapHoriz // Type changer
        5 -> Icons.Default.Edit // Content editor
        6 -> Icons.Default.Refresh // Operation mode
        7 -> Icons.Default.ClearAll // Clear all
        8 -> Icons.Default.Settings // Settings
        else -> Icons.Default.Build
    }
}