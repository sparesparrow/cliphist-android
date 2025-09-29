#!/bin/bash

# ============================================
# Security Scanning and Dependency Management
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
REPORT_DIR="$PROJECT_ROOT/security-reports"
SCAN_TYPE="${1:-all}"

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

# Help function
show_help() {
    echo "Security Scanning and Dependency Management Script"
    echo ""
    echo "Usage: $0 [SCAN_TYPE]"
    echo ""
    echo "SCAN_TYPE:"
    echo "  all           - Run all security scans (default)"
    echo "  dependencies  - Check for vulnerable dependencies"
    echo "  secrets       - Scan for secrets and sensitive data"
    echo "  permissions   - Analyze Android permissions"
    echo "  update        - Update dependencies to latest versions"
    echo "  report        - Generate comprehensive security report"
    echo ""
    echo "Examples:"
    echo "  $0 all"
    echo "  $0 dependencies"
    echo "  $0 update"
    echo ""
}

# Setup
setup() {
    cd "$PROJECT_ROOT"
    mkdir -p "$REPORT_DIR"
    
    print_info "Security reports will be saved to: $REPORT_DIR"
}

# Check for vulnerable dependencies
check_dependencies() {
    print_header "🔍 Scanning Dependencies for Vulnerabilities"
    
    # Generate dependency list
    print_info "Generating dependency list..."
    ./gradlew dependencies > "$REPORT_DIR/dependencies.txt" || true
    
    # Check for known vulnerabilities using OWASP Dependency Check
    if command -v dependency-check &> /dev/null; then
        print_info "Running OWASP Dependency Check..."
        dependency-check \
            --project "Clipboard History Android" \
            --scan . \
            --format ALL \
            --out "$REPORT_DIR" \
            --enableExperimental \
            --enableRetired || print_warning "OWASP Dependency Check completed with warnings"
    else
        print_warning "OWASP Dependency Check not installed. Install with: https://github.com/jeremylong/DependencyCheck"
    fi
    
    # Check Gradle dependencies for updates
    print_info "Checking for dependency updates..."
    ./gradlew dependencyUpdates > "$REPORT_DIR/dependency-updates.txt" || true
    
    print_success "Dependency vulnerability scan completed"
}

# Scan for secrets and sensitive data
scan_secrets() {
    print_header "🔐 Scanning for Secrets and Sensitive Data"
    
    # Use git-secrets if available
    if command -v git-secrets &> /dev/null; then
        print_info "Running git-secrets scan..."
        git secrets --scan || print_warning "git-secrets scan completed with findings"
    else
        print_warning "git-secrets not installed. Install with: https://github.com/awslabs/git-secrets"
    fi
    
    # Manual patterns check
    print_info "Checking for common secret patterns..."
    
    # Create patterns file
    cat > "$REPORT_DIR/secret-patterns.txt" << EOF
# Common secret patterns found:
EOF
    
    # Search for API keys, passwords, tokens, etc.
    grep -r -i -n --include="*.kt" --include="*.java" --include="*.xml" --include="*.properties" \
        -E "(api[_-]?key|password|secret|token|private[_-]?key)" . \
        >> "$REPORT_DIR/secret-patterns.txt" || true
    
    # Check for hardcoded URLs
    grep -r -n --include="*.kt" --include="*.java" --include="*.xml" \
        -E "https?://[^/\s]+" . \
        > "$REPORT_DIR/hardcoded-urls.txt" || true
    
    # Check for debug/test code
    grep -r -i -n --include="*.kt" --include="*.java" \
        -E "(debug|test|todo|fixme|hack)" . \
        > "$REPORT_DIR/debug-code.txt" || true
    
    print_success "Secret scanning completed"
}

# Analyze Android permissions
analyze_permissions() {
    print_header "🛡️  Analyzing Android Permissions"
    
    print_info "Extracting permissions from AndroidManifest.xml..."
    
    if [[ -f "app/src/main/AndroidManifest.xml" ]]; then
        # Extract permissions
        grep -n "uses-permission" app/src/main/AndroidManifest.xml > "$REPORT_DIR/permissions.txt" || true
        
        # Analyze dangerous permissions
        cat > "$REPORT_DIR/permission-analysis.txt" << EOF
# Android Permission Analysis
# Generated on: $(date)

## Declared Permissions:
EOF
        
        grep "uses-permission" app/src/main/AndroidManifest.xml | \
        sed 's/.*android:name="\([^"]*\)".*/\1/' | \
        while read -r permission; do
            echo "- $permission" >> "$REPORT_DIR/permission-analysis.txt"
            
            # Check if it's a dangerous permission
            case "$permission" in
                *CAMERA*|*LOCATION*|*MICROPHONE*|*CONTACTS*|*STORAGE*|*SMS*|*PHONE*)
                    echo "  ⚠️  DANGEROUS PERMISSION - Requires runtime permission" >> "$REPORT_DIR/permission-analysis.txt"
                    ;;
            esac
        done
        
        print_success "Permission analysis completed"
    else
        print_warning "AndroidManifest.xml not found"
    fi
}

# Update dependencies
update_dependencies() {
    print_header "📦 Updating Dependencies"
    
    print_info "Checking for available updates..."
    ./gradlew dependencyUpdates
    
    print_warning "Dependency updates require manual review and testing"
    print_info "Review the dependency-updates.txt report and update build.gradle files manually"
    
    # Generate update suggestions
    cat > "$REPORT_DIR/update-suggestions.md" << EOF
# Dependency Update Suggestions
Generated on: $(date)

## How to Update Dependencies

1. Review the dependency-updates.txt report
2. Update version numbers in build.gradle files
3. Test the application thoroughly
4. Run security scans again after updates

## Automated Update Commands
\`\`\`bash
# Update Gradle Wrapper
./gradlew wrapper --gradle-version=latest

# Check for plugin updates
./gradlew dependencyUpdates
\`\`\`

## Security Considerations
- Always review changelogs for breaking changes
- Test security-sensitive functionality after updates
- Monitor for new vulnerabilities in updated dependencies
EOF
    
    print_success "Update suggestions generated"
}

# Generate comprehensive security report
generate_report() {
    print_header "📊 Generating Security Report"
    
    local report_file="$REPORT_DIR/security-report.md"
    local timestamp=$(date -u +"%Y-%m-%d %H:%M:%S UTC")
    
    cat > "$report_file" << EOF
# Security Report - Clipboard History Android

**Generated:** $timestamp  
**Project:** Clipboard History Android App  
**Scan Type:** $SCAN_TYPE

## Executive Summary

This report contains the results of automated security scanning for the Clipboard History Android application.

## Dependency Vulnerabilities

EOF
    
    # Add dependency check results if available
    if [[ -f "$REPORT_DIR/dependency-check-report.html" ]]; then
        echo "✅ OWASP Dependency Check completed. See \`dependency-check-report.html\` for details." >> "$report_file"
    else
        echo "⚠️  OWASP Dependency Check not run. Install dependency-check tool for vulnerability scanning." >> "$report_file"
    fi
    
    cat >> "$report_file" << EOF

## Code Quality Issues

EOF
    
    # Add secret scanning results
    if [[ -f "$REPORT_DIR/secret-patterns.txt" ]]; then
        local secret_count=$(wc -l < "$REPORT_DIR/secret-patterns.txt")
        echo "- Secret patterns found: $secret_count" >> "$report_file"
    fi
    
    cat >> "$report_file" << EOF

## Android Security

### Permissions Analysis
EOF
    
    if [[ -f "$REPORT_DIR/permission-analysis.txt" ]]; then
        cat "$REPORT_DIR/permission-analysis.txt" >> "$report_file"
    fi
    
    cat >> "$report_file" << EOF

## Recommendations

1. **Regular Updates**: Keep dependencies updated to latest secure versions
2. **Secret Management**: Use Android Keystore for sensitive data storage
3. **Permission Minimization**: Request only necessary permissions
4. **Code Obfuscation**: Enable ProGuard/R8 for release builds
5. **SSL Pinning**: Implement certificate pinning for network security
6. **Runtime Security**: Implement root detection and anti-tampering measures

## Next Steps

1. Review all findings in detail
2. Prioritize fixes based on severity
3. Implement security improvements
4. Schedule regular security scans
5. Update dependencies regularly

---
*Report generated by security-scan.sh*
EOF
    
    print_success "Security report generated: $report_file"
    
    # Display summary
    print_info "Opening security report..."
    if command -v cat &> /dev/null; then
        echo -e "\n${BLUE}Security Report Summary:${NC}"
        head -20 "$report_file"
        echo -e "\n${BLUE}... (see full report in $report_file)${NC}"
    fi
}

# Main execution
main() {
    case "$SCAN_TYPE" in
        help|--help|-h)
            show_help
            exit 0
            ;;
        all)
            setup
            check_dependencies
            scan_secrets
            analyze_permissions
            generate_report
            ;;
        dependencies)
            setup
            check_dependencies
            ;;
        secrets)
            setup
            scan_secrets
            ;;
        permissions)
            setup
            analyze_permissions
            ;;
        update)
            setup
            update_dependencies
            ;;
        report)
            setup
            generate_report
            ;;
        *)
            print_error "Invalid scan type: $SCAN_TYPE"
            show_help
            exit 1
            ;;
    esac
    
    print_header "🎉 Security Scan Completed!"
    print_info "Reports saved in: $REPORT_DIR"
}

# Run main function
main