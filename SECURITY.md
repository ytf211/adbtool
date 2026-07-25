# Security Policy

## Supported versions

安全修复只针对最新发布版本和 `main`。Android 隐藏 API 兼容性问题通常按普通缺陷处理；涉及任意代码执行、发布资产篡改或权限越界的问题按安全问题处理。

## Reporting

请使用 GitHub 仓库的私密漏洞报告功能提交安全问题，不要在公开 Issue 中披露利用细节。报告应包含受影响版本、Android 版本、复现步骤和影响范围。

发布资产必须由标签工作流生成，并同时提供 SHA-256。安装脚本拒绝没有相邻校验文件的本地产物，也会校验从 GitHub Release 下载的产物。
