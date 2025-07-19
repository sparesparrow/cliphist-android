# Create the main ViewModel
main_view_model = '''package com.clipboardhistory.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ClipboardSettings
import com.clipboardhistory.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main ViewModel for the clipboard history application.
 * 
 * This ViewModel manages the state and business logic for the main screen,
 * including clipboard items, settings, and user interactions.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getAllClipboardItemsUseCase: GetAllClipboardItemsUseCase,
    private val addClipboardItemUseCase: AddClipboardItemUseCase,
    private val deleteClipboardItemUseCase: DeleteClipboardItemUseCase,
    private val getClipboardSettingsUseCase: GetClipboardSettingsUseCase,
    private val updateClipboardSettingsUseCase: UpdateClipboardSettingsUseCase,
    private val cleanupOldItemsUseCase: CleanupOldItemsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    /**
     * Data class representing the UI state for the main screen.
     * 
     * @property clipboardItems List of clipboard items
     * @property settings Current clipboard settings
     * @property isLoading Whether the screen is loading
     * @property error Error message if any
     * @property isServiceRunning Whether the clipboard service is running
     */
    data class MainUiState(
        val clipboardItems: List<ClipboardItem> = emptyList(),
        val settings: ClipboardSettings = ClipboardSettings(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val isServiceRunning: Boolean = false
    )
    
    init {
        loadClipboardItems()
        loadSettings()
    }
    
    /**
     * Loads clipboard items from the repository.
     */
    private fun loadClipboardItems() {
        viewModelScope.launch {
            getAllClipboardItemsUseCase().collect { items ->
                _uiState.value = _uiState.value.copy(
                    clipboardItems = items,
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Loads settings from the repository.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val settings = getClipboardSettingsUseCase()
                _uiState.value = _uiState.value.copy(settings = settings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    /**
     * Adds a new clipboard item.
     * 
     * @param content The content to add
     */
    fun addClipboardItem(content: String) {
        viewModelScope.launch {
            try {
                addClipboardItemUseCase(content)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    /**
     * Deletes a clipboard item.
     * 
     * @param item The item to delete
     */
    fun deleteClipboardItem(item: ClipboardItem) {
        viewModelScope.launch {
            try {
                deleteClipboardItemUseCase(item)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    /**
     * Updates the clipboard settings.
     * 
     * @param settings The new settings
     */
    fun updateSettings(settings: ClipboardSettings) {
        viewModelScope.launch {
            try {
                updateClipboardSettingsUseCase(settings)
                _uiState.value = _uiState.value.copy(settings = settings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    /**
     * Updates the service running state.
     * 
     * @param isRunning Whether the service is running
     */
    fun updateServiceRunningState(isRunning: Boolean) {
        _uiState.value = _uiState.value.copy(isServiceRunning = isRunning)
    }
    
    /**
     * Clears the current error.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Triggers cleanup of old clipboard items.
     */
    fun cleanupOldItems() {
        viewModelScope.launch {
            try {
                cleanupOldItemsUseCase(_uiState.value.settings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}'''

# Create MainActivity
main_activity = '''package com.clipboardhistory.presentation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.clipboardhistory.presentation.services.ClipboardService
import com.clipboardhistory.presentation.services.FloatingBubbleService
import com.clipboardhistory.presentation.ui.screens.MainScreen
import com.clipboardhistory.presentation.ui.theme.ClipboardHistoryTheme
import com.clipboardhistory.presentation.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for the Clipboard History application.
 * 
 * This activity serves as the entry point for the application and manages
 * the overall UI and service lifecycle.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(this)) {
            startServices()
        }
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            requestOverlayPermission()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ClipboardHistoryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = hiltViewModel()
                    val context = LocalContext.current
                    
                    MainScreen(
                        viewModel = viewModel,
                        onStartServices = {
                            requestPermissions()
                        },
                        onStopServices = {
                            stopServices()
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Requests necessary permissions for the application.
     */
    private fun requestPermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    requestOverlayPermission()
                }
            }
            else -> {
                requestOverlayPermission()
            }
        }
    }
    
    /**
     * Requests overlay permission for floating bubbles.
     */
    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            startServices()
        }
    }
    
    /**
     * Starts the clipboard and floating bubble services.
     */
    private fun startServices() {
        val clipboardIntent = Intent(this, ClipboardService::class.java)
        val bubbleIntent = Intent(this, FloatingBubbleService::class.java)
        
        startForegroundService(clipboardIntent)
        startForegroundService(bubbleIntent)
    }
    
    /**
     * Stops the clipboard and floating bubble services.
     */
    private fun stopServices() {
        val clipboardIntent = Intent(this, ClipboardService::class.java)
        val bubbleIntent = Intent(this, FloatingBubbleService::class.java)
        
        stopService(clipboardIntent)
        stopService(bubbleIntent)
    }
}'''

# Create main screen composable
main_screen = '''package com.clipboardhistory.presentation.ui.screens

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
}'''

# Write the UI files
with open('app/src/main/java/com/clipboardhistory/presentation/viewmodels/MainViewModel.kt', 'w') as f:
    f.write(main_view_model)

with open('app/src/main/java/com/clipboardhistory/presentation/MainActivity.kt', 'w') as f:
    f.write(main_activity)

with open('app/src/main/java/com/clipboardhistory/presentation/ui/screens/MainScreen.kt', 'w') as f:
    f.write(main_screen)

print("UI layer (ViewModels, MainActivity, and main screen) created!")