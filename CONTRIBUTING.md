# Contributing

## 开发流程

- 从 `main` 创建短期 `feature/*` 或 `fix/*` 分支，不维护长期 `dev`。
- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/)，例如 `feat(cli): preserve trailing newlines`。
- 版本遵循 Semantic Versioning；只有 `v<major>.<minor>.<patch>` 格式的标签会进入发布流程。
- 提交 PR 前运行 `mise run check`，或直接运行：

```bash
./gradlew clean check assemble
shellcheck bin/adbtool build.sh device/adbtool install.sh scripts/check-conventions.sh tests/shell/fake-adb tests/shell/run.sh tools/fetch-tools.sh
```

## PR 要求

PR 标题同样使用 Conventional Commits。PR 应说明行为变化、验证方式，以及是否在 API 30 真机或模拟器上验证过剪贴板、Toast 和通知。

建议为 `main` 启用分支保护：禁止直接推送，要求 CI 成功、至少一次审核、分支保持最新，并限制创建和删除 `v*` 标签的权限。

## 发布

发布者先确认目标提交已进入 `main`，再创建签名或受保护标签：

```bash
git tag -s v1.2.3 -m "v1.2.3"
```

推送标签会触发 Release Workflow。该工作流重新执行检查和构建，发布版本化 DEX JAR 与 SHA-256 文件。`git push` 和正式发布必须由有权限的维护者明确执行。
