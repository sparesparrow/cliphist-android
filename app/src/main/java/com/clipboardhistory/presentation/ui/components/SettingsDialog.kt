package com.clipboardhistory.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clipboardhistory.domain.model.ClipboardMode
import com.clipboardhistory.domain.model.ClipboardSettings

/**
 * Settings dialog composable.
 * 
 * This dialog allows users to configure clipboard settings.
 * 
 * @param settings Current clipboard settings
 * @param onDismiss Callback when dialog is dismissed
 * @param onSave Callback when settings are saved
 */
@Composable
fun SettingsDialog(
    settings: ClipboardSettings,
    onDismiss: () -> Unit,
    onSave: (ClipboardSettings) -> Unit
) {
    var maxHistorySize by remember { mutableStateOf(settings.maxHistorySize) }
    var autoDeleteHours by remember { mutableStateOf(settings.autoDeleteAfterHours) }
    var enableEncryption by remember { mutableStateOf(settings.enableEncryption) }
    var bubbleSize by remember { mutableStateOf(settings.bubbleSize) }
    var bubbleOpacity by remember { mutableStateOf(settings.bubbleOpacity) }
    var clipboardMode by remember { mutableStateOf(settings.clipboardMode) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Max history size setting
                SettingsSlider(
                    label = "Max History Size",
                    value = maxHistorySize,
                    onValueChange = { maxHistorySize = it },
                    valueRange = 10f..500f,
                    steps = 48,
                    valueFormatter = { "${it.toInt()} items" }
                )
                
                // Auto-delete hours setting
                SettingsSlider(
                    label = "Auto-delete After",
                    value = autoDeleteHours,
                    onValueChange = { autoDeleteHours = it },
                    valueRange = 1f..168f,
                    steps = 166,
                    valueFormatter = { "${it.toInt()} hours" }
                )
                
                // Bubble size setting
                SettingsSlider(
                    label = "Bubble Size",
                    value = bubbleSize,
                    onValueChange = { bubbleSize = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    valueFormatter = { "Size ${it.toInt()}" }
                )
                
                // Bubble opacity setting
                SettingsSlider(
                    label = "Bubble Opacity",
                    value = (bubbleOpacity * 10).toInt(),
                    onValueChange = { bubbleOpacity = it / 10f },
                    valueRange = 1f..10f,
                    steps = 8,
                    valueFormatter = { "${(it * 10).toInt()}%" }
                )
                
                // Encryption toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Encryption",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = enableEncryption,
                        onCheckedChange = { enableEncryption = it }
                    )
                }
                
                // Clipboard mode selection
                Column {
                    Text(
                        text = "Clipboard Mode",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = clipboardMode == ClipboardMode.REPLACE,
                            onClick = { clipboardMode = ClipboardMode.REPLACE },
                            label = { Text("Replace") }
                        )
                        FilterChip(
                            selected = clipboardMode == ClipboardMode.EXTEND,
                            onClick = { clipboardMode = ClipboardMode.EXTEND },
                            label = { Text("Extend") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newSettings = ClipboardSettings(
                        maxHistorySize = maxHistorySize,
                        autoDeleteAfterHours = autoDeleteHours,
                        enableEncryption = enableEncryption,
                        bubbleSize = bubbleSize,
                        bubbleOpacity = bubbleOpacity,
                        clipboardMode = clipboardMode
                    )
                    onSave(newSettings)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Settings slider composable.
 * 
 * @param label The label for the slider
 * @param value The current value
 * @param onValueChange Callback when value changes
 * @param valueRange The range of values
 * @param steps Number of steps in the slider
 * @param valueFormatter Formatter for the value display
 */
@Composable
fun SettingsSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueFormatter: (Float) -> String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = valueFormatter(value.toFloat()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange,
            steps = steps
        )
    }
}