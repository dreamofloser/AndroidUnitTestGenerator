项目链接：https://github.com/dreamofloser/AndroidUnitTestGenerator

组长：施澄宇 dreamofloser  太乙平台已绑定  dreamofloser
贡献:
项目调研情况简述：
负责调研 Android 单元测试自动生成框架的整体建设目标和应用场景。重点分析 Android 项目中手写单元测试成本高、测试覆盖不稳定、不同模块测试方式差异较大的问题，确定项目采用 Gradle 插件形式接入 Android 工程，并形成“源码扫描、结构解析、测试场景生成、测试代码输出、质量报告生成”的整体技术路线。
技术调研情况：
重点调研 Gradle Plugin、Android Gradle Plugin、JUnit4、Kotlin/Java 源码解析、测试生成流程设计、质量评分机制以及 LLM 融合方式。对比直接使用 LLM 生成完整测试代码和“LLM 辅助场景分析 + 本地模板稳定生成”两种方案，最终确定由 LLM 分析测试意图、边界条件和 Mock 策略，再由模板生成器输出可编译测试代码的架构。
对应第一阶段和大模型

组员：程诗哲 ComputerPlayerFurina 太乙平台已绑定 太乙用户f9c07
贡献：
项目调研情况简述：
负责调研 Android 应用中常见业务模块的测试需求。重点分析 ViewModel、Repository、Retrofit 网络接口、Room 数据库、Compose UI、Activity/Fragment 生命周期等模块的测试特点，区分本地单元测试、Mock 测试、UI 测试和生命周期测试的适用边界。
技术调研情况：
重点调研 AndroidX Test、JUnit4、Mockito/MockK、Kotlin Coroutine Test、MockWebServer、Room in-memory database、Compose UI Test、Robolectric 等技术。结合项目当前阶段，优先完善可以稳定生成和运行的本地单元测试模板，对 Compose UI 和生命周期测试采用保守生成策略，避免生成不可运行或依赖过重的测试代码。
对应第三第六阶段

组员：李美霖 lml1112 太乙平台已绑定 李美霖 
贡献：
项目调研情况简述：
负责调研 Android 项目源码结构和单元测试生成所需的基础信息。重点分析 Java/Kotlin 文件中类、方法、参数、返回值、注解、修饰符等信息如何被提取，并研究这些结构化信息如何转化为可生成测试代码的中间模型。
技术调研情况：
重点调研 Kotlin/Java 源码解析方式，包括轻量级文本解析、Kotlin Compiler PSI、JavaParser 等方案。结合项目当前进度，优先采用轻量级解析器完成稳定可控的类和方法识别，同时为后续接入更完整的 Kotlin PSI 解析能力预留扩展空间。
对应第二阶段和第四阶段

组员：董羽佳 dyj051130 太乙平台已绑定 董羽佳
贡献：
项目调研情况简述：
负责调研测试生成框架的结果展示、质量评估和工程化验证方式。重点分析自动生成测试后如何向使用者说明生成结果，包括生成了哪些测试类、覆盖了哪些源码、哪些方法被跳过、质量分如何计算、是否读取覆盖率报告，以及如何通过自动化任务验证生成结果是否满足要求。
技术调研情况：
重点调研 Markdown 报告生成、JaCoCo 覆盖率 XML 读取、Gradle 验证任务、CI 自动化构建、GitHub Actions、测试报告归档等工程化技术。结合项目当前情况，负责将生成结果、质量评分、覆盖率输入和 LLM 建议整理为可读报告，并研究后续如何在 CI 中自动执行测试生成和验证流程。
对应第五阶段

commit情况：
施澄宇：
程诗哲：
李美霖：
董羽佳：

commit具体描述：
