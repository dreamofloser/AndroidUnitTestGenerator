# LLM 扩展点研究

## 研究目标

当前框架以规则生成和模板生成作为主体：解析源码结构，识别类、方法、构造函数依赖、Android 类型和 Kotlin 场景，再生成可运行的本地单元测试。

LLM 暂时不接入代码，先研究未来可以放在哪些环节。核心原则是：

```text
规则负责稳定结构，LLM 负责语义理解和复杂判断。
```

也就是说，不能把整个测试生成都交给 LLM。更稳妥的方式是让规则系统先产出结构化信息，再让 LLM 在有限范围内补充测试意图、断言和说明。

## 适合加入 LLM 的位置

### 1. 被测代码语义分类

规则可以识别 “这是一个 class，有这些方法”，但很难理解方法语义。

例如：

```kotlin
fun fetchWeather(isRefresh: Boolean = false)
```

规则只能看到方法名、参数和依赖调用。LLM 可以进一步判断：

- 这是一次刷新/网络请求逻辑。
- 应该测试成功状态、失败状态、刷新结束状态。
- `uiState`、`selectedCity`、`isRefreshing` 是重要断言对象。

适合输入给 LLM 的信息：

```text
类名、方法名、构造依赖、公开属性、方法体摘要
```

不建议把整个项目无差别塞给 LLM。

### 2. 测试场景建议

规则模板现在能生成固定场景，例如：

- data class 构造测试
- Repository success/failure
- ViewModel failure/select/refresh

LLM 可以根据业务语义补更多场景，例如：

- 天气码为晴天、多云、暴雨时分别返回什么描述。
- 城市切换后是否触发网络请求。
- 空列表、null 字段、异常 message 是否正确显示。

这里 LLM 不直接写文件，而是输出结构化测试计划：

```json
[
  {
    "method": "fetchWeather",
    "scenario": "repository returns no current weather",
    "expected": "uiState becomes Error"
  }
]
```

然后仍由规则生成器根据计划生成代码。

### 3. 断言推断

规则能生成 `assertNotNull` 或 `assertTrue(result.isSuccess)`，但这些断言偏保守。

LLM 可以帮助推断更具体的断言：

- `WeatherUiState.Error.message == "Network Error"`
- `selectedCity.value == city`
- `forecastList.size == daily.time.size`
- `hourly.time` 应该被截成 `HH:mm`

更好的架构是：

```text
规则生成基础可运行测试 -> LLM 建议增强断言 -> 编译测试验证
```

如果增强断言导致测试失败，可以回退到基础断言。

### 4. Mock/Fake 数据构造

现在框架靠类型规则生成样例值，例如：

```kotlin
String -> "sample"
Double -> 1.0
List<T> -> emptyList<T>()
```

LLM 可以根据业务构造更真实的数据：

```kotlin
WeatherResponse(
    current_weather = CurrentWeather(25.0, 10.0, 1),
    daily = DailyWeather(
        time = listOf("2026-07-03"),
        weathercode = listOf(0)
    )
)
```

这一点很适合真实 Android 项目，因为很多测试失败不是结构问题，而是 mock 数据太空，无法触发业务分支。

### 5. 编译错误修复建议

自动生成测试后，Gradle 可能出现：

- import 缺失
- 泛型推断失败
- suspend/mock 写法不匹配
- Robolectric SDK 不兼容
- Kotlin 可空类型断言不匹配

这类错误适合 LLM 做 “失败日志解释 + 修复建议”。但修复动作仍建议走规则补丁，而不是让 LLM 直接重写整份测试。

可设计为：

```text
运行测试 -> 收集失败日志 -> LLM 分类错误 -> 生成器规则修复 -> 再运行测试
```

### 6. 覆盖率缺口分析

接入 JaCoCo 后，可以知道哪些类、方法、分支没有覆盖。

LLM 可以根据覆盖率报告和源码解释：

- 哪些分支缺测试。
- 哪些缺口重要。
- 哪些可以忽略。
- 下一轮应该优先生成哪些测试。

这比单纯看百分比更有价值。

### 7. 测试报告解释

当前报告偏工程统计：

```text
Generated test classes
Generated test methods
Mocked dependencies
Robolectric test classes
```

LLM 可以生成面向人的说明：

- 本轮测试覆盖了哪些业务能力。
- 哪些类适合人工继续补测。
- 哪些自动生成测试只是基础断言。
- 当前生成器的限制是什么。

这部分适合论文、课程报告、项目展示。

## 不建议马上用 LLM 的地方

### 1. 不建议直接让 LLM 扫整个项目生成全部测试

原因：

- 输出不稳定。
- 容易引用不存在的类和方法。
- 生成代码可能不可编译。
- 很难稳定复现。

### 2. 不建议让 LLM 替代 AST/规则解析

源码结构解析更适合确定性工具：

- JavaParser
- Kotlin 轻量解析器
- 未来可以接 Kotlin Compiler PSI

LLM 更适合补充语义，不适合承担基础结构识别。

### 3. 不建议一开始就让 LLM 自动改源代码

测试生成框架应该优先生成测试，不应该修改业务代码。否则风险会变大，也不利于演示。

## 推荐架构

未来可以设计成三层：

```text
第一层：规则解析层
  扫描源码、解析类、方法、依赖、Android 类型、Kotlin 类型

第二层：规则模板层
  生成基础可运行测试，保证编译通过

第三层：LLM 辅助层
  根据结构化上下文建议测试场景、增强断言、解释失败日志、生成报告说明
```

这样框架仍然可靠，LLM 只在最有价值的位置发挥作用。

## 适合本项目的落地顺序

1. 先接入 JaCoCo 覆盖率报告。
2. 让框架输出结构化上下文文件，例如 `testgen-context.json`。
3. 设计 LLM 输入输出格式，只让 LLM 返回测试计划，不直接写代码。
4. 让规则生成器根据测试计划生成代码。
5. 运行 Gradle 测试，失败时收集日志。
6. 让 LLM 对失败日志做分类和修复建议。
7. 经过人工确认后再应用修复。

## 当前结论

LLM 最适合加在 “理解业务语义、生成测试计划、增强断言、解释失败日志、分析覆盖率缺口” 这些位置。

当前阶段先不接入 LLM 是合理的，因为我们需要先把规则生成器做稳。等 Java、Android、Kotlin、ViewModel、报告这些基础能力稳定后，再加 LLM 会更可靠，也更容易展示它的价值。
