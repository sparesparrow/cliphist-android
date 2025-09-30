#!/bin/bash

# ============================================
# Development Environment Setup Script
# ============================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ANDROID_SDK_VERSION="9477386"
ANDROID_COMPILE_SDK="34"
ANDROID_BUILD_TOOLS="34.0.0"

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

# Detect OS
detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        OS="linux"
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        OS="macos"
    elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
        OS="windows"
    else
        print_error "Unsupported OS: $OSTYPE"
        exit 1
    fi
    print_info "Detected OS: $OS"
}

# Check prerequisites
check_prerequisites() {
    print_header "🔍 Checking Prerequisites"
    
    # Check Java
    if command -v java &> /dev/null; then
        local java_version=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
        print_success "Java found: $java_version"
        
        # Check if it's Java 17
        if [[ "$java_version" == "17."* ]]; then
            print_success "Java 17 detected - compatible with Android development"
        else
            print_warning "Java 17 is recommended for Android development"
        fi
    else
        print_error "Java not found. Please install Java 17 JDK"
        exit 1
    fi
    
    # Check Git
    if command -v git &> /dev/null; then
        local git_version=$(git --version | awk '{print $3}')
        print_success "Git found: $git_version"
    else
        print_error "Git not found. Please install Git"
        exit 1
    fi
    
    # Check Docker (optional)
    if command -v docker &> /dev/null; then
        local docker_version=$(docker --version | awk '{print $3}' | sed 's/,//')
        print_success "Docker found: $docker_version"
        DOCKER_AVAILABLE=true
    else
        print_warning "Docker not found. Docker builds will not be available"
        DOCKER_AVAILABLE=false
    fi
}

# Setup Android SDK
setup_android_sdk() {
    print_header "📱 Setting up Android SDK"
    
    # Check if Android SDK is already configured
    if [[ -n "$ANDROID_HOME" ]] && [[ -d "$ANDROID_HOME" ]]; then
        print_success "Android SDK found at: $ANDROID_HOME"
        echo "sdk.dir=$ANDROID_HOME" > "$PROJECT_ROOT/local.properties"
        return
    fi
    
    if [[ -n "$ANDROID_SDK_ROOT" ]] && [[ -d "$ANDROID_SDK_ROOT" ]]; then
        print_success "Android SDK found at: $ANDROID_SDK_ROOT"
        echo "sdk.dir=$ANDROID_SDK_ROOT" > "$PROJECT_ROOT/local.properties"
        export ANDROID_HOME="$ANDROID_SDK_ROOT"
        return
    fi
    
    # Determine SDK installation directory
    case "$OS" in
        linux)
            SDK_DIR="$HOME/Android/Sdk"
            CMDTOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip"
            ;;
        macos)
            SDK_DIR="$HOME/Library/Android/sdk"
            CMDTOOLS_URL="https://dl.google.com/android/repository/commandlinetools-mac-${ANDROID_SDK_VERSION}_latest.zip"
            ;;
        windows)
            SDK_DIR="$HOME/AppData/Local/Android/Sdk"
            CMDTOOLS_URL="https://dl.google.com/android/repository/commandlinetools-win-${ANDROID_SDK_VERSION}_latest.zip"
            ;;
    esac
    
    print_info "Installing Android SDK to: $SDK_DIR"
    
    # Create SDK directory
    mkdir -p "$SDK_DIR/cmdline-tools"
    
    # Download and install command line tools
    print_info "Downloading Android SDK Command Line Tools..."
    curl -o /tmp/cmdline-tools.zip "$CMDTOOLS_URL"
    
    print_info "Extracting command line tools..."
    unzip -q /tmp/cmdline-tools.zip -d "$SDK_DIR/cmdline-tools"
    mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
    rm /tmp/cmdline-tools.zip
    
    # Set environment variables
    export ANDROID_HOME="$SDK_DIR"
    export ANDROID_SDK_ROOT="$SDK_DIR"
    export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"
    
    # Accept licenses and install required packages
    print_info "Accepting SDK licenses..."
    yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses
    
    print_info "Installing required SDK packages..."
    "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
        "platform-tools" \
        "platforms;android-${ANDROID_COMPILE_SDK}" \
        "build-tools;${ANDROID_BUILD_TOOLS}"
    
    # Create local.properties
    echo "sdk.dir=$ANDROID_HOME" > "$PROJECT_ROOT/local.properties"
    
    print_success "Android SDK setup completed"
    print_info "Add these lines to your shell profile (~/.bashrc, ~/.zshrc, etc.):"
    echo "export ANDROID_HOME=\"$SDK_DIR\""
    echo "export ANDROID_SDK_ROOT=\"$SDK_DIR\""
    echo "export PATH=\"\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools\""
}

# Setup project dependencies
setup_project() {
    print_header "📦 Setting up Project Dependencies"
    
    cd "$PROJECT_ROOT"
    
    # Make gradlew executable
    chmod +x ./gradlew
    
    # Download Gradle wrapper
    print_info "Setting up Gradle wrapper..."
    ./gradlew wrapper --gradle-version=8.5
    
    # Download project dependencies
    print_info "Downloading project dependencies..."
    ./gradlew dependencies || print_warning "Some dependencies may not be available yet"
    
    print_success "Project dependencies setup completed"
}

# Setup Git hooks
setup_git_hooks() {
    print_header "🎣 Setting up Git Hooks"
    
    local hooks_dir="$PROJECT_ROOT/.git/hooks"
    
    # Pre-commit hook for code quality
    cat > "$hooks_dir/pre-commit" << 'EOF'
#!/bin/bash
# Pre-commit hook for code quality checks

echo "🔍 Running pre-commit checks..."

# Run ktlint check
echo "Running ktlint..."
./gradlew ktlintCheck --daemon
if [ $? -ne 0 ]; then
    echo "❌ ktlint check failed. Please fix formatting issues."
    echo "💡 Run './gradlew ktlintFormat' to auto-fix issues."
    exit 1
fi

# Run detekt
echo "Running detekt..."
./gradlew detekt --daemon
if [ $? -ne 0 ]; then
    echo "❌ detekt check failed. Please fix code quality issues."
    exit 1
fi

# Run lint
echo "Running Android lint..."
./gradlew lintDebug --daemon
if [ $? -ne 0 ]; then
    echo "⚠️  Lint issues found. Please review and fix critical issues."
fi

echo "✅ Pre-commit checks passed!"
EOF
    
    # Make hook executable
    chmod +x "$hooks_dir/pre-commit"
    
    # Pre-push hook for tests
    cat > "$hooks_dir/pre-push" << 'EOF'
#!/bin/bash
# Pre-push hook for running tests

echo "🧪 Running tests before push..."

./gradlew testDebugUnitTest --daemon
if [ $? -ne 0 ]; then
    echo "❌ Tests failed. Please fix failing tests before pushing."
    exit 1
fi

echo "✅ All tests passed!"
EOF
    
    chmod +x "$hooks_dir/pre-push"
    
    print_success "Git hooks setup completed"
}

# Setup IDE configuration
setup_ide_config() {
    print_header "⚙️  Setting up IDE Configuration"
    
    # Create .idea directory structure for IntelliJ/Android Studio
    mkdir -p "$PROJECT_ROOT/.idea/codeStyles"
    
    # Create code style configuration
    cat > "$PROJECT_ROOT/.idea/codeStyles/Project.xml" << 'EOF'
<component name="ProjectCodeStyleConfiguration">
  <code_scheme name="Project" version="173">
    <AndroidXmlCodeStyleSettings>
      <option name="ARRANGEMENT_SETTINGS_MIGRATED_TO_191" value="true" />
    </AndroidXmlCodeStyleSettings>
    <JetCodeStyleSettings>
      <option name="PACKAGES_TO_USE_STAR_IMPORTS">
        <value>
          <package name="java.util" alias="false" withSubpackages="false" />
          <package name="kotlinx.android.synthetic" alias="false" withSubpackages="true" />
          <package name="io.ktor" alias="false" withSubpackages="true" />
        </value>
      </option>
      <option name="PACKAGES_IMPORT_LAYOUT">
        <value>
          <package name="" alias="false" withSubpackages="true" />
          <package name="java" alias="false" withSubpackages="true" />
          <package name="javax" alias="false" withSubpackages="true" />
          <package name="kotlin" alias="false" withSubpackages="true" />
          <package name="" alias="true" withSubpackages="true" />
        </value>
      </option>
      <option name="CODE_STYLE_DEFAULTS" value="KOTLIN_OFFICIAL" />
    </JetCodeStyleSettings>
    <codeStyleSettings language="XML">
      <indentOptions>
        <option name="CONTINUATION_INDENT_SIZE" value="4" />
      </indentOptions>
      <arrangement>
        <rules>
          <section>
            <rule>
              <match>
                <AND>
                  <NAME>xmlns:android</NAME>
                  <XML_ATTRIBUTE />
                  <XML_NAMESPACE>^$</XML_NAMESPACE>
                </AND>
              </match>
            </rule>
          </section>
        </rules>
      </arrangement>
    </codeStyleSettings>
    <codeStyleSettings language="kotlin">
      <option name="CODE_STYLE_DEFAULTS" value="KOTLIN_OFFICIAL" />
    </codeStyleSettings>
  </code_scheme>
</component>
EOF
    
    # Create ktlint configuration
    cat > "$PROJECT_ROOT/.editorconfig" << 'EOF'
root = true

[*]
charset = utf-8
end_of_line = lf
indent_style = space
indent_size = 4
insert_final_newline = true
max_line_length = 120
trim_trailing_whitespace = true

[*.{kt,kts}]
indent_size = 4
ij_kotlin_allow_trailing_comma = true
ij_kotlin_allow_trailing_comma_on_call_site = true

[*.{xml,html}]
indent_size = 4

[*.{json,yml,yaml}]
indent_size = 2

[*.md]
trim_trailing_whitespace = false
EOF
    
    print_success "IDE configuration setup completed"
}

# Create development scripts
create_dev_scripts() {
    print_header "📝 Creating Development Scripts"
    
    # Make all scripts executable
    chmod +x "$PROJECT_ROOT"/scripts/*.sh
    
    # Create quick development commands
    cat > "$PROJECT_ROOT/dev.sh" << 'EOF'
#!/bin/bash
# Quick development commands

case "$1" in
    build)
        ./scripts/build.sh "${2:-debug}" "${3:-false}"
        ;;
    clean)
        ./scripts/build.sh clean
        ;;
    test)
        ./gradlew testDebugUnitTest testDebugUnitTestCoverage
        ;;
    lint)
        ./gradlew ktlintCheck detekt lintDebug
        ;;
    format)
        ./gradlew ktlintFormat
        ;;
    security)
        ./scripts/security-scan.sh "${2:-all}"
        ;;
    docker-build)
        USE_DOCKER=true ./scripts/build.sh "${2:-debug}"
        ;;
    *)
        echo "Usage: $0 {build|clean|test|lint|format|security|docker-build}"
        echo ""
        echo "Examples:"
        echo "  $0 build debug      # Build debug APK"
        echo "  $0 build release    # Build release APK"
        echo "  $0 test            # Run unit tests"
        echo "  $0 lint            # Run code quality checks"
        echo "  $0 format          # Format code with ktlint"
        echo "  $0 security        # Run security scans"
        echo "  $0 docker-build    # Build using Docker"
        ;;
esac
EOF
    
    chmod +x "$PROJECT_ROOT/dev.sh"
    
    print_success "Development scripts created"
}

# Setup Docker environment
setup_docker() {
    if [[ "$DOCKER_AVAILABLE" == "true" ]]; then
        print_header "🐳 Setting up Docker Environment"
        
        cd "$PROJECT_ROOT"
        
        print_info "Building Docker images..."
        docker-compose build android-ci
        
        print_info "Testing Docker build..."
        docker-compose run --rm android-ci ./gradlew --version
        
        print_success "Docker environment setup completed"
    else
        print_warning "Skipping Docker setup - Docker not available"
    fi
}

# Final verification
verify_setup() {
    print_header "✅ Verifying Setup"
    
    cd "$PROJECT_ROOT"
    
    # Check if we can run Gradle
    print_info "Testing Gradle..."
    ./gradlew --version
    
    # Check if we can build the project
    print_info "Testing project build..."
    ./gradlew assembleDebug --dry-run
    
    # Check code quality tools
    print_info "Testing code quality tools..."
    ./gradlew ktlintCheck --dry-run
    ./gradlew detekt --dry-run
    
    print_success "Setup verification completed!"
}

# Display setup summary
show_summary() {
    print_header "🎉 Development Environment Setup Complete!"
    
    echo -e "${GREEN}✅ Setup Summary:${NC}"
    echo "  📱 Android SDK configured"
    echo "  📦 Project dependencies downloaded"
    echo "  🎣 Git hooks installed"
    echo "  ⚙️  IDE configuration created"
    echo "  📝 Development scripts ready"
    if [[ "$DOCKER_AVAILABLE" == "true" ]]; then
        echo "  🐳 Docker environment configured"
    fi
    
    echo -e "\n${BLUE}🚀 Quick Start Commands:${NC}"
    echo "  ./dev.sh build          # Build debug APK"
    echo "  ./dev.sh test           # Run tests"
    echo "  ./dev.sh lint           # Check code quality"
    echo "  ./dev.sh security       # Run security scans"
    
    echo -e "\n${BLUE}📚 Next Steps:${NC}"
    echo "  1. Open the project in Android Studio"
    echo "  2. Sync the project"
    echo "  3. Run './dev.sh build' to test the setup"
    echo "  4. Start developing! 🎯"
    
    if [[ "$OS" != "windows" ]]; then
        echo -e "\n${YELLOW}💡 Don't forget to add Android SDK to your PATH:${NC}"
        echo "  export ANDROID_HOME=\"$ANDROID_HOME\""
        echo "  export PATH=\"\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools\""
    fi
}

# Main execution
main() {
    print_header "🚀 Android Development Environment Setup"
    
    detect_os
    check_prerequisites
    setup_android_sdk
    setup_project
    setup_git_hooks
    setup_ide_config
    create_dev_scripts
    setup_docker
    verify_setup
    show_summary
}

# Run main function
main