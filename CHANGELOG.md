# Changelog

本项目从首个正式版本开始遵循 [Keep a Changelog](https://keepachangelog.com/) 和 [Semantic Versioning](https://semver.org/)。

## [Unreleased]

## [0.1.0] - 2026-07-25

### Added

- 增加 Gradle Kotlin DSL、Gradle Wrapper 9.6.1 与 `mise` 开发环境管理。
- 增加零依赖 JVM 测试、fake ADB 壳层测试和安装事务回滚测试。
- 增加 GitHub Actions CI、标签驱动的 Release Workflow 和 Dependabot 配置。
- 发布版本化 DEX JAR 及对应 SHA-256 校验文件。

### Changed

- 将 CLI、Android 隐藏服务兼容和外部进程执行拆分为独立职责。
- 完整功能最低支持版本明确为 Android 11 / API 30，并使用 Android API 35 编译。
- 安装器支持本地产物及 GitHub Release，安装过程使用唯一暂存路径、并发锁、预检和失败回滚。
- 主机包装器可无损传递 Unicode、引号、Shell 元字符和尾随换行。

### Fixed

- 修复 Android 15 拒绝 Toast 请求时仍报告成功的问题。
- 修复通过私有 AIDL Proxy 实现类获取反射方法可能触发 `IllegalAccessException` 的问题。
- 修复旧构建脚本忽略 `javac` 失败并继续打包的问题。
- 修复安装自检覆盖剪贴板内容以及校验文件未严格绑定目标 JAR 的问题。

[Unreleased]: https://github.com/ytf211/adbtool/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/ytf211/adbtool/releases/tag/v0.1.0
