# adbtool

`adbtool` 是通过 ADB 在 Android 设备上设置/读取剪贴板、显示 Toast 和发送通知的命令行工具。设备端代码以 shell UID 通过 `app_process` 运行，并反射调用 Android 隐藏服务。

## 兼容性

- 完整功能最低支持 Android 11 / API 30。
- 使用 Android API 35 编译，D8 产物的 `min-api` 为 30。
- Android 厂商可能修改隐藏 API 或收紧 shell 权限；API 30 和 API 35 的签名装配有 JVM 测试，但真实行为仍应在目标设备验证。

## 快速开始

前提：开发机安装 `adb`，设备已开启 USB 或无线调试。

从源码构建并安装：

```bash
./gradlew clean check assemble
./install.sh
```

安装器只在 `build/dist/` 恰好存在一个版本化 JAR 时自动选择，也可以显式指定：

```bash
./install.sh --jar build/dist/adbtool-0.1.0-SNAPSHOT.jar
./install.sh --version 1.2.3
./install.sh --serial 192.168.1.10:5555 --version 1.2.3
```

`--jar` 要求相邻位置存在同名 `.sha256` 文件。`--version` 从 `ytf211/adbtool` 的 GitHub Release 下载 JAR 和校验文件；可通过 `ADBTOOL_REPOSITORY=owner/repo` 覆盖仓库。

安装过程先校验 SHA-256，再上传到唯一远端临时路径并预检；正式切换使用并发锁和备份，任何切换或 `help` 检查失败都会恢复原 JAR 与启动器。

## 使用

主机端包装器会保留每个非 NUL 参数，包括 Unicode、引号、Shell 元字符和尾随换行：

```bash
./bin/adbtool set-clip "支持中文的文本"
./bin/adbtool get-clip
./bin/adbtool toast "短 Toast"
./bin/adbtool toast --long "长 Toast"
./bin/adbtool notify "通知标题" "通知正文"
```

指定设备：

```bash
ADBTOOL_SERIAL=<序列号或IP:端口> ./bin/adbtool get-clip
```

设备端也可以直接调用：

```bash
adb shell /data/local/tmp/adbtool help
adb shell /data/local/tmp/adbtool get-clip
```

## 开发环境

项目使用 Gradle Wrapper `9.6.1` 和 Temurin JDK `21.0.11+10`。安装 `mise` 后可执行：

```bash
mise install
mise run check
```

不使用 `mise` 时，只要 `JAVA_HOME` 指向 JDK 21 即可：

```bash
./gradlew clean check assemble
shellcheck bin/adbtool build.sh device/adbtool install.sh scripts/check-conventions.sh tests/shell/fake-adb tests/shell/run.sh tools/fetch-tools.sh
```

构建会下载并校验固定版本的 Android API 35 platform 与 R8/D8。没有 Android Gradle Plugin，也没有第三方运行时依赖。

产物位于：

```text
build/dist/adbtool-<version>.jar
build/dist/adbtool-<version>.jar.sha256
```

默认开发版本为 `0.1.0-SNAPSHOT`；可通过 `./gradlew assemble -Pversion=1.2.3` 指定版本。

## 项目结构

```text
src/main/java/com/adbtool/
├── AdbTool.java          唯一入口与退出码
├── cli/                  参数解析和命令模型
├── android/              剪贴板、Toast、通知和隐藏 API 兼容
└── process/              外部进程执行与退出码
```

测试分为 JVM 测试与 fake ADB 壳层测试，统一由 `./gradlew check` 执行。普通 CI 无法验证 Toast 是否真实显示，因此正式版本仍需 API 30+ 设备验收。

## 发布与贡献

PR 和 `main` push 会执行 CI；`v*` 标签会重新构建并创建 GitHub Release，发布版本化 JAR 与 SHA-256。发布工作流不会在本地自动推送标签或发布。

分支、提交和发布规范见 [CONTRIBUTING.md](CONTRIBUTING.md)，安全报告方式见 [SECURITY.md](SECURITY.md)。

## 排错

- `adb: no devices/emulators found`：检查 USB/无线调试，或设置 `ADBTOOL_SERIAL`。
- `Android service is unavailable`：目标系统缺少对应服务或厂商限制了 shell 访问。
- `Android rejected the Toast request`：系统拒绝入队，常见原因包括通知被禁用或 Toast 队列受限。
- `get-clip` 为空：剪贴板为空，或设备安全策略禁止 shell 读取。
- 需要 Java 堆栈时，在设备命令前设置 `ADBTOOL_DEBUG=1`。
