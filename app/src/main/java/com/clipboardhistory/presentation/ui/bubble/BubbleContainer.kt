package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Container composable that renders individual bubbles with appropriate animations
 * and interaction handling based on their type.
 */
@Composable
fun BubbleContainer(
    bubble: BubbleSpec,
    onInteraction: () -> Unit = {},
    onPositionChange: (Offset) -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isVisible by remember(bubble.isVisible) { mutableStateOf(bubble.isVisible) }

    AnimatedVisibility(
        visible = isVisible,
        enter = bubble.type.getEnterTransition(),
        exit = bubble.type.getExitTransition(),
        modifier = modifier
    ) {
        BubbleWithGestures(
            bubble = bubble,
            onInteraction = onInteraction,
            onPositionChange = onPositionChange,
            onDismiss = onDismiss
        )
    }
}

/**
 * Bubble with gesture handling for dragging and interaction.
 */
@Composable
private fun BubbleWithGestures(
    bubble: BubbleSpec,
    onInteraction: () -> Unit,
    onPositionChange: (Offset) -> Unit,
    onDismiss: () -> Unit
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedOffset by animateOffsetAsState(
        targetValue = bubble.position + dragOffset,
        animationSpec = tween(
            durationMillis = if (isDragging) 0 else 300,
            easing = EaseOutCubic
        ),
        label = "bubble_position"
    )

    val animatedSize by animateDpAsState(
        targetValue = bubble.size,
        animationSpec = tween(durationMillis = 200),
        label = "bubble_size"
    )

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    animatedOffset.x.roundToInt(),
                    animatedOffset.y.roundToInt()
                )
            }
            .size(animatedSize)
            .then(
                if (bubble.type.supportsDragging) {
                    Modifier.pointerInput(bubble.id) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                onPositionChange(bubble.position + dragOffset)
                                dragOffset = Offset.Zero
                            },
                            onDragCancel = {
                                isDragging = false
                                dragOffset = Offset.Zero
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        // Render bubble content based on type
        bubble.content(bubble)

        // Add interaction overlay for tap handling
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(bubble.id) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed }) {
                                onInteraction()
                                break
                            }
                        }
                    }
                }
        )
    }
}

/**
 * Extension functions to get appropriate transitions for each bubble type.
 */
private fun BubbleType.getEnterTransition(): EnterTransition {
    return when (this) {
        BubbleType.TEXT_PASTE -> {
            slideInVertically(
                animationSpec = tween(300, easing = EaseOutCubic)
            ) { it } + fadeIn(animationSpec = tween(300))
        }
        BubbleType.TOOLBELT -> {
            scaleIn(
                animationSpec = tween(400, easing = EaseOutBack),
                initialScale = 0.8f
            ) + fadeIn(animationSpec = tween(400))
        }
        BubbleType.PINNED_ITEM -> {
            slideInHorizontally(
                animationSpec = tween(250, easing = EaseOutCubic)
            ) { -it } + fadeIn(animationSpec = tween(250))
        }
        BubbleType.SYSTEM_NOTIFICATION -> {
            slideInVertically(
                animationSpec = tween(200, easing = EaseOutCubic)
            ) { -it } + fadeIn(animationSpec = tween(200))
        }
        BubbleType.QUICK_ACTION -> {
            scaleIn(
                animationSpec = tween(150, easing = EaseOutCubic),
                initialScale = 0.5f
            ) + fadeIn(animationSpec = tween(150))
        }
    }
}

private fun BubbleType.getExitTransition(): ExitTransition {
    return when (this) {
        BubbleType.TEXT_PASTE -> {
            slideOutVertically(
                animationSpec = tween(200, easing = EaseInCubic)
            ) { it } + fadeOut(animationSpec = tween(200))
        }
        BubbleType.TOOLBELT -> {
            scaleOut(
                animationSpec = tween(300, easing = EaseInBack),
                targetScale = 0.8f
            ) + fadeOut(animationSpec = tween(300))
        }
        BubbleType.PINNED_ITEM -> {
            slideOutHorizontally(
                animationSpec = tween(200, easing = EaseInCubic)
            ) { -it } + fadeOut(animationSpec = tween(200))
        }
        BubbleType.SYSTEM_NOTIFICATION -> {
            slideOutVertically(
                animationSpec = tween(150, easing = EaseInCubic)
            ) { -it } + fadeOut(animationSpec = tween(150))
        }
        BubbleType.QUICK_ACTION -> {
            scaleOut(
                animationSpec = tween(100, easing = EaseInCubic),
                targetScale = 0.5f
            ) + fadeOut(animationSpec = tween(100))
        }
    }
}