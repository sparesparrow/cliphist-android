package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clipboardhistory.utils.TTSManager
import com.clipboardhistory.utils.VoiceRecognitionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Composable content for voice-enabled bubbles.
 * Combines TTS playback and voice recognition for creating/appending transcribed content.
 */
@Composable
fun VoiceBubbleContent(spec: VoiceBubble) {
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    // State management
    var isExpanded by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var speechProgress by remember { mutableStateOf(0f) }
    var transcriptionText by remember { mutableStateOf("") }
    var showTranscriptionSuccess by remember { mutableStateOf(false) }

    // Managers (would be injected in real implementation)
    val ttsManager = remember { TTSManager(androidx.compose.ui.platform.LocalContext.current) }
    val voiceManager = remember {
        VoiceRecognitionManager(
            androidx.compose.ui.platform.LocalContext.current,
            androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
        )
    }

    // Observe TTS state
    LaunchedEffect(ttsManager.isSpeaking) {
        ttsManager.isSpeaking.collect { speaking ->
            isSpeaking = speaking
        }
    }

    LaunchedEffect(ttsManager.speechProgress) {
        ttsManager.speechProgress.collect { progress ->
            speechProgress = progress
        }
    }

    // Observe voice recognition state
    LaunchedEffect(voiceManager.isListening) {
        voiceManager.isListening.collect { listening ->
            isListening = listening
        }
    }

    LaunchedEffect(voiceManager.transcriptionResult) {
        voiceManager.transcriptionResult.collect { result ->
            result?.let {
                transcriptionText = it
                showTranscriptionSuccess = true
                scope.launch {
                    delay(2000)
                    showTranscriptionSuccess = false
                }
            }
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
                fadeIn(animationSpec = tween(300)) togetherWith
                fadeOut(animationSpec = tween(300))
            },
            label = "voice_bubble_content"
        ) { expanded ->
            if (expanded) {
                ExpandedVoiceBubble(
                    spec = spec,
                    isSpeaking = isSpeaking,
                    isListening = isListening,
                    speechProgress = speechProgress,
                    transcriptionText = transcriptionText,
                    showTranscriptionSuccess = showTranscriptionSuccess,
                    onCollapse = { isExpanded = false },
                    onSpeak = {
                        val content = spec.getAllContent()
                        if (content.isNotEmpty()) {
                            ttsManager.speak(content)
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onStopSpeaking = {
                        ttsManager.stop()
                    },
                    onStartListening = {
                        voiceManager.startListening()
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onStopListening = {
                        voiceManager.stopListening()
                    },
                    onClearTranscription = {
                        transcriptionText = ""
                    }
                )
            } else {
                CollapsedVoiceBubble(
                    spec = spec,
                    isSpeaking = isSpeaking,
                    isListening = isListening,
                    onExpand = { isExpanded = true },
                    onLongPress = {
                        // Long press triggers TTS
                        val content = spec.getAllContent()
                        if (content.isNotEmpty() && spec.isTTSEnabled) {
                            ttsManager.speak(content)
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Collapsed view showing basic voice bubble state.
 */
@Composable
private fun CollapsedVoiceBubble(
    spec: VoiceBubble,
    isSpeaking: Boolean,
    isListening: Boolean,
    onExpand: () -> Unit,
    onLongPress: () -> Unit
) {
    val content = spec.getAllContent()
    val hasContent = content.isNotEmpty()
    val transcriptionCount = spec.transcriptionHistory.size

    // Animated background based on state
    val backgroundColor = when {
        isSpeaking -> Color(0xFF2196F3) // Blue when speaking
        isListening -> Color(0xFF4CAF50) // Green when listening
        showTranscriptionSuccess -> Color(0xFF4CAF50) // Green on success
        else -> MaterialTheme.colorScheme.primary
    }

    val animatedColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(300),
        label = "voice_bubble_color"
    )

    Surface(
        modifier = Modifier
            .size(if (hasContent) 120.dp else 80.dp)
            .clickable(onClick = onExpand, onLongClick = onLongPress),
        shape = CircleShape,
        color = animatedColor,
        shadowElevation = if (isSpeaking || isListening) 12.dp else 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isSpeaking) {
                SpeakingAnimation()
            } else if (isListening) {
                ListeningAnimation()
            } else {
                VoiceBubbleIcon(
                    hasContent = hasContent,
                    transcriptionCount = transcriptionCount,
                    isTTSEnabled = spec.isTTSEnabled,
                    isVoiceEnabled = spec.isVoiceRecognitionEnabled
                )
            }

            // Transcription count indicator
            if (transcriptionCount > 0 && !isSpeaking && !isListening) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = transcriptionCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Icon display for collapsed voice bubble.
 */
@Composable
private fun VoiceBubbleIcon(
    hasContent: Boolean,
    transcriptionCount: Int,
    isTTSEnabled: Boolean,
    isVoiceEnabled: Boolean
) {
    when {
        hasContent && isTTSEnabled -> {
            // Show speaker icon for TTS-enabled content
            Icon(
                Icons.Default.VolumeUp,
                contentDescription = "Voice bubble with content - long press to speak",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        isVoiceEnabled -> {
            // Show microphone icon for voice recognition
            Icon(
                Icons.Default.Mic,
                contentDescription = "Voice recognition bubble - tap to start listening",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        else -> {
            // Show generic voice icon
            Icon(
                Icons.Default.RecordVoiceOver,
                contentDescription = "Voice bubble",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Animation shown when TTS is speaking.
 */
@Composable
private fun SpeakingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "speaking_animation")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaking_scale"
    )

    Icon(
        Icons.Default.VolumeUp,
        contentDescription = "Speaking",
        tint = Color.White,
        modifier = Modifier
            .size(32.dp)
            .scale(scale)
    )
}

/**
 * Animation shown when voice recognition is listening.
 */
@Composable
private fun ListeningAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "listening_animation")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "listening_alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // Pulsing background
        Surface(
            modifier = Modifier
                .size(48.dp)
                .alpha(alpha),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.3f)
        ) {}

        Icon(
            Icons.Default.Mic,
            contentDescription = "Listening",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Expanded view showing full voice bubble controls and content.
 */
@Composable
private fun ExpandedVoiceBubble(
    spec: VoiceBubble,
    isSpeaking: Boolean,
    isListening: Boolean,
    speechProgress: Float,
    transcriptionText: String,
    showTranscriptionSuccess: Boolean,
    onCollapse: () -> Unit,
    onSpeak: () -> Unit,
    onStopSpeaking: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onClearTranscription: () -> Unit
) {
    val content = spec.getAllContent()
    val hasContent = content.isNotEmpty()
    val recentTranscriptions = spec.getRecentTranscriptions(3)

    Column(
        modifier = Modifier
            .width(280.dp)
            .heightIn(min = 300.dp, max = 500.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with title and controls
        VoiceBubbleHeader(spec, onCollapse)

        // TTS Controls (if enabled and has content)
        if (spec.isTTSEnabled && hasContent) {
            TTSControls(
                isSpeaking = isSpeaking,
                speechProgress = speechProgress,
                onSpeak = onSpeak,
                onStopSpeaking = onStopSpeaking
            )
        }

        // Voice Recognition Controls (if enabled)
        if (spec.isVoiceRecognitionEnabled) {
            VoiceRecognitionControls(
                isListening = isListening,
                transcriptionText = transcriptionText,
                showTranscriptionSuccess = showTranscriptionSuccess,
                onStartListening = onStartListening,
                onStopListening = onStopListening,
                onClearTranscription = onClearTranscription
            )
        }

        // Content Display
        if (hasContent) {
            VoiceContentDisplay(spec, content)
        }

        // Recent Transcriptions
        if (recentTranscriptions.isNotEmpty()) {
            RecentTranscriptionsList(recentTranscriptions)
        }

        // Statistics
        val stats = spec.getTranscriptionStats()
        if (stats.totalTranscriptions > 0) {
            TranscriptionStatsDisplay(stats)
        }
    }
}

/**
 * Header with bubble title and close button.
 */
@Composable
private fun VoiceBubbleHeader(spec: VoiceBubble, onCollapse: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.RecordVoiceOver,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Voice Bubble",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        IconButton(onClick = onCollapse, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Collapse voice bubble"
            )
        }
    }
}

/**
 * TTS controls section.
 */
@Composable
private fun TTSControls(
    isSpeaking: Boolean,
    speechProgress: Float,
    onSpeak: () -> Unit,
    onStopSpeaking: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Text-to-Speech",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSpeaking) {
                    OutlinedButton(
                        onClick = onStopSpeaking,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = onSpeak,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Speak")
                    }
                }

                // Progress indicator
                if (isSpeaking) {
                    CircularProgressIndicator(
                        progress = speechProgress,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

/**
 * Voice recognition controls section.
 */
@Composable
private fun VoiceRecognitionControls(
    isListening: Boolean,
    transcriptionText: String,
    showTranscriptionSuccess: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onClearTranscription: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (showTranscriptionSuccess) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Voice Recognition",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Transcription result
            if (transcriptionText.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = transcriptionText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = onClearTranscription,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear transcription",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isListening) {
                    OutlinedButton(
                        onClick = onStopListening,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = onStartListening,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Listen")
                    }
                }

                // Success indicator
                if (showTranscriptionSuccess) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Transcription successful",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Display the voice bubble's text content.
 */
@Composable
private fun VoiceContentDisplay(spec: VoiceBubble, content: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp, max = 120.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(12.dp)
        )
    }
}

/**
 * Display list of recent transcriptions.
 */
@Composable
private fun RecentTranscriptionsList(transcriptions: List<TranscriptionEntry>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Recent Transcriptions",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier.heightIn(max = 120.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(transcriptions.reversed()) { transcription ->
                TranscriptionItem(transcription)
            }
        }
    }
}

/**
 * Individual transcription item.
 */
@Composable
private fun TranscriptionItem(transcription: TranscriptionEntry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = transcription.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(transcription.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Confidence indicator
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when {
                        transcription.confidence >= 0.8f -> Color(0xFF4CAF50)
                        transcription.confidence >= 0.6f -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                ) {
                    Text(
                        text = "${(transcription.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Display transcription statistics.
 */
@Composable
private fun TranscriptionStatsDisplay(stats: TranscriptionStats) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                label = "Total",
                value = stats.totalTranscriptions.toString()
            )

            StatItem(
                label = "Avg Confidence",
                value = "${(stats.averageConfidence * 100).toInt()}%"
            )

            StatItem(
                label = "High Quality",
                value = stats.highConfidenceCount.toString()
            )
        }
    }
}

/**
 * Individual statistic item.
 */
@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper function
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}