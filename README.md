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

### Modern CI/CD Features
- **🚀 Automated Builds**: Multi-stage builds with containerized environments
- **🧪 Quality Assurance**: Comprehensive testing with 100% coverage target
- **🔒 Security Scanning**: CodeQL analysis and dependency vulnerability checks
- **📦 Release Management**: Automated versioning and GitHub releases
- **📊 Monitoring**: Build health tracking and performance metrics
- **🐳 Containerized Builds**: Docker-based Android SDK environment

### GitHub Actions Workflows
- **Main CI/CD Pipeline**: Complete build, test, and release automation
- **Pull Request Checks**: Code quality and formatting validation
- **Build Monitoring**: Health tracking and automated alerting
- **Security Monitoring**: Vulnerability scanning and dependency updates

### Download latest APK

You can download the latest tagged build from GitHub Releases:

- Latest release: https://github.com/sparesparrow/cliphist-android/releases/latest
- This build: https://github.com/sparesparrow/cliphist-android/releases/tag/v1.0.0-ci1

### Local Development

```bash
# Quick setup
./build-dev.sh setup

# Build debug APK
./build-dev.sh build

# Run tests
./build-dev.sh test

# Run code quality checks
./build-dev.sh quality

# Run full CI pipeline locally
./build-dev.sh ci

# Build with Docker (recommended)
USE_DOCKER=true ./build-dev.sh docker-build
```

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
- **Java 17+**: Required for building
- **Docker & Docker Compose**: For containerized builds (recommended)
- **Git**: For version control
- **Android Studio**: For local development (optional)

### Quick Setup
```bash
# Clone the repository
git clone https://github.com/yourusername/cliphist-android.git
cd cliphist-android

# Setup and build
./build-dev.sh setup
./build-dev.sh build
```

### Development Commands
```bash
# Run tests
./build-dev.sh test

# Generate coverage
./build-dev.sh coverage

# Code quality checks
./build-dev.sh quality

# Format code
./build-dev.sh format

# Build with Docker
./build-dev.sh docker-build

# Full CI pipeline
./build-dev.sh ci

# Show all available commands
./build-dev.sh help
```

### Key Dependencies
- **Jetpack Compose**: Modern UI toolkit
- **Room**: Database persistence with SQLCipher encryption
- **Hilt**: Dependency injection
- **Coroutines**: Asynchronous programming
- **Material Design 3**: Design system
- **Security**: Encrypted storage and biometric authentication

## 🆕 Recent Improvements (October 2025)

### CI/CD Enhancements
- **✅ Unified Pipeline**: Consolidated 4 separate workflows into single comprehensive pipeline
- **✅ Gradle Wrapper Validation**: Security validation of build wrapper
- **✅ Containerized Builds**: Docker-based Android SDK environment
- **✅ Dynamic Versioning**: Git-based automatic version management
- **✅ Multi-API Testing**: Testing on API levels 24, 29, and 34
- **✅ Security Scanning**: Dependency vulnerability checks
- **✅ Build Monitoring**: Health tracking with daily scheduled checks
- **✅ Performance Optimization**: Caching and parallel execution
- **✅ Automated Releases**: Automatic GitHub release creation with APKs

### Testing & Quality
- **✅ Comprehensive Test Suite**: Unit and instrumentation tests with coverage reporting
- **✅ Code Quality**: ktlint, detekt, and Android lint integration
- **✅ Continuous Monitoring**: Automated quality gates in CI/CD
- **✅ Test Matrix**: Testing across multiple Android API levels

### Developer Experience
- **✅ Consolidated Build Script**: Single `build-dev.sh` for all development tasks
- **✅ Comprehensive Commands**: Setup, build, test, quality, security, and more
- **✅ Documentation**: Detailed guides including new IMPROVEMENTS.md
- **✅ Docker Support**: Optional containerized development environment
- **✅ Fast Feedback**: Quick local validation before pushing

### Documentation
- **✅ Cleaned Up Duplicates**: Removed duplicate API.md and build scripts
- **✅ Single Source of Truth**: Comprehensive docs in appropriate locations
- **✅ Improvements Guide**: New IMPROVEMENTS.md with refactoring recommendations
- **✅ Updated Workflows**: CI/CD documentation reflects new unified pipeline

### Security & Performance
- **✅ Encrypted Storage**: SQLCipher database encryption
- **✅ Biometric Authentication**: Secure access control
- **✅ Code Obfuscation**: ProGuard/R8 optimization
- **✅ Resource Optimization**: Shrinking and minification
- **✅ Automated Security Scans**: Integrated into CI/CD pipeline

## 📚 Documentation

- **[CI/CD Guide](CI-CD.md)**: Complete pipeline documentation
- **[Deployment Guide](DEPLOYMENT.md)**: Production deployment instructions
- **[API Documentation](docs/API.md)**: Technical API reference
- **[Improvements Guide](IMPROVEMENTS.md)**: Refactoring recommendations and best practices

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
