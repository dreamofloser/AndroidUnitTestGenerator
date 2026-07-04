# 数据层模板与覆盖率报告

## Room / Retrofit 更深入模板是什么

Room 和 Retrofit 都属于 Android 项目里常见的数据层：

- Room：本地数据库，常见对象是 `@Dao`、`@Entity`、`RoomDatabase`、Repository。
- Retrofit：网络请求，常见对象是 API interface、`@GET`、`@POST`、`Response<T>`、Repository。

“更深入模板”不是只给一个空测试类，而是根据数据层代码的形态生成更贴近真实测试的代码。

当前第一版先做 mock 契约测试：

```kotlin
private val target = mockk<WeatherApiService>()

@Test
fun getCurrentWeather_returnsStubbedValue() = runTest {
    coEvery { target.getCurrentWeather(any(), any()) } returns Response.success(WeatherResponse())

    val result = target.getCurrentWeather(1.0, 1.0)

    assertNotNull(result)
    coVerify { target.getCurrentWeather(any(), any()) }
}
```

这类测试的价值是：能证明生成器识别到了 API/DAO 方法、参数、返回类型、协程调用方式，并生成了可读的 MockK 测试骨架。

## 为什么现在先保守做

Room 和 Retrofit 的真实测试比普通类复杂：

- Room in-memory database 需要数据库类、Entity、DAO、schema、Migration 信息。
- Retrofit MockWebServer 需要 baseUrl、converter、response json、headers、path/query 校验。
- 真实项目里的 Repository 常常还包了一层 `Result`、sealed class、Flow、Paging。

所以当前先做 mock 契约模板，适合这个阶段：

- 不改业务源码。
- 不强行引入大量新依赖。
- 不要求真实项目具备完整可测试数据库配置。
- 可以先验证解析器和生成器能稳定识别数据层。

下一步再扩展为真实集成测试模板：

- Room：`Room.inMemoryDatabaseBuilder`、DAO CRUD、Migration。
- Retrofit：MockWebServer、HTTP 状态码、错误体、JSON 解析。
- Repository：DAO/API fake、成功/失败/空数据/异常路径。

## JaCoCo 覆盖率集成方式

生成器现在不直接强制运行 JaCoCo，而是读取已有 JaCoCo XML：

```kotlin
testGen {
    coverageReportFile.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
}
```

原因是不同项目的测试任务不一样：

- 普通 Java/Kotlin 模块通常是 `test`。
- Android 模块通常是 `testDebugUnitTest`。
- 多 flavor 项目可能是 `testFreeDebugUnitTest`、`testPaidDebugUnitTest`。
- 多模块项目可能有多个 JaCoCo XML。

所以当前策略是“报告读取能力先稳定”，后面再做 Android variant 自动识别和多模块合并。

## 演示命令

普通 Java 示例模块：

```powershell
.\gradlew.bat :sample-target:generateUnitTests --rerun-tasks
.\gradlew.bat :sample-target:test --rerun-tasks
.\gradlew.bat :sample-target:generateUnitTests --rerun-tasks
```

查看报告：

```text
sample-target/build/reports/testgen/report.md
```

覆盖率 XML：

```text
sample-target/build/reports/jacoco/test/jacocoTestReport.xml
```
