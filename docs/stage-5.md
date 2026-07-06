# 第五阶段：报告、质量门与覆盖率读取

## 阶段目标

第五阶段强化生成结果的可解释性和可验收性，使框架不仅能生成测试，还能说明生成质量、风险和验证方式。

```text
生成结果 -> 统计指标 -> 质量分 -> 质量门 -> 风险提示 -> 覆盖率读取 -> 验证任务
```

## 已完成内容

### Markdown 报告增强

报告新增以下内容：

- `Generation quality score`：生成质量分。
- `Quality Gates`：源码解析、测试生成、断言、fallback、跳过类、覆盖率输入等状态。
- `Generated Class Mix`：按源码语言和源码类型统计。
- `Risk Review`：提示 fallback、跳过类、缺少覆盖率、模板边界等风险。
- `Validation Commands`：提示后续应运行的验证命令。
- `Skipped Reason Summary`：统计跳过原因。
- 生成结果表新增语言、类型、mock、Robolectric、Compose、Room、Retrofit 等列。

报告生成器位于：

```text
testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/report/MarkdownReportWriter.kt
```

### 质量分计算

质量分是启发式生成质量评分，不等同于业务覆盖率。主要计算因素包括：

| 因素 | 影响 |
| --- | --- |
| 生成类数 / 解析类数 | 生成比例越高，得分越高 |
| 平均断言数量 | 断言越多，得分越高 |
| 专项模板命中 | Android、Compose、Room、Retrofit 等模板提高得分 |
| 覆盖率输入 | 读取到 JaCoCo XML 时加分 |
| fallback 方法 | 数量越多，扣分越多 |
| skipped class | 数量越多，扣分越多 |

质量分用于快速判断本次生成是否需要重点复核，不能替代人工测试评审和真实覆盖率分析。

### verifyGeneratedUnitTests 任务

新增 `verifyGeneratedUnitTests` 任务，用于读取生成报告并检查：

- 是否生成测试类。
- 是否生成测试方法。
- 是否生成断言。
- 质量分是否达到阈值。
- skipped 和 fallback 风险是否需要提示。

相关代码：

```text
testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/task/VerifyGeneratedUnitTestsTask.kt
testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/report/ReportVerificationReader.kt
```

示例模块配置：

```kotlin
minimumQualityScore.set(80)
expectedTestTaskName.set(":sample-android-app:testDebugUnitTest")
```

### JaCoCo 覆盖率读取

当前框架支持读取已有 JaCoCo XML，并将覆盖率汇总写入 `report.md`。

示例配置：

```kotlin
testGen {
    coverageReportFile.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
}
```

当前没有在所有 Android 项目中自动生成 JaCoCo 报告，原因是 Android 项目的测试任务和覆盖率输出与 variant、flavor、多模块、AGP 版本相关。直接自动配置可能导致不同项目构建失败。

因此当前策略是：

```text
生成器读取已有 JaCoCo XML；没有 XML 时不阻塞测试生成，只在报告中标记 Coverage input 为 REVIEW。
```

## 当前验证结果

Android 示例模块当前可运行：

```powershell
cd D:\University\Junior\AndroidUnitTestGenerator
.\gradlew.bat :sample-android-app:generateUnitTests --rerun-tasks
.\gradlew.bat :sample-android-app:verifyGeneratedUnitTests --rerun-tasks
.\gradlew.bat :sample-android-app:testDebugUnitTest --rerun-tasks
```

当前报告摘要：

```text
Generated test classes: 10
Generated test methods: 24
Generated assertions: 44
Generation quality score: 92/100
```

## 阶段限制

- `verifyGeneratedUnitTests` 当前验证报告指标，不自动执行目标测试任务。
- Java 自动生成文件仍需更安全的旧文件清理策略。
- JaCoCo 当前读取已有 XML，尚未自动创建 Android variant 覆盖率任务。
- 质量分是生成质量评分，不代表真实业务逻辑充分覆盖。
- 风险提示仍是规则化文案，后续可结合 LLM 或更细粒度规则增强。