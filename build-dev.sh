#!/bin/bash

# ============================================
# Android Clipboard History - Development Script
# Comprehensive build and development tools
# ============================================

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
USE_DOCKER="${USE_DOCKER:-false}"

# Functions
print_header() {
    echo -e "\n${BLUE}============================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}============================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

# Check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
check_prerequisites() {
    print_info "Checking prerequisites..."
    
    if ! command_exists java; then
        print_error "Java is not installed. Please install Java 17 or later."
        exit 1
    fi
    
    if ! command_exists docker && [[ "$USE_DOCKER" == "true" ]]; then
        print_error "Docker is not installed but USE_DOCKER=true. Please install Docker."
        exit 1
    fi
    
    print_success "Prerequisites check completed"
}

# Setup Android SDK (if available)
setup_android_sdk() {
    if [[ ! -f "local.properties" ]]; then
        if [[ -n "$ANDROID_SDK_ROOT" ]] && [[ -d "$ANDROID_SDK_ROOT" ]]; then
            echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
            print_info "Created local.properties with ANDROID_SDK_ROOT: $ANDROID_SDK_ROOT"
        elif [[ -n "$ANDROID_HOME" ]] && [[ -d "$ANDROID_HOME" ]]; then
            echo "sdk.dir=$ANDROID_HOME" > local.properties
            print_info "Created local.properties with ANDROID_HOME: $ANDROID_HOME"
        else
            print_warning "Android SDK not found. Some commands may fail."
            print_info "Set ANDROID_HOME or ANDROID_SDK_ROOT environment variable"
            print_info "Or use containerized builds with: USE_DOCKER=true $0 docker-build"
        fi
    else
        print_info "local.properties already exists"
    fi
}

# Clean build artifacts
clean() {
    print_header "🧹 Cleaning Build Artifacts"
    ./gradlew clean
    print_success "Clean completed"
}

# Run unit tests
test() {
    print_header "🧪 Running Unit Tests"
    ./gradlew testDebugUnitTest --stacktrace
    print_success "Unit tests completed"
}

# Run instrumentation tests
test_instrumentation() {
    print_header "🤖 Running Instrumentation Tests"
    print_warning "This requires an emulator or connected device"
    ./gradlew connectedDebugAndroidTest --stacktrace
    print_success "Instrumentation tests completed"
}

# Run all tests
test_all() {
    print_header "🧪 Running All Tests"
    ./gradlew testDebugUnitTest connectedDebugAndroidTest --stacktrace
    print_success "All tests completed"
}

# Generate test coverage
coverage() {
    print_header "📊 Generating Test Coverage Report"
    ./gradlew jacocoTestReport
    print_success "Coverage report generated at: app/build/reports/jacoco/jacocoTestReport/html/index.html"
    
    # Try to open in browser if available
    if command_exists xdg-open; then
        xdg-open app/build/reports/jacoco/jacocoTestReport/html/index.html 2>/dev/null || true
    fi
}

# Run lint checks
lint() {
    print_header "🐛 Running Android Lint"
    ./gradlew lintDebug --stacktrace
    print_success "Lint checks completed"
    print_info "Reports available at: app/build/reports/lint/"
}

# Format code with ktlint
format() {
    print_header "✨ Formatting Code with ktlint"
    ./gradlew ktlintFormat
    print_success "Code formatting completed"
}

# Run code quality checks
quality() {
    print_header "🔍 Running Code Quality Checks"
    ./gradlew ktlintCheck detekt lintDebug --continue
    print_success "Code quality checks completed"
    print_info "Review reports in app/build/reports/"
}

# Build debug APK
build_debug() {
    print_header "🏗️  Building Debug APK"
    ./gradlew assembleDebug --stacktrace
    
    # Find and display built APKs
    print_info "Built APKs:"
    find app/build/outputs/apk/debug -name "*.apk" -type f -exec ls -lh {} \; 2>/dev/null | while read -r line; do
        echo "  📱 $line"
    done
    
    print_success "Debug APK built at: app/build/outputs/apk/debug/app-debug.apk"
}

# Build release APK
build_release() {
    print_header "🏗️  Building Release APK"
    
    # Check if keystore exists
    if [[ -f "keystore.jks" ]]; then
        print_info "Using existing keystore..."
        ./gradlew assembleRelease --stacktrace
        print_success "Release APK built at: app/build/outputs/apk/release/app-release.apk"
    else
        print_warning "No keystore found. Creating temporary keystore for testing..."
        keytool -genkey -v -keystore keystore.jks -keyalg RSA -keysize 2048 \
            -validity 10000 -alias key -storepass password -keypass password \
            -dname "CN=Test, OU=Test, O=Test, L=Test, S=Test, C=US"
        ./gradlew assembleRelease --stacktrace
        print_success "Release APK built with temporary keystore"
        print_warning "Remember to use a proper keystore for production!"
    fi
}

# Build both debug and release
build_both() {
    build_debug
    build_release
}

# Build with Docker
docker_build() {
    if ! command_exists docker; then
        print_error "Docker is not installed. Please install Docker to use containerized builds."
        exit 1
    fi
    
    print_header "🐳 Building with Docker"
    docker-compose up android-builder
    print_success "Docker build completed"
}

# Run tests with Docker
docker_test() {
    if ! command_exists docker; then
        print_error "Docker is not installed."
        exit 1
    fi
    
    print_header "🐳 Running Tests with Docker"
    docker-compose up android-test
    print_success "Docker tests completed"
}

# Run lint with Docker
docker_lint() {
    if ! command_exists docker; then
        print_error "Docker is not installed."
        exit 1
    fi
    
    print_header "🐳 Running Lint with Docker"
    docker-compose up android-lint
    print_success "Docker lint completed"
}

# Check for dependency updates
dependencies() {
    print_header "📦 Checking for Dependency Updates"
    ./gradlew dependencyUpdates
    print_success "Dependency check completed"
    print_info "Review reports in build/dependencyUpdates/"
}

# Run security scan
security() {
    print_header "🔒 Running Security Scan"
    
    # Check if security scan script exists
    if [[ -f "scripts/security-scan.sh" ]]; then
        bash scripts/security-scan.sh all
    else
        print_warning "Security scan script not found, running basic checks..."
        ./gradlew dependencyCheckAnalyze --stacktrace || true
    fi
    
    print_success "Security scan completed"
    print_info "Review reports in build/reports/"
}

# Generate build info
generate_build_info() {
    print_header "📊 Generating Build Information"
    
    local build_info_file="build-info.json"
    local version_name=$(grep 'versionName' app/build.gradle | sed 's/.*versionName "\(.*\)"/\1/' || echo "unknown")
    local version_code=$(grep 'versionCode' app/build.gradle | sed 's/.*versionCode \(.*\)/\1/' || echo "0")
    local git_commit=$(git rev-parse HEAD 2>/dev/null || echo "unknown")
    local git_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
    local build_date=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    
    cat > "$build_info_file" << EOF
{
  "version": {
    "name": "$version_name",
    "code": $version_code
  },
  "git": {
    "commit": "$git_commit",
    "branch": "$git_branch"
  },
  "build": {
    "date": "$build_date",
    "docker": $USE_DOCKER
  }
}
EOF
    
    print_success "Build info saved to $build_info_file"
    cat "$build_info_file"
}

# Setup development environment
setup() {
    print_header "⚙️  Setting Up Development Environment"
    check_prerequisites
    setup_android_sdk
    
    # Make gradlew executable
    chmod +x ./gradlew
    
    print_success "Development environment setup completed"
}

# Run full CI pipeline locally
ci() {
    print_header "🚀 Running Full CI Pipeline"
    
    setup
    clean
    quality
    test || print_warning "Some tests failed but continuing..."
    coverage || print_warning "Coverage generation failed but continuing..."
    build_debug
    security || print_warning "Security scan had issues but continuing..."
    generate_build_info
    
    print_header "🎉 Full CI Pipeline Completed!"
}

# Show help
show_help() {
    cat << 'EOF'
Android Clipboard History - Development Script

Usage: ./build-dev.sh [COMMAND]

COMMANDS:
  setup           Setup development environment and prerequisites
  clean           Clean build artifacts
  
  test            Run unit tests
  test-ui         Run instrumentation tests (requires emulator/device)
  test-all        Run all tests
  coverage        Generate test coverage report
  
  lint            Run Android lint checks
  format          Format code with ktlint
  quality         Run all code quality checks (ktlint, detekt, lint)
  
  build           Build debug APK (alias: build-debug)
  build-debug     Build debug APK
  build-release   Build release APK
  build-both      Build both debug and release APKs
  
  docker-build    Build with Docker
  docker-test     Run tests with Docker
  docker-lint     Run lint with Docker
  
  dependencies    Check for dependency updates
  security        Run security scan
  
  info            Generate build information
  ci              Run full CI pipeline locally
  
  help            Show this help message

ENVIRONMENT VARIABLES:
  ANDROID_HOME        Path to Android SDK
  ANDROID_SDK_ROOT    Path to Android SDK (alternative)
  USE_DOCKER=true     Use Docker for builds

EXAMPLES:
  ./build-dev.sh setup          # First time setup
  ./build-dev.sh build          # Build debug APK
  ./build-dev.sh quality        # Run all quality checks
  ./build-dev.sh ci             # Run full CI pipeline
  USE_DOCKER=true ./build-dev.sh docker-build

For more information, see README.md or CI-CD.md
EOF
}

# Main script logic
main() {
    cd "$PROJECT_ROOT"
    
    case "${1:-help}" in
        setup)
            setup
            ;;
        clean)
            clean
            ;;
        test)
            test
            ;;
        test-ui|test-instrumentation)
            test_instrumentation
            ;;
        test-all)
            test_all
            ;;
        coverage)
            coverage
            ;;
        lint)
            lint
            ;;
        format)
            format
            ;;
        quality)
            quality
            ;;
        build|build-debug)
            build_debug
            ;;
        build-release)
            build_release
            ;;
        build-both)
            build_both
            ;;
        docker-build)
            docker_build
            ;;
        docker-test)
            docker_test
            ;;
        docker-lint)
            docker_lint
            ;;
        dependencies|deps)
            dependencies
            ;;
        security)
            security
            ;;
        info|build-info)
            generate_build_info
            ;;
        ci)
            ci
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "Unknown command: $1"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
