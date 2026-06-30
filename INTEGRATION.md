# SpareTools Integration Guide

This document explains how the Cliphist Android project integrates with the SpareTools ecosystem.

## Overview

Cliphist Android is a Kotlin/Android application for clipboard history management. As an Android/Gradle project, it doesn't use Conan for its own packaging but can integrate with SpareTools when cross-platform tooling or shared protocols are needed.

## SpareTools Ecosystem

SpareTools provides a comprehensive package ecosystem for cross-platform development:

### Core Packages Available

- **sparetools-base/2.0.3**: Foundation utilities and version management
- **sparetools-cpython/3.12.7**: Bundled Python 3.12.7 runtime for consistent environments
- **sparetools-py/1.0.0**: Python utilities for development workflows
- **sparesparrow-protocols/1.0.0**: Shared protocol schemas (FlatBuffers)
- **sparetools-embedded/1.0.0**: Embedded systems utilities

### Available in Cloudsmith

All SpareTools packages are published to Cloudsmith at:
`https://cloudsmith.io/~sparesparrow-conan/repos/sparetools/packages/`

## Integration Points

### Build Tooling

If Cliphist Android needs Python-based build tooling (for code generation, testing, CI/CD), you can use SpareTools:

```bash
# Install Python tooling with consistent environment
conan install sparetools-cpython/3.12.7@sparesparrow/stable

# Use for build scripts, code generation, etc.
./gradlew build  # Uses bundled Python if configured
```

### Protocol Schemas

For shared data protocols between Android and other platforms:

```bash
# Install shared protocol definitions
conan install sparesparrow-protocols/1.0.0@sparesparrow/stable

# Generate Android-compatible code from FlatBuffers schemas
flatc --java schema.fbs
```

### Development Environment

For consistent development environments across team members:

```bash
# Ensure everyone uses the same Python version for tooling
conan install sparetools-cpython/3.12.7@sparesparrow/stable
```

## Cross-Platform Compatibility

SpareTools ensures consistent behavior across platforms:

- **Linux**: Native package management
- **macOS**: Homebrew integration where needed
- **Windows**: MSYS2/MinGW compatibility

## Version Management

SpareTools uses centralized version management via `sparetools-base/2.0.3`. All package versions are synchronized and tested together.

## Development Workflow

### Setting up Cloudsmith Access

```bash
# Configure Cloudsmith remote
conan remote add sparetools https://dl.cloudsmith.io/sparesparrow-conan/sparetools/
conan remote login sparetools <your-api-key>
```

### CI/CD Integration

```yaml
# Example GitHub Actions for Android CI
- name: Setup Build Environment
  run: |
    conan remote add sparetools https://dl.cloudsmith.io/sparesparrow-conan/sparetools/
    conan install sparetools-cpython/3.12.7@sparesparrow/stable

- name: Build APK
  run: ./gradlew assembleRelease
```

## Android-Specific Considerations

### Gradle Integration

If you need to integrate Conan with Gradle for native dependencies:

```kotlin
// build.gradle.kts
plugins {
    id("com.android.application")
    // Add Conan plugin if needed for native libs
}

dependencies {
    // Android dependencies
    implementation("androidx.core:core-ktx:1.12.0")

    // If using native libraries from SpareTools
    // implementation("sparesparrow-protocols") // Via CMake
}
```

### Native Code Integration

For Android NDK projects that might use SpareTools C++ packages:

```cmake
# CMakeLists.txt for Android NDK
find_package(sparesparrow-protocols REQUIRED)
target_link_libraries(your_app sparesparrow-protocols::sparesparrow-protocols)
```

## Support

For questions about SpareTools integration:

1. Check the [SpareTools documentation](https://github.com/sparesparrow/sparetools)
2. Review [Cloudsmith package list](https://cloudsmith.io/~sparesparrow-conan/repos/sparetools/packages/)
3. See [SpareTools version management](https://github.com/sparesparrow/sparetools/blob/main/VERSION_BUMPING.md)

## Related Projects

- **sparetools**: Core ecosystem (https://github.com/sparesparrow/sparetools)
- **esp32-bpm-detector**: Example C++ consumer (https://github.com/sparesparrow/esp32-bpm-detector)
- **mia**: Example Android/desktop consumer (https://github.com/sparesparrow/mia)

