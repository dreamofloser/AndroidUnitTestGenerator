# 第一阶段设计说明

## 阶段目标

第一阶段要完成一个最小可用闭环：

```text
源码文件 -> 源码解析 -> 类模型 -> 测试生成 -> 测试文件 -> 生成报告
```

这个阶段不追求生成非常聪明的测试，而是先证明框架架构可行、插件任务可运行、生成结果可落地。

## 支持输入

- 默认扫描 `src/main/java` 下的 Java 文件。
- 支持 public 顶层类。
- 支持 public、非 abstract、非 native 方法。

## 生成输出

- 生成 JUnit4 Java 测试类。
- 测试类与源类保持相同 package。
- static 方法直接通过类名调用。
- 实例方法会先创建目标类对象，再调用方法。
- 构造函数参数和方法参数先使用保守的示例值。

## 第一阶段生成策略

参数示例值规则：

| 类型 | 示例值 |
| --- | --- |
| `int` / `Integer` | `1` |
| `long` / `Long` | `1L` |
| `double` / `Double` | `1.0d` |
| `boolean` / `Boolean` | `true` |
| `String` | `"sample"` |
| `List` | `java.util.Collections.emptyList()` |
| `Set` | `java.util.Collections.emptySet()` |
| `Map` | `java.util.Collections.emptyMap()` |
| 其他对象 | `null` |

生成测试方法命名规则：

```text
方法名_shouldRunWithoutException
```

## 当前限制

- 暂不支持 Kotlin 解析。
- 暂不生成 Android 组件专用测试模板。
- 暂不对基本类型返回值生成准确断言。
- 暂不支持 Mock。
- 复杂对象参数暂时传入 `null`。

这些限制是刻意保留的，目的是让第一阶段稳定跑通，并给第二、三、四阶段留下清晰扩展点。
