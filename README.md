# Android Extended Clipboard History

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
