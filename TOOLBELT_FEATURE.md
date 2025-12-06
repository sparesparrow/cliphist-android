# ToolBelt Feature Implementation

## Overview

The ToolBelt feature adds a special widget that provides quick access to common tools and operations. It creates a "belt" of customizable tool bubbles that allow users to:

- Adjust transparency/opacity of bubbles
- Toggle visibility of private bubbles
- Toggle visibility of clipboard history bubbles
- Change bubble types dynamically
- Edit bubble content
- Switch between operation modes (overwrite, append, prepend)

## Architecture

### Domain Layer

#### ToolBeltBubble Model
- Location: `app/src/main/java/com/clipboardhistory/domain/model/ToolBeltBubble.kt`
- Represents a special bubble type used in the ToolBelt widget
- Key properties:
  - `id`: Unique identifier
  - `type`: The tool type (ToolBeltBubbleType enum)
  - `content`: The label/content for the bubble
  - `bubbleType`: Visual shape (CIRCLE, CUBE, HEXAGON, SQUARE)
  - `isPrivate`: Privacy flag
  - `isVisible`: Visibility flag
  - `operationMode`: Default operation mode (OVERWRITE, APPEND, PREPEND)
  - `position`: Position in the ToolBelt
  - `createdAt` / `updatedAt`: Timestamps

#### Enums

**ToolBeltBubbleType:**
- `GENERIC`: Custom/generic tool bubble
- `OPACITY_SLIDER`: Opacity/transparency slider
- `PRIVATE_VISIBILITY_TOGGLE`: Show/hide private bubbles
- `HISTORY_VISIBILITY_TOGGLE`: Show/hide clipboard history
- `BUBBLE_TYPE_CHANGER`: Change bubble shape
- `CONTENT_EDITOR`: Edit bubble content
- `OPERATION_MODE_SWITCHER`: Switch operation modes
- `CLEAR_ALL`: Clear all clipboard items
- `SETTINGS`: Access settings

**OperationMode:**
- `OVERWRITE`: Replace clipboard content
- `APPEND`: Append to clipboard
- `PREPEND`: Prepend to clipboard

### Presentation Layer

#### ToolBeltWidget
- Location: `app/src/main/java/com/clipboardhistory/presentation/ui/components/ToolBeltWidget.kt`
- Custom CardView containing the entire ToolBelt UI
- Features:
  - Horizontal scrollable bubble container
  - Control panel with opacity slider
  - Toggle buttons for private/history visibility
  - Individual tool bubble views
  - Event listeners for user interactions

#### ToolBeltViewModel
- Location: `app/src/main/java/com/clipboardhistory/presentation/viewmodels/ToolBeltViewModel.kt`
- Manages ToolBelt state and operations
- LiveData observables:
  - `toolBubbles`: List of current bubbles
  - `opacityLevel`: Current opacity setting
  - `showPrivateBubbles`: Private visibility toggle
  - `showHistoryBubbles`: History visibility toggle
  - `currentOperationMode`: Current operation mode
  - `selectedBubbleType`: Currently selected bubble type
  - `toolBeltEvent`: Event stream for state changes

## Integration Guide

### Step 1: Add to MainActivity

```kotlin
import com.clipboardhistory.presentation.ui.components.ToolBeltWidget
import com.clipboardhistory.presentation.viewmodels.ToolBeltViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var toolBeltWidget: ToolBeltWidget
    private lateinit var toolBeltViewModel: ToolBeltViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ToolBelt components
        toolBeltWidget = findViewById(R.id.toolbelt_widget)
        toolBeltViewModel = ViewModelProvider(this).get(ToolBeltViewModel::class.java)

        // Initialize with default tools
        toolBeltViewModel.initializeDefaultToolBelt()
        toolBeltWidget.initializeDefaultTools()

        // Observe state changes
        observeToolBeltState()
        setupToolBeltListeners()
    }

    private fun observeToolBeltState() {
        toolBeltViewModel.toolBubbles.observe(this) { bubbles ->
            // Update UI when bubbles change
            updateBubbleDisplay(bubbles)
        }

        toolBeltViewModel.opacityLevel.observe(this) { opacity ->
            toolBeltWidget.setOpacityLevel(opacity)
        }

        toolBeltViewModel.showPrivateBubbles.observe(this) { isVisible ->
            // Handle private bubble visibility
        }

        toolBeltViewModel.showHistoryBubbles.observe(this) { isVisible ->
            // Handle history bubble visibility
        }

        toolBeltViewModel.currentOperationMode.observe(this) { mode ->
            // Handle operation mode change
            updateClipboardOperationMode(mode)
        }
    }

    private fun setupToolBeltListeners() {
        toolBeltWidget.setOnBubbleClickListener { bubble ->
            handleToolBubbleClick(bubble)
        }

        toolBeltWidget.setOnOperationModeChangeListener { mode ->
            toolBeltViewModel.setOperationMode(mode)
        }
    }

    private fun handleToolBubbleClick(bubble: ToolBeltBubble) {
        when (bubble.type) {
            ToolBeltBubbleType.OPACITY_SLIDER -> {
                // Handle opacity change
            }
            ToolBeltBubbleType.PRIVATE_VISIBILITY_TOGGLE -> {
                toolBeltViewModel.setPrivateBubblesVisibility(
                    !(toolBeltViewModel.showPrivateBubbles.value ?: true)
                )
            }
            ToolBeltBubbleType.HISTORY_VISIBILITY_TOGGLE -> {
                toolBeltViewModel.setHistoryBubblesVisibility(
                    !(toolBeltViewModel.showHistoryBubbles.value ?: true)
                )
            }
            ToolBeltBubbleType.OPERATION_MODE_SWITCHER -> {
                toolBeltViewModel.cycleOperationMode()
            }
            ToolBeltBubbleType.BUBBLE_TYPE_CHANGER -> {
                showBubbleTypeSelector()
            }
            ToolBeltBubbleType.CONTENT_EDITOR -> {
                showContentEditor(bubble)
            }
            else -> {
                // Handle other bubble types
            }
        }
    }
}
```

### Step 2: Add to Layout XML

```xml
<!-- activity_main.xml -->
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- Main content -->
    <FrameLayout
        android:id="@+id/main_content"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <!-- ToolBelt Widget -->
    <com.clipboardhistory.presentation.ui.components.ToolBeltWidget
        android:id="@+id/toolbelt_widget"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp" />

</LinearLayout>
```

### Step 3: ViewModel Lifecycle

```kotlin
// In Activity/Fragment
private fun setupViewModel() {
    toolBeltViewModel = ViewModelProvider(this).get(ToolBeltViewModel::class.java)

    // Initialize ToolBelt
    toolBeltViewModel.initializeDefaultToolBelt()

    // Listen to events
    toolBeltViewModel.toolBeltEvent.observe(this) { event ->
        when (event) {
            is ToolBeltEvent.ToolBeltInitialized -> {
                // ToolBelt initialized with bubbles
            }
            is ToolBeltEvent.BubbleAdded -> {
                // A new bubble was added
                toolBeltWidget.addBubble(event.bubble)
            }
            is ToolBeltEvent.BubbleRemoved -> {
                // A bubble was removed
                toolBeltWidget.removeBubble(event.bubbleId)
            }
            is ToolBeltEvent.BubbleUpdated -> {
                // A bubble was updated
                toolBeltWidget.updateBubble(event.bubble)
            }
            is ToolBeltEvent.OpacityChanged -> {
                // Opacity was changed
            }
            is ToolBeltEvent.OperationModeChanged -> {
                // Operation mode was changed
            }
            is ToolBeltEvent.PrivateBubblesToggled -> {
                // Private bubble visibility toggled
            }
            is ToolBeltEvent.HistoryBubblesToggled -> {
                // History bubble visibility toggled
            }
            else -> {}
        }
    }
}
```

## Usage Examples

### Adding a Custom Tool Bubble

```kotlin
val customBubble = ToolBeltBubble(
    type = ToolBeltBubbleType.GENERIC,
    content = "My Tool",
    bubbleType = BubbleType.HEXAGON,
    position = 6,
    operationMode = OperationMode.APPEND
)

toolBeltViewModel.addBubble(customBubble)
toolBeltWidget.addBubble(customBubble)
```

### Changing Bubble Content

```kotlin
toolBeltViewModel.changeBubbleContent(bubbleId, "New Content")
```

### Changing Bubble Type (Shape)

```kotlin
toolBeltViewModel.changeBubbleType(bubbleId, BubbleType.SQUARE)
```

### Setting Opacity

```kotlin
toolBeltViewModel.setOpacityLevel(0.75f) // 75% opacity
```

### Changing Operation Mode

```kotlin
toolBeltViewModel.setOperationMode(OperationMode.PREPEND)
```

### Cycling Operation Modes

```kotlin
// OVERWRITE -> APPEND -> PREPEND -> OVERWRITE (cycles)
toolBeltViewModel.cycleOperationMode()
```

## Features Implemented

### 1. ToolBeltBubble Type
- [x] Special bubble type distinct from regular bubbles
- [x] Support for different visual types (circle, cube, hexagon, square)
- [x] Privacy and visibility flags
- [x] Operation mode support
- [x] Position tracking
- [x] Timestamp tracking

### 2. ToolBelt Widget
- [x] Horizontal scrollable bubble container
- [x] Opacity/transparency slider
- [x] Private bubbles visibility toggle
- [x] Clipboard history visibility toggle
- [x] Individual tool bubble views
- [x] Click event handling
- [x] Dynamic bubble addition/removal/update

### 3. Default Tool Bubbles
- [x] Opacity slider tool
- [x] Private visibility toggle
- [x] History visibility toggle
- [x] Bubble type changer
- [x] Operation mode switcher
- [x] Settings access

### 4. ToolBeltViewModel
- [x] State management with LiveData
- [x] Event system for state changes
- [x] Bubble CRUD operations
- [x] Opacity control
- [x] Visibility toggling
- [x] Operation mode management
- [x] Position/ordering support

## File Structure

```
app/src/main/java/com/clipboardhistory/
├── domain/
│   └── model/
│       └── ToolBeltBubble.kt          (NEW) - Domain model
├── presentation/
│   ├── ui/
│   │   └── components/
│   │       └── ToolBeltWidget.kt      (NEW) - UI Widget
│   └── viewmodels/
│       └── ToolBeltViewModel.kt       (NEW) - State Management
```

## Testing

### Unit Tests for ToolBeltViewModel

```kotlin
@RunWith(AndroidJUnit4::class)
class ToolBeltViewModelTest {

    private lateinit var viewModel: ToolBeltViewModel

    @Before
    fun setUp() {
        viewModel = ToolBeltViewModel()
    }

    @Test
    fun testInitializeDefaultToolBelt() {
        viewModel.initializeDefaultToolBelt()
        val bubbles = viewModel.toolBubbles.value
        assert(bubbles != null)
        assert(bubbles!!.size > 0)
    }

    @Test
    fun testAddBubble() {
        val bubble = ToolBeltBubble(content = "Test")
        viewModel.addBubble(bubble)
        assert(viewModel.toolBubbles.value?.contains(bubble) == true)
    }

    @Test
    fun testRemoveBubble() {
        val bubble = ToolBeltBubble(content = "Test")
        viewModel.addBubble(bubble)
        viewModel.removeBubble(bubble.id)
        assert(viewModel.toolBubbles.value?.contains(bubble) == false)
    }

    @Test
    fun testOpacityLevel() {
        viewModel.setOpacityLevel(0.5f)
        assert(viewModel.opacityLevel.value == 0.5f)
    }

    @Test
    fun testOperationModeCycle() {
        viewModel.cycleOperationMode()
        assert(viewModel.currentOperationMode.value == OperationMode.APPEND)
        viewModel.cycleOperationMode()
        assert(viewModel.currentOperationMode.value == OperationMode.PREPEND)
    }
}
```

## Integration Checklist

- [ ] Add ToolBeltBubble.kt to domain/model/
- [ ] Add ToolBeltWidget.kt to presentation/ui/components/
- [ ] Add ToolBeltViewModel.kt to presentation/viewmodels/
- [ ] Add ToolBeltWidget to activity_main.xml layout
- [ ] Initialize ToolBeltViewModel in MainActivity
- [ ] Setup observers for ToolBelt state changes
- [ ] Implement event handlers for bubble clicks
- [ ] Add unit tests for ToolBeltViewModel
- [ ] Add integration tests for ToolBeltWidget
- [ ] Add documentation in README

## Future Enhancements

1. **Persistence**: Save ToolBelt configuration to SharedPreferences or database
2. **Drag & Drop**: Reorder bubbles via drag and drop
3. **Animation**: Add smooth animations for bubble interactions
4. **Customization**: Allow users to customize default tools
5. **Keyboard Shortcuts**: Add keyboard support for quick access
6. **Themes**: Support different visual themes for ToolBelt
7. **Recording/Playback**: Record sequences of operations
8. **Macros**: Create and execute operation macros

## Notes

- The ToolBelt widget is responsive and adapts to different screen sizes
- All bubbles support transparency/opacity control
- Operation modes affect how clipboard content is handled
- The widget integrates seamlessly with existing bubble system
- Events are emitted for all state changes to enable reactive updates
