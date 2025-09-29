#!/bin/bash

# Clipboard History Android App - Build Script
# This script provides various build and development commands

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    if ! command_exists java; then
        print_error "Java is not installed. Please install Java 17 or later."
        exit 1
    fi
    
    if ! command_exists docker; then
        print_warning "Docker is not installed. Containerized builds will not be available."
    fi
    
    if ! command_exists docker-compose; then
        print_warning "Docker Compose is not installed. Containerized builds will not be available."
    fi
    
    print_success "Prerequisites check completed"
}

# Function to setup Android SDK (if available)
setup_android_sdk() {
    if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
        print_status "Android SDK found at: $ANDROID_HOME"
        echo "sdk.dir=$ANDROID_HOME" > local.properties
        print_success "local.properties created"
    elif [ -n "$ANDROID_SDK_ROOT" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
        print_status "Android SDK found at: $ANDROID_SDK_ROOT"
        echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
        print_success "local.properties created"
    else
        print_warning "Android SDK not found. Please set ANDROID_HOME or ANDROID_SDK_ROOT environment variable."
        print_status "You can still use containerized builds with: ./build.sh docker-build"
    fi
}

# Function to clean build artifacts
clean() {
    print_status "Cleaning build artifacts..."
    ./gradlew clean
    print_success "Clean completed"
}

# Function to run unit tests
test() {
    print_status "Running unit tests..."
    ./gradlew testDebugUnitTest
    print_success "Unit tests completed"
}

# Function to run instrumentation tests
test_instrumentation() {
    print_status "Running instrumentation tests..."
    ./gradlew connectedDebugAndroidTest
    print_success "Instrumentation tests completed"
}

# Function to run all tests
test_all() {
    print_status "Running all tests..."
    ./gradlew testDebugUnitTest connectedDebugAndroidTest
    print_success "All tests completed"
}

# Function to generate test coverage
coverage() {
    print_status "Generating test coverage report..."
    ./gradlew jacocoTestReport
    print_success "Coverage report generated at: app/build/reports/jacoco/jacocoTestReport/html/index.html"
}

# Function to run lint
lint() {
    print_status "Running lint checks..."
    ./gradlew lintDebug
    print_success "Lint checks completed"
}

# Function to run code quality checks
quality() {
    print_status "Running code quality checks..."
    ./gradlew ktlintCheck detekt lintDebug
    print_success "Code quality checks completed"
}

# Function to build debug APK
build_debug() {
    print_status "Building debug APK..."
    ./gradlew assembleDebug
    print_success "Debug APK built at: app/build/outputs/apk/debug/app-debug.apk"
}

# Function to build release APK
build_release() {
    print_status "Building release APK..."
    
    # Check if keystore exists
    if [ -f "keystore.jks" ]; then
        print_status "Using existing keystore..."
        ./gradlew assembleRelease
        print_success "Release APK built at: app/build/outputs/apk/release/app-release.apk"
    else
        print_warning "No keystore found. Creating temporary keystore for testing..."
        keytool -genkey -v -keystore keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias key -storepass password -keypass password -dname "CN=Test, OU=Test, O=Test, L=Test, S=Test, C=US"
        ./gradlew assembleRelease
        print_success "Release APK built with temporary keystore"
    fi
}

# Function to build with Docker
docker_build() {
    if ! command_exists docker; then
        print_error "Docker is not installed. Please install Docker to use containerized builds."
        exit 1
    fi
    
    print_status "Building with Docker..."
    docker-compose up android-builder
    print_success "Docker build completed"
}

# Function to run tests with Docker
docker_test() {
    if ! command_exists docker; then
        print_error "Docker is not installed. Please install Docker to use containerized builds."
        exit 1
    fi
    
    print_status "Running tests with Docker..."
    docker-compose up android-test
    print_success "Docker tests completed"
}

# Function to run lint with Docker
docker_lint() {
    if ! command_exists docker; then
        print_error "Docker is not installed. Please install Docker to use containerized builds."
        exit 1
    fi
    
    print_status "Running lint with Docker..."
    docker-compose up android-lint
    print_success "Docker lint completed"
}

# Function to check for dependency updates
dependencies() {
    print_status "Checking for dependency updates..."
    ./gradlew dependencyUpdates
    print_success "Dependency check completed"
}

# Function to run security scan
security() {
    print_status "Running security scan..."
    ./gradlew dependencyCheckAnalyze
    print_success "Security scan completed. Report at: reports/dependency-check-report.html"
}

# Function to show help
show_help() {
    echo "Clipboard History Android App - Build Script"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  setup           Setup Android SDK and prerequisites"
    echo "  clean           Clean build artifacts"
    echo "  test            Run unit tests"
    echo "  test-ui         Run instrumentation tests"
    echo "  test-all        Run all tests"
    echo "  coverage        Generate test coverage report"
    echo "  lint            Run lint checks"
    echo "  quality         Run all code quality checks"
    echo "  build           Build debug APK"
    echo "  build-release   Build release APK"
    echo "  docker-build    Build with Docker"
    echo "  docker-test     Run tests with Docker"
    echo "  docker-lint     Run lint with Docker"
    echo "  dependencies    Check for dependency updates"
    echo "  security        Run security scan"
    echo "  ci              Run full CI pipeline locally"
    echo "  help            Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 setup        # First time setup"
    echo "  $0 ci           # Run full CI pipeline"
    echo "  $0 docker-build # Build with Docker"
}

# Function to run full CI pipeline
ci() {
    print_status "Running full CI pipeline..."
    
    check_prerequisites
    setup_android_sdk
    clean
    quality
    test_all
    coverage
    build_debug
    build_release
    security
    
    print_success "Full CI pipeline completed successfully!"
}

# Main script logic
case "${1:-help}" in
    setup)
        check_prerequisites
        setup_android_sdk
        ;;
    clean)
        clean
        ;;
    test)
        test
        ;;
    test-ui)
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
    quality)
        quality
        ;;
    build)
        build_debug
        ;;
    build-release)
        build_release
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
    dependencies)
        dependencies
        ;;
    security)
        security
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