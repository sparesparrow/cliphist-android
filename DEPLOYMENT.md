# Deployment Guide

## Overview

This guide covers the complete deployment process for the Clipboard History Android app, including local development, CI/CD pipeline, and production releases.

## Quick Start

### Prerequisites

- **Java 17+**: Required for building the Android app
- **Docker & Docker Compose**: For containerized builds (optional but recommended)
- **Git**: For version control and CI/CD integration
- **Android Studio**: For local development (optional)

### First Time Setup

```bash
# Clone the repository
git clone <repository-url>
cd clipboard-history

# Run setup script
./build.sh setup

# Build the app
./build.sh build
```

## Local Development

### Using Native Android SDK

1. **Install Android SDK**:
   ```bash
   # Set environment variables
   export ANDROID_HOME=/path/to/android-sdk
   export ANDROID_SDK_ROOT=$ANDROID_HOME
   
   # Run setup
   ./build.sh setup
   ```

2. **Build and Test**:
   ```bash
   # Run full CI pipeline locally
   ./build.sh ci
   
   # Or run individual commands
   ./build.sh test
   ./build.sh build
   ./build.sh coverage
   ```

### Using Docker (Recommended)

1. **Build with Docker**:
   ```bash
   # Build the app
   ./build.sh docker-build
   
   # Run tests
   ./build.sh docker-test
   
   # Run lint checks
   ./build.sh docker-lint
   ```

2. **Using Docker Compose**:
   ```bash
   # Build debug APK
   docker-compose up android-builder
   
   # Run tests
   docker-compose up android-test
   
   # Run lint
   docker-compose up android-lint
   ```

## CI/CD Pipeline

### GitHub Actions Workflows

The project includes several GitHub Actions workflows:

#### Main CI/CD Pipeline (`main-ci-cd.yml`)

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Git tags (v*)
- Manual workflow dispatch

**Jobs:**
1. **Quality Checks**: Tests, lint, code quality
2. **Security Scan**: CodeQL, dependency scanning
3. **Build**: Debug and Release APKs
4. **Release**: GitHub Release creation
5. **Containerized Build**: Docker-based builds

#### Pull Request Checks (`pr-checks.yml`)

**Focus:** Code quality and formatting
- ktlint formatting checks
- detekt static analysis
- Android lint validation
- PR comment integration

#### Monitoring (`monitoring.yml`)

**Features:**
- Build health monitoring
- Success rate tracking
- Security monitoring
- Performance analysis
- Automated alerting

### Workflow Configuration

#### Environment Variables

Set these in your GitHub repository settings:

```bash
# For release builds
KEYSTORE_BASE64=<base64-encoded-keystore>
KEYSTORE_PASSWORD=<keystore-password>
KEY_ALIAS=<key-alias>
KEY_PASSWORD=<key-password>

# For Android SDK (if needed)
ANDROID_SDK_ROOT=/opt/android-sdk
```

#### Secrets Configuration

1. Go to your GitHub repository
2. Navigate to Settings → Secrets and variables → Actions
3. Add the following secrets:
   - `KEYSTORE_BASE64`: Base64 encoded keystore file
   - `KEYSTORE_PASSWORD`: Keystore password
   - `KEY_ALIAS`: Key alias
   - `KEY_PASSWORD`: Key password

### Creating a Release

1. **Tag a version**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **Automatic Release**: The CI/CD pipeline will:
   - Build the release APK
   - Sign it with the release keystore
   - Create a GitHub release
   - Upload the signed APK

3. **Manual Release**: Use GitHub's release interface or:
   ```bash
   gh release create v1.0.0 --title "Release v1.0.0" --notes "Release notes"
   ```

## Production Deployment

### Release Process

1. **Pre-release Checklist**:
   - [ ] All tests passing
   - [ ] Security scan clean
   - [ ] Performance tests passing
   - [ ] Documentation updated
   - [ ] Version bumped

2. **Create Release**:
   ```bash
   # Update version in build.gradle
   # Create and push tag
   git tag v1.0.0
   git push origin v1.0.0
   ```

3. **Monitor Deployment**:
   - Check GitHub Actions workflow
   - Verify APK is signed correctly
   - Test the release APK
   - Monitor for issues

### Release Artifacts

Each release includes:
- **Signed APK**: Production-ready installation file
- **Release Notes**: Automated changelog
- **Build Info**: Version, hash, timestamp
- **Security Report**: Vulnerability assessment

### Distribution

#### GitHub Releases
- APK is automatically attached to GitHub releases
- Download link: `https://github.com/owner/repo/releases/latest`

#### Firebase App Distribution (Future)
- Beta testing distribution
- Internal testing
- External testing groups

#### Google Play Store (Future)
- Production distribution
- Staged rollout
- A/B testing

## Monitoring and Maintenance

### Build Monitoring

The monitoring workflow tracks:
- **Build Success Rate**: Should be > 80%
- **Build Duration**: Performance metrics
- **Security Status**: Vulnerability monitoring
- **Dependency Health**: Update notifications

### Automated Alerts

- **Build Failures**: Immediate notification
- **Low Success Rate**: When < 80%
- **Security Issues**: Critical vulnerabilities
- **Performance Degradation**: Slow builds

### Maintenance Tasks

#### Daily
- Monitor build health
- Check for security alerts
- Review failed builds

#### Weekly
- Update dependencies
- Review performance metrics
- Check test coverage

#### Monthly
- Security audit
- Performance optimization
- Documentation updates

## Troubleshooting

### Common Issues

#### Build Failures

**Issue**: Android SDK not found
```bash
# Solution: Set environment variables
export ANDROID_HOME=/path/to/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
```

**Issue**: Keystore not found
```bash
# Solution: Create keystore or use Docker
./build.sh docker-build
```

**Issue**: Tests failing
```bash
# Solution: Run tests locally first
./build.sh test
```

#### CI/CD Issues

**Issue**: Workflow not triggering
- Check branch protection rules
- Verify workflow file syntax
- Check repository permissions

**Issue**: Release not created
- Verify tag format (v*)
- Check keystore secrets
- Review workflow logs

### Debug Commands

```bash
# Verbose build
./gradlew assembleDebug --info

# Test with logs
./gradlew testDebugUnitTest --info

# Dependency tree
./gradlew app:dependencies

# Build scan
./gradlew build --scan
```

### Getting Help

1. **Check Logs**: Review GitHub Actions workflow logs
2. **Local Testing**: Reproduce issues locally
3. **Documentation**: Check this guide and CI-CD.md
4. **Issues**: Create GitHub issue with details

## Security Considerations

### Keystore Management

- **Never commit keystores** to version control
- **Use GitHub Secrets** for CI/CD
- **Rotate keys** periodically
- **Backup keystores** securely

### Dependency Security

- **Regular updates**: Check for security patches
- **Vulnerability scanning**: Automated in CI/CD
- **License compliance**: Review dependencies
- **Supply chain security**: Verify sources

### Code Security

- **Code review**: All changes reviewed
- **Static analysis**: Automated security checks
- **Secrets scanning**: Prevent credential leaks
- **Access control**: Limit repository access

## Performance Optimization

### Build Performance

- **Gradle caching**: Optimized cache configuration
- **Parallel builds**: Multi-threaded execution
- **Incremental compilation**: Changed files only
- **Resource optimization**: Shrinking and obfuscation

### CI/CD Performance

- **Matrix builds**: Parallel job execution
- **Artifact caching**: Reuse build outputs
- **Container optimization**: Efficient Docker images
- **Resource allocation**: Appropriate runner sizes

## Future Enhancements

### Planned Features

- [ ] **Multi-architecture builds**: ARM64, x86_64 support
- [ ] **Automated testing**: UI automation with Appium
- [ ] **Performance testing**: Memory and CPU profiling
- [ ] **Beta distribution**: Firebase App Distribution
- [ ] **Crash reporting**: Automated crash analysis
- [ ] **A/B testing**: Feature flag management

### Integration Opportunities

- [ ] **Slack notifications**: Build status updates
- [ ] **Jira integration**: Issue tracking
- [ ] **SonarQube**: Code quality metrics
- [ ] **Artifactory**: Artifact management
- [ ] **Kubernetes**: Scalable build infrastructure

---

For more information, see the [main README](README.md) or [CI/CD documentation](CI-CD.md).