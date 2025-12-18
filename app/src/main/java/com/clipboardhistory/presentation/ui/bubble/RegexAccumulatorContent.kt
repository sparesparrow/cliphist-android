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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clipboardhistory.presentation.ui.bubble.AdvancedBubbleSpec.RegexAccumulator
import com.clipboardhistory.presentation.ui.bubble.RegexPattern.Delimiter

/**
 * Composable content for regex accumulator bubbles.
 * Displays accumulated content matching regex patterns with visual growth.
 */
@Composable
fun RegexAccumulatorContent(spec: RegexAccumulator) {
    var isExpanded by remember { mutableStateOf(false) }
    var lastViewedTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Auto-update last viewed time when expanded
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            lastViewedTime = System.currentTimeMillis()
        }
    }

    val hasNewContent = spec.hasNewContent(lastViewedTime)
    val itemCount = spec.accumulatedItems.size

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
            label = "regex_accumulator_content"
        ) { expanded ->
            if (expanded) {
                ExpandedRegexAccumulator(spec, onCollapse = { isExpanded = false })
            } else {
                CollapsedRegexAccumulator(
                    spec = spec,
                    itemCount = itemCount,
                    hasNewContent = hasNewContent,
                    onExpand = { isExpanded = true }
                )
            }
        }
    }
}

/**
 * Collapsed view showing summary and growth indicator.
 */
@Composable
private fun CollapsedRegexAccumulator(
    spec: RegexAccumulator,
    itemCount: Int,
    hasNewContent: Boolean,
    onExpand: () -> Unit
) {
    val dynamicSize = spec.getDynamicSize(spec.type.defaultSize)
    val animatedSize by animateDpAsState(
        targetValue = dynamicSize,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bubble_growth"
    )

    Box(
        modifier = Modifier
            .size(animatedSize)
            .clickable(onClick = onExpand),
        contentAlignment = Alignment.Center
    ) {
        // Background with growth gradient
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = when {
                hasNewContent -> MaterialTheme.colorScheme.primaryContainer
                itemCount > 0 -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (itemCount > 0) {
                    // Show count with growth animation
                    Text(
                        text = itemCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = when {
                            hasNewContent -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        fontWeight = FontWeight.Bold
                    )

                    // New content indicator
                    if (hasNewContent) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(16.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                Icons.Default.FiberNew,
                                contentDescription = "New items",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else {
                    // Empty state
                    Icon(
                        Icons.Default.Pattern,
                        contentDescription = "Regex pattern",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Pattern indicator at bottom
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .height(2.dp),
            shape = RoundedCornerShape(1.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        ) {
            Box(modifier = Modifier.fillMaxWidth(0.6f))
        }
    }
}

/**
 * Expanded view showing all accumulated items with controls.
 */
@Composable
private fun ExpandedRegexAccumulator(
    spec: RegexAccumulator,
    onCollapse: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(320.dp)
            .heightIn(min = 300.dp, max = 500.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with pattern info and controls
        RegexAccumulatorHeader(spec, onCollapse)

        // Collection status
        CollectionStatusIndicator(spec)

        // Accumulated items list
        AccumulatedItemsList(spec.accumulatedItems)

        // Action buttons
        RegexAccumulatorActions(spec)
    }
}

/**
 * Header with pattern information and controls.
 */
@Composable
private fun RegexAccumulatorHeader(spec: RegexAccumulator, onCollapse: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spec.pattern.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Pattern: ${spec.pattern.pattern}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (spec.pattern.description.isNotEmpty()) {
                Text(
                    text = spec.pattern.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Collection toggle
            IconButton(
                onClick = { /* Toggle collection */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (spec.isCollecting) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (spec.isCollecting) "Pause collection" else "Resume collection",
                    tint = if (spec.isCollecting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Close button
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Collapse"
                )
            }
        }
    }
}

/**
 * Status indicator showing collection state.
 */
@Composable
private fun CollectionStatusIndicator(spec: RegexAccumulator) {
    val itemCount = spec.accumulatedItems.size
    val maxItems = spec.pattern.maxItems

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$itemCount items collected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${(itemCount.toFloat() / maxItems * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = itemCount.toFloat() / maxItems,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            // Collection status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (spec.isCollecting) Color.Green else Color.Gray,
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (spec.isCollecting) "Collecting" else "Paused",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Max: $maxItems",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Scrollable list of accumulated items.
 */
@Composable
private fun AccumulatedItemsList(items: List<AdvancedBubbleSpec.AccumulatedItem>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        if (items.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Inbox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No items collected yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Copy content matching the pattern to start collecting",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items.reversed()) { item -> // Show newest first
                    AccumulatedItemRow(item)
                }
            }
        }
    }
}

/**
 * Individual accumulated item row.
 */
@Composable
private fun AccumulatedItemRow(item: AdvancedBubbleSpec.AccumulatedItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Content preview
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Timestamp
            Text(
                text = formatTimestamp(item.matchedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Copy button
            IconButton(
                onClick = { /* Copy this item */ },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy item",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Action buttons for the accumulator.
 */
@Composable
private fun RegexAccumulatorActions(spec: RegexAccumulator) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { /* Export all items */ },
            modifier = Modifier.weight(1f),
            enabled = spec.accumulatedItems.isNotEmpty()
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Export")
        }

        OutlinedButton(
            onClick = { /* Clear all items */ },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            enabled = spec.accumulatedItems.isNotEmpty()
        ) {
            Icon(
                Icons.Default.Clear,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Clear")
        }
    }
}

// Helper functions

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "now" // Less than 1 minute
        diff < 3600000 -> "${diff / 60000}m ago" // Minutes
        diff < 86400000 -> "${diff / 3600000}h ago" // Hours
        else -> "${diff / 86400000}d ago" // Days
    }
}