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
    val clipboardMode: ClipboardMode = ClipboardMode.REPLACE
)
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

### Integration Test Structure

- `ClipboardDatabaseTest`: Tests database operations
- `ServiceIntegrationTest`: Tests service functionality

### UI Test Structure

- `MainScreenTest`: Tests main screen interactions
- `SettingsDialogTest`: Tests settings dialog functionality
