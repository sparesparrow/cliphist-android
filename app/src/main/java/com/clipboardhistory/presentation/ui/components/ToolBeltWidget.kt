package com.clipboardhistory.presentation.ui.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import com.clipboardhistory.R
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.OperationMode
import com.clipboardhistory.domain.model.ToolBeltBubble
import com.clipboardhistory.domain.model.ToolBeltBubbleType
import com.clipboardhistory.domain.model.BubbleType

/**
 * ToolBelt Widget provides a horizontal belt of tool bubbles for quick access to common operations.
 * Features include:
 * - Transparency/opacity slider
 * - Show/hide private bubbles toggle
 * - Show/hide clipboard history bubbles toggle
 * - Change bubble type dynamically
 * - Change bubble content
 * - Switch between operation modes (overwrite, append, prepend)
 *
 * @param context The context
 * @param attrs Optional attributes
 * @param defStyleAttr Default style attribute
 */
class ToolBeltWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : CardView(context, attrs, defStyleAttr) {

    private lateinit var bubbleContainer: LinearLayout
    private lateinit var toolbeltScroller: HorizontalScrollView
    private val toolBubbles = mutableMapOf<String, ToolBeltBubbleView>()
    private var onBubbleClickListener: ((ToolBeltBubble) -> Unit)? = null
    private var onOperationModeChangeListener: ((OperationMode) -> Unit)? = null

    private var currentOpacityLevel: Float = 1.0f
    private var showPrivateBubbles: Boolean = true
    private var showHistoryBubbles: Boolean = true

    private val toolBubblesList = mutableListOf<ToolBeltBubble>()

    init {
        setupUI()
    }

    /**
     * Sets up the UI components of the ToolBelt widget.
     */
    private fun setupUI() {
        setCardBackgroundColor(Color.parseColor("#F0F0F0"))
        setCardElevation(8f)
        radius = 16f

        val container = LinearLayout(context).apply {
            layoutParams =
                FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT,
                )
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
        }

        // Control panel
        val controlPanel = LinearLayout(context).apply {
            layoutParams =
                LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT,
                )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 12)
        }

        // Opacity slider
        val opacityLabel =
            TextView(context).apply {
                text = "Opacity"
                textSize = 12f
                setTextColor(Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.2f)
            }
        controlPanel.addView(opacityLabel)

        val opacitySlider =
            SeekBar(context).apply {
                max = 100
                progress = 100
                layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.4f)
                setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            currentOpacityLevel = progress / 100f
                            updateBubbleOpacity()
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    },
                )
            }
        controlPanel.addView(opacitySlider)

        // Toggle buttons
        val privateToggle =
            createToggleButton("Private") { isChecked ->
                showPrivateBubbles = isChecked
                updateBubbleVisibility()
            }
        controlPanel.addView(privateToggle)

        val historyToggle =
            createToggleButton("History") { isChecked ->
                showHistoryBubbles = isChecked
                updateBubbleVisibility()
            }
        controlPanel.addView(historyToggle)

        container.addView(controlPanel)

        // Bubble scroller
        toolbeltScroller =
            HorizontalScrollView(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        120,
                    )
                isHorizontalScrollBarEnabled = false
            }

        bubbleContainer =
            LinearLayout(context).apply {
                layoutParams =
                    FrameLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.MATCH_PARENT,
                    )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(8, 8, 8, 8)
            }

        toolbeltScroller.addView(bubbleContainer)
        container.addView(toolbeltScroller)

        addView(container)
    }

    /**
     * Creates a toggle button with given label.
     */
    private fun createToggleButton(
        label: String,
        onToggle: (Boolean) -> Unit,
    ): ToggleButton {
        return ToggleButton(context).apply {
            textOn = label
            textOff = label
            isChecked = true
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.2f)
            setOnCheckedChangeListener { _, isChecked -> onToggle(isChecked) }
        }
    }

    /**
     * Adds a tool bubble to the ToolBelt.
     *
     * @param bubble The ToolBeltBubble to add
     */
    fun addBubble(bubble: ToolBeltBubble) {
        toolBubblesList.add(bubble)
        val bubbleView = ToolBeltBubbleView(context, bubble) { clickedBubble ->
            handleBubbleClick(clickedBubble)
        }
        toolBubbles[bubble.id] = bubbleView
        bubbleContainer.addView(bubbleView.getView())
    }

    /**
     * Removes a tool bubble from the ToolBelt.
     *
     * @param bubbleId The ID of the bubble to remove
     */
    fun removeBubble(bubbleId: String) {
        toolBubbles[bubbleId]?.let { bubbleView ->
            bubbleContainer.removeView(bubbleView.getView())
            toolBubbles.remove(bubbleId)
            toolBubblesList.removeAll { it.id == bubbleId }
        }
    }

    /**
     * Updates a tool bubble in the ToolBelt.
     *
     * @param bubble The updated ToolBeltBubble
     */
    fun updateBubble(bubble: ToolBeltBubble) {
        toolBubbles[bubble.id]?.let { bubbleView ->
            val index = toolBubblesList.indexOfFirst { it.id == bubble.id }
            if (index >= 0) {
                toolBubblesList[index] = bubble
                val newBubbleView = ToolBeltBubbleView(context, bubble) { clickedBubble ->
                    handleBubbleClick(clickedBubble)
                }
                bubbleContainer.removeViewAt(index)
                bubbleContainer.addView(newBubbleView.getView(), index)
                toolBubbles[bubble.id] = newBubbleView
            }
        }
    }

    /**
     * Gets all tool bubbles.
     *
     * @return List of ToolBeltBubbles
     */
    fun getBubbles(): List<ToolBeltBubble> = toolBubblesList.toList()

    /**
     * Sets the click listener for bubbles.
     */
    fun setOnBubbleClickListener(listener: (ToolBeltBubble) -> Unit) {
        this.onBubbleClickListener = listener
    }

    /**
     * Sets the operation mode change listener.
     */
    fun setOnOperationModeChangeListener(listener: (OperationMode) -> Unit) {
        this.onOperationModeChangeListener = listener
    }

    /**
     * Gets the current opacity level.
     */
    fun getOpacityLevel(): Float = currentOpacityLevel

    /**
     * Sets the opacity level.
     */
    fun setOpacityLevel(level: Float) {
        currentOpacityLevel = level.coerceIn(0f, 1f)
        updateBubbleOpacity()
    }

    /**
     * Clears all bubbles from the ToolBelt.
     */
    fun clearBubbles() {
        bubbleContainer.removeAllViews()
        toolBubbles.clear()
        toolBubblesList.clear()
    }

    /**
     * Initializes the ToolBelt with default tool bubbles.
     */
    fun initializeDefaultTools() {
        clearBubbles()

        // Opacity slider bubble
        addBubble(
            ToolBeltBubble(
                type = ToolBeltBubbleType.OPACITY_SLIDER,
                content = "Opacity",
                bubbleType = BubbleType.CIRCLE,
                position = 0,
            ),
        )

        // Private visibility toggle
        addBubble(
            ToolBeltBubble(
                type = ToolBeltBubbleType.PRIVATE_VISIBILITY_TOGGLE,
                content = "Private",
                bubbleType = BubbleType.CIRCLE,
                position = 1,
            ),
        )

        // History visibility toggle
        addBubble(
            ToolBeltBubble(
                type = ToolBeltBubbleType.HISTORY_VISIBILITY_TOGGLE,
                content = "History",
                bubbleType = BubbleType.CIRCLE,
                position = 2,
            ),
        )

        // Bubble type changer
        addBubble(
            ToolBeltBubble(
                type = ToolBeltBubbleType.BUBBLE_TYPE_CHANGER,
                content = "Type",
                bubbleType = BubbleType.SQUARE,
                position = 3,
            ),
        )

        // Operation mode switcher
        addBubble(
            ToolBeltBubble(
                type = ToolBeltBubbleType.OPERATION_MODE_SWITCHER,
                content = "Mode",
                bubbleType = BubbleType.HEXAGON,
                position = 4,
            ),
        )
    }

    /**
     * Handles bubble click events.
     */
    private fun handleBubbleClick(bubble: ToolBeltBubble) {
        when (bubble.type) {
            ToolBeltBubbleType.OPERATION_MODE_SWITCHER -> {
                val nextMode =
                    when (bubble.operationMode) {
                        OperationMode.OVERWRITE -> OperationMode.APPEND
                        OperationMode.APPEND -> OperationMode.PREPEND
                        OperationMode.PREPEND -> OperationMode.OVERWRITE
                    }
                onOperationModeChangeListener?.invoke(nextMode)
                val updated = bubble.copy(operationMode = nextMode)
                updateBubble(updated)
            }

            else -> onBubbleClickListener?.invoke(bubble)
        }
    }

    /**
     * Updates the opacity of all visible bubbles.
     */
    private fun updateBubbleOpacity() {
        toolBubbles.values.forEach { it.setOpacity(currentOpacityLevel) }
    }

    /**
     * Updates the visibility of bubbles based on toggle states.
     */
    private fun updateBubbleVisibility() {
        toolBubblesList.forEachIndexed { index, bubble ->
            val shouldBeVisible =
                (bubble.isPrivate && showPrivateBubbles) ||
                    (!bubble.isPrivate && showHistoryBubbles) ||
                    !bubble.isPrivate
            toolBubbles[bubble.id]?.setVisibility(shouldBeVisible)
        }
    }
}

/**
 * Individual tool bubble view within the ToolBelt.
 */
private class ToolBeltBubbleView(
    private val context: Context,
    private val bubble: ToolBeltBubble,
    private val onClickListener: (ToolBeltBubble) -> Unit,
) {
    private val bubbleView: LinearLayout = LinearLayout(context).apply {
        layoutParams =
            LinearLayout.LayoutParams(
                120,
                120,
            ).apply {
                rightMargin = 8
            }
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(Color.TRANSPARENT)
    }

    private val customBubbleView: BubbleView

    init {
        customBubbleView =
            BubbleView(
                context,
                com.clipboardhistory.domain.model.BubbleThemes.DEFAULT,
                com.clipboardhistory.domain.model.BubbleState.EMPTY,
                bubble.bubbleType,
                clipboardItem = if (bubble.content.isNotEmpty()) {
                    ClipboardItem(
                        id = System.currentTimeMillis(),
                        content = bubble.content,
                        timestamp = System.currentTimeMillis(),
                        contentType = "text/plain",
                    )
                } else null,
            )
        customBubbleView.layoutParams =
            FrameLayout.LayoutParams(
                100,
                100,
            )
        customBubbleView.setOnClickListener { onClickListener(bubble) }

        bubbleView.addView(customBubbleView)

        val label =
            TextView(context).apply {
                text = bubble.content
                textSize = 10f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = 4
                    }
            }
        bubbleView.addView(label)
    }

    fun getView(): View = bubbleView

    fun setOpacity(opacity: Float) {
        customBubbleView.alpha = opacity
    }

    fun setVisibility(isVisible: Boolean) {
        bubbleView.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}
