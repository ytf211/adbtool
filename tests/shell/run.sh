#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TEMP_DIRECTORY="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIRECTORY"' EXIT
FAKE_ADB="$ROOT/tests/shell/fake-adb"

fail() {
  echo "shell test failed: $*" >&2
  exit 1
}

payload="line one ' \$() \`; semi
line two
"
ADB_LOG="$TEMP_DIRECTORY/adb.log" \
FAKE_ADB_SERIAL='127.0.0.1:5555' \
ADB="$FAKE_ADB" \
ADBTOOL_SERIAL='127.0.0.1:5555' \
  "$ROOT/bin/adbtool" set-clip "$payload"

mapfile -d '' -t adb_args <"$TEMP_DIRECTORY/adb.log"
[[ "${adb_args[0]}" == "-s" ]] || fail "serial option missing"
[[ "${adb_args[1]}" == "127.0.0.1:5555" ]] || fail "serial changed"
[[ "${adb_args[2]}" == "shell" ]] || fail "shell command missing"
remote="${adb_args[3]}"
prefix='CLASSPATH=/data/local/tmp/adbtool.jar app_process /system/bin com.adbtool.AdbTool'
[[ "$remote" == "$prefix"* ]] || fail "unexpected remote prefix"
encoded="${remote#"$prefix" }"
bash -c "set -- $encoded; printf '%s\\0' \"\$@\"" >"$TEMP_DIRECTORY/decoded"
mapfile -d '' -t decoded <"$TEMP_DIRECTORY/decoded"
[[ "${decoded[0]}" == "set-clip" ]] || fail "command changed"
[[ "${decoded[1]}" == "$payload" ]] || fail "payload changed"

shopt -s nullglob
artifacts=("$ROOT"/build/dist/adbtool-*.jar)
shopt -u nullglob
[[ ${#artifacts[@]} -eq 1 ]] || fail "expected exactly one distribution JAR"
ADB_LOG="$TEMP_DIRECTORY/install.log" ADB="$FAKE_ADB" \
  "$ROOT/install.sh" --jar "${artifacts[0]}" >/dev/null

mapfile -d '' -t push_jar <"$TEMP_DIRECTORY/install.log.1"
mapfile -d '' -t push_launcher <"$TEMP_DIRECTORY/install.log.2"
mapfile -d '' -t preflight <"$TEMP_DIRECTORY/install.log.3"
mapfile -d '' -t transaction <"$TEMP_DIRECTORY/install.log.4"
[[ "${push_jar[0]}" == "push" ]] || fail "JAR was not pushed first"
[[ "${push_jar[2]}" == /data/local/tmp/adbtool.jar.*.new ]] || fail "JAR temp path is not unique"
[[ "${push_launcher[0]}" == "push" ]] || fail "launcher was not pushed second"
[[ "${push_launcher[2]}" == /data/local/tmp/adbtool.*.new ]] || fail "launcher temp path is not unique"
[[ "${preflight[0]}" == "shell" && "${preflight[1]}" == *"${push_jar[2]}"*" help" ]] \
  || fail "temporary JAR was not checked before install"
[[ "${transaction[0]}" == "shell" ]] || fail "transaction shell missing"
[[ "${transaction[1]}" == *"mkdir '/data/local/tmp/adbtool.install.lock'"* ]] || fail "install lock missing"
[[ "${transaction[1]}" == *"jar_replaced=1"* ]] || fail "JAR rollback state missing"
[[ "${transaction[1]}" == *"launcher_replaced=1"* ]] || fail "launcher rollback state missing"
[[ "${transaction[1]}" == *"'/data/local/tmp/adbtool' help"* ]] || fail "post-install check missing"
printf '%s\n' "${transaction[1]}" | sh -n

simulate_transaction() {
  local sandbox="$1" launcher_exit="$2" expect_success="$3"
  local script="${transaction[1]//\/data\/local\/tmp/$sandbox}"
  local jar_new="${push_jar[2]//\/data\/local\/tmp/$sandbox}"
  local launcher_new="${push_launcher[2]//\/data\/local\/tmp/$sandbox}"
  mkdir -p "$sandbox"
  printf 'old jar' >"$sandbox/adbtool.jar"
  printf '#!/bin/sh\nexit 0\n' >"$sandbox/adbtool"
  printf 'new jar' >"$jar_new"
  printf '#!/bin/sh\nexit %s\n' "$launcher_exit" >"$launcher_new"
  chmod +x "$sandbox/adbtool" "$launcher_new"

  if sh -c "$script"; then
    [[ "$expect_success" == true ]] || fail "failing transaction unexpectedly succeeded"
  else
    [[ "$expect_success" == false ]] || fail "successful transaction unexpectedly failed"
  fi

  if [[ "$expect_success" == true ]]; then
    [[ "$(<"$sandbox/adbtool.jar")" == "new jar" ]] || fail "new JAR was not installed"
  else
    [[ "$(<"$sandbox/adbtool.jar")" == "old jar" ]] || fail "old JAR was not restored"
    grep -q 'exit 0' "$sandbox/adbtool" || fail "old launcher was not restored"
  fi
  [[ ! -e "$sandbox/adbtool.install.lock" ]] || fail "install lock leaked"
}

simulate_transaction "$TEMP_DIRECTORY/remote-success" 0 true
simulate_transaction "$TEMP_DIRECTORY/remote-failure" 1 false

ADB_LOG="$TEMP_DIRECTORY/positional.log" ADB="$FAKE_ADB" \
  "$ROOT/install.sh" legacy-serial --jar "${artifacts[0]}" >/dev/null
mapfile -d '' -t positional <"$TEMP_DIRECTORY/positional.log.1"
[[ "${positional[0]}" == "-s" && "${positional[1]}" == "legacy-serial" ]] \
  || fail "legacy positional serial is not supported"

expect_rejected() {
  local jar="$1"
  if ADB_LOG="$TEMP_DIRECTORY/rejected.log" ADB="$FAKE_ADB" \
    "$ROOT/install.sh" --jar "$jar" >/dev/null 2>&1; then
    fail "installer accepted malformed checksum: $jar.sha256"
  fi
}

printf 'not the artifact' >"$TEMP_DIRECTORY/wrong-name.jar"
artifact_hash=$(sha256sum "${artifacts[0]}")
printf '%s  %s\n' "${artifact_hash%% *}" "$(basename "${artifacts[0]}")" \
  >"$TEMP_DIRECTORY/wrong-name.jar.sha256"
expect_rejected "$TEMP_DIRECTORY/wrong-name.jar"

printf 'one line only' >"$TEMP_DIRECTORY/multiple.jar"
multiple_hash=$(sha256sum "$TEMP_DIRECTORY/multiple.jar")
printf '%s  multiple.jar\n%s  another.jar\n' "${multiple_hash%% *}" "${multiple_hash%% *}" \
  >"$TEMP_DIRECTORY/multiple.jar.sha256"
expect_rejected "$TEMP_DIRECTORY/multiple.jar"

printf 'wrong digest' >"$TEMP_DIRECTORY/wrong-digest.jar"
printf '%064d  wrong-digest.jar\n' 0 >"$TEMP_DIRECTORY/wrong-digest.jar.sha256"
expect_rejected "$TEMP_DIRECTORY/wrong-digest.jar"

echo "All shell tests passed"
