package io.github.dreamofloser.testgen.generator

import io.github.dreamofloser.testgen.model.ClassModel
import io.github.dreamofloser.testgen.model.ConstructorModel
import io.github.dreamofloser.testgen.model.AssertionKind
import io.github.dreamofloser.testgen.model.GeneratedTestSource
import io.github.dreamofloser.testgen.model.MethodModel
import io.github.dreamofloser.testgen.model.ParameterModel
import io.github.dreamofloser.testgen.model.SourceClassKind
import io.github.dreamofloser.testgen.model.TestScenario

class JUnit4JavaTestGenerator(
    private val scenarioGenerator: TestScenarioGenerator = TestScenarioGenerator(),
) {
    fun generate(model: ClassModel): GeneratedTestSource {
        if (model.classKind == SourceClassKind.ACTIVITY) {
            return generateActivityLifecycleTest(model)
        }
        if (model.classKind == SourceClassKind.FRAGMENT) {
            return generateFragmentLifecycleTest(model)
        }

        val builder = StringBuilder()
        val constructor = model.bestConstructor()
        val dependencyParameters = constructor.parameters.filter { it.isDependencyParameter() }
        val dependencyNames = dependencyParameters.map { it.name }.toSet()
        val methodScenarios = model.methods.associateWith { scenarioGenerator.scenariosFor(it, dependencyNames) }
        val scenarios = methodScenarios.values.flatten()
        val mockInteractions = scenarios.flatMap { it.mockInteractions }
        val needsLiveDataRule = model.usesLiveData()
        val sourceImports = model.neededSourceImports(constructor)
        val androidImportCount = sourceImports.count { it.startsWith("android.") || it.startsWith("androidx.") }
        val needsRobolectricRunner = model.usesAndroidFramework()

        if (model.packageName.isNotBlank()) {
            builder.appendLine("package ${model.packageName};")
            builder.appendLine()
        }

        if (dependencyParameters.isNotEmpty()) {
            builder.appendLine("import org.junit.Before;")
        }
        if (needsLiveDataRule) {
            builder.appendLine("import org.junit.Rule;")
        }
        builder.appendLine("import org.junit.Test;")
        if (needsRobolectricRunner) {
            builder.appendLine("import org.junit.runner.RunWith;")
            builder.appendLine("import org.robolectric.RobolectricTestRunner;")
            builder.appendLine("import org.robolectric.annotation.Config;")
        }
        if (needsLiveDataRule) {
            builder.appendLine("import androidx.arch.core.executor.testing.InstantTaskExecutorRule;")
        }
        sourceImports.forEach { importedType ->
            builder.appendLine("import $importedType;")
        }

        if (scenarios.any { it.assertion.kind == AssertionKind.EQUALS }) {
            builder.appendLine("import static org.junit.Assert.assertEquals;")
        }
        if (scenarios.any { it.assertion.kind == AssertionKind.FALSE }) {
            builder.appendLine("import static org.junit.Assert.assertFalse;")
        }
        if (scenarios.any { it.assertion.kind == AssertionKind.NOT_NULL }) {
            builder.appendLine("import static org.junit.Assert.assertNotNull;")
        }
        if (scenarios.any { it.assertion.kind == AssertionKind.TRUE }) {
            builder.appendLine("import static org.junit.Assert.assertTrue;")
        }
        if (dependencyParameters.isNotEmpty()) {
            builder.appendLine("import static org.mockito.Mockito.mock;")
        }
        if (mockInteractions.any { it.returnValue != null }) {
            builder.appendLine("import static org.mockito.Mockito.when;")
        }
        if (mockInteractions.isNotEmpty()) {
            builder.appendLine("import static org.mockito.Mockito.verify;")
        }

        builder.appendLine()
        if (needsRobolectricRunner) {
            builder.appendLine("@RunWith(RobolectricTestRunner.class)")
            builder.appendLine("@Config(sdk = $robolectricSdk)")
        }
        builder.appendLine("public class ${model.className}Test {")
        builder.appendLine()

        if (needsLiveDataRule) {
            appendLiveDataRule(builder)
        }

        if (dependencyParameters.isNotEmpty()) {
            appendFieldsAndSetUp(builder, model, constructor, dependencyParameters)
        }

        model.methods.forEach { method ->
            val usedNames = mutableMapOf<String, Int>()
            methodScenarios.getValue(method).forEach { scenario ->
                appendTestMethod(builder, model, method, scenario, usedNames, dependencyParameters.isNotEmpty())
                builder.appendLine()
            }
        }

        builder.appendLine("}")
        return GeneratedTestSource(
            source = builder.toString(),
            testMethodCount = scenarios.size,
            assertionCount = scenarios.count { it.assertion.kind != AssertionKind.NONE },
            fallbackMethodCount = scenarios.count { it.isFallback },
            ruleMatchedMethodCount = scenarios.count { !it.isFallback },
            mockedDependencyCount = dependencyParameters.size,
            mockStubCount = mockInteractions.count { it.returnValue != null },
            mockVerificationCount = mockInteractions.size,
            liveDataRuleCount = if (needsLiveDataRule) 1 else 0,
            robolectricTestCount = if (needsRobolectricRunner) 1 else 0,
            androidImportCount = androidImportCount,
        )
    }

    private fun generateActivityLifecycleTest(model: ClassModel): GeneratedTestSource {
        val builder = StringBuilder()
        appendPackage(builder, model)
        builder.appendLine("import org.junit.Test;")
        builder.appendLine("import org.junit.runner.RunWith;")
        builder.appendLine("import org.robolectric.Robolectric;")
        builder.appendLine("import org.robolectric.RobolectricTestRunner;")
        builder.appendLine("import org.robolectric.android.controller.ActivityController;")
        builder.appendLine("import org.robolectric.annotation.Config;")
        builder.appendLine("import static org.junit.Assert.assertNotNull;")
        if (model.lifecycleBooleanGetters().isNotEmpty()) {
            builder.appendLine("import static org.junit.Assert.assertTrue;")
        }
        builder.appendLine()
        builder.appendLine("@RunWith(RobolectricTestRunner.class)")
        builder.appendLine("@Config(sdk = $robolectricSdk)")
        builder.appendLine("public class ${model.className}Test {")
        builder.appendLine()
        builder.appendLine("    @Test")
        builder.appendLine("    public void activity_shouldMoveThroughLifecycle() {")
        builder.appendLine("        ActivityController<${model.className}> controller = Robolectric.buildActivity(${model.className}.class);")
        builder.appendLine()
        builder.appendLine("        ${model.className} activity = controller.create().start().resume().get();")
        builder.appendLine()
        builder.appendLine("        assertNotNull(activity);")
        model.lifecycleBooleanGetters().forEach { method ->
            builder.appendLine("        assertTrue(activity.${method.name}());")
        }
        builder.appendLine()
        builder.appendLine("        controller.pause().stop().destroy();")
        builder.appendLine("    }")
        builder.appendLine()
        builder.appendLine("}")

        return GeneratedTestSource(
            source = builder.toString(),
            testMethodCount = 1,
            assertionCount = 1 + model.lifecycleBooleanGetters().size,
            fallbackMethodCount = 0,
            ruleMatchedMethodCount = 1,
            mockedDependencyCount = 0,
            mockStubCount = 0,
            mockVerificationCount = 0,
            robolectricTestCount = 1,
            androidImportCount = 1,
        )
    }

    private fun generateFragmentLifecycleTest(model: ClassModel): GeneratedTestSource {
        val builder = StringBuilder()
        appendPackage(builder, model)
        builder.appendLine("import android.app.Activity;")
        builder.appendLine("import org.junit.Test;")
        builder.appendLine("import org.junit.runner.RunWith;")
        builder.appendLine("import org.robolectric.Robolectric;")
        builder.appendLine("import org.robolectric.RobolectricTestRunner;")
        builder.appendLine("import org.robolectric.android.controller.ActivityController;")
        builder.appendLine("import org.robolectric.annotation.Config;")
        builder.appendLine("import static org.junit.Assert.assertNotNull;")
        builder.appendLine("import static org.junit.Assert.assertTrue;")
        builder.appendLine()
        builder.appendLine("@RunWith(RobolectricTestRunner.class)")
        builder.appendLine("@Config(sdk = $robolectricSdk)")
        builder.appendLine("public class ${model.className}Test {")
        builder.appendLine()
        builder.appendLine("    @Test")
        builder.appendLine("    public void fragment_shouldAttachAndStart() {")
        builder.appendLine("        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create().start().resume();")
        builder.appendLine("        Activity activity = controller.get();")
        builder.appendLine("        ${model.className} fragment = new ${model.className}();")
        builder.appendLine()
        builder.appendLine("        activity.getFragmentManager().beginTransaction().add(fragment, \"target\").commitNow();")
        builder.appendLine()
        builder.appendLine("        assertNotNull(fragment);")
        builder.appendLine("        assertTrue(fragment.isAdded());")
        model.lifecycleBooleanGetters().forEach { method ->
            builder.appendLine("        assertTrue(fragment.${method.name}());")
        }
        builder.appendLine()
        builder.appendLine("        controller.pause().stop().destroy();")
        builder.appendLine("    }")
        builder.appendLine()
        builder.appendLine("}")

        return GeneratedTestSource(
            source = builder.toString(),
            testMethodCount = 1,
            assertionCount = 2 + model.lifecycleBooleanGetters().size,
            fallbackMethodCount = 0,
            ruleMatchedMethodCount = 1,
            mockedDependencyCount = 0,
            mockStubCount = 0,
            mockVerificationCount = 0,
            robolectricTestCount = 1,
            androidImportCount = 1,
        )
    }

    private fun appendFieldsAndSetUp(
        builder: StringBuilder,
        model: ClassModel,
        constructor: ConstructorModel,
        dependencyParameters: List<ParameterModel>,
    ) {
        dependencyParameters.forEach { parameter ->
            builder.appendLine("    private ${parameter.type} ${parameter.name};")
        }
        builder.appendLine("    private ${model.className} target;")
        builder.appendLine()
        builder.appendLine("    @Before")
        builder.appendLine("    public void setUp() {")
        dependencyParameters.forEach { parameter ->
            builder.appendLine("        ${parameter.name} = mock(${parameter.type.classLiteralType()}.class);")
        }
        val constructorArguments = constructor.parameters.joinToString(", ") { parameter ->
            if (parameter in dependencyParameters) parameter.name else SampleValueProvider().valueFor(parameter.type)
        }
        builder.appendLine("        target = new ${model.className}($constructorArguments);")
        builder.appendLine("    }")
        builder.appendLine()
    }

    private fun appendPackage(builder: StringBuilder, model: ClassModel) {
        if (model.packageName.isNotBlank()) {
            builder.appendLine("package ${model.packageName};")
            builder.appendLine()
        }
    }

    private fun ClassModel.lifecycleBooleanGetters(): List<MethodModel> {
        return methods.filter { method ->
            method.parameters.isEmpty() &&
                method.returnType in setOf("boolean", "Boolean") &&
                method.name.startsWith("is")
        }
    }

    private fun appendLiveDataRule(builder: StringBuilder) {
        builder.appendLine("    @Rule")
        builder.appendLine("    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();")
        builder.appendLine()
    }

    private fun appendTestMethod(
        builder: StringBuilder,
        model: ClassModel,
        method: MethodModel,
        scenario: TestScenario,
        usedNames: MutableMap<String, Int>,
        hasTargetField: Boolean,
    ) {
        if (scenario.expectedException != null) {
            builder.appendLine("    @Test(expected = ${scenario.expectedException}.class)")
        } else {
            builder.appendLine("    @Test")
        }
        builder.appendLine("    public void ${method.testName(scenario, usedNames)}() throws Exception {")

        val methodArguments = scenario.arguments.joinToString(", ")

        appendMockStubs(builder, scenario)

        if (method.isStatic) {
            appendInvocation(builder, model.className, method, methodArguments, scenario)
        } else {
            if (!hasTargetField) {
                val constructor = model.bestConstructor()
                val constructorArguments = constructor.parameters.joinToString(", ") {
                    SampleValueProvider().valueFor(it.type)
                }
                builder.appendLine("        ${model.className} target = new ${model.className}($constructorArguments);")
                builder.appendLine()
            }
            appendInvocation(builder, "target", method, methodArguments, scenario)
        }

        appendMockVerifications(builder, scenario)

        builder.appendLine("    }")
    }

    private fun appendMockStubs(builder: StringBuilder, scenario: TestScenario) {
        val stubs = scenario.mockInteractions.filter { it.returnValue != null }
        if (stubs.isEmpty()) {
            return
        }

        stubs.forEach { interaction ->
            val arguments = interaction.arguments.joinToString(", ")
            builder.appendLine(
                "        when(${interaction.receiverName}.${interaction.methodName}($arguments)).thenReturn(${interaction.returnValue});",
            )
        }
        builder.appendLine()
    }

    private fun appendMockVerifications(builder: StringBuilder, scenario: TestScenario) {
        if (scenario.mockInteractions.isEmpty()) {
            return
        }

        builder.appendLine()
        scenario.mockInteractions.forEach { interaction ->
            val arguments = interaction.arguments.joinToString(", ")
            builder.appendLine("        verify(${interaction.receiverName}).${interaction.methodName}($arguments);")
        }
    }

    private fun appendInvocation(
        builder: StringBuilder,
        receiver: String,
        method: MethodModel,
        methodArguments: String,
        scenario: TestScenario,
    ) {
        val invocation = "$receiver.${method.name}($methodArguments)"

        if (method.returnType == "void" || scenario.expectedException != null) {
            builder.appendLine("        $invocation;")
            return
        }

        builder.appendLine("        ${method.returnType} result = $invocation;")

        val assertionLine = scenario.assertion.render()
        if (assertionLine != null) {
            builder.appendLine()
            builder.appendLine("        $assertionLine")
        }
    }

    private fun ClassModel.bestConstructor(): ConstructorModel {
        return constructors
            .filter { it.parameters.isEmpty() }
            .ifEmpty { constructors }
            .minByOrNull { it.parameters.size }
            ?: ConstructorModel(emptyList())
    }

    private fun MethodModel.testName(scenario: TestScenario, usedNames: MutableMap<String, Int>): String {
        val baseName = "${name}_${scenario.nameSuffix}".sanitizeMethodName()
        val count = usedNames.getOrDefault(baseName, 0)
        usedNames[baseName] = count + 1
        return if (count == 0) baseName else "${baseName}_${count + 1}"
    }

    private fun String.sanitizeMethodName(): String {
        val sanitized = replace(Regex("[^A-Za-z0-9_]"), "_")
        return if (sanitized.firstOrNull()?.isDigit() == true) "_$sanitized" else sanitized
    }

    private fun io.github.dreamofloser.testgen.model.AssertionModel.render(): String? {
        return when (kind) {
            AssertionKind.NONE -> null
            AssertionKind.EQUALS -> "assertEquals($expectedValue, result);"
            AssertionKind.TRUE -> "assertTrue(result);"
            AssertionKind.FALSE -> "assertFalse(result);"
            AssertionKind.NOT_NULL -> "assertNotNull(result);"
        }
    }

    private fun ParameterModel.isDependencyParameter(): Boolean {
        val normalizedType = type.removeSuffix("?")
        if (normalizedType.endsWith("[]")) {
            return false
        }

        return normalizedType !in nonDependencyTypes &&
            !normalizedType.startsWith("List<") &&
            !normalizedType.startsWith("Set<") &&
            !normalizedType.startsWith("Map<") &&
            !normalizedType.startsWith("java.util.List<") &&
            !normalizedType.startsWith("java.util.Set<") &&
            !normalizedType.startsWith("java.util.Map<")
    }

    private fun ClassModel.usesLiveData(): Boolean {
        return imports.any { it == "androidx.lifecycle.LiveData" || it == "androidx.lifecycle.MutableLiveData" } ||
            methods.any { method ->
                method.returnType.containsSimpleType("LiveData") ||
                    method.returnType.containsSimpleType("MutableLiveData") ||
                    method.parameters.any { parameter ->
                        parameter.type.containsSimpleType("LiveData") ||
                            parameter.type.containsSimpleType("MutableLiveData")
                    }
            }
    }

    private fun ClassModel.usesAndroidFramework(): Boolean {
        return imports.any { importedType ->
            importedType == "android.*" ||
                importedType.startsWith("android.") ||
                importedType == "androidx.test.*" ||
                importedType.startsWith("androidx.test.")
        }
    }

    private fun ClassModel.neededSourceImports(constructor: ConstructorModel): List<String> {
        val directImportsBySimpleName = imports
            .filterNot { it.endsWith(".*") }
            .associateBy { it.substringAfterLast('.') }
        val wildcardImports = imports.filter { it.endsWith(".*") }
        val usedTypes = buildList {
            addAll(constructor.parameters.map { it.type })
            methods.forEach { method ->
                add(method.returnType)
                addAll(method.parameters.map { it.type })
            }
        }

        val directImports = usedTypes
            .flatMap { it.referencedTypeNames() }
            .mapNotNull { typeName -> directImportsBySimpleName[typeName] }

        return (wildcardImports + directImports)
            .filterNot { it.startsWith("java.lang.") }
            .distinct()
            .sorted()
    }

    private fun String.referencedTypeNames(): List<String> {
        return typeNameRegex.findAll(this)
            .map { it.value.substringAfterLast('.') }
            .filterNot { it in primitiveTypeNames || it in javaLangTypes }
            .toList()
    }

    private fun String.containsSimpleType(simpleName: String): Boolean {
        return referencedTypeNames().any { it == simpleName }
    }

    private fun String.classLiteralType(): String {
        return removeSuffix("?")
            .substringBefore('<')
            .removeSuffix("[]")
    }

    private companion object {
        val typeNameRegex = Regex("""[A-Za-z_][A-Za-z0-9_.]*""")
        val primitiveTypeNames = setOf(
            "byte",
            "short",
            "int",
            "long",
            "float",
            "double",
            "boolean",
            "char",
            "void",
        )
        val javaLangTypes = setOf(
            "String",
            "Object",
            "Exception",
            "RuntimeException",
            "IllegalArgumentException",
            "IllegalStateException",
            "Throwable",
        )
        val nonDependencyTypes = setOf(
            "byte",
            "short",
            "int",
            "long",
            "float",
            "double",
            "boolean",
            "char",
            "Byte",
            "Short",
            "Integer",
            "Long",
            "Float",
            "Double",
            "Boolean",
            "Character",
            "String",
            "java.lang.String",
        )
        const val robolectricSdk = 34
    }
}
