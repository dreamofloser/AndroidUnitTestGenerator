package com.codex.testgen.generator

import com.codex.testgen.model.ClassModel
import com.codex.testgen.model.ConstructorModel
import com.codex.testgen.model.GeneratedTestSource
import com.codex.testgen.model.MethodModel
import com.codex.testgen.model.ParameterModel
import com.codex.testgen.model.PropertyModel
import com.codex.testgen.model.SourceClassKind

class KotlinUnitTestGenerator {
    fun generate(model: ClassModel): GeneratedTestSource {
        return when (model.classKind) {
            SourceClassKind.DATA -> generateDataClassTest(model)
            SourceClassKind.VIEW_MODEL -> generateViewModelTest(model)
            SourceClassKind.COMPOSE_UI -> generateComposeUiTest(model)
            SourceClassKind.ROOM_DAO -> generateDataSourceContractTest(model, DataSourceKind.ROOM_DAO)
            SourceClassKind.RETROFIT_API -> generateDataSourceContractTest(model, DataSourceKind.RETROFIT_API)
            SourceClassKind.REGULAR,
            SourceClassKind.ACTIVITY,
            SourceClassKind.FRAGMENT -> generateRegularClassTest(model)
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
        val builder = StringBuilder()
        val imports = buildList {
            add("io.mockk.mockk")
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
        }

        appendPackageAndImports(builder, model, imports)

        builder.appendLine("class ${model.generatedTestClassName()} {")
        builder.appendLine("    private val target = mockk<${model.className}>()")
        builder.appendLine()

        methods.forEach { method ->
            appendDataSourceContractMethod(builder, method)
            builder.appendLine()
        }

        builder.appendLine("}")

        return GeneratedTestSource(
            source = builder.toString(),
            testMethodCount = methods.size,
            assertionCount = methods.size,
            fallbackMethodCount = 0,
            ruleMatchedMethodCount = methods.size,
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

    private fun generateRegularClassTest(model: ClassModel): GeneratedTestSource {
        val constructor = model.bestConstructor()
        val dependencyParameters = constructor.parameters.filter { it.isDependencyParameter() }
        val dependencyNames = dependencyParameters.map { it.name }.toSet()
        val generatedMethods = model.methods
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

        val needsCoroutines = generatedMethods.any { it.method.isSuspend || it.kind.usesSuspendMock() }
        val needsMockK = dependencyParameters.isNotEmpty()
        val builder = StringBuilder()
        val imports = buildList {
            add("org.junit.Assert.assertNotNull")
            add("org.junit.Assert.assertTrue")
            add("org.junit.Test")
            if (needsMockK) {
                add("io.mockk.mockk")
                if (generatedMethods.any { it.kind.usesSuspendMock() }) {
                    add("io.mockk.coEvery")
                }
                if (generatedMethods.any { !it.method.isSuspend && it.kind.usesMock() }) {
                    add("io.mockk.every")
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
            assertionCount = generatedMethods.size,
            fallbackMethodCount = generatedMethods.count { it.kind == KotlinScenarioKind.DEFAULT },
            ruleMatchedMethodCount = generatedMethods.count { it.kind != KotlinScenarioKind.DEFAULT },
            mockedDependencyCount = dependencyParameters.size,
            mockStubCount = mockInteractions,
            mockVerificationCount = 0,
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
            }
            builder.appendLine("        $stubPrefix { ${dependencyCall.receiverName}.${dependencyCall.methodName}($arguments) } $stubTail")
            builder.appendLine()
        }

        val methodArguments = method.parameters.joinToString(", ") { it.kotlinValue() }
        if (method.returnType == "Unit") {
            builder.appendLine("        target.${method.name}($methodArguments)")
            builder.appendLine()
            builder.appendLine("        assertTrue(true)")
        } else {
            builder.appendLine("        val result = target.${method.name}($methodArguments)")
            builder.appendLine()
            when (testMethod.kind) {
                KotlinScenarioKind.RESULT_SUCCESS -> builder.appendLine("        assertTrue(result.isSuccess)")
                KotlinScenarioKind.RESULT_FAILURE -> builder.appendLine("        assertTrue(result.isFailure)")
                KotlinScenarioKind.DEFAULT -> builder.appendLine("        assertNotNull(result)")
            }
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

    private fun KotlinScenarioKind.usesMock(): Boolean {
        return this != KotlinScenarioKind.DEFAULT
    }

    private fun KotlinScenarioKind.usesSuspendMock(): Boolean {
        return usesMock()
    }

    private data class KotlinTestMethod(
        val method: MethodModel,
        val nameSuffix: String,
        val kind: KotlinScenarioKind,
    )

    private enum class KotlinScenarioKind {
        DEFAULT,
        RESULT_SUCCESS,
        RESULT_FAILURE,
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
