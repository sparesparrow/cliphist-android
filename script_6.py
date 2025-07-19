# Create UI components
clipboard_item_card = '''package com.clipboardhistory.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clipboardhistory.domain.model.ClipboardItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * Clipboard item card composable.
 * 
 * This card displays a single clipboard item with options to copy or delete it.
 * 
 * @param item The clipboard item to display
 * @param onCopyClick Callback when copy button is clicked
 * @param onDeleteClick Callback when delete button is clicked
 * @param modifier Modifier for the card
 */
@Composable
fun ClipboardItemCard(
    item: ClipboardItem,
    onCopyClick: (ClipboardItem) -> Unit,
    onDeleteClick: (ClipboardItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with timestamp and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(item.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row {
                    IconButton(
                        onClick = { onCopyClick(item) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { onDeleteClick(item) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Content
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Footer with metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.contentType.name} • ${formatSize(item.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (item.isEncrypted) {
                    Text(
                        text = "🔒 Encrypted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Formats a timestamp to a human-readable string.
 * 
 * @param timestamp The timestamp in milliseconds
 * @return Formatted timestamp string
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> {
            val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}

/**
 * Formats a size in bytes to a human-readable string.
 * 
 * @param size The size in bytes
 * @return Formatted size string
 */
private fun formatSize(size: Int): String {
    return when {
        size < 1024 -> "${size}B"
        size < 1024 * 1024 -> "${size / 1024}KB"
        else -> "${size / (1024 * 1024)}MB"
    }
}'''

# Create settings dialog
settings_dialog = '''package com.clipboardhistory.presentation.ui.components

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
}'''

# Create theme files
theme_file = '''package com.clipboardhistory.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark color scheme for the application.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

/**
 * Light color scheme for the application.
 */
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * Main theme composable for the Clipboard History application.
 * 
 * @param darkTheme Whether to use dark theme
 * @param dynamicColor Whether to use dynamic colors (Android 12+)
 * @param content The content to theme
 */
@Composable
fun ClipboardHistoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}'''

# Create color definitions
color_file = '''package com.clipboardhistory.presentation.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)'''

# Create typography
typography_file = '''package com.clipboardhistory.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography definitions for the application.
 */
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)'''

# Write the UI component files
with open('app/src/main/java/com/clipboardhistory/presentation/ui/components/ClipboardItemCard.kt', 'w') as f:
    f.write(clipboard_item_card)

with open('app/src/main/java/com/clipboardhistory/presentation/ui/components/SettingsDialog.kt', 'w') as f:
    f.write(settings_dialog)

with open('app/src/main/java/com/clipboardhistory/presentation/ui/theme/Theme.kt', 'w') as f:
    f.write(theme_file)

with open('app/src/main/java/com/clipboardhistory/presentation/ui/theme/Color.kt', 'w') as f:
    f.write(color_file)

with open('app/src/main/java/com/clipboardhistory/presentation/ui/theme/Type.kt', 'w') as f:
    f.write(typography_file)

print("UI components and theme files created!")