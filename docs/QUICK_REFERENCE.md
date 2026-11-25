# Quick Reference Card

## 🚀 Development Commands

### Essential Commands
```bash
./build-dev.sh setup       # First time setup
./build-dev.sh build       # Build debug APK
./build-dev.sh test        # Run unit tests
./build-dev.sh quality     # Run quality checks
./build-dev.sh ci          # Full CI pipeline locally
```

### All Available Commands
```bash
# Setup & Clean
./build-dev.sh setup           # Setup development environment
./build-dev.sh clean           # Clean build artifacts

# Testing
./build-dev.sh test            # Unit tests
./build-dev.sh test-ui         # Instrumentation tests
./build-dev.sh test-all        # All tests
./build-dev.sh coverage        # Generate coverage report

# Code Quality
./build-dev.sh lint            # Run Android lint
./build-dev.sh format          # Format with ktlint
./build-dev.sh quality         # All quality checks

# Building
./build-dev.sh build           # Build debug APK
./build-dev.sh build-debug     # Build debug APK
./build-dev.sh build-release   # Build release APK
./build-dev.sh build-both      # Build both APKs

# Docker
USE_DOCKER=true ./build-dev.sh docker-build
USE_DOCKER=true ./build-dev.sh docker-test
USE_DOCKER=true ./build-dev.sh docker-lint

# Maintenance
./build-dev.sh dependencies    # Check for updates
./build-dev.sh security        # Run security scan
./build-dev.sh info            # Generate build info

# Help
./build-dev.sh help            # Show all commands
```

## 📦 CI/CD Pipeline

### Triggers
- **Push** to main/develop/master
- **Pull Request** to main/develop/master
- **Tags** starting with `v*`
- **Schedule** daily at 9 AM UTC
- **Manual** workflow dispatch

### Jobs
1. **validate-wrapper** - Gradle security check
2. **quality-checks** - ktlint, detekt, lint
3. **security-scan** - Dependency vulnerabilities
4. **test** - Unit & instrumentation (API 24, 29, 34)
5. **build** - Debug & release APKs
6. **release** - Create GitHub release
7. **monitor** - Build health check
8. **notify** - Build summary

### Artifacts
- `debug-apk-{version}` - Debug APK (90 days)
- `release-apk-{version}` - Release APK (365 days)
- `test-reports-api{level}-{run}` - Test results (30 days)
- `quality-reports-{run}` - Quality reports (30 days)
- `security-reports-{run}` - Security scans (30 days)

## 📁 Project Structure

```
cliphist-android/
├── .github/workflows/
│   └── ci-cd.yml              # Unified CI/CD pipeline
├── app/
│   ├── src/
│   │   ├── main/
│   │   ├── test/              # Unit tests
│   │   └── androidTest/       # Instrumentation tests
│   ├── build.gradle           # App build config
│   └── detekt.yml             # Code quality rules
├── docs/
│   └── API.md                 # API documentation
├── scripts/
│   ├── build.sh               # Advanced build script
│   ├── security-scan.sh       # Security scanning
│   └── setup-dev.sh           # Dev environment setup
├── build-dev.sh               # 🆕 Main development script
├── README.md                  # Main documentation
├── CI-CD.md                   # Pipeline documentation
├── IMPROVEMENTS.md            # 🆕 Improvement guide
├── CONSOLIDATION_SUMMARY.md   # 🆕 Changes summary
└── QUICK_REFERENCE.md         # 🆕 This file
```

## 🔧 Environment Variables

```bash
# Android SDK (required for local builds)
export ANDROID_HOME=/path/to/android-sdk
# or
export ANDROID_SDK_ROOT=/path/to/android-sdk

# Docker builds (optional)
export USE_DOCKER=true

# Release signing (CI only)
KEYSTORE_BASE64=<base64-encoded-keystore>
KEYSTORE_PASSWORD=<password>
KEY_ALIAS=<alias>
KEY_PASSWORD=<password>
```

## 📊 Version Information

### Version Calculation
- **Version Code**: Git commit count (`git rev-list --count HEAD`)
- **Version Name**: 
  - Tags: `v1.2.3` → `1.2.3`
  - Main/Master: `0.0.0-123-abc1234`
  - Branches: `0.0.0-feature-123-abc1234`

### Build Info Fields
```json
{
  "version": { "name": "1.0.0", "code": 123 },
  "git": { "commit": "abc1234", "branch": "main" },
  "build": { "date": "2025-10-01T12:00:00Z" }
}
```

## 🐛 Troubleshooting

### Android SDK Not Found
```bash
# Set SDK location
export ANDROID_SDK_ROOT=/path/to/sdk
./build-dev.sh setup

# Or use Docker
USE_DOCKER=true ./build-dev.sh docker-build
```

### Gradle Daemon Issues
```bash
./gradlew --stop
./build-dev.sh clean
./build-dev.sh build
```

### Permission Denied
```bash
chmod +x ./build-dev.sh
chmod +x ./gradlew
```

### Build Fails in CI
1. Check workflow logs in GitHub Actions
2. Run locally: `./build-dev.sh ci`
3. Check specific job logs
4. Review artifacts for detailed reports

## 📈 Quality Metrics

### Current Status
- ✅ Build automation: 100%
- ✅ CI/CD reliability: Active monitoring
- ⚠️ Code quality: ~1000 detekt issues
- ⚠️ Test coverage: Needs expansion

### Quality Gates
- ktlint formatting
- detekt static analysis
- Android lint checks
- Security vulnerability scan
- Unit test execution
- Multi-API testing

## 🎯 Quick Workflows

### Starting New Feature
```bash
git checkout -b feature/my-feature
./build-dev.sh setup
./build-dev.sh test
# ... make changes ...
./build-dev.sh format
./build-dev.sh quality
./build-dev.sh ci
git commit -m "feat: my feature"
git push
```

### Fixing Bugs
```bash
git checkout -b fix/bug-description
./build-dev.sh test        # Verify bug
# ... fix code ...
./build-dev.sh test        # Verify fix
./build-dev.sh quality     # Check quality
git commit -m "fix: bug description"
git push
```

### Before Release
```bash
./build-dev.sh clean
./build-dev.sh ci
./build-dev.sh build-release
# Create tag
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
# CI automatically creates GitHub release
```

## 📚 Documentation Links

- **[README.md](README.md)** - Main project documentation
- **[CI-CD.md](CI-CD.md)** - CI/CD pipeline details
- **[IMPROVEMENTS.md](IMPROVEMENTS.md)** - Refactoring recommendations
- **[CONSOLIDATION_SUMMARY.md](CONSOLIDATION_SUMMARY.md)** - Recent changes
- **[docs/API.md](docs/API.md)** - API reference
- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Deployment guide

## 💡 Tips

1. **Use `help`**: Run `./build-dev.sh help` for full command list
2. **Run locally**: Use `./build-dev.sh ci` before pushing
3. **Check quality**: Run `./build-dev.sh quality` frequently
4. **Format often**: Run `./build-dev.sh format` before committing
5. **Docker fallback**: Use Docker if Android SDK not available
6. **Check logs**: Review `app/build/reports/` for detailed reports
7. **Monitor builds**: Check GitHub Actions for CI status
8. **Read docs**: Review IMPROVEMENTS.md for best practices

---

**Updated:** October 1, 2025  
**Version:** 1.0  
**Status:** ✅ Production Ready
