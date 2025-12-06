# ToolBelt Integration - Complete Example

This document provides a complete, copy-paste-ready example for integrating the ToolBelt feature into your MainActivity.

## Step 1: Update activity_main.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- Your main content -->
    <FrameLayout
        android:id="@+id/main_content"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:background="@android:color/white" />

    <!-- ToolBelt Widget -->
    <com.clipboardhistory.presentation.ui.components.ToolBeltWidget
        android:id="@+id/toolbelt_widget"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp" />

</LinearLayout>
```

## Step 2: Update MainActivity

```kotlin
package com.clipboardhistory.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.clipboardhistory.R
import com.clipboardhistory.domain.model.BubbleType
import com.clipboardhistory.domain.model.OperationMode
import com.clipboardhistory.domain.model.ToolBeltBubble
import com.clipboardhistory.domain.model.ToolBeltBubbleType
import com.clipboardhistory.presentation.ui.components.ToolBeltWidget
import com.clipboardhistory.presentation.viewmodels.ToolBeltViewModel
import com.clipboardhistory.presentation.viewmodels.ToolBeltEvent

class MainActivity : AppCompatActivity() {

    private lateinit var toolBeltWidget: ToolBeltWidget
    private lateinit var toolBeltViewModel: ToolBeltViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ToolBelt components
        initializeToolBelt()

        // Observe state changes
        observeToolBeltState()

        // Setup event listeners
        setupToolBeltListeners()
    }

    /**
     * Initializes the ToolBelt widget and ViewModel.
     */
    private fun initializeToolBelt() {
        toolBeltWidget = findViewById(R.id.toolbelt_widget)
        toolBeltViewModel = ViewModelProvider(this).get(ToolBeltViewModel::class.java)

        // Initialize with default tools
        toolBeltViewModel.initializeDefaultToolBelt()
        toolBeltWidget.initializeDefaultTools()
    }

    /**
     * Observes ToolBelt state changes from the ViewModel.
     */
    private fun observeToolBeltState() {
        // Observe tool bubbles list
        toolBeltViewModel.toolBubbles.observe(this) { bubbles ->
            // Update UI when bubbles change
            updateBubbleDisplay(bubbles)
        }

        // Observe opacity level changes
        toolBeltViewModel.opacityLevel.observe(this) { opacity ->
            toolBeltWidget.setOpacityLevel(opacity)
            // You can also update other UI elements based on opacity
        }

        // Observe private bubbles visibility
        toolBeltViewModel.showPrivateBubbles.observe(this) { isVisible ->
            // Handle private bubble visibility changes
            showToast(if (isVisible) "Private bubbles: Visible" else "Private bubbles: Hidden")
        }

        // Observe history bubbles visibility
        toolBeltViewModel.showHistoryBubbles.observe(this) { isVisible ->
            // Handle history bubble visibility changes
            showToast(if (isVisible) "History bubbles: Visible" else "History bubbles: Hidden")
        }

        // Observe operation mode changes
        toolBeltViewModel.currentOperationMode.observe(this) { mode ->
            updateClipboardOperationMode(mode)
            showToast("Operation mode: ${mode.name}")
        }

        // Observe selected bubble type
        toolBeltViewModel.selectedBubbleType.observe(this) { bubbleType ->
            // Handle selected bubble type changes
        }

        // Observe ToolBelt events
        toolBeltViewModel.toolBeltEvent.observe(this) { event ->
            handleToolBeltEvent(event)
        }
    }

    /**
     * Sets up click and change listeners for the ToolBelt widget.
     */
    private fun setupToolBeltListeners() {
        // Set bubble click listener
        toolBeltWidget.setOnBubbleClickListener { bubble ->
            handleToolBubbleClick(bubble)
        }

        // Set operation mode change listener
        toolBeltWidget.setOnOperationModeChangeListener { mode ->
            toolBeltViewModel.setOperationMode(mode)
        }
    }

    /**
     * Handles clicks on tool bubbles.
     */
    private fun handleToolBubbleClick(bubble: ToolBeltBubble) {
        when (bubble.type) {
            ToolBeltBubbleType.OPACITY_SLIDER -> {
                // Opacity is handled by slider widget
                showToast("Opacity: ${(toolBeltViewModel.opacityLevel.value ?: 1f) * 100}%")
            }

            ToolBeltBubbleType.PRIVATE_VISIBILITY_TOGGLE -> {
                val currentState = toolBeltViewModel.showPrivateBubbles.value ?: true
                toolBeltViewModel.setPrivateBubblesVisibility(!currentState)
            }

            ToolBeltBubbleType.HISTORY_VISIBILITY_TOGGLE -> {
                val currentState = toolBeltViewModel.showHistoryBubbles.value ?: true
                toolBeltViewModel.setHistoryBubblesVisibility(!currentState)
            }

            ToolBeltBubbleType.BUBBLE_TYPE_CHANGER -> {
                showBubbleTypeSelector()
            }

            ToolBeltBubbleType.CONTENT_EDITOR -> {
                showContentEditor(bubble)
            }

            ToolBeltBubbleType.OPERATION_MODE_SWITCHER -> {
                toolBeltViewModel.cycleOperationMode()
            }

            ToolBeltBubbleType.CLEAR_ALL -> {
                // Handle clear all action
                showToast("Clear all clipboard items")
                // Implement your clear logic here
            }

            ToolBeltBubbleType.SETTINGS -> {
                // Open settings
                showToast("Open settings")
                // Navigate to settings activity
            }

            else -> {
                showToast("Tool: ${bubble.type.name}")
            }
        }
    }

    /**
     * Handles ToolBelt events from the ViewModel.
     */
    private fun handleToolBeltEvent(event: ToolBeltEvent) {
        when (event) {
            is ToolBeltEvent.ToolBeltInitialized -> {
                showToast("ToolBelt initialized with ${event.bubbles.size} bubbles")
            }

            is ToolBeltEvent.BubbleAdded -> {
                toolBeltWidget.addBubble(event.bubble)
                showToast("Bubble added: ${event.bubble.content}")
            }

            is ToolBeltEvent.BubbleRemoved -> {
                toolBeltWidget.removeBubble(event.bubbleId)
                showToast("Bubble removed")
            }

            is ToolBeltEvent.BubbleUpdated -> {
                toolBeltWidget.updateBubble(event.bubble)
                showToast("Bubble updated")
            }

            is ToolBeltEvent.BubbleMoved -> {
                showToast("Bubble moved to position ${event.newPosition}")
            }

            is ToolBeltEvent.OpacityChanged -> {
                showToast("Opacity: ${(event.opacity * 100).toInt()}%")
            }

            is ToolBeltEvent.PrivateBubblesToggled -> {
                showToast("Private bubbles: ${if (event.isVisible) "Visible" else "Hidden"}")
            }

            is ToolBeltEvent.HistoryBubblesToggled -> {
                showToast("History bubbles: ${if (event.isVisible) "Visible" else "Hidden"}")
            }

            is ToolBeltEvent.OperationModeChanged -> {
                updateClipboardOperationMode(event.mode)
            }

            is ToolBeltEvent.AllBubblesCleared -> {
                showToast("All bubbles cleared")
            }
        }
    }

    /**
     * Shows a bubble type selector dialog.
     */
    private fun showBubbleTypeSelector() {
        val types = arrayOf("Circle", "Square", "Hexagon", "Cube")
        val bubbleTypes =
            arrayOf(
                BubbleType.CIRCLE,
                BubbleType.SQUARE,
                BubbleType.HEXAGON,
                BubbleType.CUBE,
            )

        android.app.AlertDialog.Builder(this)
            .setTitle("Select Bubble Type")
            .setItems(types) { _, which ->
                toolBeltViewModel.setSelectedBubbleType(bubbleTypes[which])
                showToast("Selected: ${types[which]}")
            }
            .show()
    }

    /**
     * Shows a content editor dialog for a bubble.
     */
    private fun showContentEditor(bubble: ToolBeltBubble) {
        val editText = android.widget.EditText(this).apply {
            setText(bubble.content)
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Edit Bubble Content")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newContent = editText.text.toString()
                toolBeltViewModel.changeBubbleContent(bubble.id, newContent)
                showToast("Content updated")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Updates the display of bubbles.
     */
    private fun updateBubbleDisplay(bubbles: List<ToolBeltBubble>) {
        // Update UI with bubbles
        // This is called whenever the bubble list changes
    }

    /**
     * Updates the clipboard operation mode globally.
     */
    private fun updateClipboardOperationMode(mode: OperationMode) {
        when (mode) {
            OperationMode.OVERWRITE -> {
                // Set clipboard to overwrite mode
            }

            OperationMode.APPEND -> {
                // Set clipboard to append mode
            }

            OperationMode.PREPEND -> {
                // Set clipboard to prepend mode
            }
        }
    }

    /**
     * Utility function to show a toast message.
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Example: Adding a custom tool bubble at runtime.
     */
    fun addCustomToolBubble() {
        val customBubble = ToolBeltBubble(
            type = ToolBeltBubbleType.GENERIC,
            content = "My Tool",
            bubbleType = BubbleType.HEXAGON,
            position = 6,
            operationMode = OperationMode.APPEND,
        )
        toolBeltViewModel.addBubble(customBubble)
    }

    /**
     * Example: Modifying a bubble's properties.
     */
    fun modifyBubble(bubbleId: String) {
        val bubble = toolBeltViewModel.getBubbleById(bubbleId)
        if (bubble != null) {
            val updated = bubble.copy(
                content = "Updated Label",
                bubbleType = BubbleType.CUBE,
            )
            toolBeltViewModel.updateBubble(updated)
        }
    }

    /**
     * Example: Removing a bubble by ID.
     */
    fun removeToolBubble(bubbleId: String) {
        toolBeltViewModel.removeBubble(bubbleId)
    }

    /**
     * Example: Reset ToolBelt to default state.
     */
    fun resetToolBelt() {
        toolBeltViewModel.resetToDefaults()
    }
}
```

## Step 3: Example Usage Patterns

### Adding a Custom Bubble
```kotlin
val customBubble = ToolBeltBubble(
    type = ToolBeltBubbleType.GENERIC,
    content = "Export",
    bubbleType = BubbleType.HEXAGON,
    position = 7,
)
toolBeltViewModel.addBubble(customBubble)
```

### Changing Operation Mode
```kotlin
toolBeltViewModel.cycleOperationMode() // OVERWRITE -> APPEND -> PREPEND -> OVERWRITE
// or
toolBeltViewModel.setOperationMode(OperationMode.APPEND)
```

### Adjusting Opacity
```kotlin
toolBeltViewModel.setOpacityLevel(0.75f) // 75% opacity
```

### Getting Bubbles of a Type
```kotlin
val visibilityToggles = toolBeltViewModel.getBubblesByType(
    ToolBeltBubbleType.PRIVATE_VISIBILITY_TOGGLE
)
```

### Reordering Bubbles
```kotlin
toolBeltViewModel.moveBubble(bubbleId, newPosition = 2)
```

## Step 4: Testing

Run the unit tests:
```bash
./gradlew test
```

Run specific test class:
```bash
./gradlew testDebugUnitTest --tests ToolBeltViewModelTest
```

## Troubleshooting

### Widget not showing
- Verify R.layout.activity_main has the ToolBeltWidget element
- Check that findViewById() is returning the correct view

### Opacity slider not working
- Ensure SeekBar is properly initialized
- Check that setOnSeekBarChangeListener is set

### Bubbles not updating
- Verify LiveData observers are set up correctly
- Check that ToolBeltEvent is being emitted

### Tests failing
- Run: `./gradlew clean build test`
- Check Android Studio logcat for detailed errors

## Next Steps

1. Customize default bubbles in `initializeDefaultToolBelt()`
2. Add persistence using SharedPreferences or Room Database
3. Implement drag-and-drop for reordering
4. Add animations for smooth interactions
5. Create keyboard shortcuts for quick access

---

For complete documentation, see: **TOOLBELT_FEATURE.md**
