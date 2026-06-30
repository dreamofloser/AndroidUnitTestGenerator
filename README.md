# AndroidUnitTestGenerator

面向 Android 应用的单元测试自动生成框架。当前项目已完成第一阶段基础闭环，并开始进入第二阶段规则增强：扫描 Java 源码，解析 public 类和方法，生成 JUnit4 测试代码，并根据简单规则生成断言、参数变体和异常测试。

## 当前进度

- 已实现 Gradle 插件 `com.codex.android-testgen`
- 已提供任务 `generateUnitTests`
- 已支持 Java 源码扫描和 JavaParser 解析
- 已支持 JUnit4 Java 测试生成
- 已支持简单算术返回值 `assertEquals`
- 已支持 boolean 比较的 true/false 场景
- 已支持 String 空串/null 参数变体
- 已支持简单 `throw new XxxException` 异常场景
- 已支持增强版 Markdown 生成报告
- 已提供 `sample-target` 示例模块用于演示和验证

## 项目结构

```text
AndroidUnitTestGenerator
|-- testgen-plugin        测试生成 Gradle 插件核心代码
|-- sample-target         第一阶段演示用示例模块
|-- docs                  阶段设计文档
|-- ROADMAP.md            后续开发路线
|-- settings.gradle.kts   根工程配置
`-- build.gradle.kts      根工程构建配置
```

## 快速演示

在项目根目录运行：

```bash
gradle :sample-target:generateUnitTests
```

生成结果：

```text
sample-target/src/test/java/com/example/app/
sample-target/build/reports/testgen/report.md
```

继续运行测试：

```bash
gradle :sample-target:test
```

如果命令行没有全局 `gradle`，可以在 Android Studio 右侧 Gradle 面板中运行：

```text
sample-target -> Tasks -> verification -> generateUnitTests
sample-target -> Tasks -> verification -> test
```

## 给队友的开发步骤

1. 克隆项目：

```bash
git clone https://github.com/dreamofloser/AndroidUnitTestGenerator.git
cd AndroidUnitTestGenerator
```

2. 用 Android Studio 打开项目根目录。

3. 等待 Gradle Sync 完成。

4. 运行第一阶段生成任务：

```bash
gradle :sample-target:generateUnitTests
```

5. 运行示例模块测试：

```bash
gradle :sample-target:test
```

6. 开发插件功能时，主要修改：

```text
testgen-plugin/src/main/kotlin/com/codex/testgen/
```

7. 修改后优先用 `sample-target` 验证生成效果。

## .gitignore 检查

项目已经排除了本地环境和构建产物：

```text
.gradle/
build/
out/
.idea/
*.iml
local.properties
captures/
.externalNativeBuild/
.cxx/
```

提交前可以用下面命令确认这些文件不会被提交：

```bash
git check-ignore -v local.properties
git check-ignore -v .gradle/
git check-ignore -v build/
git check-ignore -v .idea/
git check-ignore -v sample-target/build/
git check-ignore -v testgen-plugin/build/
```

也可以查看 Git 状态：

```bash
git status --ignored -s
```

如果某个本地文件已经被 Git 跟踪，单靠 `.gitignore` 不会让它自动消失，需要先取消跟踪：

```bash
git rm --cached local.properties
```

## 当前阶段说明

第一阶段已经完成基础闭环。第二阶段正在增强规则生成能力，当前已覆盖简单算术断言、boolean 比较断言、参数变体和异常测试。复杂分支、Mock、Android 组件模板和覆盖率报告会在后续阶段继续完善。
