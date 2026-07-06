# 项目本质说明

## 项目定位

AndroidUnitTestGenerator 是一个基于 Gradle 插件的 Android 单元测试自动生成框架。项目目标是在 Android 工程构建阶段扫描 Java/Kotlin 源码，解析代码结构，按规则生成可编译、可运行的本地单元测试，并输出生成质量报告。

该项目不是运行时测试工具，也不是人工编写测试的替代品。它的本质是：

```text
源码分析工具 + 测试模板生成器 + 生成质量报告器
```

## 基于 Gradle 插件的含义

Android 项目通常使用 Gradle 管理构建、测试和依赖。将本框架实现为 Gradle 插件，可以使测试生成能力直接接入 Android Studio 工程。

目标模块应用插件后：

```kotlin
plugins {
    id("io.github.dreamofloser.android-testgen")
}
```

插件会注册两个主要任务：

| 任务 | 作用 |
| --- | --- |
| `generateUnitTests` | 扫描源码、解析结构、生成测试代码、输出报告 |
| `verifyGeneratedUnitTests` | 读取生成报告，检查生成数量、断言数量和质量分是否达标 |

插件入口位于：

```text
testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/AndroidTestGenPlugin.kt
```

生成任务位于：

```text
testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/task/GenerateUnitTestsTask.kt
```

## 自动生成测试代码的实现方式

自动生成过程采用确定性的规则和模板，不依赖人工临时编写。

整体流程如下：

```text
源码目录
  -> SourceScanner 扫描 .java / .kt 文件
  -> JavaSourceParser / KotlinSourceParser 解析源码
  -> ClassModel / MethodModel 表示类、方法、参数、注解、依赖调用
  -> JUnit4JavaTestGenerator / KotlinUnitTestGenerator 选择模板
  -> 写入 src/test/java 或配置的测试输出目录
  -> MarkdownReportWriter 输出生成报告
```

核心模型位于：

```text
testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/model/SourceModel.kt
```

生成器位于：

```text
testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/generator/JUnit4JavaTestGenerator.kt
testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/generator/KotlinUnitTestGenerator.kt
```

生成器会根据源码类型选择模板，例如：

| 源码类型 | 生成模板 |
| --- | --- |
| 普通 Java/Kotlin 类 | JUnit4 基础测试、参数样例、基础断言 |
| 构造函数依赖类 | Mockito / MockK stub 与 verify |
| ViewModel | LiveData / StateFlow / Coroutine 测试 |
| Activity / Fragment | Robolectric 生命周期测试 |
| Compose UI | Compose test rule 与 testTag 断言 |
| Retrofit API | MockK 契约、注解检查、MockWebServer 请求与错误响应测试 |
| Room DAO | MockK DAO 契约测试 |

## 质量分的来源

质量分是生成结果的启发式评分，不等同于业务覆盖率。它用于衡量“本次自动生成结果是否充分、是否需要人工重点复核”。

质量分计算位于：

```text
testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/report/MarkdownReportWriter.kt
```

主要因素包括：

| 因素 | 影响 |
| --- | --- |
| 解析类数量与生成类数量比例 | 生成比例越高，得分越高 |
| 生成断言数量 | 断言越充分，得分越高 |
| 专项模板命中 | Android、Compose、Room、Retrofit、Robolectric 等模板会提高评分 |
| JaCoCo 覆盖率输入 | 如果报告读取到 JaCoCo XML，会获得额外加分 |
| fallback 模板数量 | fallback 越多，扣分越多 |
| skipped class 数量 | 跳过类越多，扣分越多 |

`verifyGeneratedUnitTests` 会读取报告并检查质量分是否低于配置阈值。示例模块当前配置：

```kotlin
minimumQualityScore.set(80)
```

## JaCoCo 覆盖率说明

当前框架已经支持读取已有 JaCoCo XML 并写入生成报告，但尚未在所有 Android 项目中自动创建和执行 JaCoCo 任务。

原因包括：

- 普通 JVM 模块通常使用 `test` 任务。
- Android 模块通常使用 `testDebugUnitTest`、`testReleaseUnitTest` 等 variant 任务。
- 多 flavor 项目可能存在 `testFreeDebugUnitTest`、`testPaidDebugUnitTest` 等任务。
- 多模块项目可能生成多个 JaCoCo XML，需要合并。
- Android Gradle Plugin 不同版本对 coverage 任务和输出路径存在差异。

因此当前采用稳定策略：

```text
如果项目已有 JaCoCo XML，生成器读取并写入报告；如果没有，则报告标记 Coverage input 为 REVIEW，但不阻塞测试生成。
```

该设计避免覆盖率任务配置失败影响测试代码生成。后续扩展方向是自动识别 Android variant、定位 JaCoCo XML、支持多模块覆盖率合并。

## 验证体系

项目采用四层验证：

| 层级 | 作用 |
| --- | --- |
| `testgen-plugin` 单元测试 | 验证解析器、生成器、报告读取器 |
| `sample-target` | 验证普通 Java/Kotlin 模块和 JaCoCo 读取 |
| `sample-android-app` | 验证 Android、Kotlin、Robolectric、MockK、MockWebServer 模板 |
| 真实 weather 项目 | 验证插件能否接入真实 Android Studio 工程 |

真实项目只作为落地验证样本，不应为了适配生成器修改业务源码。允许修改的是 Gradle 插件接入、测试依赖、生成目录和包名过滤配置。

## LLM 扩展定位

当前实现以规则和模板为主体，暂未接入 LLM。后续 LLM 适合用于：

- 基于源码语义生成测试计划。
- 建议更具体的业务断言。
- 根据编译失败日志给出修复建议。
- 分析 JaCoCo 覆盖率缺口。
- 生成面向验收或报告的解释文本。

LLM 不应替代 AST/PSI 解析，也不应直接无约束修改业务源码。推荐架构是：规则系统生成基础可运行测试，LLM 只在结构化上下文基础上提供增强建议。