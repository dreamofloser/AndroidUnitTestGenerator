# Kotlin PSI 解析与待完善清单

## 本轮完成

Kotlin 解析器已经从轻量正则解析升级为 Kotlin Compiler PSI 解析。

当前 PSI 解析能处理：

- package 和 import。
- 顶层 `class`、`interface`、`data class`。
- 主构造函数参数。
- 成员函数、`suspend fun`、函数参数、返回类型。
- 成员属性。
- 类注解和函数注解。
- ViewModel 继承识别。
- Compose 顶层函数识别。
- Room DAO / Retrofit API 基础识别。
- 函数体中的简单依赖调用，例如 `repository.getWeather(...)`。
- 带 UTF-8 BOM 的 Kotlin 文件 package 识别。
- 文本兜底只补顶层类，避免把 sealed class 的内部 `Success` / `Error` 误当成独立目标。

真实 weather 项目已验证：

```powershell
.\gradlew.bat :app:generateUnitTests --rerun-tasks
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks
```

结果：`BUILD SUCCESSFUL`。当前 weather 回归中生成 12 个测试类、16 个测试方法。

`sample-android-app` 也已经加入 Kotlin 数据层 fixture，用于在可控示例模块中验证 Kotlin PSI、Retrofit API 和 suspend Repository，不再只依赖 weather 真实项目。

## 为什么还保留局部 fallback

Kotlin Compiler PSI 能稳定拿到 AST 结构，但当前插件还没有接入完整 Kotlin 编译分析，也就是没有 classpath 符号解析和类型推断。

真实 Retrofit 接口里经常有这种写法：

```kotlin
@GET("v1/forecast")
suspend fun getCurrentWeather(
    @Query("latitude") lat: Double = 39.9042,
    @Query("timezone") timezone: String = "auto"
): WeatherResponse
```

为了在不引入完整语义分析的情况下稳定生成测试，当前对 Retrofit 接口增加了局部签名恢复：仍由 PSI 找到顶层结构，再从函数文本中恢复多行参数、默认值、行尾注释、返回类型和 `suspend` 信息。

这不是回退到全文件正则，而是 PSI 基础上的小范围兜底。

另外，生成任务会清理本轮没有生成的 `*GeneratedTest.kt` 旧文件。原因是 Kotlin 模板修正后，旧的自动生成文件如果继续留在测试目录里，Gradle 仍会编译它们，导致“明明本轮不再生成但测试还失败”的假象。清理范围只限 `GeneratedTest.kt` 后缀，不碰手写测试。

## 当前未完成

- Kotlin classpath 符号解析：还不能知道 `typealias`、导入别名、同名类型到底指向哪个符号。
- 泛型语义推断：只能解析 `Result<T>`、`Response<T>`、`List<T>` 这类常见字符串形态。
- 嵌套类和内部类：当前主要面向顶层类。
- sealed class 分支：能跳过或识别基础类型，但还不能自动生成每个分支的测试。
- 扩展函数：暂未作为被测目标生成测试。
- object / companion object：Java static 类似场景还需要单独模板。
- AndroidX Fragment：还没有切到 `FragmentScenario` 模板。
- Room 真数据库测试：还没有 `Room.inMemoryDatabaseBuilder`、Entity、Migration 测试。
- Retrofit 真实 HTTP 测试：还没有 MockWebServer 的 URL、query、header、JSON parse 校验。
- Flow / Paging / RxJava：还没有完整异步数据流模板。
- JaCoCo Android variant：当前读取已有 XML，还没有自动识别 `debug`、flavor、多模块覆盖率并合并。
- Kotlin Compiler PSI classpath 隔离：当前直接依赖 `kotlin-compiler-embeddable`，后续最好拆成隔离 parser module 或独立 classloader，减少 Gradle 插件 classpath 警告。
- LLM 辅助：还没有真正接 LLM，只保留了设计文档和扩展点。

## 待完善功能总表

| 方向 | 待完善内容 |
| --- | --- |
| Kotlin 解析 | 符号解析、类型推断、typealias、导入别名、嵌套类、object、companion object |
| Java 解析 | 注解语义、泛型返回、内部类、接口默认方法 |
| Android 组件 | AndroidX Activity/Fragment、SavedStateHandle、Intent extras、配置变更 |
| Compose UI | 点击、输入、列表、导航、语义树、更多 testTag 自动发现 |
| Room | in-memory database、DAO CRUD、Entity 默认值、Migration |
| Retrofit | MockWebServer、HTTP 错误码、headers、query/path、JSON fixture |
| Repository | Fake 数据源、成功/失败/空数据/异常路径、参数捕获 |
| 协程/异步 | Flow、StateFlow 更多场景、Dispatcher 注入、Turbine、Paging |
| 覆盖率 | Android variant JaCoCo、多模块合并、覆盖率阈值、趋势报告 |
| 报告 | 失败原因解释、生成质量评分、跳过原因分类、覆盖率建议 |
| LLM | 语义分类、测试计划生成、断言建议、编译错误修复建议 |


