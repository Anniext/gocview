<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# gocview Changelog

## [Unreleased]
### Added
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- Goc 命令集成功能
  - 在运行/调试配置中添加 Goc 选项卡
  - 支持一键切换 go/goc 命令
  - 支持自定义 goc 额外参数
  - 配置持久化保存
  - 支持所有 Go 运行配置类型（Application、Test、Build 等）
- **实时覆盖率监控和可视化**
  - 自动检测控制台输出中的 goc server 地址
  - 自动调用 `goc profile` 命令获取覆盖率数据
  - 解析覆盖率数据（文件路径、代码块位置、执行次数）
  - 工具窗口展示覆盖率数据
    - 文件级别覆盖率汇总表格
    - 覆盖率百分比颜色标识（绿/黄/橙/红）
    - 代码块详细信息面板
    - 一键刷新功能
  - 运行配置监听器自动附加到进程
  - 后台线程异步获取和解析数据
- 完整的文档支持
  - Goc 集成使用指南
  - 覆盖率可视化使用示例（USAGE_EXAMPLE.md）
  - 技术架构文档（ARCHITECTURE.md）
  - 技术设计文档
- 单元测试覆盖
  - GocCoverageService 测试
  - URL 提取测试
  - 数据解析测试
  - 文件分组测试
