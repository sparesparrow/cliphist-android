#!/bin/bash

# ============================================
# Android Build Script
# ============================================

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BUILD_TYPE="${1:-debug}"
SKIP_TESTS="${2:-false}"
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
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Check if we're in the right directory
if [[ ! -f "$PROJECT_ROOT/gradlew" ]]; then
    print_error "gradlew not found. Please run this script from the project root or scripts directory."
    exit 1
fi

cd "$PROJECT_ROOT"

# Help function
show_help() {
    echo "Android Build Script"
    echo ""
    echo "Usage: $0 [BUILD_TYPE] [SKIP_TESTS]"
    echo ""
    echo "BUILD_TYPE:"
    echo "  debug     - Build debug APK (default)"
    echo "  release   - Build release APK"
    echo "  both      - Build both debug and release APKs"
    echo "  clean     - Clean build artifacts"
    echo ""
    echo "SKIP_TESTS:"
    echo "  false     - Run tests before building (default)"
    echo "  true      - Skip tests"
    echo ""
    echo "Environment Variables:"
    echo "  USE_DOCKER=true   - Use Docker for builds"
    echo ""
    echo "Examples:"
    echo "  $0 debug"
    echo "  $0 release false"
    echo "  USE_DOCKER=true $0 both"
    echo ""
}

# Parse arguments
case "$BUILD_TYPE" in
    help|--help|-h)
        show_help
        exit 0
        ;;
    clean)
        print_header "🧹 Cleaning Build Artifacts"
        if [[ "$USE_DOCKER" == "true" ]]; then
            docker-compose run --rm android-ci ./gradlew clean
        else
            ./gradlew clean
        fi
        print_success "Clean completed"
        exit 0
        ;;
    debug|release|both)
        ;;
    *)
        print_error "Invalid build type: $BUILD_TYPE"
        show_help
        exit 1
        ;;
esac

# Setup Android SDK if needed
setup_android_sdk() {
    if [[ ! -f "local.properties" ]]; then
        if [[ -n "$ANDROID_SDK_ROOT" ]]; then
            echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
            print_info "Created local.properties with ANDROID_SDK_ROOT"
        elif [[ -n "$ANDROID_HOME" ]]; then
            echo "sdk.dir=$ANDROID_HOME" > local.properties
            print_info "Created local.properties with ANDROID_HOME"
        else
            print_warning "Neither ANDROID_SDK_ROOT nor ANDROID_HOME is set"
            print_info "You may need to set up Android SDK manually"
        fi
    fi
}

# Run code quality checks
run_quality_checks() {
    print_header "🔍 Running Code Quality Checks"
    
    if [[ "$USE_DOCKER" == "true" ]]; then
        docker-compose run --rm code-quality
    else
        ./gradlew ktlintCheck detekt lintDebug --continue
    fi
    
    print_success "Code quality checks completed"
}

# Run tests
run_tests() {
    if [[ "$SKIP_TESTS" == "true" ]]; then
        print_warning "Skipping tests as requested"
        return
    fi
    
    print_header "🧪 Running Tests"
    
    if [[ "$USE_DOCKER" == "true" ]]; then
        docker-compose run --rm test-runner
    else
        ./gradlew testDebugUnitTest testDebugUnitTestCoverage --continue
    fi
    
    print_success "Tests completed"
}

# Build APK
build_apk() {
    local build_type=$1
    print_header "🏗️  Building $build_type APK"
    
    local gradle_task=""
    case "$build_type" in
        debug)
            gradle_task="assembleDebug"
            ;;
        release)
            gradle_task="assembleRelease"
            ;;
        both)
            gradle_task="assembleDebug assembleRelease"
            ;;
    esac
    
    if [[ "$USE_DOCKER" == "true" ]]; then
        docker-compose run --rm android-ci ./gradlew $gradle_task
    else
        ./gradlew $gradle_task
    fi
    
    # Find and display built APKs
    print_info "Built APKs:"
    find app/build/outputs/apk -name "*.apk" -type f -exec ls -lh {} \; | while read -r line; do
        echo "  📱 $line"
    done
    
    print_success "$build_type APK build completed"
}

# Generate build info
generate_build_info() {
    print_header "📊 Generating Build Information"
    
    local build_info_file="build-info.json"
    local version_name=$(grep 'versionName' app/build.gradle | sed 's/.*versionName "\(.*\)"/\1/')
    local version_code=$(grep 'versionCode' app/build.gradle | sed 's/.*versionCode \(.*\)/\1/')
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
    "type": "$BUILD_TYPE",
    "docker": $USE_DOCKER,
    "tests_skipped": $SKIP_TESTS
  }
}
EOF
    
    print_success "Build info saved to $build_info_file"
    cat "$build_info_file"
}

# Main execution
main() {
    print_header "🚀 Android Build Process Started"
    print_info "Build Type: $BUILD_TYPE"
    print_info "Skip Tests: $SKIP_TESTS"
    print_info "Use Docker: $USE_DOCKER"
    
    # Setup
    setup_android_sdk
    
    # Quality checks
    run_quality_checks
    
    # Tests
    run_tests
    
    # Build
    build_apk "$BUILD_TYPE"
    
    # Generate build info
    generate_build_info
    
    print_header "🎉 Build Process Completed Successfully!"
}

# Run main function
main