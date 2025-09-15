# Clipboard History - Android Extended Clipboard Manager

A comprehensive Android clipboard management application with floating bubbles, encryption, and advanced features.

## ✨ Features

### Core Functionality
- **Clipboard Monitoring**: Automatic capture of copied text and content
- **Floating Bubbles**: Quick access to clipboard history with customizable bubbles
- **Encryption**: Secure storage of clipboard data with AES encryption
- **History Management**: Configurable history size with automatic cleanup
- **Multiple Content Types**: Support for text, images, URLs, and files

### Floating Bubble System
- **Multiple Bubble Types**: Choose from Circle, Cube, Hexagon, and Square shapes
- **Cube Flash Feature**: Cube bubbles flash content preview when tapped
- **Drag-and-Drop Actions**: Drag bubbles to highlighted areas for content editing
- **Theme Support**: Multiple color themes with Material Design 3
- **Drag & Drop**: Reposition bubbles anywhere on screen
- **Opacity Control**: Adjustable transparency levels

### Bubble Types
| Type | Description | Special Features |
|------|-------------|------------------|
| **Circle** | Classic circular bubbles | Default shape, smooth animations |
| **Cube** | 3D cube with flash preview | Flashes content when tapped, 3D effect |
| **Hexagon** | Hexagonal bubbles | Geometric design, modern look |
| **Square** | Rounded square bubbles | Clean, minimalist appearance |

### Advanced Features
- **Service Persistence**: Automatic restart and recovery mechanisms
- **Battery Optimization**: Smart power management
- **Permission Management**: Guided setup for required permissions
- **Error Handling**: Graceful degradation and user feedback
- **Accessibility**: Full accessibility compliance

## 🎨 Bubble Types & Drag-and-Drop Actions

### Cube Bubble Flash Feature
The cube bubble type includes a special flash functionality:
- **Content Preview**: When tapped, cube bubbles flash the clipboard content for 1 second
- **3D Effect**: Visual depth with lighter top face and darker side face
- **Smooth Animation**: Alpha-based flash effect with easing
- **Content Display**: Shows up to 20 characters of content during flash

### Enhanced Drag-and-Drop Action Areas
When dragging bubbles to screen edges, smart action areas appear with context-aware suggestions:

**Edge-Based Activation:**
- **Left Edge**: Horizontal action bar with smart suggestions
- **Right Edge**: Horizontal action bar with smart suggestions  
- **Top Edge**: Vertical action bar with smart suggestions
- **Bottom Edge**: Vertical action bar with smart suggestions

**Smart Action Suggestions:**
- **URLs**: Open Link, Share Link, Bookmark, Copy URL
- **Phone Numbers**: Call Number, Send SMS, Add Contact, Copy Number
- **Email Addresses**: Send Email, Add Contact, Copy Email
- **Addresses**: Open Maps, Get Directions, Share Location, Copy Address
- **Code**: Run Code, Share Code, Format Code, Copy Code
- **Text**: Copy Text, Share Text, Search Text, Translate

**Visual Features:**
- **Edge Glow**: Blue glow animation when approaching edges
- **Smart Positioning**: Action areas positioned based on drag direction
- **One-Handed Operation**: Thumb-friendly positioning and gestures
- **Context Awareness**: Actions change based on content type

### Bubble Type Selection
Users can select their preferred bubble type in Settings:
1. Open the app settings
2. Navigate to "Bubble Type" section
3. Choose from Circle, Cube, Hexagon, or Square
4. Changes apply immediately to all floating bubbles

### Enhanced Content Editing Workflow
1. **Start Dragging**: Drag any bubble with content
2. **Approach Edge**: Drag the bubble toward any screen edge
3. **Edge Glow**: Blue glow appears when approaching edge threshold
4. **Smart Actions Appear**: Context-aware action areas appear with relevant suggestions
5. **Drop on Action**: Drop the bubble on the desired smart action
6. **Smart Processing**: Content is processed according to the action (e.g., open link, call number)
7. **Areas Hide**: Action areas automatically disappear after use

**Example Workflows:**
- **URL**: Drag to edge → "Open Link" → Browser opens automatically
- **Phone Number**: Drag to edge → "Call Number" → Dialer opens with number
- **Email**: Drag to edge → "Send Email" → Email app opens with address
- **Address**: Drag to edge → "Open Maps" → Maps app opens with location

### Visual Customization
Each bubble type supports:
- **Theme Colors**: Different colors for empty, storing, replace, append, and prepend states
- **Opacity Control**: Adjustable transparency (10-100%)
- **Size Options**: 5 different size levels
- **Action Areas**: Highlighted areas for drag-and-drop content editing

## 🏗️ Architecture

### Clean Architecture
- **Domain Layer**: Business logic and use cases
- **Data Layer**: Repository pattern with Room database
- **Presentation Layer**: MVVM with Jetpack Compose
- **Service Layer**: Foreground services for clipboard monitoring

### Key Components
- **FloatingBubbleService**: Manages floating bubble lifecycle
- **ClipboardService**: Monitors clipboard changes
- **BubbleView**: Custom view with multiple shape support
- **EncryptionManager**: Handles data encryption/decryption

## ⚙️ Configuration

### Clipboard Settings
| Setting | Range | Default | Description |
|---------|-------|---------|-------------|
| **Max History Size** | 10-500 | 100 | Maximum items to store |
| **Auto-delete After** | 1-168h | 24h | Automatic cleanup timing |
| **Bubble Size** | 5 sizes | Medium | Floating bubble dimensions |
| **Bubble Opacity** | 10-100% | 80% | Bubble transparency level |
| **Bubble Type** | 4 types | Circle | Bubble shape selection |
| **Clipboard Mode** | Replace/Extend | Replace | Content handling behavior |
| **Encryption** | On/Off | On | Security feature toggle |

### Permissions Required
| Permission | Purpose | Required |
|------------|---------|----------|
| `SYSTEM_ALERT_WINDOW` | Floating bubble interface | Yes |
| `FOREGROUND_SERVICE` | Background clipboard monitoring | Yes |
| `POST_NOTIFICATIONS` | Service notifications (Android 13+) | Yes |

## 🧪 Testing Strategy

### Test Coverage
- **Unit Tests**: 100% coverage for business logic
- **Integration Tests**: Database and repository operations
- **UI Tests**: Compose screen interactions
- **Security Tests**: Encryption/decryption validation

### Test Structure
```
app/src/test/           # Unit tests
app/src/androidTest/    # Instrumentation tests
app/src/testDebug/      # Debug-specific tests
app/src/testRelease/    # Release-specific tests
```

### Running Tests
```bash
# Full test suite
./gradlew test connectedAndroidTest

# Specific test categories
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest

# Test with coverage report
./gradlew testDebugUnitTestCoverage
```

## 🔄 CI/CD Pipeline

### GitHub Actions Workflows
- **Android CI**: Automated build and testing
- **Security Scan**: Dependency vulnerability scanning
- **Code Quality**: Linting and code analysis
- **Release Management**: Automated versioning and deployment

## 📱 Screenshots

### Main Features
- Floating bubbles with different shapes
- Settings dialog with bubble type selection
- Clipboard history with encryption indicators
- Service status monitoring

### Bubble Types
- **Circle**: Classic circular design
- **Cube**: 3D cube with flash preview
- **Hexagon**: Geometric hexagonal shape
- **Square**: Rounded square design

## 🔧 Development

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 34+
- Kotlin 1.9+
- Gradle 8.0+

### Setup
```bash
# Clone the repository
git clone https://github.com/yourusername/cliphist-android.git

# Open in Android Studio
cd cliphist-android
./gradlew build
```

### Key Dependencies
- **Jetpack Compose**: Modern UI toolkit
- **Room**: Database persistence
- **Hilt**: Dependency injection
- **Coroutines**: Asynchronous programming
- **Material Design 3**: Design system

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📞 Support

For support and questions:
- Create an issue on GitHub
- Check the documentation
- Review the API documentation

---

**Note**: This app requires overlay permissions to display floating bubbles. Please grant the necessary permissions when prompted.
