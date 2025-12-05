# Android Development Consistency Guide

## Executive Summary

This document provides recommendations for maintaining consistent software development practices, CI/CD pipelines, frameworks, and SDKs across the `cliphist-android` and `ai-servis/android` projects, following industry best practices and latest Android development guidelines.

---

## 1. Current State Analysis

### 1.1 Project Comparison Matrix

| Aspect | cliphist-android | ai-servis/android | Recommendation |
|--------|------------------|-------------------|----------------|
| **Gradle Version** | 8.2 | 8.4 | Standardize to 8.4+ |
| **AGP Version** | 8.2.2 | 8.2.2 | ✅ Consistent |
| **Kotlin Version** | 1.9.22 | 1.9.22 | ✅ Consistent (upgrade to 2.0+ later) |
| **Compose BOM** | 2024.04.01 | 2024.04.01 | ✅ Consistent |
| **Compose Compiler** | 1.5.8 | 1.5.8 | ✅ Consistent |
| **Target SDK** | 34 | 34 | ✅ Consistent (plan for 35) |
| **Min SDK** | 24 | 24 | ✅ Consistent |
| **JDK Target** | 17 | 17 | ✅ Consistent |
| **Hilt** | 2.48.1 | 2.48.1 | ✅ Consistent |
| **Room** | 2.6.1 | 2.6.1 | ✅ Consistent |
| **ktlint** | ✅ (1.0.1) | ❌ Missing | Add to ai-servis |
| **detekt** | ✅ (1.23.4) | ❌ Missing | Add to ai-servis |
| **JaCoCo** | ✅ (0.8.8) | ❌ Missing | Add to ai-servis |
| **Security Scan** | ✅ Dependency-Check | ❌ Missing | Add to ai-servis |
| **Version Catalog** | ❌ Missing | ❌ Missing | Implement in both |

### 1.2 CI/CD Comparison

| Feature | cliphist-android | ai-servis/android |
|---------|------------------|-------------------|
| Gradle Wrapper Validation | ✅ | ❌ |
| Quality Checks (ktlint/detekt) | ✅ | ❌ |
| Security Scanning | ✅ | ❌ |
| Unit Tests | ✅ | ✅ |
| Instrumented Tests | ✅ (multiple API levels) | ✅ (single API level) |
| Code Coverage | ✅ | ❌ |
| Build Matrix | ✅ (debug + release) | ✅ |
| Auto Release | ✅ | ✅ |
| Daily Health Check | ✅ | ❌ |
| Concurrency Control | ✅ | ❌ |

---

## 2. Recommended Shared Configuration

### 2.1 Gradle Version Catalog (libs.versions.toml)

Create a shared version catalog that both projects can reference:

```toml
# gradle/libs.versions.toml
[versions]
# Build Tools
agp = "8.3.2"
kotlin = "1.9.24"
ksp = "1.9.24-1.0.20"

# Compose
compose-bom = "2024.06.00"
compose-compiler = "1.5.14"

# AndroidX Core
core-ktx = "1.13.1"
lifecycle = "2.8.2"
activity-compose = "1.9.0"
navigation-compose = "2.7.7"

# Architecture
room = "2.6.1"
hilt = "2.51.1"
hilt-navigation-compose = "1.2.0"

# Testing
junit = "4.13.2"
robolectric = "4.12.2"
mockito = "5.11.0"
mockk = "1.13.10"
turbine = "1.1.0"
coroutines-test = "1.8.1"

# Quality
ktlint = "1.3.0"
detekt = "1.23.6"
jacoco = "0.8.12"

[libraries]
# Core
core-ktx = { module = "androidx.core:core-ktx", version.ref = "core-ktx" }
lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activity-compose" }
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation-compose" }

# Compose BOM
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }
compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-material-icons = { module = "androidx.compose.material:material-icons-extended" }

# Room
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
room-testing = { module = "androidx.room:room-testing", version.ref = "room" }

# Hilt
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hilt-navigation-compose" }
hilt-android-testing = { module = "com.google.dagger:hilt-android-testing", version.ref = "hilt" }

# Testing
junit = { module = "junit:junit", version.ref = "junit" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
mockito-core = { module = "org.mockito:mockito-core", version.ref = "mockito" }
mockito-kotlin = { module = "org.mockito.kotlin:mockito-kotlin", version = "5.3.1" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines-test" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version = "12.1.1" }
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
```

### 2.2 Shared detekt Configuration

```yaml
# detekt.yml (standardized for both projects)
build:
  maxIssues: 0
  excludeCorrectable: false
  weights:
    complexity: 2
    LongParameterList: 1
    style: 1
    comments: 1

config:
  validation: true
  warningsAsErrors: false

complexity:
  LongMethod:
    threshold: 60
  LongParameterList:
    functionThreshold: 6
    constructorThreshold: 8
  ComplexCondition:
    threshold: 4
  TooManyFunctions:
    active: true
    thresholdInFiles: 15
    thresholdInClasses: 15

naming:
  FunctionNaming:
    functionPattern: '([a-z][a-zA-Z0-9]*)|(`.*`)'
  TopLevelPropertyNaming:
    constantPattern: '[A-Z][_A-Za-z0-9]*'

style:
  MaxLineLength:
    maxLineLength: 120
  WildcardImport:
    active: true
    excludeImports:
      - 'kotlinx.coroutines.*'
      - 'androidx.compose.*'

formatting:
  active: true
  Indentation:
    indentSize: 4
```

---

## 3. CI/CD Standardization

### 3.1 Shared Reusable Workflow Template

Create reusable workflows that both projects can reference:

```yaml
# .github/workflows/reusable/android-build.yml
name: Reusable Android Build

on:
  workflow_call:
    inputs:
      java-version:
        description: 'JDK version'
        required: false
        default: '17'
        type: string
      android-compile-sdk:
        description: 'Android compile SDK version'
        required: false
        default: '34'
        type: string
      build-type:
        description: 'Build type (debug/release/both)'
        required: false
        default: 'both'
        type: string
      run-tests:
        description: 'Run tests'
        required: false
        default: true
        type: boolean
      run-quality-checks:
        description: 'Run quality checks'
        required: false
        default: true
        type: boolean
    secrets:
      KEYSTORE_BASE64:
        required: false
      KEYSTORE_PASSWORD:
        required: false
      KEY_ALIAS:
        required: false
      KEY_PASSWORD:
        required: false

env:
  GRADLE_OPTS: -Dorg.gradle.daemon=false -Dorg.gradle.workers.max=2

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: gradle/wrapper-validation-action@v3

  quality:
    needs: validate
    if: inputs.run-quality-checks
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: ${{ inputs.java-version }}
      - uses: gradle/actions/setup-gradle@v4
      - uses: android-actions/setup-android@v3
      
      - name: Run ktlint
        run: ./gradlew ktlintCheck --continue
        continue-on-error: true
        
      - name: Run detekt
        run: ./gradlew detekt --continue
        continue-on-error: true
        
      - name: Run Android Lint
        run: ./gradlew lintDebug --continue
        continue-on-error: true
        
      - uses: actions/upload-artifact@v4
        with:
          name: quality-reports
          path: app/build/reports/

  test:
    needs: validate
    if: inputs.run-tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: ${{ inputs.java-version }}
      - uses: gradle/actions/setup-gradle@v4
      - uses: android-actions/setup-android@v3
      
      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest --stacktrace
        
      - name: Generate Coverage Report
        run: ./gradlew jacocoTestReport
        continue-on-error: true
        
      - uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: |
            app/build/reports/tests/
            app/build/reports/jacoco/

  build:
    needs: [validate, quality, test]
    if: always() && needs.validate.result == 'success'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: ${{ inputs.java-version }}
      - uses: gradle/actions/setup-gradle@v4
      - uses: android-actions/setup-android@v3
      
      - name: Build Debug
        if: contains(fromJSON('["debug", "both"]'), inputs.build-type)
        run: ./gradlew assembleDebug
        
      - name: Build Release
        if: contains(fromJSON('["release", "both"]'), inputs.build-type)
        run: ./gradlew assembleRelease
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
```

### 3.2 Recommended CI/CD Features for Both Projects

| Feature | Priority | Description |
|---------|----------|-------------|
| Gradle Wrapper Validation | High | Security best practice |
| Concurrency Control | High | Prevent duplicate builds |
| Quality Gates | High | ktlint, detekt, lint |
| Unit Tests | High | With coverage reporting |
| Instrumented Tests | Medium | On multiple API levels |
| Security Scanning | Medium | Dependency-Check or Snyk |
| Build Matrix | Medium | Debug + Release |
| Artifact Caching | Medium | Speed up builds |
| Auto-Release | Low | Tag-based releases |

---

## 4. Shared Resources via Sparetools

### 4.1 Potential Shared Components

The `sparetools` repository can provide:

1. **OpenSSL Libraries** - For apps requiring secure communications
2. **Security Utilities** - Encryption, hashing, certificate handling
3. **Build Tooling** - Common Conan packages

### 4.2 Integration Pattern for Android

```gradle
// settings.gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://dl.cloudsmith.io/public/sparesparrow-conan/openssl-conan/maven/")
            // For private packages:
            // credentials {
            //     username = providers.gradleProperty("cloudsmith.user").get()
            //     password = providers.gradleProperty("cloudsmith.apiKey").get()
            // }
        }
    }
}
```

### 4.3 GitHub Packages Integration

```gradle
// For consuming shared Android libraries from GitHub Packages
maven {
    name = "GitHubPackages"
    url = uri("https://maven.pkg.github.com/sparesparrow/sparetools")
    credentials {
        username = providers.environmentVariable("GITHUB_ACTOR").getOrElse("")
        password = providers.environmentVariable("GITHUB_TOKEN").getOrElse("")
    }
}
```

---

## 5. Action Items

### 5.1 Immediate (Week 1-2)

- [ ] **Upgrade Gradle**: Standardize both projects to Gradle 8.4+
- [ ] **Add quality tools to ai-servis**: Add ktlint and detekt plugins
- [ ] **Add JaCoCo to ai-servis**: Enable test coverage reporting
- [ ] **Standardize detekt.yml**: Use same config in both projects

### 5.2 Short-term (Month 1)

- [ ] **Implement Version Catalog**: Migrate both projects to `libs.versions.toml`
- [ ] **Enhance ai-servis CI/CD**: Add security scanning, concurrency control
- [ ] **Create reusable workflows**: Centralize common CI/CD patterns
- [ ] **Add instrumented test matrix**: Test on multiple API levels

### 5.3 Medium-term (Quarter 1)

- [ ] **Migrate to KSP**: Replace kapt with KSP for better build performance
- [ ] **Upgrade to Kotlin 2.0**: When stable, adopt Kotlin 2.0 + new Compose compiler
- [ ] **Create shared library**: Extract common utilities to a shared Android library
- [ ] **Integrate sparetools**: Consume security packages from Cloudsmith/GitHub Packages

---

## 6. Best Practices Checklist

### 6.1 Code Quality
- [ ] ktlint for code formatting
- [ ] detekt for static analysis
- [ ] Android Lint for platform-specific issues
- [ ] Baseline files for gradual adoption

### 6.2 Testing
- [ ] Unit tests with JUnit/MockK
- [ ] Coroutine tests with Turbine
- [ ] UI tests with Compose Testing
- [ ] Coverage reports with JaCoCo
- [ ] Instrumented tests on multiple API levels

### 6.3 Security
- [ ] Dependency vulnerability scanning
- [ ] Secrets management via GitHub Secrets
- [ ] ProGuard/R8 for release builds
- [ ] Keystore protection

### 6.4 CI/CD
- [ ] Gradle wrapper validation
- [ ] Build caching
- [ ] Parallel job execution
- [ ] Artifact retention policies
- [ ] Branch protection rules

---

## 7. Technology Upgrade Path

### 7.1 2024 Q4 Targets
| Technology | Current | Target | Notes |
|------------|---------|--------|-------|
| Gradle | 8.2-8.4 | 8.6+ | Better K2 support |
| AGP | 8.2.2 | 8.4+ | New features |
| Kotlin | 1.9.22 | 1.9.24 | Bug fixes |
| Compose BOM | 2024.04.01 | 2024.06.00 | Material3 updates |

### 7.2 2025 H1 Targets
| Technology | Current | Target | Notes |
|------------|---------|--------|-------|
| Kotlin | 1.9.x | 2.0+ | K2 compiler, performance |
| Compose Compiler | 1.5.x | 2.0+ | New Kotlin integration |
| Target SDK | 34 | 35 | Android 15 support |
| KSP | kapt | KSP | Faster annotation processing |

---

## Appendix A: File Structure Comparison

### cliphist-android
```
app/src/main/java/com/clipboardhistory/
├── ClipboardHistoryApplication.kt
├── data/
│   ├── database/
│   ├── encryption/
│   └── repository/
├── di/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── presentation/
│   ├── MainActivity.kt
│   ├── receivers/
│   ├── services/
│   ├── ui/
│   └── viewmodels/
└── utils/
```

### ai-servis/android
```
app/src/main/java/cz/aiservis/app/
├── AIServisApplication.kt
├── core/
│   ├── background/
│   ├── camera/
│   ├── messaging/
│   ├── networking/
│   ├── rules/
│   ├── security/
│   ├── storage/
│   └── voice/
├── data/
│   ├── db/
│   ├── remote/
│   └── repository/
├── di/
├── features/
├── ui/
└── utils/
```

**Recommendation**: Standardize on a consistent package structure:
- `core/` for utilities and services
- `data/` for data layer (database, network, repository)
- `domain/` for business logic (use cases, models)
- `presentation/` or `ui/` for UI layer
- `di/` for dependency injection modules

---

## Appendix B: Memory Integration

This guide should be stored in the knowledge graph with relationships to:
- cliphist-android project
- ai-servis/android project
- sparetools repository
- CI/CD practices
- Android best practices

---

*Last Updated: December 4, 2024*
*Version: 1.0.0*
