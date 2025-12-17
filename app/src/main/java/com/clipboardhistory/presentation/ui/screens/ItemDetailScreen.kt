package com.clipboardhistory.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.presentation.viewmodels.ItemDetailViewModel
import com.clipboardhistory.presentation.viewmodels.MainViewModel
import kotlinx.coroutines.launch

/**
 * Screen for viewing detailed information about a clipboard item and performing actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSmartActions: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Use dedicated item detail view model
    val itemDetailViewModel: ItemDetailViewModel = hiltViewModel()
    val clipboardItem by itemDetailViewModel.clipboardItem.collectAsState()
    val isEditing by itemDetailViewModel.isEditing.collectAsState()
    val editedContent by itemDetailViewModel.editedContent.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Load item data when screen is created
    androidx.compose.runtime.LaunchedEffect(itemId) {
        itemDetailViewModel.loadItem(itemId.toString())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Edit/Save button
                    IconButton(
                        onClick = {
                            if (isEditing) {
                                scope.launch {
                                    val success = itemDetailViewModel.saveEditedContent()
                                    if (success) {
                                        snackbarHostState.showSnackbar("Content updated")
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to update content")
                                    }
                                }
                            } else {
                                itemDetailViewModel.startEditing()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Save
                                         else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save changes" else "Edit content",
                        )
                    }

                    // Favorite toggle
                    IconButton(
                        onClick = {
                            scope.launch {
                                val success = itemDetailViewModel.toggleFavorite()
                                if (success) {
                                    val message = if (clipboardItem?.isFavorite == true) "Removed from favorites"
                                                 else "Added to favorites"
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        }
                    ) {
                        val isFavorite = clipboardItem?.isFavorite ?: false
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete item",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToSmartActions(itemId) },
                icon = { Icon(Icons.Default.Lightbulb, "Smart Actions") },
                text = { Text("Smart Actions") },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            clipboardItem?.let { item ->
                // Content Section
                ContentSection(
                    item = item,
                    isEditing = isEditing,
                    editedContent = editedContent,
                    onContentChange = { itemDetailViewModel.updateEditedContent(it) },
                    onCancelEdit = { itemDetailViewModel.cancelEditing() },
                )

                // Metadata Section
                MetadataSection(item = item)

                // Quick Actions Section
                QuickActionsSection(
                    onCopyToClipboard = {
                        itemDetailViewModel.copyToClipboard(context)
                        scope.launch {
                            snackbarHostState.showSnackbar("Copied to clipboard")
                        }
                    },
                    onShare = {
                        itemDetailViewModel.shareContent(context)
                    },
                )

                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to delete this clipboard item? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val success = itemDetailViewModel.deleteItem()
                            if (success) {
                                snackbarHostState.showSnackbar("Item deleted")
                                onNavigateBack()
                            } else {
                                snackbarHostState.showSnackbar("Failed to delete item")
                            }
                        }
                        showDeleteDialog = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun ContentSection(
    item: ClipboardItem,
    isEditing: Boolean,
    editedContent: String,
    onContentChange: (String) -> Unit,
    onCancelEdit: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Content",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = onContentChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 10,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onCancelEdit) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { /* Save handled in parent */ },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Save")
                    }
                }
            } else {
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MetadataSection(item: ClipboardItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Metadata",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            MetadataRow("Type", formatContentType(item.contentType.name))
            MetadataRow("Length", "${item.content.length} characters")
            MetadataRow("Created", formatTimestamp(item.timestamp))
            item.sourceApp?.let { MetadataRow("Source App", it) }
            item.encryptionKey?.let { MetadataRow("Encrypted", "Yes") }
            MetadataRow("Favorite", if (item.isFavorite) "Yes" else "No")
            MetadataRow("Status", if (item.isDeleted) "Deleted" else "Active")
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun QuickActionsSection(
    onCopyToClipboard: () -> Unit,
    onShare: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onCopyToClipboard,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy")
                }

                androidx.compose.material3.OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }
        }
    }
}

// Helper functions

private fun formatContentType(contentType: String): String {
    return when (contentType) {
        "text/plain" -> "Plain Text"
        "text/url" -> "URL"
        "text/email" -> "Email Address"
        "text/phone" -> "Phone Number"
        "text/address" -> "Address"
        "application/json" -> "JSON Data"
        "text/credit-card" -> "Credit Card"
        "text/wifi" -> "WiFi Credentials"
        else -> contentType.replaceFirstChar { it.uppercase() }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
    val dateString = dateFormat.format(java.util.Date(timestamp))

    val relativeTime = when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
        else -> "${diff / (24 * 60 * 60 * 1000)} days ago"
    }

    return "$dateString ($relativeTime)"
}