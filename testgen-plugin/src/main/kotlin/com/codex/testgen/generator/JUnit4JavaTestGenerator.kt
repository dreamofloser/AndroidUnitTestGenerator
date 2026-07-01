package com.codex.testgen.generator

import com.codex.testgen.model.ClassModel
import com.codex.testgen.model.ConstructorModel
import com.codex.testgen.model.AssertionKind
import com.codex.testgen.model.GeneratedTestSource
import com.codex.testgen.model.MethodModel
import com.codex.testgen.model.ParameterModel
import com.codex.testgen.model.TestScenario

class JUnit4JavaTestGenerator(
    private val scenarioGenerator: TestScenarioGenerator = TestScenarioGenerator(),
) {
    fun generate(model: ClassModel): GeneratedTestSource {
        val builder = StringBuilder()
        val constructor = model.bestConstructor()
        val dependencyParameters = constructor.parameters.filter { it.isDependencyParameter() }
        val dependencyNames = dependencyParameters.map { it.name }.toSet()
        val methodScenarios = model.methods.associateWith { scenarioGenerator.scenariosFor(it, dependencyNames) }
        val scenarios = methodScenarios.values.flatten()
        val mockInteractions = scenarios.flatMap { it.mockInteractions }

        if (model.packageName.isNotBlank()) {
            builder.appendLine("package ${model.packageName};")
            builder.appendLine()
        }

        if (dependencyParameters.isNotEmpty()) {
            builder.appendLine("import org.junit.Before;")
        }
        builder.appendLine("import org.junit.Test;")

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
        builder.appendLine("public class ${model.className}Test {")
        builder.appendLine()

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
            builder.appendLine("        ${parameter.name} = mock(${parameter.type}.class);")
        }
        val constructorArguments = constructor.parameters.joinToString(", ") { parameter ->
            if (parameter in dependencyParameters) parameter.name else SampleValueProvider().valueFor(parameter.type)
        }
        builder.appendLine("        target = new ${model.className}($constructorArguments);")
        builder.appendLine("    }")
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

    private fun com.codex.testgen.model.AssertionModel.render(): String? {
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

    private companion object {
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
    }
}
