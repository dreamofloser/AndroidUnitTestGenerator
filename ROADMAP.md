# Android 应用单元测试自动生成框架开发路线

## 已完成能力

- Gradle 插件和 `generateUnitTests`、`verifyGeneratedUnitTests` 任务。
- JavaParser 与 Kotlin Compiler PSI 源码解析。
- JUnit4、Mockito、MockK、Coroutine、Robolectric 基础生成。
- ViewModel、LiveData、StateFlow、Activity、Fragment、Compose、Room、Retrofit 模板。
- Markdown 报告、质量分、JaCoCo XML 读取。
- Ollama、Mock、OpenAI-compatible Provider 和结构化 LLM 测试计划。
- 固定两轮的用例指引生成器、缺口分析和跨轮去重。
- 可解释的测试难度、优先级、自动化置信度和测试价值洞察。

## 本轮完成：两轮用例指引迭代扩展

学校项目的目标是能清楚证明 LLM 参与了测试生成，不要求实现自动修复或复杂自治 Agent。

本轮内容：

1. 缺口分析器先枚举确定性生成器能够实现、但当前尚未覆盖的候选用例。
2. LLM 从候选项中输出目标方法、目标参数和输入策略。
3. 支持 `empty-string`、`blank-string`、`null`、`zero`、`negative`、`false`、`empty-list`。
4. 采纳器检查方法、参数、类型和策略。
5. 指纹由“源码类 + 方法 + 参数 + 输入策略”组成，第 2 轮排除第 1 轮已采纳指纹。
6. Java/Kotlin 生成器将模型选择转换为测试代码。
7. 报告显示每轮候选数、返回数、采纳数、重复数和剩余缺口。
8. 模型输出无效时继续保留规则生成结果。

验收标准：

- 报告出现 `GENERATED`，并显示目标参数和输入策略。
- 报告中的两轮状态均为 `EXPANDED`，累计采纳 2 个场景。
- 生成文件出现 `guide_i1_` 和 `guide_i2_` 测试方法。
- 生成测试能够通过 `testDebugUnitTest`。
- 使用真实 Ollama 时终端显示正确 Provider 和模型名。

当前验收记录：

```text
Provider: ollama
Model: deepseek-coder:6.7b-instruct
Generated test classes: 10
Generated test methods: 26
Generated assertions: 46
LLM suggestions: 8
Guide expansion iterations: 2
LLM adopted scenarios: 2
Executed tests: 26
Failed tests: 0
```

## 本轮完成：用例生成难度与 Insights

1. `TestabilityAnalyzer` 对每个方法或可生成类目标执行静态分析。
2. 难度分由控制流、依赖、异步状态、Android 耦合、外部资源和生成器限制共同组成。
3. 同时输出测试优先级、自动化置信度、推荐测试策略和边界关注点。
4. 报告增加测试价值矩阵、方法排行榜、难度来源分布和两轮边际收益。
5. 两轮用例候选按 `guideValue` 排序，优先考虑高价值且可自动生成的场景。
6. 分数全部来自可追溯规则，不由 LLM 直接生成。

示例模块当前分析结果：

```text
Analyzed test targets: 21
Average generation difficulty: 18.3
High-priority test targets: 7
Hardest target: KotlinWeatherRepository#loadForecast
Highest priority target: LoginViewModel#loadDisplayName
Plugin regression tests: 55 passed, 0 failed
```

## 结项收尾与保留技术债务

### 1. 常用生成场景补充

- 扩大缺口分析器可提出的策略集合，例如代表性有效值、枚举和简单异常场景。
- 继续完善 ViewModel、Retrofit、Room 和 Compose 模板。
- 增加跨次生成结果去重；当前已完成单次任务内的跨轮指纹去重。

### 2. 真实项目验证

- 已把插件接入 weather 项目。
- 接入只修改 Gradle 配置和测试依赖，不修改业务源码。
- 已记录 12 个测试类、17 个测试方法、49 个断言和 82/100 质量分。

### 3. 构建与报告优化

- 增加适合本项目的 `gradle.properties`，减少首次编译后的重复耗时。
- 为 LLM 响应增加简单缓存，避免重复等待。
- 报告补充 LLM 采纳率、生成测试数量和测试执行结果。
- 保持 JaCoCo 报告可以读取，不强求复杂多模块合并。

### 4. 最终演示和文档

- README、六个阶段总结和项目本质说明已整理。
- 架构图、效果评估方法和真实 Ollama 结果已写入项目本质说明。
- 最终提交前保留示例项目、weather 真实项目、生成代码、报告和测试结果截图。

## 最终回归

最终回归是指停止增加功能后，按固定顺序重新执行已有验证，确认文档和收尾修改没有破坏插件。它不是新的开发阶段。

```powershell
.\gradlew.bat -p testgen-plugin test --rerun-tasks
.\gradlew.bat :sample-android-app:generateUnitTests --rerun-tasks
.\gradlew.bat :sample-android-app:testDebugUnitTest --rerun-tasks
.\gradlew.bat :sample-android-app:verifyGeneratedUnitTests -x :sample-android-app:generateUnitTests
.\gradlew.bat :sample-target:test --rerun-tasks
.\gradlew.bat :sample-target:generateUnitTests --rerun-tasks
```

启用真实 LLM 回归前，需要在同一个 PowerShell 会话设置 Ollama 环境变量。验证任务使用 `-x :sample-android-app:generateUnitTests`，只检查当前报告，避免在缺少 LLM 配置的新会话中重新生成并覆盖真实模型结果。

## 不作为学校项目目标

- 不让 LLM 修改业务源码。
- 不实现编译错误自动修复。
- 不实现无上限的自治 Agent、工具调用循环或自动修复闭环。
- 不要求发布到 Gradle Plugin Portal 或 Maven Central。
- 不要求覆盖所有 AGP、Gradle、Kotlin 版本。
- 不做企业级权限、遥测、多租户和远程模型管理。

## 最终完成标准

- 队友从 GitHub 克隆后能够按文档完成构建。
- 示例项目能够稳定生成并运行测试。
- 真实 Android 项目能够应用插件并输出报告。
- LLM 能按固定两轮扩展至少生成两个可运行测试方法，失败时规则生成仍可工作。
- 报告能够说明扫描、生成、跳过、覆盖率和 LLM 参与情况。
- 项目结构、主要代码位置和演示命令可以在答辩中解释清楚。
