# Multi-stage Dockerfile for Android app building
FROM openjdk:17-slim AS base

# Install required packages
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    curl \
    git \
    && rm -rf /var/lib/apt/lists/*

# Set environment variables
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Create android-sdk directory
RUN mkdir -p $ANDROID_HOME

# Download and install Android SDK command line tools
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && \
    unzip commandlinetools-linux-11076708_latest.zip -d $ANDROID_HOME && \
    rm commandlinetools-linux-11076708_latest.zip

# Create cmdline-tools/latest directory structure
RUN mkdir -p $ANDROID_HOME/cmdline-tools/latest && \
    mv $ANDROID_HOME/cmdline-tools/bin $ANDROID_HOME/cmdline-tools/latest/ && \
    mv $ANDROID_HOME/cmdline-tools/lib $ANDROID_HOME/cmdline-tools/latest/

# Accept licenses and install required SDK components
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "cmake;3.22.1"

# Build stage
FROM base AS builder

# Set working directory
WORKDIR /app

# Copy gradle files first for better caching
COPY gradle/ gradle/
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle .
COPY build.gradle .

# Make gradlew executable
RUN chmod +x gradlew

# Copy app module
COPY app/ app/

# Create local.properties
RUN echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties

# Build the app
RUN ./gradlew assembleDebug --no-daemon

# Final stage - just the APK
FROM alpine:latest AS final

# Install ca-certificates for HTTPS
RUN apk --no-cache add ca-certificates

# Create app directory
WORKDIR /app

# Copy the built APK from builder stage
COPY --from=builder /app/app/build/outputs/apk/debug/app-debug.apk .

# Set default command
CMD ["sh"]