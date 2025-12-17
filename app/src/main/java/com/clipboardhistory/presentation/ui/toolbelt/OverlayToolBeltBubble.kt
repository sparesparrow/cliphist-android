package com.clipboardhistory.presentation.ui.toolbelt

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.clipboardhistory.domain.model.OperationMode

/**
 * Special bubble type for toolbelt controls that appears as floating overlays
 * Implements Issue #12 - ToolBelt overlay feature
 */
class OverlayToolBeltBubble(
    private val context: Context,
    private val windowManager: WindowManager,
    private val toolType: ToolType
) {
    enum class ToolType {
        TRANSPARENCY_SLIDER,    // Issue #11
        TOGGLE_PRIVATE,
        TOGGLE_HISTORY,
        CHANGE_TYPE,
        CHANGE_CONTENT,
        OPERATION_MODE
    }

    private var bubbleView: View? = null
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = 100
    }

    private val opacityCallback: (Float) -> Unit = { opacity ->
        params.alpha = opacity
        bubbleView?.let { windowManager.updateViewLayout(it, params) }
    }

    init {
        // Register for transparency updates
        TransparencyController.registerBubble(opacityCallback)

        // Apply initial opacity
        val currentOpacity = TransparencyController.getGlobalOpacity(context)
        TransparencyController.applyOpacityToView(params, currentOpacity)
    }

    fun show() {
        if (bubbleView != null) return

        bubbleView = ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    when (toolType) {
                        ToolType.TRANSPARENCY_SLIDER -> TransparencySliderTool()
                        ToolType.TOGGLE_PRIVATE -> ToggleTool("Private", Icons.Filled.Lock)
                        ToolType.TOGGLE_HISTORY -> ToggleTool("History", Icons.Filled.History)
                        ToolType.CHANGE_TYPE -> TypeChangerTool()
                        ToolType.CHANGE_CONTENT -> ContentEditorTool()
                        ToolType.OPERATION_MODE -> OperationModeTool()
                    }
                }
            }
        }

        try {
            windowManager.addView(bubbleView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hide() {
        bubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bubbleView = null
        }
    }

    fun updatePosition(x: Int, y: Int) {
        params.x = x
        params.y = y
        bubbleView?.let {
            try {
                windowManager.updateViewLayout(it, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun destroy() {
        TransparencyController.unregisterBubble(opacityCallback)
        hide()
    }

    @Composable
    private fun TransparencySliderTool() {
        var opacity by remember { mutableStateOf(TransparencyController.getGlobalOpacity(context)) }

        Surface(
            modifier = Modifier
                .width(250.dp)
                .padding(8.dp),
            shape = MaterialTheme.shapes.medium,
            color = Color.DarkGray.copy(alpha = 0.9f),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Transparency",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = opacity,
                    onValueChange = { newOpacity ->
                        opacity = newOpacity
                        TransparencyController.setGlobalOpacity(context, newOpacity)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Cyan
                    )
                )
                Text(
                    "${(opacity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }

    @Composable
    private fun ToggleTool(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
        val isEnabled by when(label) {
            "Private" -> BubbleStateManager.privateVisible.collectAsState()
            "History" -> BubbleStateManager.historyVisible.collectAsState()
            else -> remember { mutableStateOf(true) }
        }

        IconButton(
            onClick = {
                BubbleStateManager.toggleVisibility(context, label, !isEnabled)
            },
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = if (isEnabled) Color.Green.copy(alpha = 0.7f)
                            else Color.Red.copy(alpha = 0.7f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White
            )
        }
    }

    @Composable
    private fun TypeChangerTool() {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = Color.Blue.copy(alpha = 0.7f)
        ) {
            IconButton(onClick = { /* Show type selector dialog */ }) {
                Icon(Icons.Filled.SwapHoriz, "Change Type", tint = Color.White)
            }
        }
    }

    @Composable
    private fun ContentEditorTool() {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = Color.Magenta.copy(alpha = 0.7f)
        ) {
            IconButton(onClick = { /* Open content editor */ }) {
                Icon(Icons.Filled.Edit, "Edit Content", tint = Color.White)
            }
        }
    }

    @Composable
    private fun OperationModeTool() {
        val mode by BubbleStateManager.operationMode.collectAsState()

        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = Color.Yellow.copy(alpha = 0.7f)
        ) {
            IconButton(onClick = {
                val nextMode = when(mode) {
                    OperationMode.OVERWRITE -> OperationMode.APPEND
                    OperationMode.APPEND -> OperationMode.PREPEND
                    OperationMode.PREPEND -> OperationMode.OVERWRITE
                }
                BubbleStateManager.setOperationMode(context, nextMode)
            }) {
                Icon(
                    when(mode) {
                        OperationMode.OVERWRITE -> Icons.Filled.Refresh
                        OperationMode.APPEND -> Icons.Filled.Add
                        OperationMode.PREPEND -> Icons.Filled.KeyboardArrowUp
                    },
                    mode.name,
                    tint = Color.Black
                )
            }
        }
    }
}