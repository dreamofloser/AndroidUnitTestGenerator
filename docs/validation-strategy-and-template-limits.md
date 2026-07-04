# 验证策略与模板边界

## 为什么不能只用天气项目验证

本项目最终面向各种 Android 应用源码，不能只依赖某一个真实项目作为标准。真实项目很有价值，但它只适合做“烟雾测试”和“真实接入验证”，不适合作为所有规则是否正确的唯一依据。

推荐采用四层验证：

1. `testgen-plugin` 单元测试：验证解析器、生成器、报告统计这些核心逻辑。
2. `sample-target`：验证普通 Java 模块的生成闭环。
3. `sample-android-app`：验证 Android 专项模板，例如 Context、Resources、Intent、LiveData、Robolectric、Activity、Fragment。
4. 真实项目：验证插件能否接入真实 Android Studio 工程，生成代码能否在真实依赖环境里运行。

这样设计的好处是：模板能力由可控示例模块保障，真实项目只负责证明“能落地”，不会把生成器绑死在某个具体 App 上。

## 真实项目接入原则

接入真实项目时，生成器不应该反向修改业务源码来适配测试。

允许做的事情：

- 在 Gradle 中接入 `com.codex.android-testgen` 插件。
- 增加本地单元测试所需依赖，例如 Robolectric、MockK、coroutines test、Compose UI test。
- 配置 `testGen` 的 `sourceDir`、`testOutputDir`、`reportOutputDir`、`packageIncludes`。
- 把生成结果输出到独立目录，例如 `src/test/java/generated`。

不应该做的事情：

- 为了让生成器识别而修改 `src/main` 里的业务逻辑。
- 为了让测试通过而改真实 App 的 ViewModel、Repository、Activity 或 UI 代码。
- 把生成测试覆盖到手写测试文件上。

如果某个真实项目因为源码结构复杂导致生成失败，应该优先改生成器模板或增加配置能力，而不是改真实项目源码。

## 当前保守模板清单

下面这些地方是有意保守实现的，不是忘记做完。

| 能力 | 当前策略 | 后续扩展 |
| --- | --- | --- |
| Kotlin 解析 | 已接入 Kotlin Compiler PSI，并用局部 fallback 处理 Retrofit 多行参数 | 继续做 classpath 符号解析、类型别名、扩展函数、嵌套类、sealed hierarchy |
| Compose UI | 只处理顶层 `@Composable fun Xxx(viewModel: SomeViewModel)` 和稳定 `testTag` | 支持参数组合、列表、点击、文本输入、导航、语义树分析 |
| Activity 生命周期 | 使用 Robolectric 生成 `create/start/resume/pause/stop/destroy` 基础测试 | 支持 Intent extras、savedInstanceState、配置变更、结果返回 |
| Fragment 生命周期 | 当前优先支持平台 `android.app.Fragment` 的基础生命周期 | 扩展到 AndroidX Fragment、FragmentScenario、Navigation |
| Room DAO | 当前识别 `@Dao` 并生成 MockK 契约测试 | 接入 Room in-memory database，验证真实 SQL、Entity、Migration |
| Retrofit API | 当前识别 `@GET/@POST/...` 并生成 MockK 契约测试 | 接入 MockWebServer，验证 URL、query、headers、response parse |
| Robolectric SDK | 固定 `@Config(sdk = 34)` | 根据项目依赖和 Robolectric 版本自动选择 |
| ViewModel | 重点支持 LiveData、StateFlow、Repository 依赖、加载/失败/刷新类场景 | 支持 SavedStateHandle、分页、复杂 reducer、事件流 |
| Mock 生成 | Java 用 Mockito，Kotlin suspend 场景用 MockK | 支持 Fake 对象、参数捕获、更多 verify 策略 |
| JaCoCo 覆盖率 | 当前读取已有 JaCoCo XML 并写入生成报告 | 自动识别 Android variant 覆盖率任务，合并多模块报告 |

后续每次新增模板，都应该同步更新这份表，避免“现在先保守一点”在后面被遗忘。

## 天气项目在本项目中的位置

`D:\University\Junior\android\weather` 适合作为真实 Kotlin Android 项目的验收样本，用来回答这些问题：

- 插件能不能接入真实 Android Studio 工程。
- Kotlin 源码扫描是否能处理真实项目目录。
- 生成的测试是否能放在独立 generated 目录。
- `testDebugUnitTest` 是否能完整跑通。

它不应该承担这些职责：

- 作为所有模板设计的唯一依据。
- 作为生成规则正确性的唯一证明。
- 被修改业务代码来迎合生成器。

如果后续要做得更全面，应该继续增加多个可控 fixture，例如：

- Java Activity/Fragment 模块。
- Kotlin Activity/Fragment 模块。
- Compose 页面模块。
- Retrofit/OkHttp 模块。
- Room 数据库模块。
- Flow/coroutines 异步模块。
- XML View/RecyclerView 模块。
