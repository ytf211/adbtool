#!/usr/bin/env bash
# Compatibility entry point. Prefer ./gradlew assemble.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
exec "$ROOT/gradlew" assemble "$@"
