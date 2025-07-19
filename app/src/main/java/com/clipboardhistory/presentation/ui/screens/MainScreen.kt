package com.clipboardhistory.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ClipboardSettings
import com.clipboardhistory.presentation.ui.components.ClipboardItemCard
import com.clipboardhistory.presentation.ui.components.SettingsDialog
import com.clipboardhistory.presentation.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main screen composable for the clipboard history application.
 * 
 * This screen displays the clipboard history and provides controls
 * for managing the clipboard service and settings.
 * 
 * @param viewModel The main view model
 * @param onStartServices Callback to start services
 * @param onStopServices Callback to stop services
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onStartServices: () -> Unit,
    onStopServices: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    var showSettings by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            // Show error snackbar or toast
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Clipboard History",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Item"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (uiState.isServiceRunning) {
                        onStopServices()
                        viewModel.updateServiceRunningState(false)
                    } else {
                        onStartServices()
                        viewModel.updateServiceRunningState(true)
                    }
                }
            ) {
                Icon(
                    imageVector = if (uiState.isServiceRunning) {
                        Icons.Default.Stop
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = if (uiState.isServiceRunning) {
                        "Stop Service"
                    } else {
                        "Start Service"
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Service status indicator
            ServiceStatusCard(
                isRunning = uiState.isServiceRunning,
                modifier = Modifier.padding(16.dp)
            )
            
            // Clipboard items list
            if (uiState.clipboardItems.isEmpty()) {
                EmptyStateCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.clipboardItems) { item ->
                        ClipboardItemCard(
                            item = item,
                            onCopyClick = { clipboardItem ->
                                clipboardManager.setText(AnnotatedString(clipboardItem.content))
                            },
                            onDeleteClick = { clipboardItem ->
                                viewModel.deleteClipboardItem(clipboardItem)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Settings dialog
    if (showSettings) {
        SettingsDialog(
            settings = uiState.settings,
            onDismiss = { showSettings = false },
            onSave = { newSettings ->
                viewModel.updateSettings(newSettings)
                showSettings = false
            }
        )
    }
    
    // Add item dialog
    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { content ->
                viewModel.addClipboardItem(content)
                showAddDialog = false
            }
        )
    }
}

/**
 * Service status card composable.
 * 
 * @param isRunning Whether the service is running
 * @param modifier Modifier for the card
 */
@Composable
fun ServiceStatusCard(
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isRunning) {
                    Icons.Default.CheckCircle
                } else {
                    Icons.Default.Error
                },
                contentDescription = null,
                tint = if (isRunning) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isRunning) {
                    "Clipboard service is running"
                } else {
                    "Clipboard service is stopped"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isRunning) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )
        }
    }
}

/**
 * Empty state card composable.
 * 
 * @param modifier Modifier for the card
 */
@Composable
fun EmptyStateCard(
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ContentPaste,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No clipboard items yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start the clipboard service to begin capturing clipboard history",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Add item dialog composable.
 * 
 * @param onDismiss Callback when dialog is dismissed
 * @param onAdd Callback when item is added
 */
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Clipboard Item") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Content") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onAdd(text)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}