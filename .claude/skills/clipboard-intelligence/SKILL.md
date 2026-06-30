# Clipboard Intelligence Skill

## Purpose

Help developers and users work intelligently with clipboard history data from the
cliphist-android app. Use the MCP tools to read, search, analyze, and enrich
clipboard history, then surface patterns, suggestions, and developer workflow aids.

## Prerequisites

- LocalApiService running on the Android device (Settings → Local API → Enable)
- ADB port forwarded: `adb forward tcp:8765 tcp:8765`
- MCP server configured in `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "cliphist-android": {
      "command": "node",
      "args": ["/path/to/mcp-server/dist/index.js"],
      "env": {
        "CLIPHIST_API_URL": "http://localhost:8765",
        "CLIPHIST_API_TOKEN": "<your-token>"
      }
    }
  }
}
```

## Available MCP Tools

| Tool                    | Parameters                  | When to Use                                               |
|-------------------------|-----------------------------|-----------------------------------------------------------|
| `get_clipboard_history` | `limit` (1–100, default 20) | Start any session; get recent items for context           |
| `search_clipboard`      | `query` (required string)   | When user asks "find items about X"                       |
| `get_clipboard_item`    | `id` (required string)      | Retrieve full content of one item by ID                   |
| `add_to_clipboard`      | `content` (required string) | Write processed/transformed content back to device        |
| `get_clipboard_stats`   | none                        | Assess usage before making suggestions; check liveness    |

Call `get_clipboard_stats` first in every session — it is the cheapest call and
confirms that the LocalApiService is reachable.

---

## Search Optimization

When the user's query is ambiguous or returns few results, apply this strategy:

### Strategy Selection

Analyze the query before searching:

| Query characteristic             | Strategy       | Action                                              |
|----------------------------------|----------------|-----------------------------------------------------|
| Quoted string (e.g. `"foo bar"`) | EXACT          | Search as-is                                        |
| Contains code keywords (`fun`, `def`, `class`, `import`, `const`, `return`) | KEYWORD | Search each keyword separately, deduplicate |
| Length < 4 characters            | FUZZY          | Split into chars/substrings, search each, merge     |
| Starts with `type:` prefix       | TYPE_FILTERED  | Strip prefix, filter results by detected type label |
| All other queries                | EXACT          | Search as-is, rank results                          |

### Search Workflow

1. Call `get_clipboard_stats` — note `mostUsedContentType` and `totalItems`.
2. Select a strategy based on the table above.
3. Call `search_clipboard` with the query (or sub-queries for FUZZY/KEYWORD).
4. **If fewer than 3 results returned**, widen: search for individual words in the
   query, deduplicate by `id`, merge results.
5. **Rank results** (higher score = better match):
   - Exact content match: +100 points
   - Content starts with query: +50 points
   - Content contains query: +25 points
   - Item is a favorite (`isFavorite: true`): +10 points
   - Item timestamp within the last 7 days: +5 points
6. Present top results with their `id`, `contentType`, timestamp, and a content
   preview (first 200 chars).

---

## Data Classification

When presenting clipboard items, annotate each with its detected type using these
heuristics **in priority order**:

| Priority | Classification   | Detection rule                                                     |
|----------|------------------|--------------------------------------------------------------------|
| 1        | URL              | Content starts with `http://` or `https://`                       |
| 2        | EMAIL            | Matches `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$`                      |
| 3        | PHONE            | Matches `^\+?[\d\s()\-.]+$` and contains ≥7 digits                |
| 4        | JSON             | Trimmed content matches `^\{.*\}$` (dot-all)                      |
| 5        | XML              | Trimmed content starts with `<` and contains `</`                 |
| 6        | CODE             | Contains code keywords: `fun `, `def `, `class `, `import `, `#include`, `const `, `return ` |
| 7        | SHORT_TEXT       | Content length < 50 characters                                    |
| 8        | TEXT             | Everything else                                                   |

### Context Detection

Examine the `sourceApp` field to determine the IDE or app context:

| Package substring                               | Context       | Display Name        |
|-------------------------------------------------|---------------|---------------------|
| `com.android.vscode`, `com.cursor`              | IDE           | VS Code / Cursor    |
| `com.jetbrains.androidstudio`, `com.android.studio` | IDE       | Android Studio      |
| `com.jetbrains.ideaU`                           | IDE           | IntelliJ IDEA       |
| `com.termux`, `jackpal.androidterm`             | TERMINAL      | Terminal            |
| `com.android.chrome`, `org.mozilla.firefox`     | BROWSER       | Browser             |
| `com.slack.android`, `com.discord`              | COMMUNICATION | Messaging           |
| null or no match                                | UNKNOWN       | Unknown             |

---

## Dev Tool Integration

When an item is classified as CODE and the context is IDE:

### Language Detection

Detect programming language from content keywords:

| Keyword present          | Language   |
|--------------------------|------------|
| `fun ` or `val `         | Kotlin     |
| `def ` (no `{`)          | Python     |
| `public class` or `void ` | Java      |
| `function ` or `const `  | JavaScript |
| `#include` or `std::`    | C++        |
| No match                 | Unknown    |

### Helper Generation

Offer to generate a code helper for the clipboard item:

1. **Docstring** — KDoc for Kotlin, Javadoc for Java, `"""` for Python, JSDoc
   for JavaScript. Extract the first function/class name from the code and insert
   a skeleton with `@param`, `@return`, and a `TODO` description.

2. **Test stub** — Minimal test function named `test_<functionName>` (Python) or
   `fun test<FunctionName>()` (Kotlin) with a `TODO("Implement test")` body.

3. **README snippet** — Wrap the code in a fenced Markdown code block with the
   correct language tag (e.g., ` ```kotlin `).

After generating the helper, write it back to the device:

```
add_to_clipboard(content = "<generated helper text>")
```

Inform the user they can now paste directly from the device clipboard.

---

## Smart Suggestions

After calling `get_clipboard_history` (or on user request):

### Duplicate Detection

- Group items by exact `content` value.
- Any group with ≥2 items → suggest **batch delete** of all but the most recent.
- Report: "Found N duplicate groups. Suggest deleting X older copies."

### Stale Item Cleanup

- Items where `(now - timestamp) > 14 days` AND `isFavorite == false` → suggest
  **cleanup**.
- Report: "X items older than 14 days are not favorited. Safe to delete."

### Session Grouping

- Items sharing the same `sourceApp` with timestamps within a 5-minute window →
  suggest **grouping as a session**.
- Report: "Detected a clipboard session from <app> at <time> with N items."

### Code Bundle

- If ≥3 CODE items from the same `sourceApp` exist → offer to collect them into a
  single clipboard bundle by concatenating with `\n---\n` separator and calling
  `add_to_clipboard`.
- Report: "Found N code snippets from <app>. Offer to bundle?"

---

## Cross-Device Sync Strategy

### Recommended Batch Size

- Check `averageContentLength` from `get_clipboard_stats`.
- If `averageContentLength > 5000` characters: use batch size **5**.
- Otherwise: use batch size **20**.

### Pagination

- If `totalItems > 500`: always use `limit` parameter on `get_clipboard_history`
  (e.g., `limit: 50`) and paginate through results rather than requesting all at
  once.

### Rate Limiting

- When calling `add_to_clipboard` in a loop (bulk import), wait ~200ms between
  calls to avoid write conflicts in `LocalApiService`.

### Liveness Check

- Before any bulk operation, call `get_clipboard_stats` and verify a valid
  `totalItems` is returned. If the call fails or returns an error, the
  `LocalApiService` is unreachable — instruct the user to check:
  1. The app's Local API setting is enabled.
  2. ADB forward is active: `adb forward tcp:8765 tcp:8765`.
  3. No firewall is blocking localhost:8765.

---

## Performance Monitoring

### Timing Thresholds

| Operation                 | Threshold   | Action if exceeded                                      |
|---------------------------|-------------|---------------------------------------------------------|
| `get_clipboard_stats`     | 200ms       | LocalApiService may be under load; retry once           |
| `search_clipboard`        | 500ms       | Use pagination; consider narrowing the query            |
| `get_clipboard_history`   | 500ms       | Reduce `limit`; paginate in smaller batches             |
| `add_to_clipboard`        | 300ms       | Slow write; add 200ms delay between bulk writes         |

### Heavy Item Detection

- If `averageContentLength > 10000` characters, warn the user:
  > "Large clipboard items detected. Encryption/decryption may be slow. Consider
  > archiving items older than 30 days."

### Diagnostic Steps

1. Time a `get_clipboard_stats` call.
2. If >500ms, LocalApiService is likely under load or the DB is large.
3. Check `totalItems` — if >1000, recommend enabling auto-delete in app settings.
4. Check `averageContentLength` — if >10000, recommend filtering by content type.

---

## Example Workflow: Find and Summarize Recent Code Snippets

```
1. get_clipboard_stats()
   → Check totalItems, mostUsedContentType

2. get_clipboard_history(limit=50)
   → Filter items where classification == CODE

3. For each CODE item with IDE context:
   → Offer: "Generate docstring / test stub / README snippet?"

4. If user says yes:
   → generateHelper(item.content, language, helperType)
   → add_to_clipboard(content=helper)
   → "Helper added to clipboard — paste it in your IDE."
```

## Example Workflow: Clean Up Old Clipboard History

```
1. get_clipboard_stats()
   → Note totalItems, lastActivityTimestamp

2. get_clipboard_history(limit=100)
   → Identify stale items (>14 days, not favorite)
   → Identify duplicates

3. Report findings:
   → "Found 12 stale items and 3 duplicate groups."
   → "Recommend deleting 15 items total."

4. On confirmation, delete via the Android app or REST API directly.
```
