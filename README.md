# gocview

![Build](https://github.com/Anniext/gocview/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [ ] Get familiar with the [template documentation][template].
- [ ] Adjust the [pluginGroup](./gradle.properties) and [pluginName](./gradle.properties), as well as the [id](./src/main/resources/META-INF/plugin.xml) and [sources package](./src/main/kotlin).
- [ ] Adjust the plugin description in `README` (see [Tips][docs:plugin-description])
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the `MARKETPLACE_ID` in the above README badges. You can obtain it once the plugin is published to JetBrains Marketplace.
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate) related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.
- [ ] Configure the [CODECOV_TOKEN](https://docs.codecov.com/docs/quick-start) secret for automated test coverage reports on PRs

<!-- Plugin description -->
gocview 是一个为 IntelliJ IDEA 提供 Go 代码覆盖率可视化和 Goc 工具集成的插件。

## 主要功能

### Goc Build 配置

提供独立的 Goc Build 运行配置类型，支持：

- 在"添加新配置"菜单中直接选择"Goc Build"
- 自定义工作目录
- 配置 goc 命令和参数
- 指定输出路径
- 配置持久化保存

### 使用方法

1. 点击 IDE 右上角的运行配置下拉菜单
2. 选择"Edit Configurations..."
3. 点击左上角的"+"按钮
4. 在列表中找到并选择"Goc Build"
5. 配置以下选项：
   - **工作目录**：Go 项目的根目录
   - **Goc 命令**：默认为 `goc build`，可修改为其他命令如 `goc test`
   - **命令参数**：额外的 goc 参数，如 `--center=http://localhost:7777`
   - **输出路径**：编译输出的二进制文件路径（可选）
6. 点击"OK"保存配置
7. 点击运行按钮执行 goc 构建

### 使用场景

- 使用 goc 构建带覆盖率收集功能的 Go 应用程序
- 集成测试覆盖率分析
- 微服务覆盖率统计
- CI/CD 覆盖率报告生成
<!-- Plugin description end -->

## 前置要求

本插件需要 **Go 插件** 支持才能使用完整功能。

### 安装 Go 插件

1. 打开 <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd>
2. 搜索 "Go"
3. 点击 <kbd>Install</kbd> 安装官方 Go 插件
4. 重启 IDE

如果未安装 Go 插件，gocview 会在项目启动时显示提示通知。

## 安装方式

### 从本地文件安装（推荐）

1. 下载编译好的插件文件：`build/distributions/gocview-0.0.1.zip`
2. 打开 <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
3. 选择下载的 zip 文件
4. 重启 IDE

### 从源码编译安装

```bash
# 编译插件
./gradlew buildPlugin

# 生成的插件文件位于
# build/distributions/gocview-0.0.1.zip
```

然后按照上述"从本地文件安装"步骤进行安装。

### 从 Marketplace 安装（待发布）

- 打开 <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd>
- 搜索 "gocview"
- 点击 <kbd>Install</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
