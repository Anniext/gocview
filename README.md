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

### 1. 实时覆盖率监控

- **自动检测 Goc Server**：当你运行带有 goc 的程序时，插件会自动从控制台输出中检测 goc server 地址（如 `[goc] goc server started: http://127.0.0.1:49598`）
- **智能延迟刷新**：检测到 goc server 后，延迟 3 秒自动调用 `goc profile` 命令，给程序充足的启动时间
- **倒计时提示**：状态栏显示倒计时，让你清楚知道何时开始获取覆盖率数据
- **自动打开工具窗口**：检测到 goc server 时自动打开覆盖率工具窗口
- **可视化展示**：在工具窗口中以表格形式展示覆盖率数据，支持按文件查看详细信息

### 2. 覆盖率数据解析

插件会解析 goc profile 返回的覆盖率数据，格式如：
```
git.bestfulfill.tech/devops/demo/main.go:8.13,9.6 1 1
git.bestfulfill.tech/devops/demo/main.go:9.6,12.3 2 70
```

每行数据包含：
- **文件路径**：代码文件的完整路径
- **起始位置**：起始行.起始列
- **结束位置**：结束行.结束列
- **语句数量**：该基本块中的语句数量
- **执行次数**：该基本块被执行到的次数

### 3. 覆盖率可视化

**文件级别汇总**：
- 文件路径
- 覆盖率百分比（带颜色标识）
  - 绿色：≥80%
  - 黄色：50%-80%
  - 橙色：0%-50%
  - 红色：0%
- 已覆盖语句数
- 总语句数
- 总执行次数

**代码块详细信息**：
- 点击文件可查看该文件的所有代码块
- 显示每个代码块的起始/结束位置
- 显示语句数和执行次数
- 标识覆盖状态（已覆盖/未覆盖）
- 双击代码块可跳转到对应的代码位置

### 4. 编辑器内覆盖率高亮

**实时代码高亮**：
- 打开 Go 源文件时，自动在编辑器中高亮显示覆盖率信息
- 已覆盖的代码块显示为绿色背景
- 未覆盖的代码块显示为红色背景
- 高频执行代码（>100次）显示为橙色背景
- 鼠标悬停在高亮区域可查看详细信息（位置、语句数、执行次数）

**执行次数内嵌显示**：
- 在代码行末自动显示执行次数标记
  - `✓ 1-100`：绿色，正常覆盖
  - `⚡ 100+`：橙色，高频执行
  - `🔥 10k+`：橙色，超高频执行（热点代码）
  - `✗ 0`：红色，未覆盖
- 实时更新，无需手动刷新编辑器

**自动清除高亮**：
- 程序停止运行时，自动清除所有覆盖率高亮
- 也可以点击工具窗口的"清除高亮"按钮手动清除

### 使用方法

1. **打开覆盖率工具窗口**
   - 在 IDE 底部找到 "Goc Coverage" 工具窗口
   - 或通过 <kbd>View</kbd> > <kbd>Tool Windows</kbd> > <kbd>Goc Coverage</kbd> 打开

2. **运行带 goc 的程序**
   - 使用 run 模式启动你的 Go 应用程序
   - 确保程序输出包含 `[goc] goc server started: http://...` 信息

3. **查看覆盖率**
   - 插件会自动检测 goc server 地址
   - 自动获取并显示覆盖率数据
   - 点击"刷新覆盖率"按钮可手动更新数据

4. **查看详细信息**
   - 在文件列表中点击任意文件
   - 下方面板会显示该文件的详细代码块覆盖率信息
   - 双击代码块可跳转到对应的代码位置

5. **编辑器内查看覆盖率**
   - 打开任意 Go 源文件
   - 编辑器会自动高亮显示覆盖率信息
   - 已覆盖的代码显示绿色背景，未覆盖的显示红色背景，高频执行显示橙色背景
   - 代码行末会显示执行次数（如 `✓ 70`、`⚡ 150`、`🔥 10k+`）
   - 鼠标悬停可查看详细的覆盖率信息

6. **跳转功能**
   - 在覆盖率工具窗口中双击文件，自动跳转到该文件（优先跳转到未覆盖的代码块）
   - 在详细信息面板中双击代码块，精确跳转到该代码块的位置
   - 支持键盘快捷键：选中后按 <kbd>Enter</kbd> 键跳转
   - 鼠标悬停时显示手型光标，提示可点击
   - **智能路径解析**：自动将 Go 模块路径（如 `github.com/user/repo/main.go`）转换为实际文件路径
     - 支持项目内文件解析
     - 支持 GOPATH 路径解析
     - 支持 Go mod cache 路径解析
     - 自动读取 `go.mod` 文件识别项目模块名

7. **清除高亮**
   - 程序停止运行时，自动清除所有覆盖率高亮
   - 点击工具窗口的"清除高亮"按钮可手动清除
   - 关闭文件时自动清理该文件的高亮资源

### Goc Build 配置

提供独立的 Goc Build 运行配置类型，支持：

- 在"添加新配置"菜单中直接选择"Goc Build"
- 自定义工作目录
- 配置 goc 命令和参数
- 指定输出路径
- 配置持久化保存

### 使用场景

- 使用 goc 构建带覆盖率收集功能的 Go 应用程序
- 实时监控应用程序的代码覆盖率
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
