# 第四阶段：Android 模块与 Kotlin 支持

## 阶段目标

第四阶段将框架接入真实 Android 模块，使其能够在 Android Studio 工程中生成并运行本地单元测试。

```text
Android 源码 -> Java/Kotlin 解析 -> Android 场景识别 -> JUnit4/MockK/Robolectric/Compose 测试生成 -> testDebugUnitTest 验证
```

## 已完成内容

### Android 示例模块

新增 `sample-android-app` 模块，用于验证 Android 本地单元测试生成能力。模块覆盖以下场景：

- ViewModel 与 LiveData。
- Context、Resources、SharedPreferences。
- Intent 构造。
- Activity 生命周期。
- Fragment 生命周期。
- Kotlin data class。
- Kotlin suspend Repository。
- Kotlin Retrofit API。
- Compose UI 基础渲染。

### Android 类型 import 支持

解析器会记录源码 import，生成测试时补充必要 import，避免 Android 类型、ViewModel 类型、返回类型在测试代码中缺失。

### LiveData 本地测试规则

检测到 LiveData 场景时生成：

```java
@Rule
public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
```

该规则用于保证 LiveData 在本地单元测试中同步执行。

### Robolectric 模板

Activity、Fragment、Context、Resources、Intent 等 Android 本地测试场景使用 Robolectric。基础 Activity 生命周期模板覆盖：

```text
create -> start -> resume -> pause -> stop -> destroy
```

Fragment 当前优先支持平台 `android.app.Fragment` 的基础生命周期。AndroidX FragmentScenario 属于后续扩展。

### Kotlin 支持

第四阶段引入 Kotlin 源码生成能力，支持：

- `.kt` 文件扫描。
- data class 构造测试。
- 主构造函数参数解析。
- suspend 方法测试。
- MockK `coEvery`、`coVerify`。
- Kotlin ViewModel + StateFlow 模板。
- Compose UI 基础模板。
- Room DAO / Retrofit API 基础识别。

### Kotlin PSI 解析

Kotlin 解析器已从轻量文本解析升级为 Kotlin Compiler PSI 解析。当前可处理：

- package 和 import。
- 顶层 class、interface、data class。
- 主构造函数参数。
- 成员函数、suspend fun、参数、返回类型。
- 成员属性。
- 类注解和函数注解。
- ViewModel 继承识别。
- Compose 顶层函数识别。
- Room DAO / Retrofit API 基础识别。
- 简单依赖调用，例如 `repository.getWeather(...)`。

当前尚未接入完整 Kotlin classpath 符号解析，因此对 typealias、导入别名、复杂泛型、扩展函数、嵌套类等仍存在限制。

## 验证策略

项目采用四层验证，不只依赖单一真实项目。

| 层级 | 作用 |
| --- | --- |
| `testgen-plugin` | 验证解析器、生成器、报告读取器 |
| `sample-target` | 验证普通 Java/Kotlin 模块 |
| `sample-android-app` | 验证 Android、Kotlin、Robolectric、Compose、MockK 模板 |
| `weather` | 验证真实 Kotlin Android 工程接入 |

真实项目用于验证工具能否落地，不作为所有模板设计的唯一依据。

## 真实项目接入原则

允许修改：

- Gradle 插件接入。
- 测试依赖。
- `testGen` 配置。
- 生成目录和包名过滤。

不应修改：

- `src/main` 下业务逻辑。
- ViewModel、Repository、Activity、UI 等真实业务代码。
- 手写测试文件。

如果真实项目源码结构导致生成失败，应优先改生成器或增加模板，而不是反向修改业务源码适配生成器。

## 当前验证结果

真实 weather 项目已完成接入验证：

```powershell
cd D:\University\Junior\android\weather
.\gradlew.bat :app:generateUnitTests --rerun-tasks
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks
```

验证结果为 `BUILD SUCCESSFUL`。生成测试输出到独立 generated 目录，避免覆盖手写测试。

## 阶段限制

- 当前定位为 Android 本地单元测试，不是设备端 Instrumented Test。
- AndroidX FragmentScenario 尚未完成。
- Compose UI 主要依赖稳定 `testTag`。
- RecyclerView、Navigation、SavedStateHandle、配置变更仍需扩展。
- Kotlin PSI 尚未包含完整符号解析和类型推断。
- `kotlin-compiler-embeddable` 仍在插件 classpath 中，后续可拆成独立 parser module 或 classloader 以减少 Gradle 警告。