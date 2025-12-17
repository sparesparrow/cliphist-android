package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clipboardhistory.presentation.ui.bubble.BubbleSpec.TextPasteBubble
import com.clipboardhistory.utils.SmartInputManager

/**
 * Composable content for text paste bubbles.
 * Displays clipboard content with smart paste functionality.
 */
@Composable
fun TextPasteContent(
    spec: TextPasteBubble,
    smartInputManager: SmartInputManager? = null,
    onPaste: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isInputAvailable by smartInputManager?.isDirectInputAvailable()?.collectAsState() ?: remember { mutableStateOf(false) }

    val backgroundColor = when {
        isInputAvailable -> MaterialTheme.colorScheme.primaryContainer
        spec.isFavorite -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isInputAvailable -> MaterialTheme.colorScheme.onPrimaryContainer
        spec.isFavorite -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .clickable {
                onPaste()
            }
            .widthIn(min = 120.dp, max = 200.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = if (isInputAvailable) 8.dp else 4.dp
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith
                fadeOut(animationSpec = tween(200))
            },
            label = "text_content"
        ) { expanded ->
            if (expanded) {
                ExpandedTextContent(spec, contentColor, onDismiss)
            } else {
                CollapsedTextContent(spec, contentColor, isInputAvailable) {
                    isExpanded = true
                }
            }
        }
    }
}

/**
 * Collapsed view showing preview text and paste indicator.
 */
@Composable
private fun CollapsedTextContent(
    spec: TextPasteBubble,
    contentColor: Color,
    isInputAvailable: Boolean,
    onExpand: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(onClick = onExpand),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Content type icon
        Icon(
            imageVector = getContentTypeIcon(spec.contentType),
            contentDescription = spec.contentType.name.lowercase(),
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )

        // Preview text
        Text(
            text = spec.displayText,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Paste indicator
        Surface(
            shape = CircleShape,
            color = if (isInputAvailable) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isInputAvailable) {
                        Icons.Default.ContentPaste
                    } else {
                        Icons.Default.ContentCopy
                    },
                    contentDescription = if (isInputAvailable) "Smart paste" else "Copy to clipboard",
                    tint = if (isInputAvailable) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Expanded view showing full text and action buttons.
 */
@Composable
private fun ExpandedTextContent(
    spec: TextPasteBubble,
    contentColor: Color,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Header with type and close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = getContentTypeIcon(spec.contentType),
                    contentDescription = spec.contentType.name.lowercase(),
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = spec.contentType.name.lowercase().replace('_', ' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Collapse",
                    tint = contentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Full text content
        Text(
            text = spec.textContent,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        if (spec.isFavorite) {
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(12.dp)
                    )

                    Text(
                        text = "Favorite",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

/**
 * Maps content type to appropriate icon.
 */
private fun getContentTypeIcon(contentType: TextPasteBubble.ContentType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (contentType) {
        TextPasteBubble.ContentType.TEXT -> Icons.Default.TextFields
        TextPasteBubble.ContentType.URL -> Icons.Default.Link
        TextPasteBubble.ContentType.EMAIL -> Icons.Default.Email
        TextPasteBubble.ContentType.PHONE_NUMBER -> Icons.Default.Phone
        TextPasteBubble.ContentType.JSON -> Icons.Default.Code
        TextPasteBubble.ContentType.CODE -> Icons.Default.Code
    }
}