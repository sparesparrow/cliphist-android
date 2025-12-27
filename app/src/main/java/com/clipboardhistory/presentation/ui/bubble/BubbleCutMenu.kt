package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.runtime.Composable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clipboardhistory.utils.TextSelectionManager

/**
 * Floating bubble that appears near text selection to offer "Bubble cut" functionality.
 * This serves as a visual indicator and action button for the bubble cut feature.
 */
@Composable
fun BubbleCutMenu(
    position: Offset,
    selectedText: String,
    isVisible: Boolean,
    onCutToBubble: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) +
                scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
        exit = fadeOut(animationSpec = tween(150)) +
              scaleOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x = with(density) { position.x.toDp() },
                    y = with(density) { position.y.toDp() }
                )
        ) {
            BubbleCutButton(
                selectedText = selectedText,
                onCutToBubble = onCutToBubble,
                onDismiss = onDismiss
            )
        }
    }
}

/**
 * The actual bubble cut button with animations and visual feedback.
 */
@Composable
private fun BubbleCutButton(
    selectedText: String,
    onCutToBubble: () -> Unit,
    onDismiss: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 8.dp,
        animationSpec = tween(100),
        label = "button_elevation"
    )

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            kotlinx.coroutines.delay(1000)
            showSuccess = false
            onDismiss()
        }
    }

    Surface(
        modifier = Modifier
            .size(if (showSuccess) 120.dp else 80.dp)
            .shadow(elevation, shape = CircleShape)
            .clip(CircleShape)
            .clickable(
                onClick = {
                    isPressed = true
                    onCutToBubble()
                    showSuccess = true
                }
            ),
        shape = CircleShape,
        color = when {
            showSuccess -> Color(0xFF4CAF50) // Green for success
            isPressed -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.primary
        },
        shadowElevation = elevation
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.scale(scale)
        ) {
            if (showSuccess) {
                SuccessAnimation()
            } else {
                BubbleCutContent(selectedText)
            }
        }
    }
}

/**
 * Content for the bubble cut button showing the icon and text preview.
 */
@Composable
private fun BubbleCutContent(selectedText: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ContentCut,
            contentDescription = "Cut to bubble",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Show preview of selected text
        val previewText = selectedText.take(10) + if (selectedText.length > 10) "..." else ""
        Text(
            text = previewText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1
        )
    }
}

/**
 * Success animation shown after successful cut operation.
 */
@Composable
private fun SuccessAnimation() {
    Box(contentAlignment = Alignment.Center) {
        // Animated checkmark or success indicator
        Icon(
            imageVector = Icons.Default.ContentCut,
            contentDescription = "Success",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )

        // Pulsing background effect
        Surface(
            modifier = Modifier.matchParentSize(),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.2f)
        ) {}

        // Ripple effect
        val infiniteTransition = rememberInfiniteTransition(label = "ripple")
        val rippleAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ripple_alpha"
        )

        Surface(
            modifier = Modifier.matchParentSize(),
            shape = CircleShape,
            color = Color.White.copy(alpha = rippleAlpha)
        ) {}
    }
}

/**
 * Helper class to manage bubble cut menu state and positioning.
 */
class BubbleCutMenuManager(
    private val textSelectionManager: TextSelectionManager,
    private val onCutToBubble: (String) -> Unit
) {
    var isVisible by mutableStateOf(false)
        private set

    var position by mutableStateOf(Offset.Zero)
        private set

    var selectedText by mutableStateOf("")
        private set

    /**
     * Shows the bubble cut menu at the specified position.
     */
    fun showBubbleCutMenu(text: String, menuPosition: Offset) {
        selectedText = text
        position = menuPosition
        isVisible = true
    }

    /**
     * Hides the bubble cut menu.
     */
    fun hideBubbleCutMenu() {
        isVisible = false
        selectedText = ""
        position = Offset.Zero
    }

    /**
     * Performs the bubble cut operation.
     */
    fun performBubbleCut() {
        if (selectedText.isNotEmpty()) {
            onCutToBubble(selectedText)
        }
        // Menu will auto-hide after success animation
    }

    /**
     * Checks if the bubble cut menu should be shown for current selection.
     */
    fun shouldShowBubbleCutMenu(): Boolean {
        return textSelectionManager.hasSelectedText() &&
               textSelectionManager.isEnabled() &&
               !isVisible
    }

    /**
     * Gets the current selected text for menu display.
     */
    fun getSelectedTextForMenu(): String {
        return textSelectionManager.getSelectedText()
    }
}

/**
 * Calculates optimal position for bubble cut menu based on text selection location.
 * This is a simplified implementation - in production, you'd use accessibility
 * coordinates or text selection bounds.
 */
fun calculateBubbleCutMenuPosition(
    selectionBounds: androidx.compose.ui.geometry.Rect,
    screenSize: androidx.compose.ui.unit.IntSize,
    density: androidx.compose.ui.unit.Density
): Offset {
    val padding = with(density) { 16.dp.toPx() }

    // Position above the selection, centered horizontally
    var x = selectionBounds.center.x
    var y = selectionBounds.top - padding - 80f // 80px above selection

    // Ensure it stays within screen bounds
    x = x.coerceIn(padding, screenSize.width - padding - 80f)
    y = y.coerceIn(padding, screenSize.height - padding - 80f)

    return Offset(x, y)
}