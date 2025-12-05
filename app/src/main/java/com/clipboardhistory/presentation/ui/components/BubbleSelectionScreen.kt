package com.clipboardhistory.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clipboardhistory.domain.model.BubbleState
import com.clipboardhistory.domain.model.BubbleThemes
import com.clipboardhistory.domain.model.ClipboardItem

/**
 * Bubble selection screen for share operations.
 *
 * @param sharedText The text being shared
 * @param onReplaceBubble Callback when a bubble is selected for replacement
 * @param onAppendBubble Callback when a bubble is selected for appending
 * @param onPrependBubble Callback when a bubble is selected for prepending
 * @param onAddNewBubble Callback when user wants to add a new bubble
 * @param onCancel Callback when user cancels the operation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BubbleSelectionScreen(
    sharedText: String,
    onReplaceBubble: (ClipboardItem) -> Unit,
    onAppendBubble: (ClipboardItem) -> Unit,
    onPrependBubble: (ClipboardItem) -> Unit,
    onAddNewBubble: () -> Unit,
    onCancel: () -> Unit,
    clipboardItems: List<ClipboardItem> = emptyList(),
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Select Bubble",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            // Shared text preview
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "Shared Text:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = sharedText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Instructions
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "Choose an option:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Replace: Replace bubble content with shared text",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "• Append: Add shared text to end of bubble content",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "• Prepend: Add shared text to beginning of bubble content",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "• Or create a new bubble",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // Existing bubbles
            if (clipboardItems.isNotEmpty()) {
                Text(
                    text = "Existing Bubbles:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp),
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(clipboardItems.take(10)) { item ->
                        BubbleSelectionItem(
                            item = item,
                            onReplace = { onReplaceBubble(item) },
                            onAppend = { onAppendBubble(item) },
                            onPrepend = { onPrependBubble(item) },
                        )
                    }
                }
            } else {
                // Empty state
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No existing bubbles",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create a new bubble to store your content",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Add new bubble button
            Button(
                onClick = onAddNewBubble,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Bubble")
            }
        }
    }
}

/**
 * Individual bubble selection item.
 *
 * @param item The clipboard item
 * @param onReplace Callback when replace is selected
 * @param onAppend Callback when append is selected
 * @param onPrepend Callback when prepend is selected
 */
@Composable
fun BubbleSelectionItem(
    item: ClipboardItem,
    onReplace: () -> Unit,
    onAppend: () -> Unit,
    onPrepend: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Bubble preview
            BubblePreview(
                content = item.content,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Content preview
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.content.count { it == '\n' } + 1} lines",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Action buttons
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Replace button
                    OutlinedButton(
                        onClick = onReplace,
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Replace", style = MaterialTheme.typography.bodySmall)
                    }

                    // Append button
                    OutlinedButton(
                        onClick = onAppend,
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Append", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Prepend button
                OutlinedButton(
                    onClick = onPrepend,
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Prepend", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * Bubble preview component.
 *
 * @param content The content to display
 * @param modifier Modifier for the bubble
 */
@Composable
fun BubblePreview(
    content: String,
    modifier: Modifier = Modifier,
) {
    val theme = BubbleThemes.DEFAULT
    val state = if (content.isBlank()) BubbleState.EMPTY else BubbleState.STORING
    val color =
        when (state) {
            BubbleState.EMPTY -> Color(theme.colors.empty)
            BubbleState.STORING -> Color(theme.colors.storing)
            BubbleState.REPLACE -> Color(theme.colors.replace)
            BubbleState.APPEND -> Color(theme.colors.append)
            BubbleState.PREPEND -> Color(theme.colors.prepend)
        }

    val lineCount = if (content.isNotBlank()) content.count { it == '\n' } + 1 else 0

    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .background(color)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (lineCount > 0) {
            Text(
                text = lineCount.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f > 0.5f) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
