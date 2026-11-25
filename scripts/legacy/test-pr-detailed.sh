#!/bin/bash
# Detailed test of each PR branch with error extraction

test_pr() {
    local branch_name=$1
    local pr_number=$2
    
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Testing PR #$pr_number: $branch_name"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    # Checkout and reset
    git fetch origin "$branch_name" 2>/dev/null
    git checkout "origin/$branch_name" -b "test-$branch_name" 2>/dev/null || {
        git checkout "test-$branch_name" 2>/dev/null
        git reset --hard "origin/$branch_name"
    }
    
    # Try compilation first (faster)
    echo "Compiling..."
    COMPILE_OUTPUT=$(timeout 180 ./gradlew compileDebugKotlin --no-daemon 2>&1)
    
    if echo "$COMPILE_OUTPUT" | grep -q "BUILD SUCCESSFUL"; then
        BUILD_STATUS="✅ SUCCESS"
        ERROR_SUMMARY=""
        echo "✅ Compilation successful"
        
        # Try full build
        echo "Building APK..."
        BUILD_OUTPUT=$(timeout 300 ./gradlew assembleDebug --no-daemon 2>&1)
        if echo "$BUILD_OUTPUT" | grep -q "BUILD SUCCESSFUL"; then
            BUILD_STATUS="✅ SUCCESS (Full Build)"
        else
            BUILD_STATUS="⚠️ Compiles but assemble fails"
            ERROR_SUMMARY=$(echo "$BUILD_OUTPUT" | grep -E "error:|FAILED" | head -3 | tr '\n' '; ')
        fi
    else
        BUILD_STATUS="❌ FAILED"
        ERROR_SUMMARY=$(echo "$COMPILE_OUTPUT" | grep -E "e: file://" | head -5 | sed 's|e: file://.*/||' | tr '\n' '; ')
        echo "❌ Compilation failed"
        echo "$ERROR_SUMMARY"
    fi
    
    # Return to main
    git checkout main 2>/dev/null || true
    git branch -D "test-$branch_name" 2>/dev/null || true
    
    # Output result
    echo "## PR #$pr_number: $branch_name" 
    echo "- **Status**: $BUILD_STATUS"
    if [ -n "$ERROR_SUMMARY" ]; then
        echo "- **Errors**: $ERROR_SUMMARY"
    fi
    echo ""
}

# Test all PRs
test_pr "feature/smart-bubble-visibility" "15"
test_pr "feature/regex-accumulation-bubble" "16"
test_pr "feature/voice-tts-bubble" "17"
test_pr "feature/bubble-cut-integration" "18"
test_pr "feature/compose-bubble-types" "19"
test_pr "feature/advanced-bubble-types" "20"
test_pr "feature/collaboration-bubble" "21"
