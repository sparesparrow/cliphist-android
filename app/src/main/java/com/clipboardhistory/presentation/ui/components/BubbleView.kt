package com.clipboardhistory.presentation.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.clipboardhistory.domain.model.BubbleState
import com.clipboardhistory.domain.model.BubbleTheme
import com.clipboardhistory.domain.model.BubbleThemes
import com.clipboardhistory.domain.model.BubbleType
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom bubble view that supports themes, states, and different shapes.
 *
 * @param context The context
 * @param theme The bubble theme
 * @param state The bubble state
 * @param bubbleType The bubble shape type
 * @param content The content text
 * @param opacity The bubble opacity
 */
class BubbleView(
    context: Context,
    private var theme: BubbleTheme,
    private var state: BubbleState,
    private var bubbleType: BubbleType = BubbleType.CIRCLE,
    private var content: String? = null,
    private var opacity: Float = 1.0f,
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()

    private var bubbleColor: Int = 0
    private var textColor: Int = Color.WHITE
    private var flashAnimator: ValueAnimator? = null
    private var isFlashing: Boolean = false
    private var flashAlpha: Float = 1.0f

    init {
        updateColors()
        setupTextPaint()
    }

    /**
     * Updates the bubble colors based on current theme and state.
     */
    private fun updateColors() {
        bubbleColor = when (state) {
            BubbleState.EMPTY -> theme.colors.empty
            BubbleState.STORING -> theme.colors.storing
            BubbleState.REPLACE -> theme.colors.replace
            BubbleState.APPEND -> theme.colors.append
            BubbleState.PREPEND -> theme.colors.prepend
        }

        // Calculate text color based on background brightness
        val brightness = calculateBrightness(bubbleColor)
        textColor = if (brightness > 0.5f) Color.BLACK else Color.WHITE
    }

    /**
     * Sets up the text paint for line count display.
     */
    private fun setupTextPaint() {
        textPaint.apply {
            color = textColor
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    /**
     * Calculates the brightness of a color.
     *
     * @param color The color to calculate brightness for
     * @return Brightness value between 0 and 1
     */
    private fun calculateBrightness(color: Int): Float {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return (red * 0.299f + green * 0.587f + blue * 0.114f) / 255f
    }

    /**
     * Updates the bubble state and recalculates colors.
     *
     * @param newState The new bubble state
     */
    fun updateState(newState: BubbleState) {
        if (state != newState) {
            state = newState
            updateColors()
            setupTextPaint()
            invalidate()
        }
    }

    /**
     * Updates the bubble theme.
     *
     * @param newTheme The new bubble theme
     */
    fun updateTheme(newTheme: BubbleTheme) {
        if (theme != newTheme) {
            theme = newTheme
            updateColors()
            setupTextPaint()
            invalidate()
        }
    }

    /**
     * Updates the bubble type.
     *
     * @param newType The new bubble type
     */
    fun updateBubbleType(newType: BubbleType) {
        if (bubbleType != newType) {
            bubbleType = newType
            invalidate()
        }
    }

    /**
     * Flashes the bubble content for a short duration.
     * Only works for cube bubbles.
     */
    fun flashContent() {
        if (bubbleType != BubbleType.CUBE) return

        // Stop any existing flash animation
        flashAnimator?.cancel()

        isFlashing = true
        flashAnimator = ValueAnimator.ofFloat(1.0f, 0.3f, 1.0f).apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                flashAlpha = animator.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Reset flash state after animation
        postDelayed({
            isFlashing = false
            flashAlpha = 1.0f
            invalidate()
        }, 1000)
    }

    /**
     * Flashes the bubble content with custom text for a short duration.
     * Only works for cube bubbles.
     *
     * @param flashText The text to display during flash
     */
    fun flashContent(flashText: String) {
        if (bubbleType != BubbleType.CUBE) return

        // Store the flash text temporarily
        val originalContent = content
        content = flashText

        // Stop any existing flash animation
        flashAnimator?.cancel()

        isFlashing = true
        flashAnimator = ValueAnimator.ofFloat(1.0f, 0.3f, 1.0f).apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                flashAlpha = animator.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Reset flash state and restore original content after animation
        postDelayed({
            isFlashing = false
            flashAlpha = 1.0f
            content = originalContent
            invalidate()
        }, 1000)
    }

    /**
     * Draws a circle shape.
     */
    private fun drawCircle(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        rect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
        )
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    /**
     * Draws a cube shape with 3D effect.
     */
    private fun drawCube(canvas: Canvas, centerX: Float, centerY: Float, size: Float) {
        val halfSize = size / 2

        // Draw main face
        rect.set(
            centerX - halfSize,
            centerY - halfSize,
            centerX + halfSize,
            centerY + halfSize,
        )
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        // Draw top face (lighter)
        val topPaint = Paint(paint)
        topPaint.color = lightenColor(bubbleColor, 0.2f)
        rect.set(
            centerX - halfSize + 4,
            centerY - halfSize - 4,
            centerX + halfSize + 4,
            centerY + halfSize - 4,
        )
        canvas.drawRoundRect(rect, 8f, 8f, topPaint)

        // Draw right face (darker)
        val rightPaint = Paint(paint)
        rightPaint.color = darkenColor(bubbleColor, 0.2f)
        rect.set(
            centerX + halfSize,
            centerY - halfSize + 4,
            centerX + halfSize + 4,
            centerY + halfSize + 4,
        )
        canvas.drawRoundRect(rect, 8f, 8f, rightPaint)
    }

    /**
     * Draws a hexagon shape.
     */
    private fun drawHexagon(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        path.reset()
        for (i in 0..5) {
            val angle = (i * 60 - 30) * Math.PI / 180
            val x = centerX + (radius * cos(angle)).toFloat()
            val y = centerY + (radius * sin(angle)).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    /**
     * Draws a square shape with rounded corners.
     */
    private fun drawSquare(canvas: Canvas, centerX: Float, centerY: Float, size: Float) {
        val halfSize = size / 2
        val cornerRadius = size * 0.15f

        rect.set(
            centerX - halfSize,
            centerY - halfSize,
            centerX + halfSize,
            centerY + halfSize,
        )
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
    }

    /**
     * Lightens a color by a given factor.
     */
    private fun lightenColor(color: Int, factor: Float): Int {
        val red = (Color.red(color) + (255 - Color.red(color)) * factor).toInt()
        val green = (Color.green(color) + (255 - Color.green(color)) * factor).toInt()
        val blue = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt()
        return Color.rgb(red, green, blue)
    }

    /**
     * Darkens a color by a given factor.
     */
    private fun darkenColor(color: Int, factor: Float): Int {
        val red = (Color.red(color) * (1 - factor)).toInt()
        val green = (Color.green(color) * (1 - factor)).toInt()
        val blue = (Color.blue(color) * (1 - factor)).toInt()
        return Color.rgb(red, green, blue)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val size = minOf(width, height) - 8f
        val radius = size / 2f

        // Apply flash effect if active
        val currentAlpha = if (isFlashing) flashAlpha else 1.0f

        // Draw bubble background
        paint.apply {
            color = bubbleColor
            alpha = (opacity * currentAlpha * 255).toInt()
            style = Paint.Style.FILL
        }

        // Draw shape based on bubble type
        when (bubbleType) {
            BubbleType.CIRCLE -> drawCircle(canvas, centerX, centerY, radius)
            BubbleType.CUBE -> drawCube(canvas, centerX, centerY, size)
            BubbleType.HEXAGON -> drawHexagon(canvas, centerX, centerY, radius)
            BubbleType.SQUARE -> drawSquare(canvas, centerX, centerY, size)
        }

        // Draw content preview for cube bubbles when flashing
        if (bubbleType == BubbleType.CUBE && isFlashing && !content.isNullOrEmpty()) {
            drawContentPreview(canvas, centerX, centerY, size)
        }
    }

    /**
     * Draws a content preview for cube bubbles during flash.
     */
    private fun drawContentPreview(canvas: Canvas, centerX: Float, centerY: Float, size: Float) {
        val previewPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        previewPaint.color = Color.WHITE
        previewPaint.textSize = size * 0.15f
        previewPaint.textAlign = Paint.Align.CENTER
        previewPaint.alpha = (flashAlpha * 255).toInt()

        val previewText = content?.take(20) ?: ""
        if (previewText.isNotEmpty()) {
            canvas.drawText(previewText, centerX, centerY + size * 0.1f, previewPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(size, size)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        flashAnimator?.cancel()
    }
}

/**
 * Factory object for creating bubble views with different configurations.
 */
object BubbleViewFactory {

    /**
     * Creates a bubble view with the specified configuration.
     *
     * @param context The context
     * @param themeName The theme name
     * @param state The bubble state
     * @param bubbleType The bubble type/shape
     * @param content The content text
     * @param opacity The bubble opacity
     * @return The created bubble view
     */
    fun createBubbleView(
        context: Context,
        themeName: String,
        state: BubbleState,
        bubbleType: BubbleType = BubbleType.CIRCLE,
        content: String? = null,
        opacity: Float = 1.0f,
    ): BubbleView {
        val theme = BubbleThemes.ALL_THEMES.find { it.name == themeName } ?: BubbleThemes.DEFAULT
        return BubbleView(context, theme, state, bubbleType, content, opacity)
    }

    /**
     * Gets the color for a specific state and theme.
     *
     * @param themeName The theme name
     * @param state The bubble state
     * @return The color for the state
     */
    fun getBubbleColor(themeName: String, state: BubbleState): Int {
        val theme = BubbleThemes.ALL_THEMES.find { it.name == themeName } ?: BubbleThemes.DEFAULT
        return when (state) {
            BubbleState.EMPTY -> theme.colors.empty
            BubbleState.STORING -> theme.colors.storing
            BubbleState.REPLACE -> theme.colors.replace
            BubbleState.APPEND -> theme.colors.append
            BubbleState.PREPEND -> theme.colors.prepend
        }
    }
}
