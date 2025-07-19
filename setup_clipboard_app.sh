#!/bin/bash
# setup_clipboard_app.sh - Complete setup script for Android Clipboard History App

set -e  # Exit on any error

echo "🚀 Setting up Android Clipboard History App..."
echo "================================================"

# Function to print colored output
print_status() {
    echo -e "\033[1;32m✓\033[0m $1"
}

print_warning() {
    echo -e "\033[1;33m⚠\033[0m $1"
}

print_error() {
    echo -e "\033[1;31m✗\033[0m $1"
}

# Step 1: Backup current directory
echo "📦 Creating backup of current files..."
BACKUP_DIR="backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
cp -r *.py *.md *.xml *.gradle *.pro "$BACKUP_DIR/" 2>/dev/null || true
print_status "Backup created in $BACKUP_DIR"

# Step 2: Install required packages
echo "🔧 Installing required packages..."
pkg update -y >/dev/null 2>&1
pkg install -y python git tree unzip zip >/dev/null 2>&1
print_status "Packages installed"

# Step 3: Extract project files from Python scripts
echo "📂 Extracting project files..."
mkdir -p temp_extracted

# Run each script to extract files
for script in script.py script_*.py; do
    if [ -f "$script" ]; then
        echo "   Processing $script..."
        python "$script" >/dev/null 2>&1 || print_warning "Warning: $script had issues"
    fi
done

# Step 4: Create proper Android project structure
echo "🏗️ Creating Android project structure..."

# Main project directories
mkdir -p app/src/main/java/com/cliphistory/android
mkdir -p app/src/main/res/{layout,values,drawable,mipmap-hdpi,mipmap-mdpi,mipmap-xhdpi,mipmap-xxhdpi,mipmap-xxxhdpi}
mkdir -p app/src/test/java/com/cliphistory/android
mkdir -p app/src/androidTest/java/com/cliphistory/android
mkdir -p .github/workflows

print_status "Directory structure created"

# Step 5: Move configuration files to correct locations
echo "📋 Organizing configuration files..."

# Move Android manifest
if [ -f "AndroidManifest.xml" ]; then
    mv AndroidManifest.xml app/src/main/
    print_status "AndroidManifest.xml moved"
fi

# Move Gradle files
if [ -f "build.gradle" ]; then
    cp build.gradle app/
    print_status "App build.gradle copied"
fi

if [ -f "settings.gradle" ]; then
    # Keep in root
    print_status "settings.gradle in place"
fi

# Move ProGuard rules
if [ -f "proguard-rules.pro" ]; then
    mv proguard-rules.pro app/
    print_status "ProGuard rules moved"
fi

# Step 6: Move CI/CD files
if [ -f "ci-cd.yml" ]; then
    mv ci-cd.yml .github/workflows/
    print_status "CI/CD workflow moved"
fi

if [ -f "pr-checks.yml" ]; then
    mv pr-checks.yml .github/workflows/
    print_status "PR checks workflow moved"
fi

# Step 7: Create root build.gradle
echo "📝 Creating root build.gradle..."
cat > build.gradle << 'EOF'
plugins {
    id 'com.android.application' version '8.2.0' apply false
    id 'com.android.library' version '8.2.0' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.10' apply false
    id 'com.google.dagger.hilt.android' version '2.48' apply false
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
EOF
print_status "Root build.gradle created"

# Step 8: Create gradle wrapper properties
echo "🔄 Setting up Gradle wrapper..."
mkdir -p gradle/wrapper
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF
print_status "Gradle wrapper configured"

# Step 9: Initialize Git repository
echo "📚 Setting up Git repository..."
git init >/dev/null 2>&1
cat > .gitignore << 'EOF'
*.iml
.gradle
/local.properties
/.idea/
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
*.apk
*.aab
/app/release/
/app/debug/
EOF

git add . >/dev/null 2>&1
git commit -m "Initial Android Clipboard History App setup" >/dev/null 2>&1
print_status "Git repository initialized"

# Step 10: Create development scripts
echo "🛠️ Creating development helper scripts..."

# Create build script
cat > build.sh << 'EOF'
#!/bin/bash
echo "🔨 Building Android Clipboard History App..."
chmod +x gradlew
./gradlew assembleDebug
echo "✓ Build complete! APK location: app/build/outputs/apk/debug/"
EOF
chmod +x build.sh

# Create clean script
cat > clean.sh << 'EOF'
#!/bin/bash
echo "🧹 Cleaning project..."
rm -rf .gradle build app/build
echo "✓ Project cleaned"
EOF
chmod +x clean.sh

print_status "Helper scripts created"

# Step 11: Verify project structure
echo "🔍 Verifying project structure..."
tree -L 3 . 2>/dev/null || find . -type d | head -20

# Step 12: Check for missing source files
echo "📋 Project file summary:"
echo "   Configuration files: $(ls -1 *.gradle *.properties 2>/dev/null | wc -l)"
echo "   Documentation files: $(ls -1 *.md 2>/dev/null | wc -l)"
echo "   Python scripts: $(ls -1 script*.py 2>/dev/null | wc -l)"
echo "   Backup files: $(ls -1 $BACKUP_DIR/ 2>/dev/null | wc -l)"

# Step 13: Next steps information
echo ""
echo "🎉 Setup Complete!"
echo "=================="
print_status "Project structure created successfully"
print_status "Git repository initialized"
print_status "Build scripts ready"

echo ""
echo "📋 Next Steps:"
echo "1. Extract Kotlin source code from Python scripts"
echo "2. Review documentation: cat README.md"
echo "3. Check project summary: cat PROJECT_SUMMARY.md"
echo "4. Run: python extract_sources.py (if available)"
echo "5. For development: Transfer to Android Studio environment"
echo ""
echo "🔧 Available commands:"
echo "   ./build.sh      - Build the APK"
echo "   ./clean.sh      - Clean build files"
echo "   cat README.md   - Read documentation"
echo "   tree .          - Show project structure"

# Step 14: Create source extraction script
echo "📄 Creating source extraction helper..."
cat > extract_sources.py << 'EOF'
#!/usr/bin/env python3
import os
import glob

print("🔍 Looking for extractable source code in Python scripts...")

scripts = glob.glob('script*.py')
if scripts:
    print(f"Found {len(scripts)} Python scripts")
    print("Run each script individually to extract source files:")
    for script in sorted(scripts):
        print(f"   python {script}")
else:
    print("No extraction scripts found")
    
print("\nNote: Some scripts may create Kotlin (.kt) files in app/src/main/java/")
EOF
chmod +x extract_sources.py

echo ""
print_status "Setup script completed successfully!"
echo "Run: python extract_sources.py to continue with source extraction"
