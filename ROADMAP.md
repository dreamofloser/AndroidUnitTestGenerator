# Android 应用单元测试自动生成框架开发路线

## 第一阶段：基础闭环版

目标：做出一个可运行的 Gradle 插件，能够扫描 Java 源码并生成 JUnit4 测试骨架。

交付内容：

- Gradle 插件项目结构。
- `generateUnitTests` 任务。
- Java 源码扫描器。
- 基于 JavaParser 的类和方法提取。
- JUnit4 测试代码生成器。
- Markdown 生成报告。
- 用于验证插件的 `sample-target` 示例模块。

验收标准：

- 目标模块可以应用 `com.codex.android-testgen` 插件。
- 运行 `generateUnitTests` 后，会在 `src/test/java` 生成测试文件。
- 报告能列出扫描文件数、解析类数、生成类数和跳过原因。

## 第二阶段：规则增强版

目标：让生成的测试用例更接近真实单元测试。

当前已加入：

- 简单算术返回值断言。
- boolean 比较表达式 true/false 场景。
- String 空串/null 参数变体。
- int/long 零值和负数参数变体。
- 简单异常场景生成。
- 生成报告统计增强。

后续继续增强：

- 基本类型、字符串、集合、空值输入策略。
- 边界值生成。
- 异常测试生成。
- 简单条件和返回表达式分析。
- 更准确的断言生成。

## 第三阶段：Mock 支持版

目标：支持带依赖的业务层代码，例如 Service、Repository、Presenter。

计划功能：

- 构造函数依赖识别。
- Java 代码生成 Mockito 测试。
- Kotlin 代码生成 MockK 测试。
- 对简单依赖调用生成 `when` 和 `verify` 模板。

## 第四阶段：Android 特性版

目标：支持 Android 架构组件和常见 Android 本地行为。

计划功能：

- ViewModel 测试模板。
- LiveData 规则和观察辅助方法。
- Repository 测试模板。
- SharedPreferences 和资源访问场景。
- Robolectric 测试模板。

## 第五阶段：报告与可视化版

目标：让框架更适合展示、答辩和效果评估。

计划功能：

- HTML 报告。
- JSON 报告，方便后续工具读取。
- 生成/跳过方法统计。
- 测试执行结果汇总。
- JaCoCo/Kover 覆盖率集成。
