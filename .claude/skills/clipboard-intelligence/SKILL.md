---
name: clipboard-intelligence
description: Optimize clipboard search, improve data classification, enhance integration with development workflow
tags: ["cliphist", "android", "clipboard", "search", "developer-tools"]
---

# Clipboard Intelligence Skill

## Phase 1: Search Optimization
1. Analyze search patterns:
   `get_prompt("clipboard-search-pattern-analyzer", {
     queries: recent_searches,
     searchSuccessRate: metric,
     userFrustrations: tracked_issues
   })`

2. Select optimal search strategy:
   `get_prompt("clipboard-search-strategy-selector", {
     queryType: "code|url|text|log",
     precision: required_precision,
     latency: acceptable_latency
   })`

3. If search slow:
   `get_prompt("android-search-optimization-guide", {
     dataSize: clipboard_history_size,
     indexing: current_indexing_approach,
     latencyTarget: expected_latency
   })`

## Phase 2: Data Classification
1. Automatically classify clipboard entries:
   `get_prompt("clipboard-content-classifier", {
     contentType: detected_type,
     context: current_app_context
   })`

2. Create context-specific prompts:
   - "git-commit-templates" for git context
   - "code-snippets-${language}" for IDEs
   - "json-validator-templates" for JSON data
   - "url-categorizer" for links

## Phase 3: Integration with Development Tools
1. Detect current development context:
   - Are we in Android Studio, VS Code, Chrome?
   - What type of work are we doing?

2. Get context-aware clipboard helpers:
   `get_prompt("clipboard-helper-${currentContext}", {...})`

## Phase 4: Smart Suggestions
1. When user copies repeatedly similar items:
   - Detect pattern
   - Suggest batch operations
   - `create_prompt(name: "clipboard-bulk-action-${pattern}", ...)`

2. Learn user's favorite clipboard operations:
   `create_prompt(name: "user-clipboard-workflow-${workflowType}", ...)`

## Phase 5: Cross-Device Sync Learning
1. If syncing with desktop clipboard:
   - Learn what gets shared frequently
   - Optimize sync strategy
   - `update_prompt("clipboard-sync-strategy", improved_logic)`

## Performance Monitoring
```
Monitor:
- Search latency (target: <100ms)
- Storage growth (optimize old entries)
- Integration accuracy (right tool suggestions)

Update prompts based on performance metrics
```