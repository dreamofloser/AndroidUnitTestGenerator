package com.codex.testgen.generator

import com.codex.testgen.model.ClassModel
import com.codex.testgen.model.ConstructorModel
import com.codex.testgen.model.AssertionKind
import com.codex.testgen.model.GeneratedTestSource
import com.codex.testgen.model.MethodModel
import com.codex.testgen.model.TestScenario

class JUnit4JavaTestGenerator(
    private val scenarioGenerator: TestScenarioGenerator = TestScenarioGenerator(),
) {
    fun generate(model: ClassModel): GeneratedTestSource {
        val builder = StringBuilder()
        val methodScenarios = model.methods.associateWith { scenarioGenerator.scenariosFor(it) }
        val scenarios = methodScenarios.values.flatten()

        if (model.packageName.isNotBlank()) {
            builder.appendLine("package ${model.packageName};")
            builder.appendLine()
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

        builder.appendLine()
        builder.appendLine("public class ${model.className}Test {")
        builder.appendLine()

        model.methods.forEach { method ->
            val usedNames = mutableMapOf<String, Int>()
            methodScenarios.getValue(method).forEach { scenario ->
                appendTestMethod(builder, model, method, scenario, usedNames)
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
        )
    }

    private fun appendTestMethod(
        builder: StringBuilder,
        model: ClassModel,
        method: MethodModel,
        scenario: TestScenario,
        usedNames: MutableMap<String, Int>,
    ) {
        if (scenario.expectedException != null) {
            builder.appendLine("    @Test(expected = ${scenario.expectedException}.class)")
        } else {
            builder.appendLine("    @Test")
        }
        builder.appendLine("    public void ${method.testName(scenario, usedNames)}() throws Exception {")

        val methodArguments = scenario.arguments.joinToString(", ")

        if (method.isStatic) {
            appendInvocation(builder, model.className, method, methodArguments, scenario)
        } else {
            val constructor = model.bestConstructor()
            val constructorArguments = constructor.parameters.joinToString(", ") {
                SampleValueProvider().valueFor(it.type)
            }
            builder.appendLine("        ${model.className} target = new ${model.className}($constructorArguments);")
            builder.appendLine()
            appendInvocation(builder, "target", method, methodArguments, scenario)
        }

        builder.appendLine("    }")
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
}
