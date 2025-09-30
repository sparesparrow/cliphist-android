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