# IntelliJ 插件项目开发规范

本规范定义了 IntelliJ IDEA 插件项目的开发标准、代码规范和最佳实践。

## 项目信息

- **插件名称**: gocview
- **插件组**: com.github.anniext.gocview
- **平台类型**: IntelliJ IDEA Community (IC)
- **最低支持版本**: 2024.3 (Build 243+)
- **开发语言**: Kotlin
- **JVM 版本**: 21
- **构建工具**: Gradle 9.0.0

## 代码组织规范

### 包结构

项目采用标准的 Maven/Gradle 目录结构：

```
src/
├── main/
│   ├── kotlin/
│   │   └── com/github/anniext/gocview/
│   │       ├── services/        # 服务层
│   │       ├── startup/         # 启动活动
│   │       ├── toolWindow/      # 工具窗口
│   │       └── MyBundle.kt      # 国际化资源
│   └── resources/
│       ├── META-INF/
│       │   └── plugin.xml       # 插件配置
│       └── messages/            # 国际化消息
└── test/
    ├── kotlin/                  # 测试代码
    └── testData/                # 测试数据
```

### 命名规范

#### 包命名
- 使用小写字母
- 采用反向域名格式：`com.github.anniext.gocview`
- 按功能模块划分子包：`services`, `toolWindow`, `actions`, `listeners` 等

#### 类命名
- 使用 PascalCase（大驼峰）
- 类名应清晰表达其职责
- 遵循 IntelliJ 平台命名约定：
  - Service 类：`*Service`
  - Action 类：`*Action`
  - Listener 类：`*Listener`
  - Factory 类：`*Factory`
  - Tool Window：`*ToolWindowFactory`

#### 函数和变量命名
- 使用 camelCase（小驼峰）
- 常量使用 UPPER_SNAKE_CASE
- 布尔变量使用 `is`, `has`, `can` 等前缀
- 私有成员变量不使用下划线前缀

#### 文件命名
- Kotlin 文件名与主类名一致
- 一个文件只包含一个公共类（除非是紧密相关的辅助类）
- 扩展函数可以放在 `*Extensions.kt` 文件中

## Kotlin 编码规范

### 基本原则

1. **遵循 Kotlin 官方编码规范**
   - 使用 4 个空格缩进（不使用 Tab）
   - 每行最大长度 120 字符
   - 使用 Kotlin 惯用写法

2. **空安全**
   - 优先使用非空类型
   - 合理使用 `?.`, `?:`, `!!` 操作符
   - 避免不必要的 `!!` 强制解包

3. **不可变性优先**
   - 优先使用 `val` 而非 `var`
   - 优先使用不可变集合
   - 使用 `data class` 表示数据模型

### 代码风格

```kotlin
// 类定义
class MyService(
    private val project: Project,
    private val settings: MySettings
) : Disposable {
    
    companion object {
        private const val DEFAULT_TIMEOUT = 5000L
        
        fun getInstance(project: Project): MyService {
            return project.service()
        }
    }
    
    // 属性声明
    private val cache: MutableMap<String, String> = mutableMapOf()
    
    // 函数定义
    fun processData(input: String): Result<String> {
        return runCatching {
            // 实现逻辑
            input.trim()
        }
    }
    
    override fun dispose() {
        cache.clear()
    }
}
```

### 注释规范

```kotlin
/**
 * 服务类的简短描述
 *
 * 详细说明服务的功能和用途
 *
 * @property project 当前项目实例
 * @property settings 配置信息
 */
class MyService(
    private val project: Project,
    private val settings: MySettings
) {
    
    /**
     * 处理输入数据
     *
     * @param input 待处理的输入字符串
     * @return 处理结果，成功返回处理后的字符串，失败返回错误信息
     */
    fun processData(input: String): Result<String> {
        // 单行注释说明关键步骤
        return runCatching {
            input.trim()
        }
    }
}
```

## IntelliJ 平台开发规范

### 插件配置 (plugin.xml)

```xml
<idea-plugin>
    <!-- 插件唯一标识，使用反向域名 -->
    <id>com.github.anniext.gocview</id>
    
    <!-- 插件名称，简洁明了 -->
    <name>gocview</name>
    
    <!-- 供应商信息 -->
    <vendor email="your@email.com" url="https://github.com/Anniext">anniext</vendor>
    
    <!-- 依赖的平台模块 -->
    <depends>com.intellij.modules.platform</depends>
    
    <!-- 国际化资源 -->
    <resource-bundle>messages.MyBundle</resource-bundle>
    
    <!-- 扩展点 -->
    <extensions defaultExtensionNs="com.intellij">
        <toolWindow 
            factoryClass="com.github.anniext.gocview.toolWindow.MyToolWindowFactory" 
            id="MyToolWindow"
            anchor="right"
            icon="AllIcons.Toolwindows.ToolWindowStructure"/>
    </extensions>
    
    <!-- 动作 -->
    <actions>
        <action 
            id="MyAction" 
            class="com.github.anniext.gocview.actions.MyAction"
            text="My Action"
            description="Action description">
            <add-to-group group-id="ToolsMenu" anchor="first"/>
        </action>
    </actions>
</idea-plugin>
```

### 服务开发

#### Application 级别服务

```kotlin
@Service
class MyApplicationService {
    
    companion object {
        fun getInstance(): MyApplicationService {
            return service()
        }
    }
    
    fun doSomething() {
        // 应用级别的操作
    }
}
```

#### Project 级别服务

```kotlin
@Service(Service.Level.PROJECT)
class MyProjectService(private val project: Project) : Disposable {
    
    companion object {
        fun getInstance(project: Project): MyProjectService {
            return project.service()
        }
    }
    
    fun doSomething() {
        // 项目级别的操作
    }
    
    override fun dispose() {
        // 清理资源
    }
}
```

### 线程安全

1. **UI 线程操作**
   ```kotlin
   ApplicationManager.getApplication().invokeLater {
       // UI 更新操作
   }
   ```

2. **后台任务**
   ```kotlin
   ApplicationManager.getApplication().executeOnPooledThread {
       // 后台任务
   }
   ```

3. **读写锁**
   ```kotlin
   // 读操作
   ApplicationManager.getApplication().runReadAction {
       // 读取 PSI 等
   }
   
   // 写操作
   ApplicationManager.getApplication().runWriteAction {
       // 修改文档等
   }
   ```

### 通知和消息

```kotlin
// 信息通知
Notifications.Bus.notify(
    Notification(
        "MyNotificationGroup",
        "标题",
        "消息内容",
        NotificationType.INFORMATION
    ),
    project
)

// 气球提示
JBPopupFactory.getInstance()
    .createHtmlTextBalloonBuilder("提示内容", MessageType.INFO, null)
    .createBalloon()
    .show(RelativePoint.getCenterOf(component), Balloon.Position.above)
```

## 测试规范

### 测试组织

```kotlin
class MyServiceTest {
    
    private lateinit var service: MyService
    
    @Before
    fun setUp() {
        service = MyService()
    }
    
    @Test
    fun `test basic functionality`() {
        // Given
        val input = "test"
        
        // When
        val result = service.processData(input)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("test", result.getOrNull())
    }
    
    @After
    fun tearDown() {
        // 清理
    }
}
```

### 测试覆盖率

- 核心业务逻辑测试覆盖率应达到 80% 以上
- 关键路径必须有测试覆盖
- 使用 Kover 插件生成测试报告

## 构建和发布

### Gradle 配置

```kotlin
// build.gradle.kts 关键配置
intellijPlatform {
    pluginConfiguration {
        name = "gocview"
        version = "0.0.1"
        
        // 从 README.md 提取描述
        description = providers.fileContents(layout.projectDirectory.file("README.md"))
        
        ideaVersion {
            sinceBuild = "243"
            untilBuild = provider { null } // 不限制最高版本
        }
    }
}
```

### 版本管理

- 遵循语义化版本 (SemVer)：`MAJOR.MINOR.PATCH`
- 在 `gradle.properties` 中维护版本号
- 在 `CHANGELOG.md` 中记录版本变更

### 发布流程

1. 更新版本号
2. 更新 CHANGELOG.md
3. 运行测试：`./gradlew test`
4. 运行验证：`./gradlew runPluginVerifier`
5. 构建插件：`./gradlew buildPlugin`
6. 发布到 JetBrains Marketplace：`./gradlew publishPlugin`

## 国际化 (i18n)

### 消息资源

```properties
# messages/MyBundle.properties
action.name=My Action
action.description=This is my action
notification.title=Notification Title
notification.content=Notification content
```

### 使用方式

```kotlin
// 在代码中使用
val message = MyBundle.message("action.name")

// 在 plugin.xml 中使用
<action text="MyBundle.message('action.name')"/>
```

## 性能优化

### 最佳实践

1. **延迟初始化**
   - 使用 `lazy` 委托
   - 避免在构造函数中执行重操作

2. **缓存策略**
   - 合理使用缓存减少重复计算
   - 注意内存泄漏，及时清理缓存

3. **异步处理**
   - 耗时操作放在后台线程
   - 使用 `ProgressManager` 显示进度

4. **资源管理**
   - 实现 `Disposable` 接口
   - 在 `dispose()` 中释放资源

## 安全规范

1. **输入验证**
   - 验证所有外部输入
   - 防止路径遍历攻击

2. **权限控制**
   - 最小权限原则
   - 不请求不必要的权限

3. **敏感信息**
   - 不在代码中硬编码密钥
   - 使用 `PasswordSafe` 存储敏感信息

## 文档规范

### 代码文档

- 所有公共 API 必须有 KDoc 注释
- 复杂逻辑添加行内注释
- 使用中文编写注释

### README.md

- 包含插件简介和功能说明
- 提供安装和使用指南
- 包含开发和贡献指南
- 保持 `<!-- Plugin description -->` 标记完整

### CHANGELOG.md

- 记录每个版本的变更
- 分类：Added, Changed, Deprecated, Removed, Fixed, Security
- 使用中文描述变更内容

## 依赖管理

### 版本目录 (libs.versions.toml)

```toml
[versions]
kotlin = "2.1.0"
intellijPlatform = "2.1.0"

[plugins]
kotlin = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
intelliJPlatform = { id = "org.jetbrains.intellij.platform", version.ref = "intellijPlatform" }

[libraries]
junit = { group = "junit", name = "junit", version = "4.13.2" }
```

### 依赖原则

- 优先使用 IntelliJ 平台提供的库
- 避免引入大型第三方依赖
- 定期更新依赖版本
- 检查依赖的许可证兼容性

## Git 工作流

### 分支策略

- `main`: 主分支，保持稳定
- `develop`: 开发分支
- `feature/*`: 功能分支
- `bugfix/*`: 修复分支
- `release/*`: 发布分支

### 提交规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

类型 (type)：
- `feat`: 新功能
- `fix`: 修复
- `docs`: 文档
- `style`: 格式
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建/工具

示例：
```
feat(toolWindow): 添加新的工具窗口面板

- 实现基本的 UI 布局
- 添加数据刷新功能
- 集成项目服务

Closes #123
```

## 质量检查清单

发布前检查：

- [ ] 代码通过所有测试
- [ ] 代码符合 Kotlin 编码规范
- [ ] 通过 Plugin Verifier 验证
- [ ] 更新 CHANGELOG.md
- [ ] 更新版本号
- [ ] README.md 信息准确
- [ ] 所有公共 API 有文档
- [ ] 没有 TODO 或 FIXME 注释
- [ ] 性能测试通过
- [ ] 兼容目标 IDE 版本

## 常见问题

### 调试插件

```bash
# 运行插件开发实例
./gradlew runIde

# 运行测试
./gradlew test

# 运行 UI 测试
./gradlew runIdeForUiTests
```

### 插件验证

```bash
# 验证插件兼容性
./gradlew runPluginVerifier
```

### 构建问题

- 清理构建缓存：`./gradlew clean`
- 刷新依赖：`./gradlew --refresh-dependencies`
- 查看依赖树：`./gradlew dependencies`

## 参考资源

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/)
- [Kotlin 编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- [JetBrains Marketplace](https://plugins.jetbrains.com/)
