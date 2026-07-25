# Changelog

本项目从首个正式版本开始遵循 [Keep a Changelog](https://keepachangelog.com/) 和 [Semantic Versioning](https://semver.org/)。

## [Unreleased]

### Changed

- 使用 Gradle Kotlin DSL、Gradle Wrapper 和 `mise` 管理构建环境。
- 将 CLI、Android 隐藏服务兼容和外部进程执行拆分为独立职责。
- 完整功能最低支持版本明确为 Android 11 / API 30。
- 安装与主机包装脚本增加校验、无副作用自检和特殊字符测试。
