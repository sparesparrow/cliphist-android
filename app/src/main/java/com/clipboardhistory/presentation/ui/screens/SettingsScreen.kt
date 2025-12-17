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
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clipboardhistory.domain.model.BubbleType
import com.clipboardhistory.presentation.viewmodels.MainViewModel
import com.clipboardhistory.presentation.viewmodels.SettingsViewModel
import com.clipboardhistory.utils.PermissionUtils
import kotlinx.coroutines.launch

/**
 * Settings screen for configuring clipboard behavior and appearance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Use a separate settings view model for settings-specific logic
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.settings.collectAsState()
    val permissionStatus by settingsViewModel.permissionStatus.collectAsState()

    var showClearDataDialog by remember { mutableStateOf(false) }
    var showResetSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Permissions Section
            SettingsSection(title = "Permissions") {
                PermissionSetting(
                    title = "System Alert Window",
                    description = "Required to display floating clipboard bubbles",
                    icon = Icons.Default.ContentCopy,
                    isGranted = permissionStatus.overlayGranted,
                    onRequestPermission = {
                        settingsViewModel.requestOverlayPermission(context)
                    },
                )

                PermissionSetting(
                    title = "Notifications",
                    description = "Required to show clipboard monitoring notifications",
                    icon = Icons.Default.Notifications,
                    isGranted = permissionStatus.notificationsGranted,
                    onRequestPermission = {
                        settingsViewModel.requestNotificationPermission(context as androidx.activity.ComponentActivity)
                    },
                )

                PermissionSetting(
                    title = "Usage Access",
                    description = "Required to detect which app copied content",
                    icon = Icons.Default.Accessibility,
                    isGranted = permissionStatus.usageAccessGranted,
                    onRequestPermission = {
                        settingsViewModel.requestUsageAccessPermission(context)
                    },
                )

                PermissionSetting(
                    title = "Battery Optimization",
                    description = "Required to run monitoring in background",
                    icon = Icons.Default.BatteryAlert,
                    isGranted = permissionStatus.batteryOptimizationGranted,
                    onRequestPermission = {
                        settingsViewModel.requestBatteryOptimizationPermission(context)
                    },
                )

                PermissionSetting(
                    title = "Accessibility Service",
                    description = "Required for advanced clipboard monitoring",
                    icon = Icons.Default.Accessibility,
                    isGranted = permissionStatus.accessibilityGranted,
                    onRequestPermission = {
                        settingsViewModel.requestAccessibilityPermission(context)
                    },
                )
            }

            // Clipboard Settings
            SettingsSection(title = "Clipboard Monitoring") {
                SwitchSetting(
                    title = "Enable Clipboard Monitoring",
                    description = "Automatically capture clipboard content",
                    checked = settings.enableClipboardMonitoring,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsViewModel.updateClipboardMonitoring(enabled)
                        }
                    },
                )

                SwitchSetting(
                    title = "Accessibility Monitoring",
                    description = "Enhanced monitoring using accessibility service",
                    checked = settings.enableAccessibilityMonitoring,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsViewModel.updateAccessibilityMonitoring(enabled)
                        }
                    },
                )

                SwitchSetting(
                    title = "Enable Encryption",
                    description = "Encrypt clipboard data for security",
                    checked = settings.enableEncryption,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsViewModel.updateEncryption(enabled)
                        }
                    },
                )
            }

            // Appearance Settings
            SettingsSection(title = "Appearance") {
                SliderSetting(
                    title = "Bubble Size",
                    description = "Size of floating clipboard bubbles (1-5)",
                    value = settings.bubbleSize.toFloat(),
                    valueRange = 1f..5f,
                    steps = 4,
                    onValueChange = { size ->
                        scope.launch {
                            settingsViewModel.updateBubbleSize(size.toInt())
                        }
                    },
                )

                SliderSetting(
                    title = "Bubble Opacity",
                    description = "Transparency of floating bubbles",
                    value = settings.bubbleOpacity,
                    valueRange = 0.1f..1.0f,
                    onValueChange = { opacity ->
                        scope.launch {
                            settingsViewModel.updateBubbleOpacity(opacity)
                        }
                    },
                )

                BubbleTypeSelector(
                    selectedType = settings.bubbleType,
                    onTypeSelected = { type ->
                        scope.launch {
                            settingsViewModel.updateBubbleType(type)
                        }
                    },
                )
            }

            // Data Management
            SettingsSection(title = "Data Management") {
                ButtonSetting(
                    title = "Clear All Data",
                    description = "Permanently delete all clipboard history",
                    buttonText = "Clear Data",
                    isDestructive = true,
                    onClick = { showClearDataDialog = true },
                )

                ButtonSetting(
                    title = "Reset Settings",
                    description = "Reset all settings to default values",
                    buttonText = "Reset",
                    isDestructive = true,
                    onClick = { showResetSettingsDialog = true },
                )
            }

            // About Section
            SettingsSection(title = "About") {
                InfoSetting(
                    title = "Version",
                    value = "1.0.0", // This would come from BuildConfig
                )

                InfoSetting(
                    title = "Database Status",
                    value = "Encrypted SQLite", // This could show actual status
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data") },
            text = { Text("This will permanently delete all clipboard history. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            settingsViewModel.clearAllData()
                            snackbarHostState.showSnackbar("All data cleared")
                        }
                        showClearDataDialog = false
                    },
                ) {
                    Text("Clear Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Reset Settings Confirmation Dialog
    if (showResetSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showResetSettingsDialog = false },
            title = { Text("Reset Settings") },
            text = { Text("This will reset all settings to their default values.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            settingsViewModel.resetSettings()
                            snackbarHostState.showSnackbar("Settings reset to defaults")
                        }
                        showResetSettingsDialog = false
                    },
                ) {
                        Text("Reset")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetSettingsDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun PermissionSetting(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!isGranted) {
            OutlinedButton(onClick = onRequestPermission) {
                Text("Grant")
            }
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SliderSetting(
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = String.format("%.1f", value),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BubbleTypeSelector(
    selectedType: BubbleType,
    onTypeSelected: (BubbleType) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = "Bubble Type",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "Shape of the floating clipboard bubbles",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BubbleType.values().forEach { type ->
                OutlinedButton(
                    onClick = { onTypeSelected(type) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = type.name,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ButtonSetting(
    title: String,
    description: String,
    buttonText: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isDestructive) {
            OutlinedButton(
                onClick = onClick,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(buttonText)
            }
        } else {
            Button(onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun InfoSetting(
    title: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}