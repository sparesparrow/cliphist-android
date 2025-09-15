# API Documentation

## Overview

This document describes the internal API structure of the Android Extended Clipboard History app.

## Domain Models

### ClipboardItem

Represents a single clipboard item with content and metadata.

```kotlin
data class ClipboardItem(
    val id: String,
    val content: String,
    val timestamp: Long,
    val contentType: ContentType,
    val isEncrypted: Boolean,
    val size: Int
)
```

### ClipboardSettings

Configuration settings for the clipboard service.

```kotlin
data class ClipboardSettings(
    val maxHistorySize: Int = 100,
    val autoDeleteAfterHours: Int = 24,
    val enableEncryption: Boolean = true,
    val bubbleSize: Int = 3,
    val bubbleOpacity: Float = 0.8f,
    val clipboardMode: ClipboardMode = ClipboardMode.REPLACE,
    val selectedTheme: String = "Default",
    val enableEdgeStates: Boolean = true,
    val bubbleType: BubbleType = BubbleType.CIRCLE
)
```

### BubbleType

Enumeration of supported bubble shapes and types.

```kotlin
enum class BubbleType {
    CIRCLE,          // Circular bubble (default)
    CUBE,            // Cube-shaped bubble that flashes content
    HEXAGON,         // Hexagonal bubble
    SQUARE           // Square bubble with rounded corners
}
```

### BubbleState

Enumeration of bubble states for different behaviors.

```kotlin
enum class BubbleState {
    EMPTY,           // Bubble has no content
    STORING,         // Bubble stores existing content (normal state)
    REPLACE,         // Next copy will replace bubble content
    APPEND           // Next copy will append to bubble content
}
```

## Use Cases

### AddClipboardItemUseCase

Adds a new clipboard item to the repository.

```kotlin
suspend operator fun invoke(
    content: String, 
    contentType: ContentType = ContentType.TEXT
): ClipboardItem
```

### GetAllClipboardItemsUseCase

Retrieves all clipboard items as a Flow.

```kotlin
operator fun invoke(): Flow<List<ClipboardItem>>
```

### DeleteClipboardItemUseCase

Deletes a clipboard item from the repository.

```kotlin
suspend operator fun invoke(item: ClipboardItem)
```

## Repository Interface

### ClipboardRepository

Main repository interface for clipboard operations.

```kotlin
interface ClipboardRepository {
    fun getAllItems(): Flow<List<ClipboardItem>>
    suspend fun getItemById(id: String): ClipboardItem?
    suspend fun insertItem(item: ClipboardItem)
    suspend fun updateItem(item: ClipboardItem)
    suspend fun deleteItem(item: ClipboardItem)
    suspend fun deleteAllItems()
    suspend fun getSettings(): ClipboardSettings
    suspend fun updateSettings(settings: ClipboardSettings)
}
```

## Services

### ClipboardService

Foreground service for monitoring clipboard changes.

**Key Methods:**
- `onCreate()`: Initialize service and clipboard listener
- `onStartCommand()`: Handle service start commands
- `onDestroy()`: Clean up resources

### FloatingBubbleService

Foreground service for managing floating bubbles.

**Key Methods:**
- `createEmptyBubble()`: Create new empty bubble
- `createFullBubble()`: Create bubble with content
- `handleBubbleClick()`: Process bubble interactions
- `setupDragBehavior()`: Configure drag and drop functionality
- `showHighlightedAreas()`: Display action areas during drag
- `hideHighlightedAreas()`: Hide action areas
- `handleDropAction()`: Process drop actions on action areas

**Bubble Types Support:**
- Supports all BubbleType enum values
- Automatic shape rendering based on type
- Special flash functionality for cube bubbles
- Drag-and-drop action areas for content editing

## UI Components

### BubbleView

Custom view for rendering different bubble shapes and states.

**Key Features:**
- Multiple shape support (Circle, Cube, Hexagon, Square)
- Theme-based color system
- State-based visual feedback
- Flash animation for cube bubbles

**Key Methods:**
```kotlin
fun updateBubbleType(newType: BubbleType)
fun flashContent()
fun flashContent(flashText: String)
fun updateState(newState: BubbleState)
fun updateTheme(newTheme: BubbleTheme)
```

### HighlightedAreaView

Custom view for displaying action areas during drag operations.

**Key Features:**
- Three action areas: Prepend, Replace, Append
- Fade in/out animations
- Drop detection and action mapping
- Theme-consistent styling

**Key Methods:**
```kotlin
fun show(): Shows action areas with animation
fun hide(): Hides action areas with animation
fun getActionForPosition(x: Float, y: Float): BubbleState?
```

**Shape Rendering:**
- `drawCircle()`: Renders circular bubbles
- `drawCube()`: Renders 3D cube with depth effect
- `drawHexagon()`: Renders hexagonal bubbles
- `drawSquare()`: Renders rounded square bubbles

### BubbleViewFactory

Factory for creating bubble views with different configurations.

```kotlin
fun createBubbleView(
    context: Context,
    themeName: String,
    state: BubbleState,
    bubbleType: BubbleType = BubbleType.CIRCLE,
    content: String? = null,
    opacity: Float = 1.0f
): BubbleView
```

### SettingsDialog

Composable dialog for configuring app settings.

**New Features:**
- Bubble type selection with visual previews
- Theme selection with color chips
- Real-time settings updates

## Database

### ClipboardItemDao

Data Access Object for clipboard items.

```kotlin
@Dao
interface ClipboardItemDao {
    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<ClipboardItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ClipboardItemEntity)

    @Delete
    suspend fun deleteItem(item: ClipboardItemEntity)

    @Query("DELETE FROM clipboard_items WHERE timestamp < :timestamp")
    suspend fun deleteItemsOlderThan(timestamp: Long)
}
```

## Encryption

### EncryptionManager

Handles encryption and decryption of clipboard data.

```kotlin
class EncryptionManager {
    fun encrypt(plaintext: String): String?
    fun decrypt(encryptedText: String): String?
    fun storeSecureString(key: String, value: String)
    fun getSecureString(key: String, defaultValue: String = ""): String
}
```

## Bubble Types Implementation

### Circle Bubble
- **Shape**: Perfect circle using `drawRoundRect`
- **Behavior**: Standard bubble functionality with drag-and-drop actions
- **Animation**: Smooth scaling and opacity changes

### Cube Bubble
- **Shape**: 3D cube with three faces (main, top, right)
- **Special Feature**: Flash content preview when tapped
- **Animation**: Alpha-based flash effect with content display
- **3D Effect**: Lighter top face, darker right face for depth
- **Drag Actions**: Full drag-and-drop action support

### Hexagon Bubble
- **Shape**: Regular hexagon using `Path` drawing
- **Behavior**: Standard bubble functionality with drag-and-drop actions
- **Design**: Geometric, modern appearance

### Square Bubble
- **Shape**: Rounded square with configurable corner radius
- **Behavior**: Standard bubble functionality with drag-and-drop actions
- **Design**: Clean, minimalist appearance

## Drag-and-Drop Action System

### Action Areas
Three highlighted areas appear when dragging bubbles with content:

**Prepend Area (Purple)**
- **Action**: Add clipboard content to the beginning of bubble content
- **Visual**: Purple background with white border
- **Position**: Left side of action area bar

**Replace Area (Red-Orange)**
- **Action**: Replace bubble content with current clipboard content
- **Visual**: Red-orange background with white border
- **Position**: Center of action area bar

**Append Area (Green)**
- **Action**: Add clipboard content to the end of bubble content
- **Visual**: Green background with white border
- **Position**: Right side of action area bar

### Drag Detection
- **Threshold**: 20px movement to start drag
- **Trigger**: Only for bubbles with content
- **Visual Feedback**: Action areas fade in when drag starts
- **Drop Detection**: Position-based action determination

## Flash Functionality

### Cube Bubble Flash
The cube bubble type includes special flash functionality:

**Implementation:**
```kotlin
fun flashContent() {
    if (bubbleType != BubbleType.CUBE) return
    
    // Start flash animation
    flashAnimator = ValueAnimator.ofFloat(1.0f, 0.3f, 1.0f)
    // Apply alpha changes during animation
    // Reset after 1 second
}

fun flashContent(flashText: String) {
    // Temporarily replace content with flash text
    // Animate flash effect
    // Restore original content after animation
}
```

**Features:**
- 1-second flash duration
- Smooth alpha animation with easing
- Content preview display (up to 20 characters)
- Automatic content restoration
- Only works for cube bubble type

## Error Handling

All use cases and repository methods handle errors gracefully:

- Network errors: Logged and handled silently
- Database errors: Logged with fallback to previous state
- Encryption errors: Logged and content stored unencrypted as fallback
- Permission errors: User is notified and guided to grant permissions

## Testing

### Unit Test Structure

Each component has corresponding unit tests:

- `MainViewModelTest`: Tests ViewModel state management
- `ClipboardRepositoryImplTest`: Tests repository operations
- `EncryptionManagerTest`: Tests encryption/decryption
- `BubbleViewTest`: Tests bubble types and flash functionality

### Integration Test Structure

- `ClipboardDatabaseTest`: Tests database operations
- `ServiceIntegrationTest`: Tests service functionality

### UI Test Structure

- `MainScreenTest`: Tests main screen interactions
- `SettingsDialogTest`: Tests settings configuration
- `BubbleInteractionTest`: Tests bubble interactions and animations

## Performance Considerations

### Bubble Rendering
- Efficient shape drawing with minimal object allocation
- Hardware acceleration for smooth animations
- Optimized flash animation with ValueAnimator

### Memory Management
- Proper cleanup of animation resources
- Efficient theme and state updates
- Minimal memory footprint for bubble views

### Battery Optimization
- Smart animation timing and duration
- Efficient service lifecycle management
- Background processing optimization
