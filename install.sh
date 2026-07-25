#!/usr/bin/env bash
# Installs a verified adbtool DEX JAR and launcher on an Android device.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
ADB="${ADB:-adb}"
SERIAL="${ADBTOOL_SERIAL:-}"
VERSION="${ADBTOOL_VERSION:-}"
JAR_PATH="${ADBTOOL_JAR:-}"
REPOSITORY="${ADBTOOL_REPOSITORY:-ytf211/adbtool}"
TEMP_DIRECTORY=""
SERIAL_ARGUMENT_SET=false

usage() {
  cat <<'EOF'
Usage: ./install.sh [SERIAL] [--serial SERIAL] [--jar PATH | --version VERSION]

Options:
  --serial SERIAL    Target adb serial or host:port
  --jar PATH         Install a local versioned JAR with adjacent .sha256 file
  --version VERSION  Download adbtool-VERSION.jar from GitHub Releases
  -h, --help         Show this help

Environment: ADB, ADBTOOL_SERIAL, ADBTOOL_JAR, ADBTOOL_VERSION,
             ADBTOOL_REPOSITORY
EOF
}

while (($#)); do
  case "$1" in
    --serial)
      [[ $# -ge 2 ]] || { echo "--serial requires a value" >&2; exit 2; }
      SERIAL="$2"
      SERIAL_ARGUMENT_SET=true
      shift 2
      ;;
    --jar)
      [[ $# -ge 2 ]] || { echo "--jar requires a value" >&2; exit 2; }
      JAR_PATH="$2"
      shift 2
      ;;
    --version)
      [[ $# -ge 2 ]] || { echo "--version requires a value" >&2; exit 2; }
      VERSION="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      if [[ "$1" != -* && "$SERIAL_ARGUMENT_SET" == false ]]; then
        SERIAL="$1"
        SERIAL_ARGUMENT_SET=true
        shift
      else
        echo "Unknown argument: $1" >&2
        usage >&2
        exit 2
      fi
      ;;
  esac
done

if [[ -n "$JAR_PATH" && -n "$VERSION" ]]; then
  echo "Use either --jar or --version, not both" >&2
  exit 2
fi

cleanup() {
  if [[ -n "$TEMP_DIRECTORY" ]]; then
    rm -rf "$TEMP_DIRECTORY"
  fi
}
trap cleanup EXIT

if [[ -n "$VERSION" ]]; then
  if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+([+-][0-9A-Za-z.-]+)?$ ]]; then
    echo "Invalid semantic version: $VERSION" >&2
    exit 2
  fi
  TEMP_DIRECTORY="$(mktemp -d)"
  asset="adbtool-$VERSION.jar"
  release_url="https://github.com/$REPOSITORY/releases/download/v$VERSION"
  curl -fsSL --retry 2 -o "$TEMP_DIRECTORY/$asset" "$release_url/$asset"
  curl -fsSL --retry 2 -o "$TEMP_DIRECTORY/$asset.sha256" "$release_url/$asset.sha256"
  JAR_PATH="$TEMP_DIRECTORY/$asset"
elif [[ -z "$JAR_PATH" ]]; then
  shopt -s nullglob
  candidates=("$ROOT"/build/dist/adbtool-*.jar)
  shopt -u nullglob
  if [[ ${#candidates[@]} -ne 1 ]]; then
    echo "Expected exactly one build/dist/adbtool-*.jar; found ${#candidates[@]}" >&2
    echo "Run ./gradlew clean assemble, or pass --jar/--version." >&2
    exit 1
  fi
  JAR_PATH="${candidates[0]}"
fi

if [[ ! -f "$JAR_PATH" ]]; then
  echo "JAR not found: $JAR_PATH" >&2
  exit 1
fi
CHECKSUM_PATH="$JAR_PATH.sha256"
if [[ ! -f "$CHECKSUM_PATH" ]]; then
  echo "Checksum file not found: $CHECKSUM_PATH" >&2
  exit 1
fi
verify_checksum() {
  local jar="$1" checksum_file="$2"
  local expected="" listed="" extra="" actual=""
  local -a checksum_lines=()
  mapfile -t checksum_lines <"$checksum_file"
  if [[ ${#checksum_lines[@]} -ne 1 ]]; then
    echo "Checksum file must contain exactly one line: $checksum_file" >&2
    exit 1
  fi
  read -r expected listed extra <<<"${checksum_lines[0]}" || true
  listed=${listed#\*}
  if [[ ! "$expected" =~ ^[0-9a-fA-F]{64}$ || "$listed" != "$(basename "$jar")" || -n "$extra" ]]; then
    echo "Invalid checksum file: $checksum_file" >&2
    exit 1
  fi
  actual=$(sha256sum "$jar")
  actual=${actual%% *}
  if [[ "${actual,,}" != "${expected,,}" ]]; then
    echo "Checksum mismatch for $jar" >&2
    exit 1
  fi
  printf '%s: OK\n' "$(basename "$jar")"
}

verify_checksum "$JAR_PATH" "$CHECKSUM_PATH"

if [[ -n "$SERIAL" && "$SERIAL" == *:* ]]; then
  "$ADB" connect "$SERIAL" >/dev/null
fi

run_adb() {
  if [[ -n "$SERIAL" ]]; then
    "$ADB" -s "$SERIAL" "$@"
  else
    "$ADB" "$@"
  fi
}

remote_jar="/data/local/tmp/adbtool.jar"
remote_launcher="/data/local/tmp/adbtool"
remote_suffix="$$.$RANDOM"
remote_jar_new="$remote_jar.$remote_suffix.new"
remote_launcher_new="$remote_launcher.$remote_suffix.new"
remote_jar_backup="$remote_jar.$remote_suffix.backup"
remote_launcher_backup="$remote_launcher.$remote_suffix.backup"
remote_lock="/data/local/tmp/adbtool.install.lock"

run_adb push "$JAR_PATH" "$remote_jar_new"
run_adb push "$ROOT/device/adbtool" "$remote_launcher_new"
run_adb shell "CLASSPATH='$remote_jar_new' app_process /system/bin com.adbtool.AdbTool help" >/dev/null

transaction="set -e; jar_had=0; launcher_had=0; jar_replaced=0; launcher_replaced=0; lock_had=0; finish() { status=\$?; trap - 0; set +e; if [ \"\$status\" -ne 0 ]; then if [ \"\$jar_replaced\" -eq 1 ]; then if [ \"\$jar_had\" -eq 1 ]; then mv -f '$remote_jar_backup' '$remote_jar'; else rm -f '$remote_jar'; fi; fi; if [ \"\$launcher_replaced\" -eq 1 ]; then if [ \"\$launcher_had\" -eq 1 ]; then mv -f '$remote_launcher_backup' '$remote_launcher'; else rm -f '$remote_launcher'; fi; fi; fi; rm -f '$remote_jar_new' '$remote_launcher_new' '$remote_jar_backup' '$remote_launcher_backup'; if [ \"\$lock_had\" -eq 1 ]; then rmdir '$remote_lock'; fi; exit \"\$status\"; }; trap finish 0; mkdir '$remote_lock'; lock_had=1; chmod 644 '$remote_jar_new'; chmod 755 '$remote_launcher_new'; if [ -e '$remote_jar' ]; then cp -p '$remote_jar' '$remote_jar_backup'; jar_had=1; fi; if [ -e '$remote_launcher' ]; then cp -p '$remote_launcher' '$remote_launcher_backup'; launcher_had=1; fi; mv -f '$remote_launcher_new' '$remote_launcher'; launcher_replaced=1; mv -f '$remote_jar_new' '$remote_jar'; jar_replaced=1; '$remote_launcher' help >/dev/null"
run_adb shell "$transaction"

printf 'Installed %s on %s\n' "$(basename "$JAR_PATH")" "${SERIAL:-the current adb device}"
