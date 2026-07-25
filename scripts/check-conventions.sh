#!/usr/bin/env bash
set -euo pipefail

pattern='^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([a-z0-9._/-]+\))?!?: .+'

check_subject() {
  local subject="$1"
  if [[ "$subject" == Merge\ * ]]; then
    return
  fi
  if [[ ! "$subject" =~ $pattern ]]; then
    echo "Non-conventional commit or PR title: $subject" >&2
    exit 1
  fi
}

if [[ -n "${PR_TITLE:-}" ]]; then
  check_subject "$PR_TITLE"
fi

if [[ -n "${BASE_SHA:-}" && "$BASE_SHA" != 0000000000000000000000000000000000000000 ]]; then
  while IFS= read -r subject; do
    check_subject "$subject"
  done < <(git log --format=%s "$BASE_SHA..HEAD")
fi
