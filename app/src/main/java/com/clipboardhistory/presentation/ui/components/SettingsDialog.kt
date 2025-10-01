package com.clipboardhistory.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clipboardhistory.domain.model.BubbleThemes
import com.clipboardhistory.domain.model.BubbleType
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
    onSave: (ClipboardSettings) -> Unit,
) {
    var maxHistorySize by remember { mutableStateOf(settings.maxHistorySize) }
    var autoDeleteHours by remember { mutableStateOf(settings.autoDeleteAfterHours) }
    var enableEncryption by remember { mutableStateOf(settings.enableEncryption) }
    var bubbleSize by remember { mutableStateOf(settings.bubbleSize) }
    var bubbleOpacity by remember { mutableStateOf(settings.bubbleOpacity) }
    var selectedTheme by remember { mutableStateOf(settings.selectedTheme) }
    var selectedBubbleType by remember { mutableStateOf(settings.bubbleType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Max history size setting
                SettingsSlider(
                    label = "Max History Size",
                    value = maxHistorySize,
                    onValueChange = { maxHistorySize = it },
                    valueRange = 10f..500f,
                    steps = 48,
                    valueFormatter = { "${it.toInt()} items" },
                )

                // Auto-delete hours setting
                SettingsSlider(
                    label = "Auto-delete After",
                    value = autoDeleteHours,
                    onValueChange = { autoDeleteHours = it },
                    valueRange = 1f..168f,
                    steps = 166,
                    valueFormatter = { "${it.toInt()} hours" },
                )

                // Bubble size setting
                SettingsSlider(
                    label = "Bubble Size",
                    value = bubbleSize,
                    onValueChange = { bubbleSize = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    valueFormatter = { "Size ${it.toInt()}" },
                )

                // Bubble opacity setting
                SettingsSlider(
                    label = "Bubble Opacity",
                    value = (bubbleOpacity * 10).toInt(),
                    onValueChange = { bubbleOpacity = it / 10f },
                    valueRange = 1f..10f,
                    steps = 8,
                    valueFormatter = { "${(it * 10).toInt()}%" },
                )

                // Theme selection
                Column {
                    Text(
                        text = "Bubble Theme",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(BubbleThemes.ALL_THEMES) { theme ->
                            ThemeChip(
                                theme = theme,
                                isSelected = selectedTheme == theme.name,
                                onClick = { selectedTheme = theme.name },
                            )
                        }
                    }
                }

                // Encryption toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Enable Encryption",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(
                        checked = enableEncryption,
                        onCheckedChange = { enableEncryption = it },
                    )
                }

                // Bubble type selection
                Column {
                    Text(
                        text = "Bubble Type",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(BubbleType.values()) { bubbleType ->
                            BubbleTypeChip(
                                bubbleType = bubbleType,
                                isSelected = selectedBubbleType == bubbleType,
                                onClick = { selectedBubbleType = bubbleType },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newSettings =
                        ClipboardSettings(
                            maxHistorySize = maxHistorySize,
                            autoDeleteAfterHours = autoDeleteHours,
                            enableEncryption = enableEncryption,
                            bubbleSize = bubbleSize,
                            bubbleOpacity = bubbleOpacity,
                            selectedTheme = selectedTheme,
                            bubbleType = selectedBubbleType,
                        )
                    onSave(newSettings)
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Bubble type chip composable for bubble type selection.
 *
 * @param bubbleType The bubble type
 * @param isSelected Whether this type is selected
 * @param onClick Callback when type is clicked
 */
@Composable
fun BubbleTypeChip(
    bubbleType: BubbleType,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .clickable { onClick() }
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(16.dp),
                ),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Bubble type icon/preview
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape =
                                when (bubbleType) {
                                    BubbleType.CIRCLE -> CircleShape
                                    BubbleType.CUBE -> RoundedCornerShape(4.dp)
                                    BubbleType.HEXAGON -> RoundedCornerShape(8.dp)
                                    BubbleType.SQUARE -> RoundedCornerShape(8.dp)
                                },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text =
                        when (bubbleType) {
                            BubbleType.CIRCLE -> "●"
                            BubbleType.CUBE -> "■"
                            BubbleType.HEXAGON -> "⬡"
                            BubbleType.SQUARE -> "□"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = bubbleType.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
    }
}

/**
 * Theme chip composable for theme selection.
 *
 * @param theme The bubble theme
 * @param isSelected Whether this theme is selected
 * @param onClick Callback when theme is clicked
 */
@Composable
fun ThemeChip(
    theme: com.clipboardhistory.domain.model.BubbleTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = theme.colors

    Card(
        modifier =
            Modifier
                .clickable { onClick() }
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(16.dp),
                ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Theme preview with color samples
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(colors.empty)),
                )
                Box(
                    modifier =
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(colors.storing)),
                )
                Box(
                    modifier =
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(colors.replace)),
                )
                Box(
                    modifier =
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(colors.append)),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = theme.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
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
    valueFormatter: (Float) -> String,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = valueFormatter(value.toFloat()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange,
            steps = steps,
        )
    }
}
