#!/usr/bin/env bash
# Git extension: create-new-feature.sh
# Adapted from core scripts/bash/create-new-feature.sh for extension layout.
# Sources common.sh from the project's installed scripts, falling back to
# git-common.sh for minimal git helpers.

set -e

JSON_MODE=false
DRY_RUN=false
ALLOW_EXISTING=false
SHORT_NAME=""
BRANCH_NUMBER=""
USE_TIMESTAMP=false
# SPECKIT-LOCAL-PATCH: Jira-id support
JIRA_ID_ARG=""
NO_JIRA_ID=false
ARGS=()
i=1
while [ $i -le $# ]; do
    arg="${!i}"
    case "$arg" in
        --json)
            JSON_MODE=true
            ;;
        --dry-run)
            DRY_RUN=true
            ;;
        --allow-existing-branch)
            ALLOW_EXISTING=true
            ;;
        --short-name)
            if [ $((i + 1)) -gt $# ]; then
                echo 'Error: --short-name requires a value' >&2
                exit 1
            fi
            i=$((i + 1))
            next_arg="${!i}"
            if [[ "$next_arg" == --* ]]; then
                echo 'Error: --short-name requires a value' >&2
                exit 1
            fi
            SHORT_NAME="$next_arg"
            ;;
        --number)
            if [ $((i + 1)) -gt $# ]; then
                echo 'Error: --number requires a value' >&2
                exit 1
            fi
            i=$((i + 1))
            next_arg="${!i}"
            if [[ "$next_arg" == --* ]]; then
                echo 'Error: --number requires a value' >&2
                exit 1
            fi
            BRANCH_NUMBER="$next_arg"
            if [[ ! "$BRANCH_NUMBER" =~ ^[0-9]+$ ]]; then
                echo 'Error: --number must be a non-negative integer' >&2
                exit 1
            fi
            ;;
        --timestamp)
            USE_TIMESTAMP=true
            ;;
        --jira-id)
            # SPECKIT-LOCAL-PATCH: explicit Jira-id override
            if [ $((i + 1)) -gt $# ]; then
                echo 'Error: --jira-id requires a value' >&2
                exit 1
            fi
            i=$((i + 1))
            next_arg="${!i}"
            if [[ "$next_arg" == --* ]]; then
                echo 'Error: --jira-id requires a value' >&2
                exit 1
            fi
            JIRA_ID_ARG="$next_arg"
            ;;
        --no-jira-id)
            # SPECKIT-LOCAL-PATCH: opt out of Jira-id requirement; use sequential
            NO_JIRA_ID=true
            ;;
        --help|-h)
            echo "Usage: $0 [--json] [--dry-run] [--allow-existing-branch] [--short-name <name>] [--number N] [--timestamp] [--jira-id <ID>] [--no-jira-id] <feature_description>"
            echo ""
            echo "Options:"
            echo "  --json              Output in JSON format"
            echo "  --dry-run           Compute branch name without creating the branch"
            echo "  --allow-existing-branch  Switch to branch if it already exists instead of failing"
            echo "  --short-name <name> Provide a custom short name (2-4 words) for the branch"
            echo "  --number N          Specify branch number manually (overrides Jira/auto-detection)"
            echo "  --timestamp         Use timestamp prefix (YYYYMMDD-HHMMSS) instead of sequential numbering"
            echo "  --jira-id <ID>      Use a Jira-style ticket id (e.g. DD-41656) as the branch prefix [LOCAL PATCH]"
            echo "  --no-jira-id        Skip Jira-id prompt and fall back to sequential numbering [LOCAL PATCH]"
            echo "  --help, -h          Show this help message"
            echo ""
            echo "Environment variables:"
            echo "  GIT_BRANCH_NAME     Use this exact branch name, bypassing all prefix/suffix generation"
            echo "  JIRA_ID             Use as Jira-id prefix (equivalent to --jira-id) [LOCAL PATCH]"
            echo ""
            echo "Examples:"
            echo "  $0 'DD-41656 - Add extended test disqualification warning'   # Jira id auto-detected"
            echo "  $0 --jira-id DD-41656 'Add extended test disqualification warning'"
            echo "  $0 --no-jira-id 'Quick spike for X'                            # sequential 001-... fallback"
            echo "  $0 'Add user authentication system' --short-name 'user-auth' --no-jira-id"
            echo "  $0 --timestamp --short-name 'user-auth' 'Add user authentication' --no-jira-id"
            echo "  GIT_BRANCH_NAME=my-branch $0 'feature description'"
            exit 0
            ;;
        *)
            ARGS+=("$arg")
            ;;
    esac
    i=$((i + 1))
done

FEATURE_DESCRIPTION="${ARGS[*]}"
if [ -z "$FEATURE_DESCRIPTION" ]; then
    echo "Usage: $0 [--json] [--dry-run] [--allow-existing-branch] [--short-name <name>] [--number N] [--timestamp] <feature_description>" >&2
    exit 1
fi

# Trim whitespace and validate description is not empty
FEATURE_DESCRIPTION=$(echo "$FEATURE_DESCRIPTION" | xargs)
if [ -z "$FEATURE_DESCRIPTION" ]; then
    echo "Error: Feature description cannot be empty or contain only whitespace" >&2
    exit 1
fi

# SPECKIT-LOCAL-PATCH: capture-and-strip a leading Jira-style ticket id from
# the description, regardless of how the prefix is ultimately resolved. The
# captured id is used later as the branch prefix unless the user overrode it
# (--jira-id / JIRA_ID env / --no-jira-id / --timestamp / --number). The
# strip itself is unconditional — auto-generated short-name suffixes should
# never duplicate the leading id, no matter which prefix mode is active.
DESC_JIRA_ID=""
if [[ "$FEATURE_DESCRIPTION" =~ ^([A-Z][A-Z0-9_]*-[0-9]+)([[:space:]]*[-–:]?[[:space:]]*)?(.*)$ ]]; then
    DESC_JIRA_ID="${BASH_REMATCH[1]}"
    _stripped="${BASH_REMATCH[3]}"
    # Only strip if there is a description left after removing the id; if the
    # entire input was just the id, leave FEATURE_DESCRIPTION alone so the
    # downstream short-name generator still has something to work with.
    if [ -n "$_stripped" ]; then
        FEATURE_DESCRIPTION="$_stripped"
    fi
fi

# Function to get highest number from specs directory
get_highest_from_specs() {
    local specs_dir="$1"
    local highest=0

    if [ -d "$specs_dir" ]; then
        for dir in "$specs_dir"/*; do
            [ -d "$dir" ] || continue
            dirname=$(basename "$dir")
            # Match sequential prefixes (>=3 digits), but skip timestamp dirs.
            if echo "$dirname" | grep -Eq '^[0-9]{3,}-' && ! echo "$dirname" | grep -Eq '^[0-9]{8}-[0-9]{6}-'; then
                number=$(echo "$dirname" | grep -Eo '^[0-9]+')
                number=$((10#$number))
                if [ "$number" -gt "$highest" ]; then
                    highest=$number
                fi
            fi
        done
    fi

    echo "$highest"
}

# Function to get highest number from git branches
get_highest_from_branches() {
    git branch -a 2>/dev/null | sed 's/^[* ]*//; s|^remotes/[^/]*/||' | _extract_highest_number
}

# Extract the highest sequential feature number from a list of ref names (one per line).
_extract_highest_number() {
    local highest=0
    while IFS= read -r name; do
        [ -z "$name" ] && continue
        if echo "$name" | grep -Eq '^[0-9]{3,}-' && ! echo "$name" | grep -Eq '^[0-9]{8}-[0-9]{6}-'; then
            number=$(echo "$name" | grep -Eo '^[0-9]+' || echo "0")
            number=$((10#$number))
            if [ "$number" -gt "$highest" ]; then
                highest=$number
            fi
        fi
    done
    echo "$highest"
}

# Function to get highest number from remote branches without fetching (side-effect-free)
get_highest_from_remote_refs() {
    local highest=0

    for remote in $(git remote 2>/dev/null); do
        local remote_highest
        remote_highest=$(GIT_TERMINAL_PROMPT=0 git ls-remote --heads "$remote" 2>/dev/null | sed 's|.*refs/heads/||' | _extract_highest_number)
        if [ "$remote_highest" -gt "$highest" ]; then
            highest=$remote_highest
        fi
    done

    echo "$highest"
}

# Function to check existing branches and return next available number.
check_existing_branches() {
    local specs_dir="$1"
    local skip_fetch="${2:-false}"

    if [ "$skip_fetch" = true ]; then
        local highest_remote=$(get_highest_from_remote_refs)
        local highest_branch=$(get_highest_from_branches)
        if [ "$highest_remote" -gt "$highest_branch" ]; then
            highest_branch=$highest_remote
        fi
    else
        git fetch --all --prune >/dev/null 2>&1 || true
        local highest_branch=$(get_highest_from_branches)
    fi

    local highest_spec=$(get_highest_from_specs "$specs_dir")

    local max_num=$highest_branch
    if [ "$highest_spec" -gt "$max_num" ]; then
        max_num=$highest_spec
    fi

    echo $((max_num + 1))
}

# Function to clean and format a branch name
clean_branch_name() {
    local name="$1"
    echo "$name" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9]/-/g' | sed 's/-\+/-/g' | sed 's/^-//' | sed 's/-$//'
}

# ---------------------------------------------------------------------------
# Source common.sh for resolve_template, json_escape, get_repo_root, has_git.
#
# Search locations in priority order:
#  1. .specify/scripts/bash/common.sh under the project root (installed project)
#  2. scripts/bash/common.sh under the project root (source checkout fallback)
#  3. git-common.sh next to this script (minimal fallback — lacks resolve_template)
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(CDPATH="" cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Find project root by walking up from the script location
_find_project_root() {
    local dir="$1"
    while [ "$dir" != "/" ]; do
        if [ -d "$dir/.specify" ] || [ -d "$dir/.git" ]; then
            echo "$dir"
            return 0
        fi
        dir="$(dirname "$dir")"
    done
    return 1
}

_common_loaded=false
_PROJECT_ROOT=$(_find_project_root "$SCRIPT_DIR") || true

if [ -n "$_PROJECT_ROOT" ] && [ -f "$_PROJECT_ROOT/.specify/scripts/bash/common.sh" ]; then
    source "$_PROJECT_ROOT/.specify/scripts/bash/common.sh"
    _common_loaded=true
elif [ -n "$_PROJECT_ROOT" ] && [ -f "$_PROJECT_ROOT/scripts/bash/common.sh" ]; then
    source "$_PROJECT_ROOT/scripts/bash/common.sh"
    _common_loaded=true
elif [ -f "$SCRIPT_DIR/git-common.sh" ]; then
    source "$SCRIPT_DIR/git-common.sh"
    _common_loaded=true
fi

if [ "$_common_loaded" != "true" ]; then
    echo "Error: Could not locate common.sh or git-common.sh. Please ensure the Specify core scripts are installed." >&2
    exit 1
fi

# Resolve repository root
if type get_repo_root >/dev/null 2>&1; then
    REPO_ROOT=$(get_repo_root)
elif git rev-parse --show-toplevel >/dev/null 2>&1; then
    REPO_ROOT=$(git rev-parse --show-toplevel)
elif [ -n "$_PROJECT_ROOT" ]; then
    REPO_ROOT="$_PROJECT_ROOT"
else
    echo "Error: Could not determine repository root." >&2
    exit 1
fi

# Check if git is available at this repo root
if type has_git >/dev/null 2>&1; then
    if has_git "$REPO_ROOT"; then
        HAS_GIT=true
    else
        HAS_GIT=false
    fi
elif git -C "$REPO_ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    HAS_GIT=true
else
    HAS_GIT=false
fi

cd "$REPO_ROOT"

SPECS_DIR="$REPO_ROOT/specs"

# Function to generate branch name with stop word filtering
generate_branch_name() {
    local description="$1"

    local stop_words="^(i|a|an|the|to|for|of|in|on|at|by|with|from|is|are|was|were|be|been|being|have|has|had|do|does|did|will|would|should|could|can|may|might|must|shall|this|that|these|those|my|your|our|their|want|need|add|get|set)$"

    local clean_name=$(echo "$description" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9]/ /g')

    local meaningful_words=()
    for word in $clean_name; do
        [ -z "$word" ] && continue
        if ! echo "$word" | grep -qiE "$stop_words"; then
            if [ ${#word} -ge 3 ]; then
                meaningful_words+=("$word")
            elif echo "$description" | grep -qw -- "${word^^}"; then
                meaningful_words+=("$word")
            fi
        fi
    done

    if [ ${#meaningful_words[@]} -gt 0 ]; then
        local max_words=3
        if [ ${#meaningful_words[@]} -eq 4 ]; then max_words=4; fi

        local result=""
        local count=0
        for word in "${meaningful_words[@]}"; do
            if [ $count -ge $max_words ]; then break; fi
            if [ -n "$result" ]; then result="$result-"; fi
            result="$result$word"
            count=$((count + 1))
        done
        echo "$result"
    else
        local cleaned=$(clean_branch_name "$description")
        echo "$cleaned" | tr '-' '\n' | grep -v '^$' | head -3 | tr '\n' '-' | sed 's/-$//'
    fi
}

# SPECKIT-LOCAL-PATCH: resolve the Jira id used as the branch prefix.
# Resolution priority (highest first):
#   1. --jira-id <ID> flag
#   2. JIRA_ID environment variable
#   3. Auto-detect from the start of the feature description (e.g. "DD-41656 - foo")
#   4. Interactive prompt when stdin is a TTY
#   5. Error out (non-interactive) — caller must supply the id
# Bypasses (no Jira id used at all):
#   - GIT_BRANCH_NAME (explicit, full branch name overrides everything)
#   - --no-jira-id flag (use sequential / timestamp numbering instead)
#   - --timestamp flag (forces timestamp prefix; Jira id is ignored)
#   - --number N flag (forces sequential prefix N; Jira id is ignored)
_jira_pattern='^[A-Z][A-Z0-9_]*-[0-9]+$'
# Capture env var first so subsequent JIRA_ID= assignments don't shadow it.
_JIRA_ID_ENV="${JIRA_ID:-${SPECKIT_JIRA_ID:-}}"
JIRA_ID=""
if [ -z "${GIT_BRANCH_NAME:-}" ] \
   && [ "$NO_JIRA_ID" != true ] \
   && [ "$USE_TIMESTAMP" != true ] \
   && [ -z "$BRANCH_NUMBER" ]; then

    # 1. --jira-id flag
    if [ -n "$JIRA_ID_ARG" ]; then
        JIRA_ID="$JIRA_ID_ARG"
    # 2. JIRA_ID / SPECKIT_JIRA_ID env var (captured above)
    elif [ -n "$_JIRA_ID_ENV" ]; then
        JIRA_ID="$_JIRA_ID_ENV"
    fi

    # 3. Auto-detected from the description prefix (captured + stripped earlier)
    if [ -z "$JIRA_ID" ] && [ -n "$DESC_JIRA_ID" ]; then
        JIRA_ID="$DESC_JIRA_ID"
    fi

    # 4. Interactive prompt
    if [ -z "$JIRA_ID" ] && [ -t 0 ]; then
        printf 'Jira ticket id (e.g. DD-41656), or press enter to skip and use sequential numbering: ' >&2
        IFS= read -r _input_jira_id || _input_jira_id=""
        if [ -n "$_input_jira_id" ]; then
            JIRA_ID="$_input_jira_id"
        else
            NO_JIRA_ID=true
        fi
    fi

    # 5. Non-interactive and still nothing → error out with a helpful message
    if [ -z "$JIRA_ID" ] && [ "$NO_JIRA_ID" != true ]; then
        cat >&2 <<'EOF'
Error: no Jira ticket id provided.

Provide one in any of these ways:
  - Start the description with the id, e.g. "DD-41656 - your feature"
  - Pass the flag: --jira-id DD-41656
  - Set an env var: JIRA_ID=DD-41656 (or SPECKIT_JIRA_ID=DD-41656)
  - Or opt out with --no-jira-id to use sequential numbering instead.
EOF
        exit 2
    fi

    # Validate the Jira id (covers --jira-id, env, prompt; auto-detect already matched the pattern)
    if [ -n "$JIRA_ID" ] && [[ ! "$JIRA_ID" =~ $_jira_pattern ]]; then
        echo "Error: '$JIRA_ID' is not a valid Jira id (expected pattern: ABC-123 or PROJ-4567)." >&2
        exit 2
    fi
fi

# Check for GIT_BRANCH_NAME env var override (exact branch name, no prefix/suffix)
if [ -n "${GIT_BRANCH_NAME:-}" ]; then
    BRANCH_NAME="$GIT_BRANCH_NAME"
    # Extract FEATURE_NUM from the branch name if it starts with a numeric prefix
    # Check timestamp pattern first (YYYYMMDD-HHMMSS-) since it also matches the simpler ^[0-9]+ pattern
    if echo "$BRANCH_NAME" | grep -Eq '^[0-9]{8}-[0-9]{6}-'; then
        FEATURE_NUM=$(echo "$BRANCH_NAME" | grep -Eo '^[0-9]{8}-[0-9]{6}')
        BRANCH_SUFFIX="${BRANCH_NAME#${FEATURE_NUM}-}"
    elif echo "$BRANCH_NAME" | grep -Eq '^[0-9]+-'; then
        FEATURE_NUM=$(echo "$BRANCH_NAME" | grep -Eo '^[0-9]+')
        BRANCH_SUFFIX="${BRANCH_NAME#${FEATURE_NUM}-}"
    elif echo "$BRANCH_NAME" | grep -Eq '^[A-Z][A-Z0-9_]*-[0-9]+-'; then
        # SPECKIT-LOCAL-PATCH: Jira-prefixed branch (e.g. DD-41656-foo-bar,
        # PROJ_X-9-foo, CCT2-12-foo). Pattern matches the same Jira-id grammar
        # used by --jira-id validation and description auto-detection (see
        # _jira_pattern earlier in this file).
        FEATURE_NUM=$(echo "$BRANCH_NAME" | grep -Eo '^[A-Z][A-Z0-9_]*-[0-9]+')
        BRANCH_SUFFIX="${BRANCH_NAME#${FEATURE_NUM}-}"
    else
        FEATURE_NUM="$BRANCH_NAME"
        BRANCH_SUFFIX="$BRANCH_NAME"
    fi
else
    # Generate branch name
    if [ -n "$SHORT_NAME" ]; then
        BRANCH_SUFFIX=$(clean_branch_name "$SHORT_NAME")
    else
        BRANCH_SUFFIX=$(generate_branch_name "$FEATURE_DESCRIPTION")
    fi

    # Warn if --number and --timestamp are both specified
    if [ "$USE_TIMESTAMP" = true ] && [ -n "$BRANCH_NUMBER" ]; then
        >&2 echo "[specify] Warning: --number is ignored when --timestamp is used"
        BRANCH_NUMBER=""
    fi

    # Determine branch prefix
    if [ -n "$JIRA_ID" ]; then
        # SPECKIT-LOCAL-PATCH: Jira-prefixed branch
        FEATURE_NUM="$JIRA_ID"
        BRANCH_NAME="${FEATURE_NUM}-${BRANCH_SUFFIX}"
    elif [ "$USE_TIMESTAMP" = true ]; then
        FEATURE_NUM=$(date +%Y%m%d-%H%M%S)
        BRANCH_NAME="${FEATURE_NUM}-${BRANCH_SUFFIX}"
    else
        if [ -z "$BRANCH_NUMBER" ]; then
            if [ "$DRY_RUN" = true ] && [ "$HAS_GIT" = true ]; then
                BRANCH_NUMBER=$(check_existing_branches "$SPECS_DIR" true)
            elif [ "$DRY_RUN" = true ]; then
                HIGHEST=$(get_highest_from_specs "$SPECS_DIR")
                BRANCH_NUMBER=$((HIGHEST + 1))
            elif [ "$HAS_GIT" = true ]; then
                BRANCH_NUMBER=$(check_existing_branches "$SPECS_DIR")
            else
                HIGHEST=$(get_highest_from_specs "$SPECS_DIR")
                BRANCH_NUMBER=$((HIGHEST + 1))
            fi
        fi

        FEATURE_NUM=$(printf "%03d" "$((10#$BRANCH_NUMBER))")
        BRANCH_NAME="${FEATURE_NUM}-${BRANCH_SUFFIX}"
    fi
fi

# GitHub enforces a 244-byte limit on branch names
MAX_BRANCH_LENGTH=244
_byte_length() { printf '%s' "$1" | LC_ALL=C wc -c | tr -d ' '; }
BRANCH_BYTE_LEN=$(_byte_length "$BRANCH_NAME")
if [ -n "${GIT_BRANCH_NAME:-}" ] && [ "$BRANCH_BYTE_LEN" -gt $MAX_BRANCH_LENGTH ]; then
    >&2 echo "Error: GIT_BRANCH_NAME must be 244 bytes or fewer in UTF-8. Provided value is ${BRANCH_BYTE_LEN} bytes."
    exit 1
elif [ "$BRANCH_BYTE_LEN" -gt $MAX_BRANCH_LENGTH ]; then
    PREFIX_LENGTH=$(( ${#FEATURE_NUM} + 1 ))
    MAX_SUFFIX_LENGTH=$((MAX_BRANCH_LENGTH - PREFIX_LENGTH))

    TRUNCATED_SUFFIX=$(echo "$BRANCH_SUFFIX" | cut -c1-$MAX_SUFFIX_LENGTH)
    TRUNCATED_SUFFIX=$(echo "$TRUNCATED_SUFFIX" | sed 's/-$//')

    ORIGINAL_BRANCH_NAME="$BRANCH_NAME"
    BRANCH_NAME="${FEATURE_NUM}-${TRUNCATED_SUFFIX}"

    >&2 echo "[specify] Warning: Branch name exceeded GitHub's 244-byte limit"
    >&2 echo "[specify] Original: $ORIGINAL_BRANCH_NAME (${#ORIGINAL_BRANCH_NAME} bytes)"
    >&2 echo "[specify] Truncated to: $BRANCH_NAME (${#BRANCH_NAME} bytes)"
fi

if [ "$DRY_RUN" != true ]; then
    if [ "$HAS_GIT" = true ]; then
        branch_create_error=""
        if ! branch_create_error=$(git checkout -q -b "$BRANCH_NAME" 2>&1); then
            current_branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || true)"
            if git branch --list "$BRANCH_NAME" | grep -q .; then
                if [ "$ALLOW_EXISTING" = true ]; then
                    if [ "$current_branch" = "$BRANCH_NAME" ]; then
                        :
                    elif ! switch_branch_error=$(git checkout -q "$BRANCH_NAME" 2>&1); then
                        >&2 echo "Error: Failed to switch to existing branch '$BRANCH_NAME'. Please resolve any local changes or conflicts and try again."
                        if [ -n "$switch_branch_error" ]; then
                            >&2 printf '%s\n' "$switch_branch_error"
                        fi
                        exit 1
                    fi
                elif [ "$USE_TIMESTAMP" = true ]; then
                    >&2 echo "Error: Branch '$BRANCH_NAME' already exists. Rerun to get a new timestamp or use a different --short-name."
                    exit 1
                else
                    >&2 echo "Error: Branch '$BRANCH_NAME' already exists. Please use a different feature name or specify a different number with --number."
                    exit 1
                fi
            else
                >&2 echo "Error: Failed to create git branch '$BRANCH_NAME'."
                if [ -n "$branch_create_error" ]; then
                    >&2 printf '%s\n' "$branch_create_error"
                else
                    >&2 echo "Please check your git configuration and try again."
                fi
                exit 1
            fi
        fi
    else
        >&2 echo "[specify] Warning: Git repository not detected; skipped branch creation for $BRANCH_NAME"
    fi

    printf '# To persist: export SPECIFY_FEATURE=%q\n' "$BRANCH_NAME" >&2
fi

if $JSON_MODE; then
    if command -v jq >/dev/null 2>&1; then
        if [ "$DRY_RUN" = true ]; then
            jq -cn \
                --arg branch_name "$BRANCH_NAME" \
                --arg feature_num "$FEATURE_NUM" \
                '{BRANCH_NAME:$branch_name,FEATURE_NUM:$feature_num,DRY_RUN:true}'
        else
            jq -cn \
                --arg branch_name "$BRANCH_NAME" \
                --arg feature_num "$FEATURE_NUM" \
                '{BRANCH_NAME:$branch_name,FEATURE_NUM:$feature_num}'
        fi
    else
        if type json_escape >/dev/null 2>&1; then
            _je_branch=$(json_escape "$BRANCH_NAME")
            _je_num=$(json_escape "$FEATURE_NUM")
        else
            _je_branch="$BRANCH_NAME"
            _je_num="$FEATURE_NUM"
        fi
        if [ "$DRY_RUN" = true ]; then
            printf '{"BRANCH_NAME":"%s","FEATURE_NUM":"%s","DRY_RUN":true}\n' "$_je_branch" "$_je_num"
        else
            printf '{"BRANCH_NAME":"%s","FEATURE_NUM":"%s"}\n' "$_je_branch" "$_je_num"
        fi
    fi
else
    echo "BRANCH_NAME: $BRANCH_NAME"
    echo "FEATURE_NUM: $FEATURE_NUM"
    if [ "$DRY_RUN" != true ]; then
        printf '# To persist in your shell: export SPECIFY_FEATURE=%q\n' "$BRANCH_NAME"
    fi
fi
