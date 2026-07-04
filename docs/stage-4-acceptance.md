# 第四阶段验收清单

这一阶段已经进入真实 Android 模块和 Kotlin 项目支持，后续每次改生成器，都建议按下面顺序验收。

## 必跑命令

```powershell
cd D:\University\Junior\AndroidUnitTestGenerator
.\gradlew.bat -p testgen-plugin test --rerun-tasks
.\gradlew.bat :sample-target:generateUnitTests --rerun-tasks
.\gradlew.bat :sample-target:test --rerun-tasks
.\gradlew.bat :sample-android-app:generateUnitTests --rerun-tasks
.\gradlew.bat :sample-android-app:testDebugUnitTest --rerun-tasks
```

真实项目验收：

```powershell
cd D:\University\Junior\android\weather
.\gradlew.bat :app:generateUnitTests --rerun-tasks
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks
```

## 当前 fixture 覆盖

| 模块 | 覆盖内容 |
| --- | --- |
| `testgen-plugin` | 解析器、生成器、报告读取器单元测试 |
| `sample-target` | 普通 Java、Mockito、JaCoCo XML 汇总 |
| `sample-android-app` | Android Java、Robolectric、LiveData、Activity、Fragment、Kotlin data class、Kotlin Retrofit API、Kotlin suspend Repository |
| `weather` | 真实 Kotlin Android 项目、Compose UI、Retrofit、ViewModel、Repository；当前回归生成 12 个测试类、16 个测试方法 |

## 通过标准

- 所有命令都应该 `BUILD SUCCESSFUL`。
- 生成报告中不应该出现异常中断。
- 生成测试必须放在配置的测试输出目录。
- 自动生成测试不能覆盖手写测试。
- Kotlin 生成器应清理本轮不再生成的 `*GeneratedTest.kt` 旧文件，避免旧文件继续参与编译。
- 真实项目只允许改 Gradle 接入、测试依赖、生成目录，不应该为了适配生成器修改 `src/main` 业务代码。

## 当前已知技术债

- Kotlin PSI 解析已经接入，但还没有完整 classpath 符号解析。
- `kotlin-compiler-embeddable` 直接在插件 classpath 中，后续最好隔离成 parser module 或独立 classloader。
- Retrofit 当前是 MockK 契约测试，还不是 MockWebServer 集成测试。
- Room 当前是待扩展项，还没有 in-memory database fixture。

