#!/bin/bash
# Test all open PR branches and document build results

set -e

PR_BRANCHES=(
    "feature/smart-bubble-visibility:15"
    "feature/regex-accumulation-bubble:16"
    "feature/voice-tts-bubble:17"
    "feature/bubble-cut-integration:18"
    "feature/compose-bubble-types:19"
    "feature/advanced-bubble-types:20"
    "feature/collaboration-bubble:21"
)

RESULTS_FILE="pr-build-results.md"
echo "# PR Build Test Results" > "$RESULTS_FILE"
echo "Generated: $(date)" >> "$RESULTS_FILE"
echo "" >> "$RESULTS_FILE"

for branch_info in "${PR_BRANCHES[@]}"; do
    IFS=':' read -r branch_name pr_number <<< "$branch_info"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Testing PR #$pr_number: $branch_name"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    # Checkout branch
    git checkout "origin/$branch_name" -b "test-$branch_name" 2>/dev/null || git checkout "test-$branch_name"
    git reset --hard "origin/$branch_name"
    
    # Try to build
    echo "Attempting build..."
    if ./gradlew assembleDebug --no-daemon 2>&1 | tee "build-$branch_name.log" | tail -3 | grep -q "BUILD SUCCESSFUL"; then
        BUILD_STATUS="✅ SUCCESS"
        echo "✅ Build successful for PR #$pr_number"
    else
        BUILD_STATUS="❌ FAILED"
        echo "❌ Build failed for PR #$pr_number"
    fi
    
    # Document result
    echo "## PR #$pr_number: $branch_name" >> "$RESULTS_FILE"
    echo "- **Status**: $BUILD_STATUS" >> "$RESULTS_FILE"
    echo "- **Branch**: \`$branch_name\`" >> "$RESULTS_FILE"
    echo "- **Build Log**: \`build-$branch_name.log\`" >> "$RESULTS_FILE"
    echo "" >> "$RESULTS_FILE"
    
    # Cleanup
    git checkout main 2>/dev/null || true
    git branch -D "test-$branch_name" 2>/dev/null || true
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "All PR branches tested. Results saved to $RESULTS_FILE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
