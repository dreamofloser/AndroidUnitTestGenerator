# 第四阶段设计说明

## 阶段目标

第四阶段开始接入真实 Android 模块，让框架不只服务普通 Java 模块，也能在 Android Studio 工程里生成并运行本地单元测试。

本阶段先完成 Android Library 模块闭环，并逐步补充 Android 本地测试模板：

```text
Android 源码 -> 解析 Java/Kotlin 类和 import -> 识别 Android 场景 -> 生成 JUnit4/Mockito/MockK/LiveData/Robolectric 测试 -> testDebugUnitTest 运行通过
```

## 已完成内容

### 1. 新增 Android 示例模块

新增 `sample-android-app` 模块，使用 Android Gradle Plugin，并应用本项目的测试生成插件：

```kotlin
plugins {
    id("com.android.library")
    id("com.codex.android-testgen")
}
```

模块中包含几类常见 Android 单元测试对象：

- `LoginViewModel`：ViewModel、LiveData、Repository 依赖。
- `SettingsManager`：SharedPreferences 依赖。
- `AppInfoProvider`：Context 和资源字符串调用。
- `ResourceLabelFormatter`：Resources 字符串读取。
- `ShareIntentFactory`：Intent 构造和 Intent 参数。
- `LifecycleDemoActivity`：Activity 生命周期。
- `LifecycleDemoFragment`：Fragment 生命周期。

### 2. 支持 Android 类型 import

解析器现在会记录源码中的 import，生成测试时会根据构造函数参数、方法参数和返回值自动补充测试文件需要的 import。

例如被测代码使用：

```java
import android.content.Context;
import androidx.lifecycle.LiveData;
```

生成测试会自动带上：

```java
import android.content.Context;
import androidx.lifecycle.LiveData;
```

### 3. 支持 LiveData 本地单元测试规则

当检测到 `LiveData` 或 `MutableLiveData` 时，生成测试会自动加入：

```java
@Rule
public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
```

这样 ViewModel 中同步调用 `setValue` 的场景可以在本地单元测试中稳定运行。

### 4. Android 模块生成与运行闭环

当前已经可以运行：

```powershell
.\gradlew.bat :sample-android-app:generateUnitTests --rerun-tasks
.\gradlew.bat :sample-android-app:testDebugUnitTest --rerun-tasks
```

生成结果包括：

```text
sample-android-app/src/test/java/com/example/androidapp/
sample-android-app/build/reports/testgen/report.md
```

### 5. 支持 Robolectric 测试模板

当检测到 `android.*` 或 Android 测试相关 import 时，生成测试会自动加入：

```java
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
```

这里固定 `sdk = 34` 是因为当前 Robolectric 版本支持到 Android 34，而项目 Android 编译 SDK 更高。固定 SDK 可以避免本地单元测试因为 targetSdk 过新而初始化失败。

### 6. 支持 Android 专项报告统计

生成报告现在会统计：

```text
LiveData rules
Robolectric test classes
Android imports
```

这样可以更清楚地展示当前框架对 Android 场景的支持程度。

当前 Android 示例模块生成结果：

```text
Generated test classes: 7
Generated test methods: 14
LiveData rules: 1
Robolectric test classes: 6
Android imports: 7
```

### 7. 新增生成器回归测试

插件测试中增加了 Android 类型导入和 LiveData 规则生成的回归用例，防止后续改动破坏 Android 模块支持。

### 8. 支持 Kotlin 项目测试生成

真实 Kotlin Android 项目中，主源码经常仍放在 `src/main/java` 目录下，但文件扩展名是 `.kt`。当前生成器已经支持同时扫描 `.java` 和 `.kt` 文件。

Kotlin 第一版支持：

- 顶层 `data class` 构造测试。
- 可空基础类型、可空对象、`List<T>` 属性断言。
- 普通 Kotlin 类主构造函数依赖识别。
- `suspend fun` 本地单元测试。
- Repository 调用依赖时生成 MockK `coEvery`。
- Kotlin ViewModel + StateFlow + coroutine dispatcher 测试模板。
- 基础 Compose UI 测试模板，支持 `createComposeRule` 和 `onNodeWithTag`。
- Room DAO / Retrofit API mock 契约测试模板。
- 生成 `.kt` 测试文件，类名使用 `GeneratedTest` 后缀，避免覆盖手写测试。

已经用真实 weather 项目验证：

```text
项目路径：D:\University\Junior\android\weather
生成目录：app/src/test/java/generated
生成结果：11 个测试类，15 个测试方法，43 个断言
验证命令：.\gradlew.bat :app:testDebugUnitTest --rerun-tasks
验证结果：BUILD SUCCESSFUL
```

Compose UI 第一版会识别满足下面条件的顶层 Composable：

```text
@Composable
fun Xxx(viewModel: SomeViewModel)
```

并且函数体里存在稳定测试标签，例如：

```kotlin
Modifier.testTag("LoadingSpinner")
```

生成测试会使用：

```kotlin
@get:Rule
val composeTestRule = createComposeRule()

composeTestRule.setContent {
    Xxx(viewModel = viewModel)
}

composeTestRule.onNodeWithTag("LoadingSpinner").assertExists()
```

### 9. 支持 Activity/Fragment 生命周期模板

Java 生成器已经可以识别基础 Activity 和 Fragment，并为它们生成 Robolectric 生命周期测试。

Activity 当前会生成：

```java
ActivityController<LifecycleDemoActivity> controller = Robolectric.buildActivity(LifecycleDemoActivity.class);

LifecycleDemoActivity activity = controller.create().start().resume().get();

assertNotNull(activity);

controller.pause().stop().destroy();
```

如果源码中存在无参 boolean getter，例如 `isCreated()`、`isStarted()`、`isResumed()`，生成器会自动加入对应断言。

Fragment 当前会生成：

```java
ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create().start().resume();
Activity activity = controller.get();
LifecycleDemoFragment fragment = new LifecycleDemoFragment();

activity.getFragmentManager().beginTransaction().add(fragment, "target").commitNow();

assertNotNull(fragment);
assertTrue(fragment.isAdded());

controller.pause().stop().destroy();
```

当前 Fragment 模板先支持平台 `android.app.Fragment`，AndroidX Fragment 会在后续用 `FragmentScenario` 或 AndroidX Test 方案继续扩展。

### 10. 真实项目验证原则

真实项目只作为验收样本，不作为唯一标准。当前 weather 项目用于验证 Kotlin Android 工程能否接入插件、生成测试、运行 `testDebugUnitTest`。

接入真实项目时，不应该为了让生成器通过而修改业务源码。允许修改的是 Gradle 插件接入、测试依赖、生成目录和包名过滤配置。生成器能力不足时，应该改生成器或增加模板，而不是反向改真实 App 的业务逻辑。

### 11. Room / Retrofit 深入模板第一版

Room 和 Retrofit 都属于 Android 数据层测试。当前先实现保守但稳定的 mock 契约模板：

- `@Dao` 接口会识别为 Room DAO。
- 带 `@GET`、`@POST`、`@PUT`、`@PATCH`、`@DELETE` 等 HTTP 注解的接口会识别为 Retrofit API。
- `suspend fun` 会生成 `runTest`、`coEvery`、`coVerify`。
- 普通方法会生成 `every`、`verify`。
- 返回 `Response<T>` 时，会生成 `Response.success(T())`。

这一版重点验证“数据源方法能被正确 stub、调用、verify”，不直接启动真实数据库或真实 HTTP mock server。

真实 Room in-memory database、Migration、MockWebServer、response parse、headers/query/path 校验，适合等 DAO/API mock 契约模板稳定后继续扩展。

### 12. JaCoCo 覆盖率写入生成报告

生成任务现在可以读取已有 JaCoCo XML，并把覆盖率汇总写入 `report.md`。

`sample-target` 已经接入标准 JaCoCo 任务：

```powershell
.\gradlew.bat :sample-target:generateUnitTests --rerun-tasks
.\gradlew.bat :sample-target:test --rerun-tasks
.\gradlew.bat :sample-target:generateUnitTests --rerun-tasks
```

第二步生成：

```text
sample-target/build/reports/jacoco/test/jacocoTestReport.xml
```

第三步会把覆盖率写入：

```text
sample-target/build/reports/testgen/report.md
```

当前采用“有 XML 就读取，没有就跳过”的弱依赖方式，避免还没跑 JaCoCo 时阻止测试生成。

## 当前限制

- 目前支持的是 Android 本地单元测试，不是设备端 Instrumented Test。
- 当前 Android 组件以 ViewModel、Manager、Provider、基础 Activity/Fragment 生命周期为主。
- Fragment 当前优先支持平台 `android.app.Fragment`，AndroidX Fragment 生命周期模板仍需继续扩展。
- RecyclerView、Room in-memory database、Retrofit MockWebServer、Flow、RxJava 场景会在后续继续拆分完善。
- Compose UI 目前只支持基础渲染和 testTag 断言，更复杂的交互、列表、导航、截图对比仍需要继续扩展。
- Activity/Fragment 目前只覆盖基础生命周期，更复杂的 Intent、savedInstanceState、配置变更、Fragment 参数等场景仍需扩展。

## 下一步计划

第四阶段后续会继续扩展这些方向：

- Robolectric 场景：Intent extras、savedInstanceState、配置变更、Activity result。
- AndroidX 测试模板：ViewModel、LiveData、SavedStateHandle。
- AndroidX Fragment：FragmentScenario、Navigation、参数传递。
- 网络和数据库依赖：Retrofit MockWebServer、OkHttp、Room in-memory database、Migration 的模板。
- 异步代码：Executor、Handler、Coroutine、Flow、RxJava 的测试规则。
- 覆盖率报告：Android variant 覆盖率、真实项目多模块覆盖率合并。
