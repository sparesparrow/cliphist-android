#!/bin/bash
# Cursor orchestrates full test execution:
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🧪 Clipboard History - Full Test Suite"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Require device once for whole suite
if ! adb get-state 1>/dev/null 2>&1; then
  echo "No adb device/emulator detected. Connect one and retry."
  exit 1
fi

# Clean previous results
rm -rf test-results/
mkdir -p test-results/{screenshots,logs,reports}

# Helper: run a command file by extracting its bash block
run_command_md() {
  local name=$1
  local file=".cursor/commands/${name}.md"
  if [ ! -f "$file" ]; then
    echo "Missing command file: $file"
    return 127
  fi
  awk '/^```bash/{flag=1;next}/^```/{flag=0}flag' "$file" | bash
}

# Test execution sequence (command file basenames)
tests=(
  "test-clipboard-capture"
  "test-floating-bubble-types"
  "test-smart-actions-drag-drop"
  "test-encryption-security"
  "test-service-persistence-memory"
  "test-ui-accessibility"
)

results=()
for test in "${tests[@]}"; do
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "Running: $test"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  # Execute test (invoke corresponding cursor command)
  start_time=$(date +%s)
  run_command_md "$test" 2>&1 | tee test-results/logs/${test}.log
  exit_code=${PIPESTATUS[0]}
  end_time=$(date +%s)
  duration=$((end_time - start_time))

  # Record result
  if [ $exit_code -eq 0 ]; then
    results+=("$test:PASS:${duration}s")
    echo "✅ $test: PASS (${duration}s)"
  else
    results+=("$test:FAIL:${duration}s")
    echo "❌ $test: FAIL (${duration}s)"
  fi
done

# Generate HTML report
python3 << 'EOF'
import json
from datetime import datetime

results = """${results[@]}"""
tests_data = []
for result in results.split():
    name, status, duration = result.split(':')
    tests_data.append({
        'name': name.replace('test-', '').replace('-', ' ').title(),
        'status': status,
        'duration': duration
    })

html = f"""
<!DOCTYPE html>
<html>
<head>
    <title>Clipboard History - Test Report</title>
    <style>
        body {{ font-family: 'Segoe UI', Arial, sans-serif; margin: 40px; background: #f5f5f5; }}
        .container {{ max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; }}
        h1 {{ color: #21808d; }}
        table {{ width: 100%; border-collapse: collapse; margin: 20px 0; }}
        th, td {{ padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }}
        th {{ background: #21808d; color: white; }}
        .pass {{ color: #4caf50; font-weight: bold; }}
        .fail {{ color: #f44336; font-weight: bold; }}
        .summary {{ display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin: 20px 0; }}
        .summary-card {{ padding: 20px; border-radius: 8px; text-align: center; }}
        .summary-card.total {{ background: #e3f2fd; }}
        .summary-card.passed {{ background: #e8f5e9; }}
        .summary-card.failed {{ background: #ffebee; }}
        .summary-card h2 {{ margin: 0; font-size: 2em; }}
    </style>
</head>
<body>
    <div class="container">
        <h1>📋 Clipboard History Test Report</h1>
        <p>Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>

        <div class="summary">
            <div class="summary-card total">
                <h2>{len(tests_data)}</h2>
                <p>Total Tests</p>
            </div>
            <div class="summary-card passed">
                <h2>{sum(1 for t in tests_data if t['status'] == 'PASS')}</h2>
                <p>Passed</p>
            </div>
            <div class="summary-card failed">
                <h2>{sum(1 for t in tests_data if t['status'] == 'FAIL')}</h2>
                <p>Failed</p>
            </div>
        </div>

        <h2>Test Results</h2>
        <table>
            <tr>
                <th>Test Name</th>
                <th>Status</th>
                <th>Duration</th>
            </tr>
"""

for test in tests_data:
    status_class = 'pass' if test['status'] == 'PASS' else 'fail'
    html += f"""
            <tr>
                <td>{test['name']}</td>
                <td class="{status_class}">{test['status']}</td>
                <td>{test['duration']}</td>
            </tr>
"""

html += """
        </table>

        <h2>📸 Screenshots</h2>
        <p>All test screenshots saved in: <code>test-results/screenshots/</code></p>

        <h2>📋 Logs</h2>
        <p>Detailed logs available in: <code>test-results/logs/</code></p>
    </div>
</body>
</html>
"""

with open('test-results/reports/full-test-suite-report.html', 'w') as f:
    f.write(html)

print("✅ HTML report generated: test-results/reports/full-test-suite-report.html")
EOF

# Final summary
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Test Suite Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
passed=$(echo "${results[@]}" | grep -o "PASS" | wc -l)
failed=$(echo "${results[@]}" | grep -o "FAIL" | wc -l)
echo "Total: ${#tests[@]} tests"
echo "Passed: $passed ✅"
echo "Failed: $failed ❌"
echo ""
echo "📁 Artifacts:"
echo "   - HTML Report: test-results/reports/full-test-suite-report.html"
echo "   - Screenshots: test-results/screenshots/"
echo "   - Logs: test-results/logs/"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"