# 第一阶段：最小可用生成闭环

## 阶段目标

第一阶段建立测试生成框架的最小可用闭环，验证整体架构可行。

```text
源码文件 -> 源码扫描 -> 源码解析 -> 类模型 -> 测试生成 -> 测试文件 -> 生成报告
```

## 已完成内容

- 创建 Gradle 插件工程 `testgen-plugin`。
- 注册 `generateUnitTests` 任务。
- 实现 Java 源码扫描。
- 实现基础 Java 类解析。
- 建立统一源码模型 `ClassModel`、`MethodModel`、`ParameterModel`。
- 生成 JUnit4 Java 测试类。
- 输出 Markdown 生成报告。

## 生成策略

第一阶段采用保守参数样例值，目标是保证生成代码可读、可落地。

| 类型 | 示例值 |
| --- | --- |
| `int` / `Integer` | `1` |
| `long` / `Long` | `1L` |
| `double` / `Double` | `1.0d` |
| `boolean` / `Boolean` | `true` |
| `String` | `"sample"` |
| `List` | `Collections.emptyList()` |
| `Set` | `Collections.emptySet()` |
| `Map` | `Collections.emptyMap()` |
| 其他对象 | `null` |

生成测试方法命名形式：

```text
methodName_shouldRunWithoutException
```

## 主要代码位置

| 功能 | 位置 |
| --- | --- |
| 插件入口 | `testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/AndroidTestGenPlugin.kt` |
| 生成任务 | `testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/task/GenerateUnitTestsTask.kt` |
| 源码扫描 | `testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/scanner/SourceScanner.kt` |
| Java 解析 | `testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/parser/JavaSourceParser.kt` |
| Java 测试生成 | `testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/generator/JUnit4JavaTestGenerator.kt` |
| 生成报告 | `testgen-plugin/src/main/kotlin/io/github/dreamofloser/testgen/report/MarkdownReportWriter.kt` |

## 阶段限制

- 仅支持基础 Java 源码解析。
- 不生成复杂业务断言。
- 不支持 Mock。
- 不支持 Android 组件模板。
- 复杂对象参数采用保守处理。

该阶段的作用是验证框架主链路，不追求测试语义完整性。