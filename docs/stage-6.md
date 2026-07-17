# 第六阶段：数据层深度模板

## 阶段目标

第六阶段强化 Android 数据层测试模板，重点覆盖 Repository、Retrofit API、Room DAO 及其调用链。

```text
数据层源码 -> 数据源类型识别 -> Mock/Fake/MockWebServer 模板 -> 成功路径 -> 失败路径 -> 交互验证
```

## 已完成内容

### Repository Result<T> 模板

对返回 `Result<T>` 的 Repository 方法，生成器可以生成成功和失败两类路径：

- 成功路径：mock 数据源返回成功数据，断言 `result.isSuccess`，断言 `result.getOrNull()` 不为空。
- 失败路径：mock 数据源抛出异常，断言 `result.isFailure`，断言异常 message。
- 依赖交互验证：suspend 依赖使用 `coVerify`，普通依赖使用 `verify`。

示例：

```kotlin
@Test
fun getWeather_success_returnsSuccess() = runTest {
    coEvery { apiService.getCurrentWeather(any(), any()) } returns WeatherResponse()

    val result = target.getWeather(1.0, 1.0)

    assertTrue(result.isSuccess)
    assertNotNull(result.getOrNull())
    coVerify { apiService.getCurrentWeather(any(), any()) }
}
```

### Retrofit API mock 契约测试

对 Retrofit API interface，生成器会生成 MockK 契约测试，验证方法可以被 stub、调用和 verify。

```kotlin
coEvery { target.getCurrentWeather(any(), any()) } returns Response.success(WeatherResponse())
val result = target.getCurrentWeather(1.0, 1.0)
assertNotNull(result)
coVerify { target.getCurrentWeather(any(), any()) }
```

### Retrofit 注解契约测试

生成器会通过反射检查 Retrofit 注解：

- HTTP 方法注解，例如 `@GET`。
- path，例如 `v1/forecast`。
- `@Query` 参数名。

该测试不访问网络，用于验证接口声明契约没有被改错。

### MockWebServer 本地请求测试

生成器已支持第一版 MockWebServer 测试。该测试不会访问真实网络，而是在测试进程中启动本地 fake HTTP server，再用 Retrofit 指向本地 server。

当前覆盖：

- GET 请求。
- query 参数。
- path 校验。
- HTTP method 校验。
- `Response<T>` 的 500 错误响应。

示例断言：

```kotlin
assertEquals("GET", request.method)
assertEquals("/forecast/raw?city=sample", request.path)
assertFalse(result.isSuccessful)
assertEquals(500, result.code())
```

### Room DAO 基础模板

当前 Room DAO 主要支持 MockK 契约测试：

- 识别 `@Dao`。
- 识别 suspend DAO 方法。
- 生成 `coEvery`、`coVerify`。

Room 真数据库测试仍属于后续扩展。

## 为什么数据层模板先保守实现

Room 和 Retrofit 的真实集成测试需要额外上下文：

- Room in-memory database 需要 database class、entity、DAO、schema、migration 信息。
- Retrofit MockWebServer 深度测试需要 converter、JSON fixture、header、path、query、错误体等信息。
- Repository 常见返回类型包括 `Result<T>`、sealed class、Flow、PagingData 等，需要逐步扩展。

因此当前先实现稳定的 mock 契约和局部 MockWebServer 模板，保证在不同项目中能编译、能运行、能报告。

## 当前验证结果

Android 示例模块当前验证结果：

```text
Generated test classes: 10
Generated test methods: 24
Generated assertions: 44
Generation quality score: 92/100
```

真实 weather 项目验证结果：

```text
Generated test classes: 12
Generated test methods: 17
Generated assertions: 49
Generation quality score: 82/100
```

## 阶段结束后的 LLM 增强

第六阶段完成后，项目继续实现了结构化 LLM 测试规划和场景到测试代码的映射。2026 年 7 月 15 日使用真实 Ollama 与 `deepseek-coder:6.7b-instruct` 验证：

```text
Generated test classes: 10
Generated test methods: 26
Generated assertions: 46
LLM agent suggestions: 8
Guide expansion iterations: 2
LLM adopted test methods: 2
Executed tests: 26
Failed tests: 0
```

LLM 当前从缺口分析器提供的预校验候选中选择真实方法、目标参数和输入策略，支持空字符串、仅空格字符串、null、零值、负数、false、空集合等策略。默认执行固定两轮：第 1 轮采纳后记录“源码类 + 方法 + 参数 + 输入策略”指纹，第 2 轮重新分析缺口并排除已采纳指纹。采纳器会拒绝重复或无法安全生成的场景，模型不可用时仍保留规则生成结果。

用例指引迭代实现位于：

```text
testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/guide/
|-- TestCaseGuide.kt
|-- TestCaseGapAnalyzer.kt
|-- TestCaseGuideGenerator.kt
`-- IterativeTestSuiteExpander.kt
```

报告的 `Iterative Test Suite Expansion` 表格记录每轮候选数、模型返回数、采纳数、重复数、拒绝数、剩余缺口和累计采纳数。本次真实 Ollama 验证两轮状态均为 `EXPANDED`，分别生成空字符串和仅空格字符串用例，26 个测试全部通过。

## 阶段结束后的测试难度分析

项目进一步加入 `TestabilityAnalyzer`，在测试生成前对方法级目标进行可解释分析：

| 指标 | 含义 |
| --- | --- |
| Generation difficulty | 自动生成测试所需处理的结构复杂度 |
| Test priority | 该目标的分支、外部依赖、关键操作和边界输入价值 |
| Automation confidence | 当前规则与专项模板可靠生成测试的把握 |
| Recommended strategy | JUnit、Mockito/MockK、Coroutine、ViewModel、Robolectric、Room 或 Retrofit |

报告新增 `Testability Insights`、`Test Value Matrix`、`Method Ranking`、`Difficulty Driver Distribution` 和 `Iteration Gain Insight`。示例模块共分析 21 个目标，平均生成难度为 18.3/100，高优先级目标为 7 个；最难目标是 `KotlinWeatherRepository#loadForecast`，最高测试优先级目标是 `LoginViewModel#loadDisplayName`。两轮 LLM 相对 24 个规则基线测试分别增加 1 个方法，最终为 26 个。

评分实现位于：

```text
testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/analysis/
|-- TestabilityInsight.kt
`-- TestabilityAnalyzer.kt
```

评分由 AST/PSI 提取证据确定性计算，不使用 LLM 直接给出分数。LLM 读取排序后的候选指标，只负责选择下一条增量用例。

## 保留的扩展方向

| 方向 | 待扩展内容 |
| --- | --- |
| Retrofit | 普通返回体 `HttpException`、POST body、Header、`@Path`、JSON fixture、错误体解析 |
| Room | `Room.inMemoryDatabaseBuilder`、DAO CRUD、Entity 默认值、Migration |
| Repository | 空数据、缓存命中/未命中、多依赖调用、sealed result、Flow、PagingData |
| Kotlin 语义 | classpath 符号解析、typealias、导入别名、扩展函数、嵌套类 |
| 异步数据流 | Flow、StateFlow 更多场景、Turbine、Paging |
| 覆盖率 | Android variant JaCoCo 自动定位、多模块合并、覆盖率阈值 |
| LLM 深化 | 复杂业务断言和覆盖率缺口分析；结构化计划、两轮用例指引和边界场景生成已经完成 |

## 后续建议

优先级建议如下：

1. 扩展 Retrofit 普通返回体的 `HttpException` 测试。
2. 增加 Retrofit POST、Header、`@Path`、JSON fixture 模板。
3. 增加 Room in-memory database fixture。
4. 扩展 Repository Flow、PagingData、缓存策略模板。
5. 在现有结构化 LLM 测试计划基础上扩展复杂断言和覆盖率缺口分析。
