#!/usr/bin/env bash
# 一键安装:把 adbtool.jar 与设备端包装脚本推送到设备。
#
# 用法: ./install.sh [设备序列号]
#   不传参数则使用当前唯一连接的设备;也可用 ADBTOOL_SERIAL 指定。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
ADB="${ADB:-adb}"
SERIAL="${1:-${ADBTOOL_SERIAL:-}}"

JAR="$ROOT/dist/adbtool.jar"
DEV_WRAPPER="$ROOT/device/adbtool"

if [[ ! -f "$JAR" ]]; then
  echo "未找到 $JAR,请先运行 ./build.sh 构建。" >&2
  exit 1
fi

# 指定了网络地址则尝试自动连接。
if [[ -n "$SERIAL" && "$SERIAL" == *:* ]]; then
  "$ADB" connect "$SERIAL" >/dev/null 2>&1 || true
fi

# 统一的 adb 调用:指定了 SERIAL 就带 -s,否则用默认设备。
run_adb() {
  if [[ -n "$SERIAL" ]]; then "$ADB" -s "$SERIAL" "$@"; else "$ADB" "$@"; fi
}

echo "目标设备: ${SERIAL:-<当前连接的设备>}"
run_adb push "$JAR" /data/local/tmp/adbtool.jar
run_adb push "$DEV_WRAPPER" /data/local/tmp/adbtool
run_adb shell chmod 755 /data/local/tmp/adbtool

echo
echo "安装完成。快速自检:"
run_adb shell "/data/local/tmp/adbtool set-clip 'adbtool installed ok'"
got=$(run_adb shell "/data/local/tmp/adbtool get-clip")
echo "  剪贴板回读: $got"
echo
echo "用法一(主机端): ./bin/adbtool get-clip"
echo "用法二(设备端): adb shell /data/local/tmp/adbtool get-clip"
