package com.clipboardhistory.presentation.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.clipboardhistory.domain.model.BubbleState
import com.clipboardhistory.domain.model.BubbleThemes
import com.clipboardhistory.domain.model.SmartAction

/**
 * View that displays highlighted action areas for drag-and-drop smart actions.
 *
 * This view renders action rectangles along screen edges when bubbles are dragged,
 * providing visual feedback and hit-testing for smart actions.
 *
 * @param context The context
 * @param themeName The theme name for colors
 * @param attrs Optional attribute set
 * @param defStyleAttr Default style attribute
 */
class HighlightedAreaView
    @JvmOverloads
    constructor(
        context: Context,
        private val themeName: String,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : View(context, attrs, defStyleAttr) {
        /** Enum representing which screen edge triggered activation */
        enum class ActivationEdge { NONE, LEFT, RIGHT, TOP, BOTTOM }

        // Theme colors
        private val theme = BubbleThemes.ALL_THEMES.find { it.name == themeName } ?: BubbleThemes.DEFAULT

        // Paint objects
        private val backgroundPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

        private val actionPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

        private val textPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = ACTION_TEXT_SIZE
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }

        private val iconPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = ACTION_ICON_SIZE
                textAlign = Paint.Align.CENTER
            }

        private val glowPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

        private val highlightPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = Color.WHITE
            }

        // State
        private var currentEdge: ActivationEdge = ActivationEdge.NONE
        private var smartActions: List<SmartAction> = emptyList()
        private var actionRects: List<ActionRect> = emptyList()
        private var edgeGlowAlpha: Float = 0f
        private var showAnimator: ValueAnimator? = null
        private var animationProgress: Float = 0f
        private var hoveredActionIndex: Int = -1

        // Action rectangle data
        private data class ActionRect(
            val rect: RectF,
            val action: SmartAction,
            val color: Int,
            val icon: String,
        )

        companion object {
            private const val ACTION_WIDTH = 120f
            private const val ACTION_HEIGHT = 80f
            private const val ACTION_SPACING = 16f
            private const val ACTION_CORNER_RADIUS = 12f
            private const val ACTION_TEXT_SIZE = 14f
            private const val ACTION_ICON_SIZE = 32f
            private const val BACKGROUND_ALPHA = 0.85f
            private const val GLOW_WIDTH = 40f
            private const val ANIMATION_DURATION_MS = 200L
        }

        init {
            // Semi-transparent dark background
            backgroundPaint.color = Color.argb(220, 30, 30, 30)
        }

        /**
         * Shows the highlighted area with default actions.
         */
        fun show() {
            visibility = VISIBLE
            startShowAnimation()
            invalidate()
        }

        /**
         * Shows the highlighted area with smart actions based on content.
         *
         * @param actions List of smart actions to display
         * @param edge The activation edge
         */
        fun showWithSmartActions(
            actions: List<SmartAction>,
            edge: ActivationEdge,
        ) {
            smartActions = actions
            currentEdge = edge
            visibility = VISIBLE
            calculateActionRects()
            startShowAnimation()
            invalidate()
        }

        /**
         * Hides the highlighted area with animation.
         */
        fun hide() {
            showAnimator?.cancel()
            showAnimator =
                ValueAnimator.ofFloat(animationProgress, 0f).apply {
                    duration = ANIMATION_DURATION_MS
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { animator ->
                        animationProgress = animator.animatedValue as Float
                        invalidate()
                    }
                    start()
                }

            postDelayed({
                visibility = GONE
                smartActions = emptyList()
                actionRects = emptyList()
            }, ANIMATION_DURATION_MS)
        }

        /**
         * Updates the edge glow effect based on distance from edge.
         *
         * @param alpha The glow intensity (0-1)
         */
        fun updateEdgeGlow(alpha: Float) {
            edgeGlowAlpha = alpha.coerceIn(0f, 1f)
            invalidate()
        }

        /**
         * Gets the BubbleState action for a given position.
         *
         * @param x X coordinate in view space
         * @param y Y coordinate in view space
         * @return The BubbleState at the position, or null if no action
         */
        fun getActionForPosition(
            x: Float,
            y: Float,
        ): BubbleState? {
            val smartAction = getSmartActionForPosition(x, y)
            return smartAction?.action
        }

        /**
         * Gets the SmartAction for a given position.
         *
         * @param x X coordinate in view space
         * @param y Y coordinate in view space
         * @return The SmartAction at the position, or null if no action
         */
        fun getSmartActionForPosition(
            x: Float,
            y: Float,
        ): SmartAction? {
            for (actionRect in actionRects) {
                if (actionRect.rect.contains(x, y)) {
                    return actionRect.action
                }
            }
            return null
        }

        /**
         * Updates hover state for visual feedback.
         *
         * @param x X coordinate
         * @param y Y coordinate
         */
        fun updateHoverPosition(
            x: Float,
            y: Float,
        ) {
            val newHoveredIndex =
                actionRects.indexOfFirst { it.rect.contains(x, y) }
            if (newHoveredIndex != hoveredActionIndex) {
                hoveredActionIndex = newHoveredIndex
                invalidate()
            }
        }

        /**
         * Starts the show animation.
         */
        private fun startShowAnimation() {
            showAnimator?.cancel()
            showAnimator =
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = ANIMATION_DURATION_MS
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { animator ->
                        animationProgress = animator.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
        }

        /**
         * Calculates action rectangle positions based on edge and available space.
         */
        private fun calculateActionRects() {
            actionRects = emptyList()
            if (smartActions.isEmpty()) return

            val rects = mutableListOf<ActionRect>()
            val totalWidth = smartActions.size * ACTION_WIDTH + (smartActions.size - 1) * ACTION_SPACING
            val totalHeight = smartActions.size * ACTION_HEIGHT + (smartActions.size - 1) * ACTION_SPACING

            when (currentEdge) {
                ActivationEdge.LEFT, ActivationEdge.RIGHT -> {
                    // Vertical layout
                    val startY = (height - totalHeight) / 2
                    val x =
                        if (currentEdge == ActivationEdge.LEFT) {
                            ACTION_SPACING
                        } else {
                            width - ACTION_WIDTH - ACTION_SPACING
                        }

                    smartActions.forEachIndexed { index, action ->
                        val y = startY + index * (ACTION_HEIGHT + ACTION_SPACING)
                        val rect = RectF(x, y, x + ACTION_WIDTH, y + ACTION_HEIGHT)
                        rects.add(
                            ActionRect(
                                rect = rect,
                                action = action,
                                color = getColorForAction(action),
                                icon = getIconForAction(action),
                            ),
                        )
                    }
                }
                ActivationEdge.TOP, ActivationEdge.BOTTOM -> {
                    // Horizontal layout
                    val startX = (width - totalWidth) / 2
                    val y =
                        if (currentEdge == ActivationEdge.TOP) {
                            ACTION_SPACING
                        } else {
                            height - ACTION_HEIGHT - ACTION_SPACING
                        }

                    smartActions.forEachIndexed { index, action ->
                        val x = startX + index * (ACTION_WIDTH + ACTION_SPACING)
                        val rect = RectF(x, y, x + ACTION_WIDTH, y + ACTION_HEIGHT)
                        rects.add(
                            ActionRect(
                                rect = rect,
                                action = action,
                                color = getColorForAction(action),
                                icon = getIconForAction(action),
                            ),
                        )
                    }
                }
                ActivationEdge.NONE -> {
                    // Default centered layout
                    val centerX = width / 2f
                    val centerY = height / 2f
                    val startX = centerX - totalWidth / 2

                    smartActions.forEachIndexed { index, action ->
                        val x = startX + index * (ACTION_WIDTH + ACTION_SPACING)
                        val rect = RectF(x, centerY - ACTION_HEIGHT / 2, x + ACTION_WIDTH, centerY + ACTION_HEIGHT / 2)
                        rects.add(
                            ActionRect(
                                rect = rect,
                                action = action,
                                color = getColorForAction(action),
                                icon = getIconForAction(action),
                            ),
                        )
                    }
                }
            }

            actionRects = rects
        }

        /**
         * Gets the color for an action based on its type.
         */
        private fun getColorForAction(action: SmartAction): Int {
            return when (action.label) {
                "Open Link" -> theme.colors.storing
                "Call Number" -> theme.colors.append
                "Send Email" -> theme.colors.prepend
                "Open Maps" -> theme.colors.replace
                "Search Text" -> theme.colors.storing
                else -> theme.colors.storing
            }
        }

        /**
         * Gets the icon character for an action.
         */
        private fun getIconForAction(action: SmartAction): String {
            return when (action.label) {
                "Open Link" -> "🔗"
                "Call Number" -> "📞"
                "Send Email" -> "✉"
                "Open Maps" -> "📍"
                "Search Text" -> "🔍"
                else -> "•"
            }
        }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (smartActions.isNotEmpty()) {
                calculateActionRects()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            if (animationProgress <= 0f) return

            // Apply animation alpha
            val alpha = (BACKGROUND_ALPHA * animationProgress * 255).toInt()

            // Draw semi-transparent background
            backgroundPaint.alpha = alpha
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

            // Draw edge glow effect
            if (edgeGlowAlpha > 0f) {
                drawEdgeGlow(canvas)
            }

            // Draw action rectangles
            for ((index, actionRect) in actionRects.withIndex()) {
                drawActionRect(canvas, actionRect, index == hoveredActionIndex)
            }
        }

        /**
         * Draws the edge glow effect.
         */
        private fun drawEdgeGlow(canvas: Canvas) {
            val glowAlpha = (edgeGlowAlpha * animationProgress * 255).toInt()

            when (currentEdge) {
                ActivationEdge.LEFT -> {
                    glowPaint.shader =
                        LinearGradient(
                            0f,
                            0f,
                            GLOW_WIDTH,
                            0f,
                            Color.argb(glowAlpha, 255, 255, 255),
                            Color.TRANSPARENT,
                            Shader.TileMode.CLAMP,
                        )
                    canvas.drawRect(0f, 0f, GLOW_WIDTH, height.toFloat(), glowPaint)
                }
                ActivationEdge.RIGHT -> {
                    glowPaint.shader =
                        LinearGradient(
                            width - GLOW_WIDTH,
                            0f,
                            width.toFloat(),
                            0f,
                            Color.TRANSPARENT,
                            Color.argb(glowAlpha, 255, 255, 255),
                            Shader.TileMode.CLAMP,
                        )
                    canvas.drawRect(width - GLOW_WIDTH, 0f, width.toFloat(), height.toFloat(), glowPaint)
                }
                ActivationEdge.TOP -> {
                    glowPaint.shader =
                        LinearGradient(
                            0f,
                            0f,
                            0f,
                            GLOW_WIDTH,
                            Color.argb(glowAlpha, 255, 255, 255),
                            Color.TRANSPARENT,
                            Shader.TileMode.CLAMP,
                        )
                    canvas.drawRect(0f, 0f, width.toFloat(), GLOW_WIDTH, glowPaint)
                }
                ActivationEdge.BOTTOM -> {
                    glowPaint.shader =
                        LinearGradient(
                            0f,
                            height - GLOW_WIDTH,
                            0f,
                            height.toFloat(),
                            Color.TRANSPARENT,
                            Color.argb(glowAlpha, 255, 255, 255),
                            Shader.TileMode.CLAMP,
                        )
                    canvas.drawRect(0f, height - GLOW_WIDTH, width.toFloat(), height.toFloat(), glowPaint)
                }
                ActivationEdge.NONE -> {}
            }
        }

        /**
         * Draws a single action rectangle with icon and label.
         */
        private fun drawActionRect(
            canvas: Canvas,
            actionRect: ActionRect,
            isHovered: Boolean,
        ) {
            val rect = actionRect.rect

            // Scale animation
            val scale = if (isHovered) 1.1f else 1f
            val scaledRect =
                if (isHovered) {
                    val centerX = rect.centerX()
                    val centerY = rect.centerY()
                    val halfWidth = rect.width() / 2 * scale
                    val halfHeight = rect.height() / 2 * scale
                    RectF(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight)
                } else {
                    rect
                }

            // Draw action background
            actionPaint.color = actionRect.color
            actionPaint.alpha = (animationProgress * 255).toInt()
            canvas.drawRoundRect(scaledRect, ACTION_CORNER_RADIUS, ACTION_CORNER_RADIUS, actionPaint)

            // Draw highlight border if hovered
            if (isHovered) {
                highlightPaint.alpha = (animationProgress * 255).toInt()
                canvas.drawRoundRect(scaledRect, ACTION_CORNER_RADIUS, ACTION_CORNER_RADIUS, highlightPaint)
            }

            // Draw icon
            iconPaint.alpha = (animationProgress * 255).toInt()
            canvas.drawText(
                actionRect.icon,
                scaledRect.centerX(),
                scaledRect.centerY() - 8f,
                iconPaint,
            )

            // Draw label
            textPaint.alpha = (animationProgress * 255).toInt()
            canvas.drawText(
                actionRect.action.label,
                scaledRect.centerX(),
                scaledRect.centerY() + ACTION_TEXT_SIZE + 8f,
                textPaint,
            )
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            showAnimator?.cancel()
            showAnimator = null
        }
    }
