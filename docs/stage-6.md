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

## 尚未完成

| 方向 | 待扩展内容 |
| --- | --- |
| Retrofit | 普通返回体 `HttpException`、POST body、Header、`@Path`、JSON fixture、错误体解析 |
| Room | `Room.inMemoryDatabaseBuilder`、DAO CRUD、Entity 默认值、Migration |
| Repository | 空数据、缓存命中/未命中、多依赖调用、sealed result、Flow、PagingData |
| Kotlin 语义 | classpath 符号解析、typealias、导入别名、扩展函数、嵌套类 |
| 异步数据流 | Flow、StateFlow 更多场景、Turbine、Paging |
| 覆盖率 | Android variant JaCoCo 自动定位、多模块合并、覆盖率阈值 |
| LLM 辅助 | 测试计划生成、断言建议、失败日志解释、覆盖率缺口分析 |

## 后续建议

优先级建议如下：

1. 扩展 Retrofit 普通返回体的 `HttpException` 测试。
2. 增加 Retrofit POST、Header、`@Path`、JSON fixture 模板。
3. 增加 Room in-memory database fixture。
4. 扩展 Repository Flow、PagingData、缓存策略模板。
5. 将覆盖率缺口分析和 LLM 测试计划作为增强层接入。