package io.github.dreamofloser.testgen.generator

import io.github.dreamofloser.testgen.model.ClassModel
import io.github.dreamofloser.testgen.model.ConstructorModel
import io.github.dreamofloser.testgen.model.GeneratedTestSource
import io.github.dreamofloser.testgen.model.MethodModel
import io.github.dreamofloser.testgen.model.ParameterModel
import io.github.dreamofloser.testgen.model.PropertyModel
import io.github.dreamofloser.testgen.model.SourceClassKind
import io.github.dreamofloser.testgen.llm.LlmGenerationGuidance
import io.github.dreamofloser.testgen.llm.LlmInputStrategy
import io.github.dreamofloser.testgen.llm.LlmTestScenario
import io.github.dreamofloser.testgen.llm.llmKotlinValue
import io.github.dreamofloser.testgen.llm.supportedLlmInputStrategies

class KotlinUnitTestGenerator {
    fun generate(model: ClassModel, guidance: LlmGenerationGuidance = LlmGenerationGuidance()): GeneratedTestSource {
        return when (model.classKind) {
            SourceClassKind.DATA -> generateDataClassTest(model)
            SourceClassKind.VIEW_MODEL -> generateViewModelTest(model)
            SourceClassKind.COMPOSE_UI -> generateComposeUiTest(model)
            SourceClassKind.ROOM_DAO -> generateDataSourceContractTest(model, DataSourceKind.ROOM_DAO)
            SourceClassKind.RETROFIT_API -> generateDataSourceContractTest(model, DataSourceKind.RETROFIT_API)
            SourceClassKind.REGULAR,
            SourceClassKind.ACTIVITY,
            SourceClassKind.FRAGMENT -> generateRegularClassTest(model, guidance)
        }
    }

    private fun generateDataClassTest(model: ClassModel): GeneratedTestSource {
        val constructor = model.bestConstructor()
        val builder = StringBuilder()

        appendPackageAndImports(
            builder = builder,
            model = model,
            imports = listOf(
                "org.junit.Assert.assertEquals",
                "org.junit.Assert.assertNotNull",
                "org.junit.Assert.assertNull",
                "org.junit.Test",
            ),
        )

        builder.appendLine("class ${model.generatedTestClassName()} {")
        builder.appendLine()
        builder.appendLine("    @Test")
        builder.appendLine("    fun creates${model.className}WithSampleValues() {")
        builder.appendLine("        val target = ${model.className}(")
        constructor.parameters.forEachIndexed { index, parameter ->
            val suffix = if (index == constructor.parameters.lastIndex) "" else ","
            builder.appendLine("            ${parameter.name} = ${parameter.kotlinValue()}$suffix")
        }
        builder.appendLine("        )")
        builder.appendLine()
        builder.appendLine("        assertNotNull(target)")
        constructor.parameters.forEach { parameter ->
            builder.appendLine("        ${parameter.kotlinAssertion()}")
        }
        builder.appendLine("    }")
        builder.appendLine()
        builder.appendLine("}")

        return GeneratedTestSource(
            source = builder.toString(),
            testMethodCount = 1,
            assertionCount = constructor.parameters.size + 1,
            fallbackMethodCount = 0,
            ruleMatchedMethodCount = 1,
            mockedDependencyCount = 0,
            mockStubCount = 0,
            mockVerificationCount = 0,
        )
    }

    private fun generateDataSourceContractTest(
        model: ClassModel,
        dataSourceKind: DataSourceKind,
    ): GeneratedTestSource {
        val methods = model.methods
            .filter { method -> method.name != model.className }
            .ifEmpty { emptyList() }
        val needsCoroutines = methods.any { it.isSuspend }
        val endpointMethods = methods.filter { dataSourceKind == DataSourceKind.RETROFIT_API && it.httpMethod != null && it.httpPath != null }
        val mockWebServerMethods = methods.filter { dataSourceKind == DataSourceKind.RETROFIT_API && it.canGenerateRetrofitMockWebServerTest() }
        val mockWebServerErrorMethods = mockWebServerMethods.filter { it.returnType.normalizedType().startsWith("Response<") }
        val builder = StringBuilder()
        val imports = buildList {
            add("io.mockk.mockk")
            add("org.junit.Assert.assertEquals")
            if (mockWebServerErrorMethods.isNotEmpty()) {
                add("org.junit.Assert.assertFalse")
            }
            add("org.junit.Assert.assertNotNull")
            add("org.junit.Assert.assertTrue")
            add("org.junit.Test")
            if (methods.any { it.isSuspend }) {
                add("io.mockk.coEvery")
                add("io.mockk.coVerify")
                add("kotlinx.coroutines.test.runTest")
            }
            if (methods.any { !it.isSuspend }) {
                add("io.mockk.every")
                add("io.mockk.verify")
            }
            if (methods.any { it.returnType.normalizedType().startsWith("Response<") }) {
                add("retrofit2.Response")
            }
            endpointMethods.mapNotNull { it.retrofitHttpAnnotationImport() }.forEach { add(it) }
            if (endpointMethods.any { it.httpQueryNames.isNotEmpty() }) {
                add("retrofit2.http.Query")
            }
            if (mockWebServerMethods.isNotEmpty()) {
                add("okhttp3.ResponseBody")
                add("okhttp3.mockwebserver.MockResponse")
                add("okhttp3.mockwebserver.MockWebServer")
                add("retrofit2.Converter")
                add("retrofit2.Retrofit")
                add("java.lang.reflect.Type")
            }
        }

        appendPackageAndImports(builder, model, imports)

        builder.appendLine("class ${model.generatedTestClassName()} {")
        builder.appendLine("    private val target = mockk<${model.className}>()")
        builder.appendLine()

        methods.forEach { method ->
            appendDataSourceContractMethod(builder, method)
            builder.appendLine()
        }

        endpointMethods.forEach { method ->
            appendRetrofitEndpointMetadataTest(builder, model, method)
            builder.appendLine()
        }

        mockWebServerMethods.forEach { method ->
            appendRetrofitMockWebServerTest(builder, model, method)
            builder.appendLine()
        }

        mockWebServerErrorMethods.forEach { method ->
            appendRetrofitMockWebServerErrorTest(builder, model, method)
            builder.appendLine()
        }

        if (mockWebServerMethods.isNotEmpty()) {
            appendRetrofitSampleConverterFactory(builder)
        }

        builder.appendLine("}")

        return GeneratedTestSource(
            source = builder.toString(),
            testMethodCount = methods.size + endpointMethods.size + mockWebServerMethods.size + mockWebServerErrorMethods.size,
            assertionCount = methods.size +
                endpointMethods.sumOf { it.retrofitEndpointAssertionCount() } +
                mockWebServerMethods.sumOf { it.retrofitMockWebServerAssertionCount() } +
                mockWebServerErrorMethods.sumOf { it.retrofitMockWebServerErrorAssertionCount() },
            fallbackMethodCount = 0,
            ruleMatchedMethodCount = methods.size + endpointMethods.size + mockWebServerMethods.size + mockWebServerErrorMethods.size,
            mockedDependencyCount = 1,
            mockStubCount = methods.size,
            mockVerificationCount = methods.size,
            roomDaoTestCount = if (dataSourceKind == DataSourceKind.ROOM_DAO) 1 else 0,
            retrofitApiTestCount = if (dataSourceKind == DataSourceKind.RETROFIT_API) 1 else 0,
        )
    }

    private fun generateViewModelTest(model: ClassModel): GeneratedTestSource {
        val constructor = model.bestConstructor()
        val dependencyParameters = constructor.parameters.filter { it.isDependencyParameter() }
        val repository = dependencyParameters.firstOrNull()
        val uiState = model.properties.firstOrNull { it.name == "uiState" }
        val selectedCity = model.properties.firstOrNull { it.name == "selectedCity" }
        val isRefreshing = model.properties.firstOrNull { it.name == "isRefreshing" }
        val generatedMethods = buildList {
            uiState?.let { add(ViewModelTestMethod("initialState_isNotNull", ViewModelScenarioKind.INITIAL_STATE)) }
            if (repository != null && uiState != null && model.methods.any { it.name == "fetchWeather" }) {
                add(ViewModelTestMethod("fetchWeather_failure_updatesUiState", ViewModelScenarioKind.FETCH_FAILURE))
            }
            if (repository != null && selectedCity != null && model.methods.any { it.name == "selectCity" }) {
                add(ViewModelTestMethod("selectCity_updatesSelectedCity", ViewModelScenarioKind.SELECT_CITY))
            }
            if (repository != null && isRefreshing != null && model.methods.any { it.name == "refreshWeather" }) {
                add(ViewModelTestMethod("refreshWeather_failure_stopsRefreshing", ViewModelScenarioKind.REFRESH_FAILURE))
            }
        }

        val builder = StringBuilder()
        appendPackageAndImports(
            builder = builder,
            model = model,
            imports = listOf(
                "io.mockk.coEvery",
                "io.mockk.mockk",
                "kotlinx.coroutines.Dispatchers",
                "kotlinx.coroutines.ExperimentalCoroutinesApi",
                "kotlinx.coroutines.test.StandardTestDispatcher",
                "kotlinx.coroutines.test.resetMain",
                "kotlinx.coroutines.test.runTest",
                "kotlinx.coroutines.test.setMain",
                "org.junit.After",
                "org.junit.Assert.assertEquals",
                "org.junit.Assert.assertFalse",
                "org.junit.Assert.assertNotNull",
                "org.junit.Assert.assertTrue",
                "org.junit.Before",
                "org.junit.Test",
            ),
        )

        builder.appendLine("@OptIn(ExperimentalCoroutinesApi::class)")
        builder.appendLine("class ${model.generatedTestClassName()} {")
        builder.appendLine("    private val testDispatcher = StandardTestDispatcher()")
        dependencyParameters.forEach { parameter ->
            builder.appendLine("    private val ${parameter.name} = mockk<${parameter.type.classLiteralType()}>()")
        }
        builder.appendLine("    private lateinit var target: ${model.className}")
        builder.appendLine()
        builder.appendLine("    @Before")
        builder.appendLine("    fun setUp() {")
        builder.appendLine("        Dispatchers.setMain(testDispatcher)")
        val constructorArguments = constructor.parameters.joinToString(", ") { parameter ->
            if (parameter in dependencyParameters) parameter.name else parameter.kotlinValue()
        }
        builder.appendLine("        target = ${model.className}($constructorArguments)")
        builder.appendLine("    }")
        builder.appendLine()
        builder.appendLine("    @After")
        builder.appendLine("    fun tearDown() {")
        builder.appendLine("        Dispatchers.resetMain()")
        builder.appendLine("    }")
        builder.appendLine()

        generatedMethods.forEach { testMethod ->
            appendViewModelTestMethod(builder, testMethod, repository, uiState, selectedCity, isRefreshing)
            builder.appendLine()
        }

        builder.appendLine("}")

        return GeneratedTestSource(
            source = builder.toString(),
            testMethodCount = generatedMethods.size,
            assertionCount = generatedMethods.size,
            fallbackMethodCount = generatedMethods.count { it.kind == ViewModelScenarioKind.INITIAL_STATE },
            ruleMatchedMethodCount = generatedMethods.count { it.kind != ViewModelScenarioKind.INITIAL_STATE },
            mockedDependencyCount = dependencyParameters.size,
            mockStubCount = generatedMethods.count { it.kind.needsRepositoryStub() },
            mockVerificationCount = 0,
        )
    }

    private fun generateComposeUiTest(model: ClassModel): GeneratedTestSource {
        val composableMethod = model.methods.firstOrNull()
        val viewModelParameter = composableMethod?.parameters?.firstOrNull { it.type.endsWith("ViewModel") }
        val builder = StringBuilder()

        appendPackageAndImports(
            builder = builder,
            model = model,
            imports = listOfNotNull(
                model.findImportForSimpleName("City"),
                model.findImportForSimpleName("WeatherUiState"),
                "androidx.compose.ui.test.junit4.createComposeRule",
                "androidx.compose.ui.test.onNodeWithTag",
                "io.mockk.every",
                "io.mockk.mockk",
                "kotlinx.coroutines.flow.MutableStateFlow",
                "org.junit.Rule",
                "org.junit.Test",
                "org.junit.runner.RunWith",
                "org.robolectric.RobolectricTestRunner",
                "org.robolectric.annotation.Config",
            ),
        )

        builder.appendLine("@RunWith(RobolectricTestRunner::class)")
        builder.appendLine("@Config(sdk = [34], instrumentedPackages = [\"androidx.loader.content\"])")
        builder.appendLine("class ${model.generatedTestClassName()} {")
        builder.appendLine()
        builder.appendLine("    @get:Rule")
        builder.appendLine("    val composeTestRule = createComposeRule()")
        builder.appendLine()
        builder.appendLine("    @Test")
        builder.appendLine("    fun ${composableMethod?.name}_rendersLoadingState() {")

        if (viewModelParameter != null) {
            builder.appendLine("        val ${viewModelParameter.name} = mockk<${viewModelParameter.type.classLiteralType()}>(relaxed = true)")
            builder.appendLine("        every { ${viewModelParameter.name}.uiState } returns MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)")
            builder.appendLine("        every { ${viewModelParameter.name}.selectedCity } returns MutableStateFlow(City(\"sample\", 1.0, 1.0))")
            builder.appendLine("        every { ${viewModelParameter.name}.isRefreshing } returns MutableStateFlow(false)")
            builder.appendLine("        every { ${viewModelParameter.name}.fetchWeather(any()) } returns Unit")
            builder.appendLine("        every { ${viewModelParameter.name}.refreshWeather() } returns Unit")
            builder.appendLine("        every { ${viewModelParameter.name}.selectCity(any()) } returns Unit")
            builder.appendLine()
        }

        builder.appendLine("        composeTestRule.setContent {")
        val arguments = composableMethod?.parameters.orEmpty().joinToString(", ") { parameter ->
            "${parameter.name} = ${parameter.name}"
        }
        builder.appendLine("            ${composableMethod?.name}($arguments)")
        builder.appendLine("        }")
        builder.appendLine()
        builder.appendLine("        composeTestRule.onNodeWithTag(\"LoadingSpinner\").assertExists()")
        builder.appendLine("    }")
        builder.appendLine()
        builder.appendLine("}")

        return GeneratedTestSource(
            source = builder.toString(),
            testMethodCount = 1,
            assertionCount = 1,
            fallbackMethodCount = 0,
            ruleMatchedMethodCount = 1,
            mockedDependencyCount = if (viewModelParameter != null) 1 else 0,
            mockStubCount = if (viewModelParameter != null) 6 else 0,
            mockVerificationCount = 0,
            robolectricTestCount = 1,
            androidImportCount = 1,
            composeTestCount = 1,
        )
    }

    private fun generateRegularClassTest(
        model: ClassModel,
        guidance: LlmGenerationGuidance,
    ): GeneratedTestSource {
        val constructor = model.bestConstructor()
        val dependencyParameters = constructor.parameters.filter { it.isDependencyParameter() }
        val dependencyNames = dependencyParameters.map { it.name }.toSet()
        val ruleGeneratedMethods = model.methods
            .filter { method -> method.name != model.className }
            .flatMap { method ->
                if (method.returnType.startsWith("Result<")) {
                    listOf(
                        KotlinTestMethod(method, "success_returnsSuccess", KotlinScenarioKind.RESULT_SUCCESS),
                        KotlinTestMethod(method, "failure_returnsFailure", KotlinScenarioKind.RESULT_FAILURE),
                    )
                } else {
                    listOf(KotlinTestMethod(method, "withDefaultInputs_runs", KotlinScenarioKind.DEFAULT))
                }
            }
        val llmGeneratedMethods = guidance.acceptedScenarios.mapNotNull { scenario ->
            model.methods.singleOrNull { it.name == scenario.methodName }?.llmBoundaryTestMethod(scenario)
        }
        val generatedMethods = (llmGeneratedMethods + ruleGeneratedMethods)
            .distinctBy { testMethod ->
                Triple(
                    testMethod.method.name,
                    testMethod.kind,
                    testMethod.arguments ?: testMethod.method.parameters.map { it.kotlinValue() },
                )
            }

        val needsCoroutines = generatedMethods.any { it.method.isSuspend || it.kind.usesSuspendMock() }
        val needsMockK = dependencyParameters.isNotEmpty()
        val builder = StringBuilder()
        val imports = buildList {
            add("org.junit.Assert.assertEquals")
            add("org.junit.Assert.assertNotNull")
            add("org.junit.Assert.assertTrue")
            add("org.junit.Test")
            if (needsMockK) {
                add("io.mockk.mockk")
                if (generatedMethods.any { it.kind.usesSuspendMock() }) {
                    add("io.mockk.coEvery")
                    add("io.mockk.coVerify")
                }
                if (generatedMethods.any { !it.method.isSuspend && it.kind.usesMock() }) {
                    add("io.mockk.every")
                    add("io.mockk.verify")
                }
            }
            if (needsCoroutines) {
                add("kotlinx.coroutines.test.runTest")
            }
        }

        appendPackageAndImports(builder, model, imports)

        builder.appendLine("class ${model.generatedTestClassName()} {")
        builder.appendLine()
        dependencyParameters.forEach { parameter ->
            builder.appendLine("    private val ${parameter.name} = mockk<${parameter.type.classLiteralType()}>()")
        }
        if (constructor.parameters.isNotEmpty()) {
            val constructorArguments = constructor.parameters.joinToString(", ") { parameter ->
                if (parameter in dependencyParameters) parameter.name else parameter.kotlinValue()
            }
            builder.appendLine("    private val target = ${model.className}($constructorArguments)")
            builder.appendLine()
        }

        generatedMethods.forEach { testMethod ->
            appendRegularTestMethod(builder, testMethod, dependencyNames)
            builder.appendLine()
        }

        builder.appendLine("}")

        val mockInteractions = generatedMethods.count { it.kind.usesMock() }
        return GeneratedTestSource(
            source = builder.toString(),
            testMethodCount = generatedMethods.size,
            assertionCount = generatedMethods.sumOf { it.assertionCount() },
            fallbackMethodCount = generatedMethods.count { it.kind == KotlinScenarioKind.DEFAULT },
            ruleMatchedMethodCount = generatedMethods.count { it.kind != KotlinScenarioKind.DEFAULT },
            llmAdoptedMethodCount = generatedMethods.count { it.kind == KotlinScenarioKind.LLM_BOUNDARY },
            mockedDependencyCount = dependencyParameters.size,
            mockStubCount = mockInteractions,
            mockVerificationCount = mockInteractions,
        )
    }

    private fun appendPackageAndImports(
        builder: StringBuilder,
        model: ClassModel,
        imports: List<String>,
    ) {
        if (model.packageName.isNotBlank()) {
            builder.appendLine("package ${model.packageName}")
            builder.appendLine()
        }

        (model.neededSourceImports() + imports)
            .filterNot { it.startsWith("kotlin.") }
            .distinct()
            .sorted()
            .forEach { builder.appendLine("import $it") }
        builder.appendLine()
    }

    private fun appendRegularTestMethod(
        builder: StringBuilder,
        testMethod: KotlinTestMethod,
        dependencyNames: Set<String>,
    ) {
        val method = testMethod.method
        val wrapperStart = if (method.isSuspend || testMethod.kind.usesSuspendMock()) " = runTest {" else " {"
        builder.appendLine("    @Test")
        builder.appendLine("    fun ${method.name}_${testMethod.nameSuffix}()$wrapperStart")

        val dependencyCall = method.dependencyCalls.firstOrNull { it.receiverName in dependencyNames }
        if (dependencyCall != null) {
            val arguments = dependencyCall.arguments.joinToString(", ") { argument ->
                method.parameters.firstOrNull { it.name == argument }?.let { "any()" } ?: argument
            }
            val stubPrefix = if (method.isSuspend || testMethod.kind.usesSuspendMock()) "coEvery" else "every"
            val stubTail = when (testMethod.kind) {
                KotlinScenarioKind.RESULT_SUCCESS -> "returns ${method.resultInnerType().kotlinStandaloneValue()}"
                KotlinScenarioKind.RESULT_FAILURE -> "throws RuntimeException(\"sample failure\")"
                KotlinScenarioKind.DEFAULT -> "returns ${method.returnType.kotlinStandaloneValue()}"
                KotlinScenarioKind.LLM_BOUNDARY -> "returns ${method.resultInnerType().kotlinStandaloneValue()}"
            }
            builder.appendLine("        $stubPrefix { ${dependencyCall.receiverName}.${dependencyCall.methodName}($arguments) } $stubTail")
            builder.appendLine()
        }

        val methodArguments = (testMethod.arguments ?: method.parameters.map { it.kotlinValue() }).joinToString(", ")
        if (method.returnType == "Unit") {
            builder.appendLine("        target.${method.name}($methodArguments)")
            builder.appendLine()
            builder.appendLine("        assertTrue(true)")
        } else {
            builder.appendLine("        val result = target.${method.name}($methodArguments)")
            builder.appendLine()
            when (testMethod.kind) {
                KotlinScenarioKind.RESULT_SUCCESS -> {
                    builder.appendLine("        assertTrue(result.isSuccess)")
                    builder.appendLine("        assertNotNull(result.getOrNull())")
                }
                KotlinScenarioKind.RESULT_FAILURE -> {
                    builder.appendLine("        assertTrue(result.isFailure)")
                    builder.appendLine("        assertEquals(\"sample failure\", result.exceptionOrNull()?.message)")
                }
                KotlinScenarioKind.DEFAULT,
                KotlinScenarioKind.LLM_BOUNDARY -> builder.appendLine("        assertNotNull(result)")
            }
        }

        if (dependencyCall != null && testMethod.kind.usesMock()) {
            val verifyPrefix = if (method.isSuspend || testMethod.kind.usesSuspendMock()) "coVerify" else "verify"
            val arguments = dependencyCall.arguments.joinToString(", ") { argument ->
                method.parameters.firstOrNull { it.name == argument }?.let { "any()" } ?: argument
            }
            builder.appendLine("        $verifyPrefix { ${dependencyCall.receiverName}.${dependencyCall.methodName}($arguments) }")
        }

        builder.appendLine("    }")
    }

    private fun appendDataSourceContractMethod(
        builder: StringBuilder,
        method: MethodModel,
    ) {
        val wrapperStart = if (method.isSuspend) " = runTest {" else " {"
        val stubPrefix = if (method.isSuspend) "coEvery" else "every"
        val verifyPrefix = if (method.isSuspend) "coVerify" else "verify"
        val invocationArguments = method.parameters.joinToString(", ") { it.kotlinValue() }
        val mockArguments = method.parameters.joinToString(", ") { "any()" }

        builder.appendLine("    @Test")
        builder.appendLine("    fun ${method.name}_returnsStubbedValue()$wrapperStart")
        builder.appendLine("        $stubPrefix { target.${method.name}($mockArguments) } returns ${method.returnType.kotlinStandaloneValue()}")
        builder.appendLine()

        if (method.returnType == "Unit") {
            builder.appendLine("        target.${method.name}($invocationArguments)")
            builder.appendLine()
            builder.appendLine("        assertTrue(true)")
        } else {
            builder.appendLine("        val result = target.${method.name}($invocationArguments)")
            builder.appendLine()
            builder.appendLine("        assertNotNull(result)")
        }

        builder.appendLine("        $verifyPrefix { target.${method.name}($mockArguments) }")
        builder.appendLine("    }")
    }

    private fun appendRetrofitEndpointMetadataTest(
        builder: StringBuilder,
        model: ClassModel,
        method: MethodModel,
    ) {
        val annotationName = method.httpMethod.orEmpty()
        val pathAccessor = if (annotationName == "HTTP") "path" else "value"

        builder.appendLine("    @Test")
        builder.appendLine("    fun ${method.name}_hasRetrofitEndpointMetadata() {")
        builder.appendLine("        val method = ${model.className}::class.java.declaredMethods.single { it.name == \"${method.name}\" }")
        builder.appendLine("        val annotation = method.getAnnotation(${annotationName}::class.java)")
        builder.appendLine()
        builder.appendLine("        assertNotNull(annotation)")
        builder.appendLine("        assertEquals(\"${method.httpPath.orEmpty()}\", annotation!!.$pathAccessor)")
        if (method.httpQueryNames.isNotEmpty()) {
            val expectedQueries = method.httpQueryNames.joinToString(", ") { "\"$it\"" }
            builder.appendLine("        val queryNames = method.parameterAnnotations")
            builder.appendLine("            .flatMap { annotations -> annotations.filterIsInstance<Query>().map { it.value } }")
            builder.appendLine("        assertEquals(listOf($expectedQueries), queryNames)")
        }
        builder.appendLine("    }")
    }
    private fun appendRetrofitMockWebServerTest(
        builder: StringBuilder,
        model: ClassModel,
        method: MethodModel,
    ) {
        val invocationArguments = method.parameters.joinToString(", ") { it.kotlinValue() }
        val expectedPath = method.retrofitExpectedMockWebServerPath()
        val responseBodyValue = method.retrofitResponseBodyValue()

        builder.appendLine("    @Test")
        builder.appendLine("    fun ${method.name}_requestsExpectedUrlWithMockWebServer() = runTest {")
        builder.appendLine("        val server = MockWebServer()")
        builder.appendLine("        try {")
        builder.appendLine("            server.enqueue(MockResponse().setResponseCode(200).setBody(\"{}\"))")
        builder.appendLine("            server.start()")
        builder.appendLine("            val service = Retrofit.Builder()")
        builder.appendLine("                .baseUrl(server.url(\"/\"))")
        builder.appendLine("                .addConverterFactory(sampleConverterFactory($responseBodyValue))")
        builder.appendLine("                .build()")
        builder.appendLine("                .create(${model.className}::class.java)")
        builder.appendLine()
        builder.appendLine("            val result = service.${method.name}($invocationArguments)")
        builder.appendLine()
        builder.appendLine("            assertNotNull(result)")
        if (method.returnType.normalizedType().startsWith("Response<")) {
            builder.appendLine("            assertTrue(result.isSuccessful)")
        }
        builder.appendLine("            val request = server.takeRequest()")
        builder.appendLine("            assertEquals(\"${method.httpMethod}\", request.method)")
        builder.appendLine("            assertEquals(\"$expectedPath\", request.path)")
        builder.appendLine("        } finally {")
        builder.appendLine("            server.shutdown()")
        builder.appendLine("        }")
        builder.appendLine("    }")
    }

    private fun appendRetrofitMockWebServerErrorTest(
        builder: StringBuilder,
        model: ClassModel,
        method: MethodModel,
    ) {
        val invocationArguments = method.parameters.joinToString(", ") { it.kotlinValue() }
        val expectedPath = method.retrofitExpectedMockWebServerPath()
        val responseBodyValue = method.retrofitResponseBodyValue()

        builder.appendLine("    @Test")
        builder.appendLine("    fun ${method.name}_returnsHttpErrorWithMockWebServer() = runTest {")
        builder.appendLine("        val server = MockWebServer()")
        builder.appendLine("        try {")
        builder.appendLine("            server.enqueue(MockResponse().setResponseCode(500).setBody(\"{\\\"message\\\":\\\"sample failure\\\"}\"))")
        builder.appendLine("            server.start()")
        builder.appendLine("            val service = Retrofit.Builder()")
        builder.appendLine("                .baseUrl(server.url(\"/\"))")
        builder.appendLine("                .addConverterFactory(sampleConverterFactory($responseBodyValue))")
        builder.appendLine("                .build()")
        builder.appendLine("                .create(${model.className}::class.java)")
        builder.appendLine()
        builder.appendLine("            val result = service.${method.name}($invocationArguments)")
        builder.appendLine()
        builder.appendLine("            assertFalse(result.isSuccessful)")
        builder.appendLine("            assertEquals(500, result.code())")
        builder.appendLine("            val request = server.takeRequest()")
        builder.appendLine("            assertEquals(\"${method.httpMethod}\", request.method)")
        builder.appendLine("            assertEquals(\"$expectedPath\", request.path)")
        builder.appendLine("        } finally {")
        builder.appendLine("            server.shutdown()")
        builder.appendLine("        }")
        builder.appendLine("    }")
    }
    private fun appendRetrofitSampleConverterFactory(builder: StringBuilder) {
        builder.appendLine("    private fun sampleConverterFactory(value: Any): Converter.Factory {")
        builder.appendLine("        return object : Converter.Factory() {")
        builder.appendLine("            override fun responseBodyConverter(")
        builder.appendLine("                type: Type,")
        builder.appendLine("                annotations: Array<Annotation>,")
        builder.appendLine("                retrofit: Retrofit,")
        builder.appendLine("            ): Converter<ResponseBody, *> {")
        builder.appendLine("                return Converter<ResponseBody, Any> { value }")
        builder.appendLine("            }")
        builder.appendLine("        }")
        builder.appendLine("    }")
        builder.appendLine()
    }
    private fun appendViewModelTestMethod(
        builder: StringBuilder,
        testMethod: ViewModelTestMethod,
        repository: ParameterModel?,
        uiState: PropertyModel?,
        selectedCity: PropertyModel?,
        isRefreshing: PropertyModel?,
    ) {
        builder.appendLine("    @Test")
        builder.appendLine("    fun ${testMethod.name}() = runTest {")

        when (testMethod.kind) {
            ViewModelScenarioKind.INITIAL_STATE -> {
                builder.appendLine("        assertNotNull(target.${uiState?.name}.value)")
            }

            ViewModelScenarioKind.FETCH_FAILURE -> {
                appendRepositoryFailureStub(builder, repository)
                builder.appendLine()
                builder.appendLine("        target.fetchWeather()")
                builder.appendLine("        testDispatcher.scheduler.advanceUntilIdle()")
                builder.appendLine()
                val stateType = uiState?.stateInnerType().orEmpty()
                if (stateType.isNotBlank()) {
                    builder.appendLine("        assertTrue(target.${uiState?.name}.value is $stateType.Error)")
                } else {
                    builder.appendLine("        assertNotNull(target.${uiState?.name}.value)")
                }
            }

            ViewModelScenarioKind.SELECT_CITY -> {
                appendRepositoryFailureStub(builder, repository)
                val cityType = selectedCity?.stateInnerType().orEmpty().ifBlank { "City" }
                builder.appendLine("        val city = ${cityType}(\"sample\", 1.0, 1.0)")
                builder.appendLine()
                builder.appendLine("        target.selectCity(city)")
                builder.appendLine("        testDispatcher.scheduler.advanceUntilIdle()")
                builder.appendLine()
                builder.appendLine("        assertEquals(city, target.${selectedCity?.name}.value)")
            }

            ViewModelScenarioKind.REFRESH_FAILURE -> {
                appendRepositoryFailureStub(builder, repository)
                builder.appendLine()
                builder.appendLine("        target.refreshWeather()")
                builder.appendLine("        testDispatcher.scheduler.advanceUntilIdle()")
                builder.appendLine()
                builder.appendLine("        assertFalse(target.${isRefreshing?.name}.value)")
            }
        }

        builder.appendLine("    }")
    }

    private fun appendRepositoryFailureStub(builder: StringBuilder, repository: ParameterModel?) {
        if (repository == null) {
            return
        }

        builder.appendLine("        coEvery { ${repository.name}.getWeather(any(), any()) } returns Result.failure(Exception(\"sample failure\"))")
    }

    private fun ClassModel.bestConstructor(): ConstructorModel {
        return constructors
            .filter { it.parameters.isEmpty() }
            .ifEmpty { constructors }
            .minByOrNull { it.parameters.size }
            ?: ConstructorModel(emptyList())
    }

    private fun ClassModel.generatedTestClassName(): String = "${className}GeneratedTest"

    private fun ClassModel.findImportForSimpleName(simpleName: String): String? {
        return imports.firstOrNull { it.substringAfterLast('.') == simpleName }
    }

    private fun ClassModel.neededSourceImports(): List<String> {
        val sourceImports = imports
            .filterNot { it.startsWith("kotlin.") }

        val directImportsBySimpleName = imports
            .filterNot { it.endsWith(".*") }
            .associateBy { it.substringAfterLast('.') }
        val wildcardImports = imports.filter { it.endsWith(".*") }
        val usedTypes = buildList {
            constructors.forEach { constructor ->
                addAll(constructor.parameters.map { it.type })
            }
            methods.forEach { method ->
                add(method.returnType)
                addAll(method.parameters.map { it.type })
            }
        }

        val directImports = usedTypes
            .flatMap { it.referencedTypeNames() }
            .mapNotNull { typeName -> directImportsBySimpleName[typeName] }

        return (sourceImports + wildcardImports + directImports).distinct()
    }

    private fun String.referencedTypeNames(): List<String> {
        return typeNameRegex.findAll(this)
            .map { it.value.substringAfterLast('.') }
            .filterNot { it in primitiveAndStringTypes || it == "Result" || it == "Unit" }
            .toList()
    }

    private fun MethodModel.retrofitHttpAnnotationImport(): String? {
        return httpMethod?.let { "retrofit2.http.$it" }
    }

    private fun MethodModel.retrofitEndpointAssertionCount(): Int {
        return 2 + if (httpQueryNames.isNotEmpty()) 1 else 0
    }

    private fun MethodModel.canGenerateRetrofitMockWebServerTest(): Boolean {
        return isSuspend &&
            httpMethod == "GET" &&
            !httpPath.orEmpty().contains("{") &&
            returnType != "Unit" &&
            httpQueryNames.size == parameters.size &&
            parameters.all { it.type.normalizedType() in primitiveAndStringTypes }
    }

    private fun MethodModel.retrofitMockWebServerAssertionCount(): Int {
        return 3 + if (returnType.normalizedType().startsWith("Response<")) 1 else 0
    }

    private fun MethodModel.retrofitMockWebServerErrorAssertionCount(): Int {
        return 4
    }
    private fun MethodModel.retrofitExpectedMockWebServerPath(): String {
        val path = httpPath.orEmpty().let { if (it.startsWith("/")) it else "/$it" }
        if (httpQueryNames.isEmpty()) {
            return path
        }

        val query = httpQueryNames
            .zip(parameters)
            .joinToString("&") { (queryName, parameter) -> "$queryName=${parameter.kotlinValue().trim('\"')}" }
        return "$path?$query"
    }

    private fun MethodModel.retrofitResponseBodyValue(): String {
        val type = returnType.normalizedType()
        val bodyType = if (type.startsWith("Response<")) type.genericInnerType() else type
        return bodyType.kotlinStandaloneValue()
    }

    private fun MethodModel.resultInnerType(): String {
        return returnType
            .removePrefix("Result<")
            .removeSuffix(">")
            .trim()
    }

    private fun ParameterModel.kotlinAssertion(): String {
        return when {
            type.normalizedType().startsWith("List<") || type.normalizedType().startsWith("MutableList<") -> {
                "assertEquals(${kotlinValue()}, target.$name)"
            }
            type.isNullableObjectType() -> "assertNull(target.$name)"
            type.endsWith("?") -> "assertEquals(${kotlinValue()}, target.$name)"
            type.normalizedType() in setOf("Double", "Float") -> "assertEquals(${kotlinValue()}, target.$name, 0.0)"
            else -> "assertEquals(${kotlinValue()}, target.$name)"
        }
    }

    private fun ParameterModel.kotlinValue(): String = type.kotlinStandaloneValue()

    private fun String.kotlinStandaloneValue(): String {
        val type = normalizedType()
        if (endsWith("?") && type !in primitiveAndStringTypes && !type.startsWith("List<")) {
            return "null"
        }

        return when {
            type == "String" -> "\"sample\""
            type == "Int" -> "1"
            type == "Long" -> "1L"
            type == "Float" -> "1.0f"
            type == "Double" -> "1.0"
            type == "Boolean" -> "true"
            type.startsWith("List<") -> "emptyList<${type.genericInnerType()}>()"
            type.startsWith("MutableList<") -> "mutableListOf<${type.genericInnerType()}>()"
            type.startsWith("Result<") -> "Result.success(${removePrefix("Result<").removeSuffix(">").kotlinStandaloneValue()})"
            type.startsWith("Response<") -> "Response.success(${type.genericInnerType().kotlinStandaloneValue()})"
            type == "Unit" -> "Unit"
            else -> "$type()"
        }
    }

    private fun ParameterModel.isDependencyParameter(): Boolean {
        val normalizedType = type.normalizedType()
        return normalizedType !in primitiveAndStringTypes &&
            !normalizedType.startsWith("List<") &&
            !normalizedType.startsWith("MutableList<") &&
            !type.endsWith("?")
    }

    private fun String.isNullableObjectType(): Boolean {
        return endsWith("?") && normalizedType() !in primitiveAndStringTypes
    }

    private fun String.normalizedType(): String {
        return removeSuffix("?")
            .substringBefore('=')
            .trim()
    }

    private fun String.classLiteralType(): String {
        return normalizedType().substringBefore('<')
    }

    private fun String.genericInnerType(): String {
        return substringAfter('<')
            .substringBeforeLast('>')
            .trim()
    }

    private fun PropertyModel.stateInnerType(): String {
        return type.substringAfter('<', "")
            .substringBeforeLast('>')
            .trim()
    }

    private fun MethodModel.llmBoundaryTestMethod(scenario: LlmTestScenario): KotlinTestMethod? {
        val requestedStrategy = LlmInputStrategy.fromWireName(scenario.inputStrategy)
        val boundary = parameters
            .mapIndexedNotNull { index, parameter ->
                val strategies = parameter.supportedLlmInputStrategies()
                val strategy = requestedStrategy?.takeIf { it in strategies }
                    ?: if (requestedStrategy == null) strategies.firstOrNull() else null
                if (!scenario.targetParameter.isNullOrBlank() &&
                    parameter.name != scenario.targetParameter
                ) {
                    null
                } else {
                    strategy?.let { index to it }
                }
            }
            .firstOrNull()
            ?: return null
        val boundaryValue = parameters[boundary.first].llmKotlinValue(boundary.second)
            ?: return null
        val arguments = parameters.mapIndexed { index, parameter ->
            if (index == boundary.first) boundaryValue else parameter.kotlinValue()
        }
        return KotlinTestMethod(
            method = this,
            nameSuffix = "llm_${scenario.testName.safeTestName()}",
            kind = KotlinScenarioKind.LLM_BOUNDARY,
            arguments = arguments,
        )
    }
    private fun String.safeTestName(): String {
        val cleaned = replace(Regex("[^A-Za-z0-9_]"), "_").trim('_').take(60)
        return cleaned.ifBlank { "suggestedBoundary" }
    }
    private fun KotlinTestMethod.assertionCount(): Int {
        return when (kind) {
            KotlinScenarioKind.RESULT_SUCCESS,
            KotlinScenarioKind.RESULT_FAILURE -> 2
            KotlinScenarioKind.DEFAULT,
            KotlinScenarioKind.LLM_BOUNDARY -> 1
        }
    }

    private fun KotlinScenarioKind.usesMock(): Boolean {
        return this == KotlinScenarioKind.RESULT_SUCCESS ||
            this == KotlinScenarioKind.RESULT_FAILURE ||
            this == KotlinScenarioKind.LLM_BOUNDARY
    }

    private fun KotlinScenarioKind.usesSuspendMock(): Boolean {
        return usesMock()
    }

    private data class KotlinTestMethod(
        val method: MethodModel,
        val nameSuffix: String,
        val kind: KotlinScenarioKind,
        val arguments: List<String>? = null,
    )

    private enum class KotlinScenarioKind {
        DEFAULT,
        RESULT_SUCCESS,
        RESULT_FAILURE,
        LLM_BOUNDARY,
    }

    private enum class DataSourceKind {
        ROOM_DAO,
        RETROFIT_API,
    }

    private data class ViewModelTestMethod(
        val name: String,
        val kind: ViewModelScenarioKind,
    )

    private enum class ViewModelScenarioKind {
        INITIAL_STATE,
        FETCH_FAILURE,
        SELECT_CITY,
        REFRESH_FAILURE,
    }

    private fun ViewModelScenarioKind.needsRepositoryStub(): Boolean {
        return this != ViewModelScenarioKind.INITIAL_STATE
    }

    private companion object {
        val typeNameRegex = Regex("""[A-Za-z_][A-Za-z0-9_.]*""")
        val primitiveAndStringTypes = setOf(
            "String",
            "Int",
            "Long",
            "Float",
            "Double",
            "Boolean",
            "Short",
            "Byte",
            "Char",
        )
    }
}
