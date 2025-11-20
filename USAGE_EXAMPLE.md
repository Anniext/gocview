# Gocview 使用示例

本文档展示如何使用 gocview 插件监控和可视化 Go 应用程序的代码覆盖率。

## 前置条件

1. 已安装 gocview 插件
2. 已安装 Go 插件
3. 已安装 [goc](https://github.com/qiniu/goc) 工具

安装 goc：
```bash
go install github.com/qiniu/goc@latest
```

## 使用步骤

### 1. 准备 Go 项目

假设你有一个简单的 Go 项目：

```go
// main.go
package main

import (
    "fmt"
    "net/http"
)

func main() {
    http.HandleFunc("/", handleRoot)
    http.HandleFunc("/hello", handleHello)
    
    fmt.Println("Server starting on :8080")
    http.ListenAndServe(":8080", nil)
}

func handleRoot(w http.ResponseWriter, r *http.Request) {
    fmt.Fprintf(w, "Welcome!")
}

func handleHello(w http.ResponseWriter, r *http.Request) {
    name := r.URL.Query().Get("name")
    if name == "" {
        name = "World"
    }
    fmt.Fprintf(w, "Hello, %s!", name)
}
```

### 2. 使用 goc 构建应用

在项目根目录执行：

```bash
# 启动 goc server
goc server &

# 使用 goc 构建应用
goc build --center=http://localhost:7777 -o myapp .
```

### 3. 在 IDE 中运行应用

#### 方式一：使用 Goc Build 配置（如果已实现）

1. 点击 IDE 右上角的运行配置下拉菜单
2. 选择 "Edit Configurations..."
3. 点击 "+" 添加 "Goc Build" 配置
4. 配置参数后运行

#### 方式二：使用普通运行配置

1. 创建一个 Go Application 运行配置
2. 在 "Program arguments" 中添加必要的参数
3. 运行应用

当应用启动时，控制台会输出类似：
```
[goc] goc server started: http://127.0.0.1:49598
Server starting on :8080
```

### 4. 查看覆盖率

1. **打开工具窗口**
   - 在 IDE 底部找到 "Goc Coverage" 工具窗口
   - 插件会自动检测到 goc server 地址

2. **自动获取覆盖率**
   - 检测到 goc server 后，插件会自动调用 `goc profile` 获取覆盖率数据
   - 数据会以表格形式展示

3. **查看文件覆盖率**
   - 表格显示每个文件的覆盖率百分比
   - 颜色标识：
     - 🟢 绿色：覆盖率 ≥ 80%
     - 🟡 黄色：覆盖率 50%-80%
     - 🟠 橙色：覆盖率 0%-50%
     - 🔴 红色：覆盖率 0%

4. **查看详细信息**
   - 点击任意文件行
   - 下方面板会显示该文件的详细代码块信息
   - 包括每个代码块的位置、语句数和执行次数

### 5. 触发代码执行以更新覆盖率

访问应用的不同端点来触发代码执行：

```bash
# 访问根路径
curl http://localhost:8080/

# 访问 hello 端点
curl http://localhost:8080/hello?name=Alice
```

然后点击工具窗口中的 "刷新覆盖率" 按钮，查看更新后的覆盖率数据。

## 覆盖率数据说明

### 文件级别汇总

| 列名 | 说明 |
|------|------|
| 文件 | 源代码文件的完整路径 |
| 覆盖率 | 该文件的覆盖率百分比 |
| 已覆盖 | 已执行的语句数量 |
| 总语句数 | 文件中的总语句数量 |
| 执行次数 | 所有代码块的总执行次数 |

### 代码块详细信息

| 列名 | 说明 |
|------|------|
| 起始位置 | 代码块的起始行:列 |
| 结束位置 | 代码块的结束行:列 |
| 语句数 | 该代码块包含的语句数量 |
| 执行次数 | 该代码块被执行的次数 |
| 状态 | 已覆盖/未覆盖 |

## 原始数据格式

goc profile 返回的原始数据格式：

```
文件路径:起始行.起始列,结束行.结束列 语句数 执行次数
```

示例：
```
git.bestfulfill.tech/devops/demo/main.go:8.13,9.6 1 1
git.bestfulfill.tech/devops/demo/main.go:9.6,12.3 2 70
```

解释：
- `main.go:8.13,9.6`：从第8行第13列到第9行第6列
- `1`：该代码块包含1条语句
- `1`：该代码块被执行了1次

## 常见问题

### Q: 工具窗口显示"等待 goc server 启动..."

**A:** 确保：
1. 应用程序正在运行
2. 控制台输出包含 `[goc] goc server started: http://...`
3. 如果使用自定义端口，确保格式正确

### Q: 点击"刷新覆盖率"后显示错误

**A:** 可能的原因：
1. goc 命令未安装或不在 PATH 中
2. goc server 地址不可访问
3. 应用程序已停止运行

解决方法：
```bash
# 检查 goc 是否安装
which goc

# 手动测试 goc profile 命令
goc profile --center=http://127.0.0.1:49598
```

### Q: 覆盖率数据为空

**A:** 可能的原因：
1. 应用程序刚启动，还没有执行任何代码
2. goc 未正确注入到应用程序中

解决方法：
1. 触发应用程序的各个功能
2. 确保使用 `goc build` 构建应用
3. 点击"刷新覆盖率"按钮

## 高级用法

### 持续监控

你可以在开发过程中保持应用运行，每次修改代码后：
1. 重新构建并运行应用
2. 执行测试或手动触发功能
3. 刷新覆盖率查看变化

### 集成测试

在运行集成测试时：
1. 使用 goc 构建测试应用
2. 运行测试套件
3. 在 IDE 中查看覆盖率报告

### 多服务监控

如果有多个服务：
1. 每个服务使用不同的 goc server 端口
2. 在不同的运行配置中启动各个服务
3. 工具窗口会自动检测最新启动的服务

## 最佳实践

1. **定期刷新**：在执行新功能后及时刷新覆盖率
2. **关注低覆盖率文件**：优先为覆盖率低的文件编写测试
3. **查看详细信息**：通过详细面板了解哪些代码块未被执行
4. **结合测试**：在运行测试时监控覆盖率变化

## 参考资源

- [Goc 官方文档](https://github.com/qiniu/goc)
- [Go 代码覆盖率最佳实践](https://go.dev/blog/cover)
