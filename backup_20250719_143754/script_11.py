# Create GitHub Actions workflow for CI/CD
github_actions_ci = '''name: Android CI/CD

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]
  release:
    types: [ published ]

jobs:
  test:
    name: Run Tests
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Run lint
      run: ./gradlew lint
      
    - name: Run unit tests
      run: ./gradlew test
      
    - name: Run instrumentation tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 29
        script: ./gradlew connectedAndroidTest
        
    - name: Upload test reports
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: test-reports
        path: |
          app/build/reports/
          app/build/test-results/
          
    - name: Upload lint reports
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: lint-reports
        path: app/build/reports/lint-results*.html

  build:
    name: Build APK
    runs-on: ubuntu-latest
    needs: test
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Build debug APK
      run: ./gradlew assembleDebug
      
    - name: Upload debug APK
      uses: actions/upload-artifact@v3
      with:
        name: debug-apk
        path: app/build/outputs/apk/debug/app-debug.apk

  build-release:
    name: Build Release APK
    runs-on: ubuntu-latest
    needs: test
    if: github.event_name == 'release'
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Create keystore from secret
      run: |
        echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > keystore.jks
        
    - name: Build release APK
      run: ./gradlew assembleRelease
      env:
        KEYSTORE_PATH: keystore.jks
        KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
        KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        
    - name: Upload release APK
      uses: actions/upload-artifact@v3
      with:
        name: release-apk
        path: app/build/outputs/apk/release/app-release.apk
        
    - name: Upload release APK to GitHub Release
      uses: actions/upload-release-asset@v1
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      with:
        upload_url: ${{ github.event.release.upload_url }}
        asset_path: app/build/outputs/apk/release/app-release.apk
        asset_name: clipboard-history-${{ github.event.release.tag_name }}.apk
        asset_content_type: application/vnd.android.package-archive

  security-scan:
    name: Security Scan
    runs-on: ubuntu-latest
    needs: build
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Run CodeQL Analysis
      uses: github/codeql-action/analyze@v2
      with:
        languages: java
        
    - name: Run dependency check
      uses: dependency-check/Dependency-Check_Action@main
      with:
        project: 'clipboard-history'
        path: '.'
        format: 'ALL'
        
    - name: Upload dependency check results
      uses: actions/upload-artifact@v3
      with:
        name: dependency-check-report
        path: reports/'''

# Create pull request workflow
github_actions_pr = '''name: Pull Request Checks

on:
  pull_request:
    branches: [ main, develop ]

jobs:
  code-quality:
    name: Code Quality Checks
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Run ktlint
      run: ./gradlew ktlintCheck
      
    - name: Run detekt
      run: ./gradlew detekt
      
    - name: Run lint
      run: ./gradlew lint
      
    - name: Comment PR with lint results
      uses: yutailang0119/action-android-lint@v3
      with:
        report-path: app/build/reports/lint-results*.xml
      continue-on-error: true
        
    - name: Upload code quality reports
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: code-quality-reports
        path: |
          app/build/reports/ktlint/
          app/build/reports/detekt/
          app/build/reports/lint-results*.html'''

# Create ProGuard rules
proguard_rules = '''# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep all classes used by Room
-keep class androidx.room.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Keep all classes used by Hilt
-keep class dagger.hilt.android.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Keep all classes used by SQLCipher
-keep class net.sqlcipher.** { *; }

# Keep all model classes
-keep class com.clipboardhistory.domain.model.** { *; }
-keep class com.clipboardhistory.data.database.** { *; }

# Keep all service classes
-keep class com.clipboardhistory.presentation.services.** { *; }

# Keep encryption-related classes
-keep class com.clipboardhistory.data.encryption.** { *; }
-keep class androidx.security.crypto.** { *; }

# Keep Jetpack Compose classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Android components
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }

# Keep all classes that might be used by reflection
-keepclassmembers class * {
    @javax.inject.* <methods>;
    @dagger.* <methods>;
}

# Remove debug logging
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}'''

# Create README.md
readme_md = '''# Android Extended Clipboard History

A modern Android application that provides an extended clipboard history with floating bubble interface and secure encryption.

## Features

- 🔒 **Secure Encryption**: All clipboard data is encrypted using Android Keystore
- 📱 **Floating Bubbles**: Intuitive floating bubble interface for quick access
- 📋 **Extended History**: Keep track of up to 500 clipboard items
- 🎨 **Modern UI**: Built with Jetpack Compose and Material Design 3
- ⚡ **Performance**: Optimized with MVVM architecture and Room database
- 🔧 **Customizable**: Adjustable bubble size, opacity, and behavior modes
- 🧪 **Well Tested**: Comprehensive unit and UI tests

## Architecture

The app follows Clean Architecture principles with MVVM pattern:

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Activity  │  │ ViewModels  │  │  Compose Screens    │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                     Domain Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Models    │  │ Use Cases   │  │   Repositories      │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Database   │  │ Encryption  │  │   Services          │  │
│  │   (Room)    │  │ (SQLCipher) │  │  (Foreground)       │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM + Clean Architecture
- **Database**: Room with SQLCipher encryption
- **Dependency Injection**: Dagger Hilt
- **Security**: Android Keystore, EncryptedSharedPreferences
- **Testing**: JUnit, Espresso, Mockito
- **Build System**: Gradle with Kotlin DSL
- **CI/CD**: GitHub Actions

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24 or later
- JDK 17

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/clipboard-history.git
   cd clipboard-history
   ```

2. Open the project in Android Studio

3. Sync the project with Gradle files

4. Run the app on an emulator or device

### Building

To build the project:

```bash
./gradlew assembleDebug
```

To run tests:

```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Permissions

The app requires the following permissions:

- `SYSTEM_ALERT_WINDOW`: For floating bubble interface
- `FOREGROUND_SERVICE`: For background clipboard monitoring
- `POST_NOTIFICATIONS`: For service notifications (Android 13+)

## Security

- All clipboard data is encrypted using AES-256 encryption
- Keys are stored in Android Keystore for hardware-backed security
- Database is encrypted with SQLCipher
- No sensitive data is logged or transmitted

## Usage

1. **Grant Permissions**: Allow overlay and notification permissions
2. **Start Service**: Tap the play button to start clipboard monitoring
3. **Use Floating Bubbles**: 
   - Empty bubble: Tap to capture current clipboard
   - Full bubbles: Tap to paste content
   - Mode bubble: Toggle between replace and extend modes
4. **Manage History**: View and manage clipboard history in the main screen

## Configuration

The app supports various configuration options:

- **Max History Size**: 10-500 items
- **Auto-delete**: 1-168 hours
- **Bubble Size**: 5 different sizes
- **Bubble Opacity**: 10-100%
- **Clipboard Mode**: Replace or Extend
- **Encryption**: Can be toggled on/off

## Testing

The project includes comprehensive test coverage:

- **Unit Tests**: Business logic and data layer
- **Integration Tests**: Database operations
- **UI Tests**: Compose screens and user interactions
- **Security Tests**: Encryption/decryption functionality

Run all tests:
```bash
./gradlew test connectedAndroidTest
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Material Design 3 for UI components
- Room database for local persistence
- SQLCipher for database encryption
- Jetpack Compose for modern UI development
- Android Keystore for secure key management

## Support

If you encounter any issues or have questions, please [open an issue](https://github.com/yourusername/clipboard-history/issues) on GitHub.
'''

# Create API documentation
api_docs = '''# API Documentation

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
'''

# Write GitHub Actions workflows
with open('.github/workflows/ci-cd.yml', 'w') as f:
    f.write(github_actions_ci)

with open('.github/workflows/pr-checks.yml', 'w') as f:
    f.write(github_actions_pr)

# Write ProGuard rules
with open('app/proguard-rules.pro', 'w') as f:
    f.write(proguard_rules)

# Write documentation
with open('README.md', 'w') as f:
    f.write(readme_md)

with open('docs/API.md', 'w') as f:
    f.write(api_docs)

print("GitHub Actions workflows, ProGuard rules, and documentation created!")