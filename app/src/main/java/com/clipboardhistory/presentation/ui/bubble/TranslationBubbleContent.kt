package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.runtime.Composable
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Composable content for translation bubbles.
 * Provides multi-language translation with clipboard content integration.
 */
@Composable
fun TranslationBubbleContent(
    sourceText: String = "",
    sourceLanguage: String = "auto",
    targetLanguage: String = "en",
    onTranslationComplete: (String) -> Unit = {}
) {
    var currentSourceText by remember { mutableStateOf(sourceText) }
    var currentSourceLang by remember { mutableStateOf(sourceLanguage) }
    var currentTargetLang by remember { mutableStateOf(targetLanguage) }
    var translatedText by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    // Auto-translate when text changes
    LaunchedEffect(currentSourceText, currentSourceLang, currentTargetLang) {
        if (currentSourceText.isNotEmpty() && currentSourceText.length > 3) {
            isTranslating = true
            // Simulate translation delay
            kotlinx.coroutines.delay(500)
            translatedText = performTranslation(currentSourceText, currentSourceLang, currentTargetLang)
            isTranslating = false
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "translation_content"
        ) { expanded ->
            if (expanded) {
                ExpandedTranslationView(
                    sourceText = currentSourceText,
                    translatedText = translatedText,
                    sourceLanguage = currentSourceLang,
                    targetLanguage = currentTargetLang,
                    isTranslating = isTranslating,
                    onSourceTextChange = { currentSourceText = it },
                    onSourceLanguageChange = { currentSourceLang = it },
                    onTargetLanguageChange = { currentTargetLang = it },
                    onCollapse = { isExpanded = false },
                    onCopyTranslation = {
                        translatedText?.let { onTranslationComplete(it) }
                    }
                )
            } else {
                CollapsedTranslationView(
                    sourceText = currentSourceText,
                    translatedText = translatedText,
                    isTranslating = isTranslating,
                    onExpand = { isExpanded = true }
                )
            }
        }
    }
}

/**
 * Collapsed translation view showing brief info.
 */
@Composable
private fun CollapsedTranslationView(
    sourceText: String,
    translatedText: String?,
    isTranslating: Boolean,
    onExpand: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(250.dp)
            .clickable(onClick = onExpand)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isTranslating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = "Translation",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isTranslating) "Translating..." else "Translation",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (translatedText != null && !isTranslating) {
                Text(
                    text = translatedText.take(30) + if (translatedText.length > 30) "..." else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (sourceText.isNotEmpty()) {
                Text(
                    text = sourceText.take(30) + if (sourceText.length > 30) "..." else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            Icons.Default.ExpandMore,
            contentDescription = "Expand translation",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Expanded translation view with full controls.
 */
@Composable
private fun ExpandedTranslationView(
    sourceText: String,
    translatedText: String?,
    sourceLanguage: String,
    targetLanguage: String,
    isTranslating: Boolean,
    onSourceTextChange: (String) -> Unit,
    onSourceLanguageChange: (String) -> Unit,
    onTargetLanguageChange: (String) -> Unit,
    onCollapse: () -> Unit,
    onCopyTranslation: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Translation",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = onCollapse, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.ExpandLess,
                    contentDescription = "Collapse translation"
                )
            }
        }

        // Language selector
        LanguageSelector(
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            onSourceLanguageChange = onSourceLanguageChange,
            onTargetLanguageChange = onTargetLanguageChange
        )

        // Source text input
        OutlinedTextField(
            value = sourceText,
            onValueChange = onSourceTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter text to translate...") },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(8.dp)
        )

        // Translation result
        if (isTranslating) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text(
                        text = "Translating...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (translatedText != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = translatedText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = onCopyTranslation,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy translation",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Translation quality indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Translation quality",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "High confidence",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }

        // Quick language swap
        OutlinedButton(
            onClick = {
                val temp = sourceLanguage
                onSourceLanguageChange(targetLanguage)
                onTargetLanguageChange(temp)
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = CircleShape
        ) {
            Icon(
                Icons.Default.SwapHoriz,
                contentDescription = "Swap languages"
            )
        }
    }
}

/**
 * Language selection component.
 */
@Composable
private fun LanguageSelector(
    sourceLanguage: String,
    targetLanguage: String,
    onSourceLanguageChange: (String) -> Unit,
    onTargetLanguageChange: (String) -> Unit
) {
    // Common languages for quick selection
    val commonLanguages = listOf(
        "auto" to "Auto-detect",
        "en" to "English",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "it" to "Italian",
        "pt" to "Portuguese",
        "ja" to "Japanese",
        "ko" to "Korean",
        "zh" to "Chinese"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Source language
        LanguageDropdown(
            selectedLanguage = sourceLanguage,
            languages = commonLanguages,
            onLanguageSelected = onSourceLanguageChange,
            modifier = Modifier.weight(1f)
        )

        // Arrow
        Icon(
            Icons.Default.ArrowForward,
            contentDescription = "to",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Target language
        LanguageDropdown(
            selectedLanguage = targetLanguage,
            languages = commonLanguages.filter { it.first != "auto" }, // No auto-detect for target
            onLanguageSelected = onTargetLanguageChange,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Dropdown for language selection.
 */
@Composable
private fun LanguageDropdown(
    selectedLanguage: String,
    languages: List<Pair<String, String>>,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = languages.find { it.first == selectedLanguage }?.second ?: selectedLanguage,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onLanguageSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Mock translation function.
 * In a real implementation, this would call a translation API.
 */
private fun performTranslation(text: String, fromLang: String, toLang: String): String {
    // This is a mock implementation
    // In a real app, you'd integrate with Google Translate, DeepL, or similar service

    if (text.isEmpty()) return ""

    // Simple mock translations for demonstration
    val mockTranslations = mapOf(
        "Hello world" to "Hola mundo",
        "How are you?" to "¿Cómo estás?",
        "Thank you" to "Gracias",
        "Goodbye" to "Adiós",
        "Good morning" to "Buenos días",
        "I love programming" to "Me encanta programar"
    )

    return mockTranslations[text] ?: "[$text translated from $fromLang to $toLang]"
}