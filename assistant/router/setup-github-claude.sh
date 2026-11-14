#!/usr/bin/env bash
# Open a PR that adds .github/workflows/claude.yml (claude-workflow.yml) to a GitHub repo, so
# issues from the Claude Panel that mention @claude get worked on. Does not merge, and never
# touches secrets: the repo needs ANTHROPIC_API_KEY or CLAUDE_CODE_OAUTH_TOKEN (set it yourself).
#   setup-github-claude.sh --update owner/repo ...   refresh claude.yml on an open PR's branch
#   setup-github-claude.sh owner/repo [owner/repo ...]
#   setup-github-claude.sh --all          every slug in repos.json
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
BRANCH=ci/claude-code-action
WF=.github/workflows/claude.yml
[ $# -gt 0 ] || { sed -n '2,7p' "$0"; exit 2; }
UPDATE=0; [ "$1" = --update ] && { UPDATE=1; shift; }
if [ "${1:-}" = --all ]; then mapfile -t SLUGS < <(jq -r '.[].slug' "$HERE/repos.json"); else SLUGS=("$@"); fi
# Writing .github/workflows/* needs the token's `workflow` scope (GitHub answers 404 without it).
if ! gh auth status 2>&1 | grep -q "'workflow'"; then
  echo "gh token lacks the 'workflow' scope: run  gh auth refresh -h github.com -s workflow  first" >&2
  exit 3
fi
CONTENT=$(base64 -w0 "$HERE/claude-workflow.yml")
for slug in "${SLUGS[@]}"; do
  [[ $slug =~ ^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$ ]] || { echo "$slug: not owner/repo" >&2; continue; }
  if gh api "repos/$slug/contents/$WF" --silent 2>/dev/null; then echo "$slug: $WF already on the default branch"; continue; fi
  if pr=$(gh pr list -R "$slug" --head "$BRANCH" --json url -q '.[0].url' 2>/dev/null) && [ -n "$pr" ]; then
    if [ "$UPDATE" = 1 ]; then
      sha=$(gh api "repos/$slug/contents/$WF?ref=$BRANCH" -q .sha)
      gh api -X PUT "repos/$slug/contents/$WF" -f branch="$BRANCH" -f content="$CONTENT" -f sha="$sha" \
        -f message="ci: claude.yml uses ANTHROPIC_API_KEY or the OAuth token, and GITHUB_TOKEN" --silent
      echo "$slug: updated $pr"
    else
      echo "$slug: PR already open: $pr"
    fi
    continue
  fi
  base=$(gh repo view "$slug" --json defaultBranchRef -q .defaultBranchRef.name)
  sha=$(gh api "repos/$slug/git/ref/heads/$base" -q .object.sha)
  gh api "repos/$slug/git/refs" -f ref="refs/heads/$BRANCH" -f sha="$sha" --silent 2>/dev/null || true
  gh api -X PUT "repos/$slug/contents/$WF" -f branch="$BRANCH" -f content="$CONTENT" \
    -f message="ci: Claude Code GitHub Action for @claude issues from the owner" --silent
  gh pr create -R "$slug" --base "$base" --head "$BRANCH" \
    --title "ci: add Claude Code GitHub Action (@claude, owner only)" \
    --body "Adds \`$WF\`: \`anthropics/claude-code-action@v1\` runs when **sparesparrow** mentions @claude in a new issue or an issue comment, and finishes code changes with a **draft** PR. Issues come from the Claude Panel on the phone (cliphist-android \`assistant/router\`).

Before merging, set the secret: \`claude setup-token\`, then \`gh secret set CLAUDE_CODE_OAUTH_TOKEN -R $slug\` (or run \`/install-github-app\` in Claude Code).

🤖 Generated with [Claude Code](https://claude.com/claude-code)"
done
