# ============================================
# Android Build Environment - Multi-stage
# ============================================

# Stage 1: Base Android SDK setup
FROM openjdk:17-jdk-slim as android-base

# Set environment variables
ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    PATH=$PATH:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools \
    GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.workers.max=2 -Dorg.gradle.parallel=false"

# Install system dependencies
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    git \
    curl \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Create android sdk directory
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools

# Download and install Android SDK Command Line Tools
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip

# Accept licenses and install required SDK components
RUN yes | sdkmanager --licenses \
    && sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" \
    && sdkmanager "platforms;android-33" "platforms;android-32" "platforms;android-31" \
    && sdkmanager "build-tools;33.0.2" "build-tools;33.0.1" "build-tools;32.0.0"

# Stage 2: Development environment with additional tools
FROM android-base as android-dev

# Install additional development tools
RUN apt-get update && apt-get install -y \
    vim \
    nano \
    htop \
    tree \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Create working directory
WORKDIR /workspace

# Copy Gradle wrapper and build files first (for better caching)
COPY gradle/ ./gradle/
COPY gradlew gradlew.bat gradle.properties settings.gradle ./
COPY build.gradle ./

# Make gradlew executable
RUN chmod +x ./gradlew

# Download Gradle dependencies (this will be cached if build files don't change)
RUN ./gradlew --version

# Copy app build configuration
COPY app/build.gradle app/proguard-rules.pro app/detekt.yml ./app/

# Download project dependencies
RUN ./gradlew dependencies || true

# Stage 3: Production build environment
FROM android-dev as android-build

# Copy source code
COPY app/src/ ./app/src/
COPY app/src/main/AndroidManifest.xml ./app/src/main/

# Create local.properties
RUN echo "sdk.dir=${ANDROID_SDK_ROOT}" > local.properties

# Build the application
RUN ./gradlew clean assembleDebug assembleRelease || true

# Stage 4: CI/CD optimized image
FROM android-base as android-ci

# Install CI/CD specific tools
RUN apt-get update && apt-get install -y \
    jq \
    curl \
    git \
    && rm -rf /var/lib/apt/lists/*

# Create working directory
WORKDIR /workspace

# Copy gradle configuration
COPY gradle/ ./gradle/
COPY gradlew gradlew.bat gradle.properties settings.gradle build.gradle ./
RUN chmod +x ./gradlew

# Warm up Gradle
RUN ./gradlew --version

# Create entrypoint script for CI builds
RUN echo '#!/bin/bash\n\
set -e\n\
\n\
# Create local.properties\n\
echo "sdk.dir=${ANDROID_SDK_ROOT}" > local.properties\n\
\n\
# Accept licenses\n\
yes | sdkmanager --licenses >/dev/null 2>&1 || true\n\
\n\
# Run the provided command\n\
exec "$@"\n\
' > /entrypoint.sh && chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
CMD ["./gradlew", "help"]

# ============================================
# Docker Compose Support
# ============================================

# Create docker-compose.yml companion file