package io.github.dreamofloser.testgen.generator

import io.github.dreamofloser.testgen.model.AssertionKind
import io.github.dreamofloser.testgen.model.AssertionModel
import io.github.dreamofloser.testgen.model.MethodModel
import io.github.dreamofloser.testgen.model.MockInteractionModel
import io.github.dreamofloser.testgen.model.ParameterModel
import io.github.dreamofloser.testgen.model.TestScenario
import io.github.dreamofloser.testgen.llm.LlmInputStrategy
import io.github.dreamofloser.testgen.llm.LlmTestScenario
import io.github.dreamofloser.testgen.llm.llmJavaValue
import io.github.dreamofloser.testgen.llm.supportedLlmInputStrategies

class TestScenarioGenerator(
    private val sampleValueProvider: SampleValueProvider = SampleValueProvider(),
) {
    fun scenariosFor(
        method: MethodModel,
        dependencyNames: Set<String> = emptySet(),
        llmScenarios: List<LlmTestScenario> = emptyList(),
    ): List<TestScenario> {
        val scenarios = mutableListOf<TestScenario>()

        val mockScenarios = mockInteractionScenarios(method, dependencyNames)
        if (mockScenarios.isNotEmpty()) {
            scenarios += mockScenarios
        } else {
            exceptionScenario(method)?.let { scenarios += it }
            comparisonScenarios(method).takeIf { it.isNotEmpty() }?.let { scenarios += it }
            arithmeticScenarios(method).takeIf { it.isNotEmpty() }?.let { scenarios += it }

            if (scenarios.isEmpty()) {
                scenarios += parameterVariationScenarios(method)
            }
        }

        scenarios += llmBoundaryScenarios(method, llmScenarios)

        if (scenarios.isEmpty()) {
            scenarios += fallbackScenario(method)
        }

        return scenarios
            .sortedBy { if (it.ruleName == "llm-boundary") 0 else 1 }
            .distinctBy { it.arguments }
    }

    private fun llmBoundaryScenarios(
        method: MethodModel,
        llmScenarios: List<LlmTestScenario>,
    ): List<TestScenario> {
        return llmScenarios.mapNotNull { llmScenario ->
            val requestedStrategy = LlmInputStrategy.fromWireName(llmScenario.inputStrategy)
            val boundary = method.parameters
                .mapIndexedNotNull { index, parameter ->
                    val strategies = parameter.supportedLlmInputStrategies()
                    val strategy = requestedStrategy?.takeIf { it in strategies }
                        ?: if (requestedStrategy == null) strategies.firstOrNull() else null
                    if (!llmScenario.targetParameter.isNullOrBlank() &&
                        parameter.name != llmScenario.targetParameter
                    ) {
                        null
                    } else {
                        strategy?.let { index to it }
                    }
                }
                .firstOrNull()
                ?: return@mapNotNull null
            val boundaryValue = method.parameters[boundary.first].llmJavaValue(boundary.second)
                ?: return@mapNotNull null
            val arguments = method.parameters.mapIndexed { index, parameter ->
                if (index == boundary.first) boundaryValue else sampleValueProvider.valueFor(parameter.type)
            }
            TestScenario(
                nameSuffix = "llm_${llmScenario.testName.safeTestName()}",
                arguments = arguments,
                assertion = fallbackAssertionFor(method),
                ruleName = "llm-boundary",
            )
        }
    }

    private fun String.safeTestName(): String {
        val cleaned = replace(Regex("[^A-Za-z0-9_]"), "_").trim('_').take(60)
        return cleaned.ifBlank { "suggestedBoundary" }
    }

    private fun mockInteractionScenarios(method: MethodModel, dependencyNames: Set<String>): List<TestScenario> {
        if (dependencyNames.isEmpty()) {
            return emptyList()
        }

        val dependencyCall = method.dependencyCalls.firstOrNull { it.receiverName in dependencyNames }
            ?: return emptyList()
        val arguments = method.parameters.map { sampleValueProvider.valueFor(it.type) }
        val parameterValues = method.parameters.mapIndexed { index, parameter -> parameter.name to arguments[index] }.toMap()
        val resolvedCallArguments = dependencyCall.arguments.map { argument ->
            parameterValues[argument] ?: argument
        }
        val returnValue = stubValueFor(method.returnType)

        return listOf(
            TestScenario(
                nameSuffix = "withMocked${dependencyCall.receiverName.capitalized()}_shouldUseDependency",
                arguments = arguments,
                assertion = assertionForStubbedReturn(method.returnType, returnValue),
                ruleName = "mockito",
                mockInteractions = listOf(
                    MockInteractionModel(
                        receiverName = dependencyCall.receiverName,
                        methodName = dependencyCall.methodName,
                        arguments = resolvedCallArguments,
                        returnValue = returnValue,
                    ),
                ),
            ),
        )
    }

    private fun exceptionScenario(method: MethodModel): TestScenario? {
        val exceptionType = method.thrownStatementTypes.firstOrNull()
            ?: method.thrownExceptions.firstOrNull()
            ?: return null

        if (method.parameters.isEmpty()) {
            return null
        }

        return TestScenario(
            nameSuffix = "whenInvalidInput_shouldThrow${exceptionType.simpleName()}",
            arguments = method.parameters.map { invalidValueFor(it) },
            assertion = AssertionModel(AssertionKind.NONE),
            expectedException = exceptionType,
            ruleName = "exception",
        )
    }

    private fun comparisonScenarios(method: MethodModel): List<TestScenario> {
        if (method.returnType !in booleanTypes) {
            return emptyList()
        }

        val expression = method.returnExpressions.firstOrNull() ?: return emptyList()
        val match = comparisonRegex.matchEntire(expression.withoutParentheses()) ?: return emptyList()
        val parameter = method.parameters.firstOrNull { it.name == match.groupValues[1] } ?: return emptyList()
        val operator = match.groupValues[2]
        val threshold = match.groupValues[3].toIntOrNull() ?: return emptyList()

        val trueValue: Int
        val falseValue: Int

        when (operator) {
            ">=" -> {
                trueValue = threshold
                falseValue = threshold - 1
            }
            ">" -> {
                trueValue = threshold + 1
                falseValue = threshold
            }
            "<=" -> {
                trueValue = threshold
                falseValue = threshold + 1
            }
            "<" -> {
                trueValue = threshold - 1
                falseValue = threshold
            }
            "==" -> {
                trueValue = threshold
                falseValue = threshold + 1
            }
            "!=" -> {
                trueValue = threshold + 1
                falseValue = threshold
            }
            else -> return emptyList()
        }

        return listOf(
            TestScenario(
                nameSuffix = "when${parameter.name.capitalized()}Is${trueValue.safeName()}_shouldReturnTrue",
                arguments = method.argumentsWith(parameter, trueValue.toString()),
                assertion = AssertionModel(AssertionKind.TRUE),
                ruleName = "comparison",
            ),
            TestScenario(
                nameSuffix = "when${parameter.name.capitalized()}Is${falseValue.safeName()}_shouldReturnFalse",
                arguments = method.argumentsWith(parameter, falseValue.toString()),
                assertion = AssertionModel(AssertionKind.FALSE),
                ruleName = "comparison",
            ),
        )
    }

    private fun arithmeticScenarios(method: MethodModel): List<TestScenario> {
        if (method.returnType !in numericTypes) {
            return emptyList()
        }

        val expression = method.returnExpressions.firstOrNull() ?: return emptyList()
        val match = arithmeticRegex.matchEntire(expression.withoutParentheses()) ?: return emptyList()
        val leftName = match.groupValues[1]
        val operator = match.groupValues[2]
        val rightNameOrLiteral = match.groupValues[3]
        val leftParameter = method.parameters.firstOrNull { it.name == leftName } ?: return emptyList()
        val rightParameter = method.parameters.firstOrNull { it.name == rightNameOrLiteral }

        val baselineArguments = method.parameters.associateWith { parameter ->
            when (parameter) {
                leftParameter -> "1"
                rightParameter -> "1"
                else -> sampleValueProvider.valueFor(parameter.type)
            }
        }
        val rightValue = rightParameter?.let { baselineArguments[it]?.toIntOrNull() }
            ?: rightNameOrLiteral.toIntOrNull()
            ?: return emptyList()
        val leftValue = baselineArguments[leftParameter]?.toIntOrNull() ?: return emptyList()
        val expected = evaluateInt(leftValue, operator, rightValue) ?: return emptyList()

        return listOf(
            TestScenario(
                nameSuffix = "withDefaultInputs_shouldReturnExpectedValue",
                arguments = method.parameters.map { baselineArguments.getValue(it) },
                assertion = AssertionModel(AssertionKind.EQUALS, expected.toString()),
                ruleName = "arithmetic",
            ),
        )
    }

    private fun parameterVariationScenarios(method: MethodModel): List<TestScenario> {
        val scenarios = mutableListOf<TestScenario>()
        val defaultArguments = method.parameters.map { sampleValueProvider.valueFor(it.type) }

        if (method.parameters.isNotEmpty()) {
            scenarios += TestScenario(
                nameSuffix = "withDefaultInputs_shouldRun",
                arguments = defaultArguments,
                assertion = fallbackAssertionFor(method),
                ruleName = "default-input",
                isFallback = method.returnExpressions.isEmpty(),
            )
        }

        method.parameters.forEachIndexed { index, parameter ->
            when (parameter.normalizedType()) {
                "String", "java.lang.String" -> {
                    scenarios += method.variationScenario(index, "\"\"", "when${parameter.name.capitalized()}IsEmpty_shouldRun")
                    scenarios += method.variationScenario(index, "null", "when${parameter.name.capitalized()}IsNull_shouldRun")
                }
                "boolean", "Boolean" -> {
                    scenarios += method.variationScenario(index, "false", "when${parameter.name.capitalized()}IsFalse_shouldRun")
                }
                "int", "Integer", "long", "Long" -> {
                    scenarios += method.variationScenario(index, "0", "when${parameter.name.capitalized()}IsZero_shouldRun")
                    scenarios += method.variationScenario(index, "-1", "when${parameter.name.capitalized()}IsNegative_shouldRun")
                }
            }
        }

        return scenarios
    }

    private fun MethodModel.variationScenario(
        parameterIndex: Int,
        value: String,
        nameSuffix: String,
    ): TestScenario {
        val arguments = parameters.mapIndexed { index, parameter ->
            if (index == parameterIndex) value else sampleValueProvider.valueFor(parameter.type)
        }

        return TestScenario(
            nameSuffix = nameSuffix,
            arguments = arguments,
            assertion = fallbackAssertionFor(this),
            ruleName = "parameter-variation",
            isFallback = returnExpressions.isEmpty(),
        )
    }

    private fun fallbackScenario(method: MethodModel): TestScenario {
        return TestScenario(
            nameSuffix = "shouldRunWithoutException",
            arguments = method.parameters.map { sampleValueProvider.valueFor(it.type) },
            assertion = fallbackAssertionFor(method),
            ruleName = "fallback",
            isFallback = true,
        )
    }

    private fun fallbackAssertionFor(method: MethodModel): AssertionModel {
        return if (method.returnType !in primitiveAndVoidTypes) {
            AssertionModel(AssertionKind.NOT_NULL)
        } else {
            AssertionModel(AssertionKind.NONE)
        }
    }

    private fun assertionForStubbedReturn(returnType: String, returnValue: String?): AssertionModel {
        if (returnType == "void") {
            return AssertionModel(AssertionKind.NONE)
        }

        if (returnValue == null) {
            return AssertionModel(AssertionKind.NOT_NULL)
        }

        return when (returnType) {
            "boolean", "Boolean" -> if (returnValue == "true") {
                AssertionModel(AssertionKind.TRUE)
            } else {
                AssertionModel(AssertionKind.FALSE)
            }
            else -> {
                if (returnValue.startsWith("mock(")) {
                    AssertionModel(AssertionKind.NOT_NULL)
                } else {
                    AssertionModel(AssertionKind.EQUALS, returnValue)
                }
            }
        }
    }

    private fun stubValueFor(returnType: String): String? {
        return when (returnType.removeSuffix("?")) {
            "void" -> null
            "String", "java.lang.String" -> "\"sample\""
            "boolean", "Boolean" -> "true"
            "byte", "Byte" -> "(byte) 1"
            "short", "Short" -> "(short) 1"
            "int", "Integer" -> "1"
            "long", "Long" -> "1L"
            "float", "Float" -> "1.0f"
            "double", "Double" -> "1.0d"
            "char", "Character" -> "'a'"
            else -> {
                when {
                    returnType.startsWith("List<") || returnType.startsWith("java.util.List<") -> {
                        "java.util.Collections.emptyList()"
                    }
                    returnType.startsWith("Set<") || returnType.startsWith("java.util.Set<") -> {
                        "java.util.Collections.emptySet()"
                    }
                    returnType.startsWith("Map<") || returnType.startsWith("java.util.Map<") -> {
                        "java.util.Collections.emptyMap()"
                    }
                    else -> "mock(${returnType.classLiteralType()}.class)"
                }
            }
        }
    }

    private fun MethodModel.argumentsWith(parameter: ParameterModel, value: String): List<String> {
        return parameters.map {
            if (it.name == parameter.name) value else sampleValueProvider.valueFor(it.type)
        }
    }

    private fun invalidValueFor(parameter: ParameterModel): String {
        return when (parameter.normalizedType()) {
            "String", "java.lang.String" -> "null"
            "boolean", "Boolean" -> "false"
            "long", "Long" -> "0L"
            "float", "Float" -> "0.0f"
            "double", "Double" -> "0.0d"
            "int", "Integer", "short", "Short", "byte", "Byte" -> "0"
            else -> "null"
        }
    }

    private fun ParameterModel.normalizedType(): String = type.removeSuffix("?")

    private fun String.withoutParentheses(): String {
        return trim().removePrefix("(").removeSuffix(")").trim()
    }

    private fun String.capitalized(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun Int.safeName(): String {
        return if (this < 0) "Negative${-this}" else toString()
    }

    private fun String.simpleName(): String = substringAfterLast('.')

    private fun String.classLiteralType(): String {
        return removeSuffix("?")
            .substringBefore('<')
            .removeSuffix("[]")
    }

    private fun evaluateInt(left: Int, operator: String, right: Int): Int? {
        return when (operator) {
            "+" -> left + right
            "-" -> left - right
            "*" -> left * right
            "/" -> if (right == 0) null else left / right
            else -> null
        }
    }

    private companion object {
        val booleanTypes = setOf("boolean", "Boolean")
        val numericTypes = setOf("byte", "short", "int", "long", "Byte", "Short", "Integer", "Long")
        val primitiveBoundaryTypes = setOf("String", "java.lang.String", "boolean", "Boolean", "byte", "Byte", "short", "Short", "int", "Integer", "long", "Long", "float", "Float", "double", "Double")
        val primitiveAndVoidTypes = setOf(
            "void",
            "byte",
            "short",
            "int",
            "long",
            "float",
            "double",
            "boolean",
            "char",
        )
        val comparisonRegex = Regex("""([A-Za-z_][A-Za-z0-9_]*)\s*(>=|>|<=|<|==|!=)\s*(-?\d+)""")
        val arithmeticRegex = Regex("""([A-Za-z_][A-Za-z0-9_]*)\s*([+\-*/])\s*([A-Za-z_][A-Za-z0-9_]*|-?\d+)""")
    }
}
