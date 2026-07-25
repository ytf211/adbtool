#!/usr/bin/env bash
# Compatibility entry point for provisioning the pinned Android toolchain.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
exec "$ROOT/gradlew" provisionAndroidPlatform provisionR8 "$@"
