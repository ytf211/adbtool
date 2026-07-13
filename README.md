# adbtool

在 Android 设备上**设置剪贴板 / 读取剪贴板 / 弹出 Toast / 发送通知**的命令行工具。

以 `app_process` 方式在设备端运行一段 Java 代码,通过反射调用系统隐藏服务
(`IClipboard`、`INotificationManager`)完成这些没有现成 shell 命令的操作 ——
与 scrcpy 等工具同一思路。通知则复用系统内建的 `cmd notification post`。


---

## 目录结构

```
adb/
├── src/com/adbtool/AdbTool.java   核心源码(四项功能)
├── build.sh                       编译打包:javac + d8 → dist/adbtool.jar
├── dist/adbtool.jar               产物(含 classes.dex),推到设备运行
├── device/adbtool                 设备端包装脚本(/data/local/tmp/adbtool)
├── bin/adbtool                    主机端包装脚本(经 adb 转发,自动处理引号)
├── install.sh                     一键推送安装 + 自检
└── tools/fetch-tools.sh           按需下载 android.jar / d8.jar(仅重新构建时需要)
```

---

## 安装

前提:开发机已装 `adb`,设备已开启 USB/无线调试。

```bash
# 默认使用当前连接的设备;多设备时用 ADBTOOL_SERIAL 指定
./install.sh
# 或
ADBTOOL_SERIAL=<序列号或IP:端口> ./install.sh
```

安装会把 `adbtool.jar` 与设备端脚本推到 `/data/local/tmp/`,并做一次剪贴板回读自检。

---

## 使用

### 方式一 · 主机端(推荐)

在开发机上直接调用,`bin/adbtool` 会处理好引号转义:

```bash
./bin/adbtool set-clip "要放到剪贴板的文本 支持中文"
./bin/adbtool get-clip                 # 打印当前剪贴板文本
./bin/adbtool toast "这是一条吐司"
./bin/adbtool toast -l "长时间吐司"     # -l/--long 显示更久
./bin/adbtool notify "通知标题" "通知正文"
```

指定其他设备:`ADBTOOL_SERIAL=xxxx ./bin/adbtool get-clip`

### 方式二 · 设备端

在 `adb shell` 内或经 adb 直接调用设备端脚本:

```bash
adb shell /data/local/tmp/adbtool set-clip "hello"
adb shell /data/local/tmp/adbtool get-clip
adb shell /data/local/tmp/adbtool toast "hi"
adb shell /data/local/tmp/adbtool notify "标题" "正文"
```

> 注意:文本含空格/特殊字符时,请在**主机 shell** 层再包一层引号,例如
> `adb shell "/data/local/tmp/adbtool set-clip 'a b c'"`;用方式一则无需操心。

### 子命令一览

| 子命令 | 说明 |
|--------|------|
| `set-clip <文本>` | 设置剪贴板内容 |
| `get-clip` | 读取并打印剪贴板文本 |
| `toast [-l\|--long] <文本>` | 弹出 Toast(`-l` 长时) |
| `notify <标题> [正文]` | 发送通知(固定 tag,重复调用就地更新) |
| `help` | 帮助 |

---

## 重新构建

改动 `src/` 后重新编译打包并推送:

```bash
./build.sh          # 产出 dist/adbtool.jar(首次会自动下载工具链)
./install.sh        # 推送到设备
```

构建依赖:JDK(默认 `/usr/lib/jvm/java-21-openjdk-arm64`,可用 `JAVA_HOME` 覆盖)。
`build.sh` 首次运行会自动执行 `tools/fetch-tools.sh` 联网下载 `android.jar` 与 `d8.jar`。
> 若只是使用工具而非改源码,直接用随包的预编译 `dist/adbtool.jar` 即可,无需构建。

---

## 实现要点

- **运行载体**:`CLASSPATH=/data/local/tmp/adbtool.jar app_process /system/bin com.adbtool.AdbTool <args>`。
  以 shell(uid 2000)身份运行,向系统服务出示调用包名 `com.android.shell`(与该 uid 匹配)。
- **剪贴板**:反射拿到 `IClipboard`,调用 `setPrimaryClip` / `getPrimaryClip`。
  按参数类型自动装配实参,兼容不同 API 版本签名(API 35 为
  `(ClipData, pkg, attributionTag, userId, deviceId)`)。以 shell 身份读取不受前台限制阻拦。
- **Toast**:反射拿到 `INotificationManager`,调用 `enqueueTextToast`,传入自建 `Binder` 令牌;
  文本 Toast 由 SystemUI 渲染,进程保持存活到展示完毕。
- **通知**:调用系统 `cmd notification post`(其内部已处理通知通道创建等细节)。

---

## 排错

- **`get-clip` 返回空**:剪贴板本身为空,或被安全策略拦截。以 shell 身份通常可读;
  若设备厂商加严,可尝试 `adb shell appops set com.android.shell READ_CLIPBOARD allow`。
- **Toast 不显示**:确认屏幕点亮、勿扰未全屏拦截;命令会阻塞约 2~4 秒(等待展示)属正常。
- **`app_process` 找不到类**:确认 `adbtool.jar` 已推送且 `CLASSPATH` 正确(重跑 `./install.sh`)。
- **设备未连接**:`adb connect <IP:端口>`(无线)或插上 USB,或设 `ADBTOOL_SERIAL` 后重试。
