# AndroidUnitTestGenerator

面向 Android 应用源码的单元测试自动生成 Gradle 插件。项目已形成 Java/Kotlin 解析、JUnit4/Mockito/MockK/Coroutine/Robolectric/数据层模板、质量报告、测试难度分析，以及受约束的 LLM 两轮用例扩展闭环。

> 最终课程设计报告：[安卓应用单元测试自动生成框架设计与实现](docs/final-report/README.md)

## 当前进度

- 已实现 Gradle 插件 `io.github.dreamofloser.android-testgen`
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
- 已支持 Kotlin data class 构造断言测试、suspend Repository + MockK 成功/失败路径测试
- 已支持 Kotlin ViewModel + StateFlow + coroutine dispatcher 测试模板
- 已支持基础 Compose UI 测试模板，能生成 `createComposeRule` + `onNodeWithTag` 测试
- 已支持基础 Activity/Fragment 生命周期测试模板，能生成 Robolectric 生命周期测试
- 已支持基础 Room DAO / Retrofit API mock 契约测试模板，并开始支持 Retrofit MockWebServer 本地请求测试和 `Response<T>` 错误响应测试
- 已支持读取 JaCoCo XML，并把覆盖率汇总写入 Markdown 生成报告
- 已支持增强版 Markdown 生成报告，包含质量分、质量门、风险提示、语言/类型分布和验证命令
- 已支持 `verifyGeneratedUnitTests` 验证任务，可检查生成报告、质量分、生成数量和可选风险门禁
- 已提供 `sample-target` 示例模块用于演示和验证
- 已提供 `sample-android-app` Android 示例模块用于第四阶段验证
- 已在 `sample-android-app` 中加入 Kotlin 数据层 fixture，用于验证 PSI、Retrofit、suspend Repository

- 已接入 LLM Agent 结构化测试规划，可选择目标参数和边界输入策略并生成测试方法
- 已实现固定两轮的用例指引生成器，支持缺口分析、跨轮指纹去重和增量用例扩展
- 已实现可解释的测试难度分析，输出生成难度、测试优先级、自动化置信度、测试策略和价值矩阵
- LLM 配置支持 Gradle 参数与环境变量，模型文件与 GitHub 仓库分离
- 插件支持发布到 Maven Local 并接入任意 Android App 模块

## 项目结构

```text
AndroidUnitTestGenerator
|-- testgen-plugin        测试生成 Gradle 插件核心代码
|-- sample-target         普通 Java 示例模块
|-- sample-android-app    Android Library 示例模块
|-- docs                  阶段设计文档和验证策略
|   +-- final-report      最终课程报告页和架构图
|-- ROADMAP.md            后续开发路线
|-- settings.gradle.kts   根工程配置
`-- build.gradle.kts      根工程构建配置
```

## 固定版本与验证环境

仓库使用 Gradle Wrapper 固定构建工具，不要求安装全局 `gradle`。以下版本用于当前最终验收：

| 项目 | 当前版本或配置 | 说明 |
| --- | --- | --- |
| 测试生成插件 | `0.1.0-SNAPSHOT` | 插件 ID：`io.github.dreamofloser.android-testgen` |
| Gradle Wrapper | `9.4.1` | 首次运行时下载发行包，发行包本身不提交仓库 |
| Kotlin Gradle Plugin | `2.2.0` | 同时提供 Kotlin Compiler PSI 解析能力 |
| Android Gradle Plugin | `9.2.1` | 用于 `sample-android-app` |
| Android SDK | `compileSdk 36.1`，`minSdk 24` | SDK 安装目录由每台电脑自行配置 |
| JDK | `21` | 当前验证机器为 JDK `21.0.8`；示例 Android 源码兼容级别为 Java 11 |
| Android Studio | `AI-253.32098.37.2534.15232325` | 当前验证安装；不是仓库内置依赖，也不要求队友使用完全相同的补丁版本 |
| Ollama | 客户端 `0.24.0` | 仅启用真实 LLM 时需要，默认规则生成不依赖 Ollama |

当前插件是源码项目和 Maven Local 快照，**尚未发布到 Gradle Plugin Portal 或公共 Maven 仓库**。本仓库内的示例模块通过 `pluginManagement.includeBuild("testgen-plugin")` 使用插件；接入仓库外的 Android 项目时，需要先在本机执行：

```powershell
.\gradlew.bat -p testgen-plugin publishToMavenLocal
```

该命令只会写入执行者自己的 Maven Local 缓存。另一台电脑必须重新执行，不能通过 GitHub 自动获得本机发布结果。

## GitHub 与本地环境边界

| 内容 | 是否上传 GitHub | 说明 |
| --- | --- | --- |
| 插件源码、测试、示例源码、阶段文档 | 是 | 克隆仓库后可以查看和继续开发 |
| 最终课程报告 | 是 | 位于 `docs/final-report/README.md`，架构图位于同目录 `assets/` |
| Gradle Wrapper 脚本与 Wrapper JAR | 是 | Gradle 9.4.1 发行包首次运行时从网络下载 |
| Android Studio、JDK、Android SDK/Build Tools | 否 | 属于每台电脑的开发环境；`local.properties` 中的 `sdk.dir` 已忽略 |
| Ollama 程序、后台服务和模型权重 | 否 | 模型通常为 GB 级，只保存在使用者电脑中 |
| `OLLAMA_MODELS` 和本机磁盘路径 | 否 | 当前机器可使用 `D:\ollama\models`，其他电脑可以选择任意目录 |
| LLM API Key | 否 | 只能通过环境变量提供，不得写入源码、README 或提交记录 |
| `.gradle/`、`build/`、`.idea/`、`*.iml` | 否 | 本机缓存、构建产物和 IDE 状态，由 `.gitignore` 排除 |
| Maven Local 中的插件快照 | 否 | `publishToMavenLocal` 后生成，路径通常位于用户目录的 `.m2/repository` |
| 原始 TestGen/JaCoCo/四模型运行报告 | 否 | 位于各模块 `build/reports/`，需要运行任务重新生成；最终统计已写入课程报告 |
| 真实天气项目 `D:\University\Junior\android\weather` | 否 | 仅作为本机阶段性验证样本，不属于本仓库 |
| Word/WPS 版课程报告 | 否 | GitHub 上传的是可直接浏览的 Markdown 报告和配套图片 |

因此，GitHub 仓库提供的是**可复现的源码、配置、测试和说明**，不是完整开发环境镜像。克隆后仍需准备 JDK 和 Android SDK；启用 LLM 时还需单独准备 Ollama 与模型。

### 本地 LLM 模型

LLM 默认关闭，未安装 Ollama 时仍可运行规则生成。当前四模型对照实验使用以下 Ollama 标签，模型文件不会出现在 `sample-android-app` 或 Git 仓库中：

```powershell
ollama pull deepseek-coder:6.7b-instruct
ollama pull qwen2.5-coder:3b-instruct
ollama pull granite-code:3b-instruct
ollama pull phi3:mini
```

当前主要验收配置为：

```powershell
$env:TESTGEN_LLM_ENABLED = "true"
$env:TESTGEN_LLM_PROVIDER = "ollama"
$env:TESTGEN_LLM_MODEL = "deepseek-coder:6.7b-instruct"
$env:TESTGEN_LLM_ENDPOINT = "http://localhost:11434/api/generate"
```

`OLLAMA_MODELS` 只是 Ollama 的本机存储设置，必须在拉取模型前配置并重启 Ollama。例如：

```powershell
[System.Environment]::SetEnvironmentVariable("OLLAMA_MODELS", "D:\OllamaModels", "User")
```

这个路径不应写入项目构建脚本。另一台电脑可以使用 C 盘、D 盘或 Ollama 默认目录，只要 `ollama list` 能看到配置的模型即可。

### 首次构建为什么较慢

仓库不会提交 Gradle、Android SDK、Maven 依赖、Kotlin 编译器、Robolectric、MockK 等缓存。新电脑第一次 Gradle Sync 或运行测试时需要联网下载这些内容，耗时可能达到数十分钟；后续构建会复用本机缓存。若网络中断，应先完成 Android SDK 36.1 安装并确认 Gradle 能访问依赖仓库，再重新执行 Wrapper 命令。

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
.\gradlew.bat :sample-android-app:verifyGeneratedUnitTests
.\gradlew.bat :sample-android-app:testDebugUnitTest
```

Android 示例模块的纯规则基线会生成 10 个测试类、24 个测试方法，并在报告中统计 LiveData、Robolectric、Android import 和 Compose UI 使用情况。启用真实 Ollama 两轮扩展后，当前验收结果为 10 个测试类、26 个测试方法。模块同时覆盖 Kotlin data class、Retrofit API 和 suspend Repository 场景。

真实 Kotlin 项目接入时，推荐把自动生成结果输出到单独目录：

```kotlin
testGen {
    sourceDir.set(layout.projectDirectory.dir("src/main/java"))
    testOutputDir.set(layout.projectDirectory.dir("src/test/java/generated"))
    reportOutputDir.set(layout.buildDirectory.dir("reports/testgen"))
    packageIncludes.set(listOf("你的包名"))
    minimumQualityScore.set(50)
    expectedTestTaskName.set(":app:testDebugUnitTest")
    guideExpansionIterations.set(2)
    maxGuidesPerClassPerIteration.set(1)
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
testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/
```

7. 修改后优先用 `testgen-plugin` 的单元测试验证生成器逻辑，再用 `sample-target` 验证普通 Java 生成效果，用 `sample-android-app` 验证 Android 生成效果，最后再选择真实项目做烟雾测试。第五阶段后，可以先运行 `verifyGeneratedUnitTests` 检查生成报告质量，再运行实际测试任务。

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

## LLM 与真实项目演示

本地 Ollama 模型不保存在仓库中。另一台电脑需要准备 JDK 21、Android SDK 36、Ollama 和配置使用的模型；模型可以存放在任意磁盘。运行 LLM 演示前，在 PowerShell 中设置当前会话配置：

```powershell
$env:TESTGEN_LLM_ENABLED = "true"
$env:TESTGEN_LLM_PROVIDER = "ollama"
$env:TESTGEN_LLM_MODEL = "deepseek-coder:6.7b-instruct"
$env:TESTGEN_LLM_ENDPOINT = "http://localhost:11434/api/generate"
.\gradlew.bat :sample-android-app:generateUnitTests --rerun-tasks
.\gradlew.bat :sample-android-app:testDebugUnitTest --rerun-tasks
```

默认执行两轮扩展。第 1 轮由 LLM 从生成器支持的缺口中选择用例，第 2 轮排除已采纳指纹后继续补充；报告位于 `sample-android-app/build/reports/testgen/report.md`。

`sample-android-app` 只是回归测试样品。处理真实 App 时，应先把 `testgen-plugin` 发布到 Maven Local，再在目标 App 模块应用插件：

```powershell
.\gradlew.bat -p testgen-plugin publishToMavenLocal
```

完整的目标项目配置、模型部署边界和课堂演示流程见 `docs/project-overview.md`。

## 当前阶段说明

第一阶段完成基础闭环。第二阶段增强规则生成能力，覆盖简单算术断言、boolean 比较断言、参数变体和异常测试。第三阶段支持 Mockito，用于处理带 Repository/Service/Dao 等依赖对象的业务类。第四阶段接入 Android 示例模块，并支持 Kotlin Compiler PSI、ViewModel/LiveData、Robolectric、Context/Resources/SharedPreferences、Intent、Kotlin data class、suspend Repository + MockK、StateFlow、Compose、Activity/Fragment、Room DAO 和 Retrofit API 等模板。第五阶段加入质量分、质量门、风险提示、语言/类型分布、跳过原因汇总和 `verifyGeneratedUnitTests`。第六阶段强化 Repository 与 Retrofit 数据层模板。

六个阶段完成后，项目继续加入受约束的 LLM 测试规划增强：真实 Ollama/DeepSeek 会输出结构化 JSON 用例指引，采纳器校验方法、参数和输入策略，再由确定性生成器转换为可编译测试。当前真实模型验收记录为 10 个测试类、26 个测试方法、46 个断言、8 条聚焦建议、两轮各采纳 1 个场景，目标测试 26/26 通过。LLM 仅参与测试代码生成，不修改业务源码，也不负责自动修复编译错误。

生成前还会执行 `TestabilityAnalyzer`。该模块基于控制流、依赖、异步状态、Android 耦合、外部资源和生成器支持缺口进行确定性评分，并在报告中输出 `Testability Insights`、测试价值矩阵、方法排行榜、主要难度来源和两轮增量收益。示例模块当前分析 21 个目标，其中 7 个属于高优先级候选。
