---
name: speckit-git-feature
description: Create a feature branch with sequential or timestamp numbering
compatibility: Requires spec-kit project structure with .specify/ directory
metadata:
  author: github-spec-kit
  source: git:commands/speckit.git.feature.md
---

# Create Feature Branch

Create and switch to a new git feature branch for the given specification. This command handles **branch creation only** — the spec directory and files are created by the core `/speckit.specify` workflow.

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Environment Variable Override

If the user explicitly provided `GIT_BRANCH_NAME` (e.g., via environment variable, argument, or in their request), pass it through to the script by setting the `GIT_BRANCH_NAME` environment variable before invoking the script. When `GIT_BRANCH_NAME` is set:
- The script uses the exact value as the branch name, bypassing all prefix/suffix generation
- `--short-name`, `--number`, `--timestamp`, `--jira-id`, and `--no-jira-id` flags are ignored
- `FEATURE_NUM` is extracted from the name if it starts with a numeric prefix (`123-` or `YYYYMMDD-HHMMSS-`) or a Jira-style id (`^[A-Z][A-Z0-9_]*-[0-9]+-`); otherwise set to the full branch name

## SPECKIT-LOCAL-PATCH: Jira-id branch prefix (this project)

This project's local convention is to prefix feature branches with the Jira ticket id (e.g. `DD-41656-extended-test-disqualification`) instead of a sequential `001-...` number. The patched script supports this via:

- **Auto-detection** — if the feature description starts with a token matching `^[A-Z][A-Z0-9_]*-[0-9]+` (e.g. `DD-41656`, `CCT-1222`, `PROJ_X-9`), the script uses it as the branch prefix and strips it from the short-name suffix automatically.
- **Explicit flag** — `--jira-id <ID>` (or `JIRA_ID=<ID>` / `SPECKIT_JIRA_ID=<ID>` env var) overrides auto-detection.
- **Opt-out** — `--no-jira-id` falls back to sequential `001-...` numbering for features that aren't tracked in Jira (spikes, refactors, etc.).

**Before invoking the script you MUST do the following:**

1. Inspect the user's feature description. If it starts with a Jira-style id matching `^[A-Z][A-Z0-9_]*-[0-9]+` (e.g. `DD-41656`, `CCT-1222`, `PROJ_X-9`), proceed normally — the script will detect and use it. Skip step 2.
2. If the description does NOT start with a Jira id (and the user has not set `JIRA_ID` / `SPECKIT_JIRA_ID` / `GIT_BRANCH_NAME` already), ASK the user before invoking the script:
   > "What Jira ticket is this for? (e.g. `DD-41656`. Reply with `none` to skip Jira numbering and use a sequential `001-...` prefix.)"
3. Wait for the user's reply.
   - If they give a Jira id matching `^[A-Z][A-Z0-9_]*-[0-9]+`, pass it via `--jira-id <ID>` to the script.
   - If they reply with `none` (or similar), pass `--no-jira-id`.
   - If they give an invalid id, ask once more for clarification.
4. Then invoke the script with the appropriate flag.

The script is invoked non-interactively from this skill, so it cannot prompt the user itself — if you skip step 2 above and the user's description has no Jira id, the script will exit 2 with a helpful error. Recover by asking the user and re-invoking.

## Prerequisites

- Verify Git is available by running `git rev-parse --is-inside-work-tree 2>/dev/null`
- If Git is not available, warn the user and skip branch creation

## Branch Numbering Mode

Determine the branch numbering strategy by checking configuration in this order:

1. Check `.specify/extensions/git/git-config.yml` for `branch_numbering` value
2. Check `.specify/init-options.json` for `branch_numbering` value (backward compatibility)
3. Default to `sequential` if neither exists

## Execution

Generate a concise short name (2-4 words) for the branch:
- Analyze the feature description and extract the most meaningful keywords
- Use action-noun format when possible (e.g., "add-user-auth", "fix-payment-bug")
- Preserve technical terms and acronyms (OAuth2, API, JWT, etc.)

Run the appropriate script based on your platform. Pass `--jira-id <ID>` or `--no-jira-id` UNLESS the description already starts with a Jira id — in that case auto-detect handles it and no flag is needed. The script exits 2 only when none of these three signals — explicit flag, env var, or description prefix — is present:

- **Bash (Jira id auto-detected from description, no flag needed)**: `.specify/extensions/git/scripts/bash/create-new-feature.sh --json --short-name "<short-name>" "<feature description starting with the Jira id, e.g. 'DD-41656 - foo bar'>"`
- **Bash (Jira id explicit)**: `.specify/extensions/git/scripts/bash/create-new-feature.sh --json --jira-id "<JIRA-ID>" --short-name "<short-name>" "<feature description>"`
- **Bash (no Jira id, sequential fallback)**: `.specify/extensions/git/scripts/bash/create-new-feature.sh --json --no-jira-id --short-name "<short-name>" "<feature description>"`
- **Bash (timestamp)**: `.specify/extensions/git/scripts/bash/create-new-feature.sh --json --timestamp --no-jira-id --short-name "<short-name>" "<feature description>"`
- **PowerShell**: `.specify/extensions/git/scripts/powershell/create-new-feature.ps1 -Json -ShortName "<short-name>" "<feature description>"` (PowerShell variant has not been patched for Jira-id support; on Windows pass `GIT_BRANCH_NAME=<JIRA-ID>-<short-name>` instead)
- **PowerShell (timestamp)**: `.specify/extensions/git/scripts/powershell/create-new-feature.ps1 -Json -Timestamp -ShortName "<short-name>" "<feature description>"`

**IMPORTANT**:
- Do NOT pass `--number` — the script determines the correct next number automatically
- Always include the JSON flag (`--json` for Bash, `-Json` for PowerShell) so the output can be parsed reliably
- You must only ever run this script once per feature
- The JSON output will contain `BRANCH_NAME` and `FEATURE_NUM`

## Graceful Degradation

If Git is not installed or the current directory is not a Git repository:
- Branch creation is skipped with a warning: `[specify] Warning: Git repository not detected; skipped branch creation`
- The script still outputs `BRANCH_NAME` and `FEATURE_NUM` so the caller can reference them

## Output

The script outputs JSON with:
- `BRANCH_NAME`: The branch name (e.g., `003-user-auth` or `20260319-143022-user-auth`)
- `FEATURE_NUM`: The numeric or timestamp prefix used