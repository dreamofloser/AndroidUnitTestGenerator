# AndroidUnitTestGenerator

面向 Android 应用的单元测试自动生成框架。当前项目已完成普通 Java 模块的规则化生成、Mockito 依赖验证，并开始接入真实 Android 模块：扫描源码，解析 public 类和方法，生成 JUnit4/Mockito 测试代码，并输出 Markdown 生成报告。

## 当前进度

- 已实现 Gradle 插件 `com.codex.android-testgen`
- 已提供任务 `generateUnitTests`
- 已支持 Java 源码扫描和 JavaParser 解析
- 已支持 JUnit4 Java 测试生成
- 已支持简单算术返回值 `assertEquals`
- 已支持 boolean 比较的 true/false 场景
- 已支持 String 空串/null 参数变体
- 已支持简单 `throw new XxxException` 异常场景
- 已支持构造函数依赖识别
- 已支持 Mockito mock、when stub 和 verify
- 已支持源码 import 解析，能为生成测试补充 Android 类型导入
- 已支持 LiveData 本地单元测试规则 `InstantTaskExecutorRule`
- 已支持 Robolectric Runner 和固定 SDK 配置
- 已提供 Context、Resources、SharedPreferences、Intent 示例场景
- 已开始支持 Kotlin 源码扫描和 Kotlin/JUnit4 测试生成
- 已接入 Kotlin Compiler PSI，用 AST 解析 Kotlin 顶层类、接口、函数、属性、注解和 import
- 已支持 Kotlin data class 构造断言测试、suspend Repository + MockK 测试
- 已支持 Kotlin ViewModel + StateFlow + coroutine dispatcher 测试模板
- 已支持基础 Compose UI 测试模板，能生成 `createComposeRule` + `onNodeWithTag` 测试
- 已支持基础 Activity/Fragment 生命周期测试模板，能生成 Robolectric 生命周期测试
- 已支持基础 Room DAO / Retrofit API mock 契约测试模板
- 已支持读取 JaCoCo XML，并把覆盖率汇总写入 Markdown 生成报告
- 已支持增强版 Markdown 生成报告
- 已提供 `sample-target` 示例模块用于演示和验证
- 已提供 `sample-android-app` Android 示例模块用于第四阶段验证
- 已在 `sample-android-app` 中加入 Kotlin 数据层 fixture，用于验证 PSI、Retrofit、suspend Repository

## 项目结构

```text
AndroidUnitTestGenerator
|-- testgen-plugin        测试生成 Gradle 插件核心代码
|-- sample-target         普通 Java 示例模块
|-- sample-android-app    Android Library 示例模块
|-- docs                  阶段设计文档和验证策略
|-- ROADMAP.md            后续开发路线
|-- settings.gradle.kts   根工程配置
`-- build.gradle.kts      根工程构建配置
```

## 快速演示

在项目根目录运行：

```powershell
.\gradlew.bat :sample-target:generateUnitTests
```

生成结果：

```text
sample-target/src/test/java/com/example/app/
sample-target/build/reports/testgen/report.md
```

继续运行测试：

```powershell
.\gradlew.bat :sample-target:test
```

`sample-target:test` 会继续生成 JaCoCo XML/HTML 覆盖率报告。想把覆盖率写入测试生成报告时，推荐顺序是：

```powershell
.\gradlew.bat :sample-target:generateUnitTests --rerun-tasks
.\gradlew.bat :sample-target:test --rerun-tasks
.\gradlew.bat :sample-target:generateUnitTests --rerun-tasks
```

第三条命令会读取：

```text
sample-target/build/reports/jacoco/test/jacocoTestReport.xml
```

并把 Line、Branch、Instruction、Method 等覆盖率写入：

```text
sample-target/build/reports/testgen/report.md
```

运行 Android 示例模块：

```powershell
.\gradlew.bat :sample-android-app:generateUnitTests
.\gradlew.bat :sample-android-app:testDebugUnitTest
```

Android 示例模块当前会生成 7 个测试类、14 个测试方法，并在报告中统计 LiveData、Robolectric、Android import 和 Compose UI 使用情况。
随着 Kotlin 数据层 fixture 加入，Android 示例模块还会覆盖 Kotlin data class、Retrofit API 和 suspend Repository 场景。

真实 Kotlin 项目接入时，推荐把自动生成结果输出到单独目录：

```kotlin
testGen {
    sourceDir.set(layout.projectDirectory.dir("src/main/java"))
    testOutputDir.set(layout.projectDirectory.dir("src/test/java/generated"))
    reportOutputDir.set(layout.buildDirectory.dir("reports/testgen"))
    packageIncludes.set(listOf("你的包名"))
}
```

真实项目只作为验收样本，不应该为了适配生成器而修改业务源码。允许修改的是 Gradle 插件接入配置、测试依赖和生成测试输出目录；生成结果建议放到 `src/test/java/generated` 这类独立目录，方便和手写测试区分。

如果命令行没有全局 `gradle`，可以在 Android Studio 右侧 Gradle 面板中展开对应模块，运行 `generateUnitTests`、`test` 或 `testDebugUnitTest`。

## 给队友的开发步骤

1. 克隆项目：

```bash
git clone https://github.com/dreamofloser/AndroidUnitTestGenerator.git
cd AndroidUnitTestGenerator
```

2. 用 Android Studio 打开项目根目录。

3. 等待 Gradle Sync 完成。

4. 运行第一阶段生成任务：

```powershell
.\gradlew.bat :sample-target:generateUnitTests
```

5. 运行示例模块测试：

```powershell
.\gradlew.bat :sample-target:test
```

6. 开发插件功能时，主要修改：

```text
testgen-plugin/src/main/kotlin/com/codex/testgen/
```

7. 修改后优先用 `testgen-plugin` 的单元测试验证生成器逻辑，再用 `sample-target` 验证普通 Java 生成效果，用 `sample-android-app` 验证 Android 生成效果，最后再选择真实项目做烟雾测试。

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

第一阶段完成基础闭环。第二阶段增强规则生成能力，覆盖简单算术断言、boolean 比较断言、参数变体和异常测试。第三阶段支持 Mockito，用于处理带 Repository/Service/Dao 等依赖对象的业务类。第四阶段已经接入 Android 示例模块，并开始支持真实 Kotlin Android 项目，覆盖 Android 类型 import、Kotlin Compiler PSI 解析、ViewModel/LiveData 本地单元测试、Robolectric、Context/Resources/SharedPreferences 依赖 Mock、Intent 场景、Kotlin data class、suspend Repository + MockK、Kotlin ViewModel + StateFlow、基础 Compose UI 测试、Activity/Fragment 生命周期测试、Room DAO / Retrofit API mock 契约测试、JaCoCo 覆盖率汇总。更复杂的 Android 组件模板、LLM 辅助生成和覆盖率报告会继续完善。

