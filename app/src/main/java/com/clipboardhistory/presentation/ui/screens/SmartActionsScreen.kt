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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clipboardhistory.domain.model.SmartAction
import com.clipboardhistory.presentation.viewmodels.MainViewModel
import com.clipboardhistory.presentation.viewmodels.SmartActionsViewModel
import kotlinx.coroutines.launch

/**
 * Screen for displaying and executing smart actions for a clipboard item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartActionsScreen(
    itemId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onActionExecuted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Use dedicated smart actions view model
    val smartActionsViewModel: SmartActionsViewModel = hiltViewModel()
    val clipboardItem by smartActionsViewModel.clipboardItem.collectAsState()
    val availableActions by smartActionsViewModel.availableActions.collectAsState()
    val isLoading by smartActionsViewModel.isLoading.collectAsState()

    val showActionDialog = remember { mutableStateOf<SmartAction?>(null) }

    // Load item data when screen is created
    androidx.compose.runtime.LaunchedEffect(itemId) {
        smartActionsViewModel.loadItem(itemId.toString())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Actions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Clipboard Item Preview
            clipboardItem?.let { item ->
                ClipboardItemPreview(
                    item = item,
                    onToggleFavorite = {
                        scope.launch {
                            smartActionsViewModel.toggleFavorite(itemId)
                        }
                    },
                )
            }

            // Available Actions
            if (availableActions.isNotEmpty()) {
                Text(
                    text = "Available Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp),
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(availableActions) { action ->
                        SmartActionCard(
                            action = action,
                            onActionClick = { showActionDialog.value = action },
                        )
                    }
                }
            } else if (!isLoading) {
                // No actions available
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No smart actions available for this content",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Quick Actions Row
            QuickActionsRow(
                onCopyToClipboard = {
                    scope.launch {
                        clipboardItem?.let { item ->
                            smartActionsViewModel.copyToClipboard(item.content)
                            scope.launch {
                                snackbarHostState.showSnackbar("Copied to clipboard")
                            }
                        }
                    }
                },
                onShare = {
                    scope.launch {
                        clipboardItem?.let { item ->
                            smartActionsViewModel.shareContent(item.content, item.contentType.name)
                            scope.launch {
                                snackbarHostState.showSnackbar("Sharing...")
                            }
                        }
                    }
                },
            )
        }
    }

    // Action Confirmation Dialog
    showActionDialog.value?.let { action ->
        SmartActionDialog(
            action = action,
            clipboardContent = clipboardItem?.content ?: "",
            onConfirm = {
                scope.launch {
                    val result = smartActionsViewModel.executeSmartAction(
                        content = clipboardItem?.content ?: "",
                        contentType = clipboardItem?.contentType?.name ?: "TEXT",
                        action = action,
                    )

                    when (result) {
                        is com.clipboardhistory.domain.usecase.ExecuteSmartActionUseCase.SmartActionResult.Success -> {
                            snackbarHostState.showSnackbar(result.message)
                            onActionExecuted()
                        }
                        is com.clipboardhistory.domain.usecase.ExecuteSmartActionUseCase.SmartActionResult.Failure -> {
                            snackbarHostState.showSnackbar("Failed: ${result.error}")
                        }
                    }
                }
                showActionDialog.value = null
            },
            onDismiss = { showActionDialog.value = null },
        )
    }
}

@Composable
private fun ClipboardItemPreview(
    item: com.clipboardhistory.domain.model.ClipboardItem,
    onToggleFavorite: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Clipboard Content",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (item.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (item.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Type: ${formatContentType(item.contentType.name)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = formatTimestamp(item.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SmartActionCard(
    action: SmartAction,
    onActionClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = onActionClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = getActionIcon(action.type),
                contentDescription = action.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = getActionDescription(action.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Execute action",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    onCopyToClipboard: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
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

        OutlinedButton(
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

@Composable
private fun SmartActionDialog(
    action: SmartAction,
    clipboardContent: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Execute ${action.label}") },
        text = {
            Column {
                Text("Are you sure you want to ${action.label.lowercase()}?")
                if (clipboardContent.length < 100) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"$clipboardContent\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Execute")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

// Helper functions

private fun getActionIcon(actionType: SmartAction.ActionType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (actionType) {
        SmartAction.ActionType.OPEN_LINK -> Icons.Default.Launch
        SmartAction.ActionType.CALL_NUMBER -> Icons.Default.Call
        SmartAction.ActionType.SEND_EMAIL -> Icons.Default.Email
        SmartAction.ActionType.OPEN_MAPS -> Icons.Default.Map
        SmartAction.ActionType.SEARCH_WEB -> androidx.compose.material.icons.Icons.Default.Search
        SmartAction.ActionType.SEND_SMS -> androidx.compose.material.icons.Icons.Default.Message
        SmartAction.ActionType.ADD_CONTACT -> androidx.compose.material.icons.Icons.Default.PersonAdd
        SmartAction.ActionType.OPEN_APP -> Icons.Default.Launch
        SmartAction.ActionType.COPY_TO_CLIPBOARD -> Icons.Default.ContentCopy
        SmartAction.ActionType.SHARE_CONTENT -> Icons.Default.Share
        SmartAction.ActionType.FORMAT_JSON -> androidx.compose.material.icons.Icons.Default.Code
        SmartAction.ActionType.VALIDATE_JSON -> androidx.compose.material.icons.Icons.Default.CheckCircle
        SmartAction.ActionType.CONNECT_WIFI -> androidx.compose.material.icons.Icons.Default.Wifi
        SmartAction.ActionType.MASK_CARD -> androidx.compose.material.icons.Icons.Default.CreditCard
        SmartAction.ActionType.OPEN_CALENDAR -> Icons.Default.DateRange
        SmartAction.ActionType.OPEN_CONTACT -> androidx.compose.material.icons.Icons.Default.Contacts
        SmartAction.ActionType.CUSTOM -> androidx.compose.material.icons.Icons.Default.Settings
    }
}

private fun getActionDescription(actionType: SmartAction.ActionType): String {
    return when (actionType) {
        SmartAction.ActionType.OPEN_LINK -> "Open this URL in your browser"
        SmartAction.ActionType.CALL_NUMBER -> "Call this phone number"
        SmartAction.ActionType.SEND_EMAIL -> "Send email to this address"
        SmartAction.ActionType.OPEN_MAPS -> "Show this location on map"
        SmartAction.ActionType.SEARCH_WEB -> "Search the web for this content"
        SmartAction.ActionType.SEND_SMS -> "Send SMS to this number"
        SmartAction.ActionType.ADD_CONTACT -> "Add this number to contacts"
        SmartAction.ActionType.OPEN_APP -> "Open the associated application"
        SmartAction.ActionType.COPY_TO_CLIPBOARD -> "Copy content to clipboard"
        SmartAction.ActionType.SHARE_CONTENT -> "Share content with other apps"
        SmartAction.ActionType.FORMAT_JSON -> "Format JSON for readability"
        SmartAction.ActionType.VALIDATE_JSON -> "Check if JSON is valid"
        SmartAction.ActionType.CONNECT_WIFI -> "Connect to this WiFi network"
        SmartAction.ActionType.MASK_CARD -> "Mask credit card number"
        SmartAction.ActionType.OPEN_CALENDAR -> "Create calendar event"
        SmartAction.ActionType.OPEN_CONTACT -> "Open contacts application"
        SmartAction.ActionType.CUSTOM -> "Custom action"
    }
}

private fun formatContentType(contentType: String): String {
    return when (contentType) {
        "text/plain" -> "Plain Text"
        "text/url" -> "URL"
        "text/email" -> "Email"
        "text/phone" -> "Phone Number"
        "text/address" -> "Address"
        "application/json" -> "JSON"
        "text/credit-card" -> "Credit Card"
        else -> contentType
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        else -> "${diff / (24 * 60 * 60 * 1000)}d ago"
    }
}