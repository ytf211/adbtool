#!/usr/bin/env bash
# 下载构建所需的 android.jar(SDK 35 stub)与 d8.jar(dex 编译器)到本目录。
# 仅在需要重新编译 src/ 时使用;直接使用预编译的 dist/adbtool.jar 无需运行此脚本。
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
PLATFORM_URL="https://dl.google.com/android/repository/platform-35_r01.zip"
BUILDTOOLS_URL="https://dl.google.com/android/repository/build-tools_r34-linux.zip"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

if [[ ! -f "$DIR/android.jar" ]]; then
  echo "[1/2] 下载 android.jar ..."
  curl -fsSL -o "$TMP/platform.zip" "$PLATFORM_URL"
  unzip -o -q "$TMP/platform.zip" -d "$TMP/platform"
  find "$TMP/platform" -name android.jar -exec cp {} "$DIR/android.jar" \;
  echo "    -> $DIR/android.jar"
else
  echo "[1/2] android.jar 已存在,跳过"
fi

if [[ ! -f "$DIR/d8.jar" ]]; then
  echo "[2/2] 下载 d8.jar ..."
  curl -fsSL -o "$TMP/bt.zip" "$BUILDTOOLS_URL"
  unzip -o -q "$TMP/bt.zip" -d "$TMP/bt"
  find "$TMP/bt" -name d8.jar -path '*lib*' -exec cp {} "$DIR/d8.jar" \;
  echo "    -> $DIR/d8.jar"
else
  echo "[2/2] d8.jar 已存在,跳过"
fi

echo "工具链就绪。"
