#!/usr/bin/env bash
# 构建 adbtool.jar(含 classes.dex),可直接经 app_process 在设备运行。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-arm64}"
JAVAC="$JAVA_HOME/bin/javac"
ANDROID_JAR="$ROOT/tools/android.jar"
D8_JAR="$ROOT/tools/d8.jar"
BUILD="$ROOT/build"
DIST="$ROOT/dist"
OUT_JAR="$DIST/adbtool.jar"

# 缺少构建工具链(android.jar / d8.jar)时自动下载。
if [[ ! -f "$ANDROID_JAR" || ! -f "$D8_JAR" ]]; then
  echo "[0/3] 未找到工具链,执行 tools/fetch-tools.sh 下载"
  bash "$ROOT/tools/fetch-tools.sh"
fi

echo "[1/3] 清理"
rm -rf "$BUILD"; mkdir -p "$BUILD" "$DIST"

echo "[2/3] javac 编译 (source/target 8)"
"$JAVAC" -source 8 -target 8 -encoding UTF-8 -nowarn \
    -classpath "$ANDROID_JAR" \
    -d "$BUILD" \
    "$ROOT"/src/com/adbtool/*.java 2>&1 | grep -v "obsolete\|deprecated\|bootstrap" || true

echo "[3/3] d8 生成 dex"
CLASSES=$(cd "$BUILD" && find . -name '*.class')
( cd "$BUILD" && java -cp "$D8_JAR" com.android.tools.r8.D8 \
    --min-api 26 --release \
    --lib "$ANDROID_JAR" \
    --output "$OUT_JAR" \
    $CLASSES )

echo "完成: $OUT_JAR"
unzip -l "$OUT_JAR" | grep -E "classes.dex|Archive" || true
