package io.github.dreamofloser.testgen.generator

import io.github.dreamofloser.testgen.llm.LlmGenerationGuidance
import io.github.dreamofloser.testgen.llm.LlmTestScenario
import io.github.dreamofloser.testgen.model.ClassModel
import io.github.dreamofloser.testgen.model.ConstructorModel
import io.github.dreamofloser.testgen.model.MethodModel
import io.github.dreamofloser.testgen.model.ParameterModel
import io.github.dreamofloser.testgen.model.SourceClassKind
import io.github.dreamofloser.testgen.model.SourceLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LlmGuidedGenerationTest {
    private val scenario = LlmTestScenario(
        methodName = "normalize",
        category = "boundary",
        testName = "emptyValueIsHandled",
        given = "an empty value",
        whenAction = "normalize is called",
        then = "a result is returned",
        requiresMock = false,
        targetParameter = "value",
        inputStrategy = "empty-string",
    )
    private val method = MethodModel(
        name = "normalize",
        returnType = "String",
        parameters = listOf(ParameterModel("value", "String")),
        isStatic = false,
        thrownExceptions = emptyList(),
    )

    @Test
    fun kotlinGeneratorUsesLlmSelectedInputStrategy() {
        val model = ClassModel(
            packageName = "sample",
            className = "TextFormatter",
            sourceFile = File("TextFormatter.kt"),
            constructors = listOf(ConstructorModel(emptyList())),
            methods = listOf(method),
            language = SourceLanguage.KOTLIN,
            classKind = SourceClassKind.REGULAR,
        )

        val result = KotlinUnitTestGenerator().generate(
            model,
            LlmGenerationGuidance(acceptedScenarios = listOf(scenario)),
        )

        assertTrue(result.source.contains("normalize_llm_emptyValueIsHandled"))
        val emptyStringArgument = 34.toChar().toString().repeat(2)
        assertTrue(result.source.contains("target.normalize($emptyStringArgument)"))
        assertEquals(1, result.llmAdoptedMethodCount)
    }

    @Test
    fun javaScenarioGeneratorUsesLlmSelectedInputStrategy() {
        val scenarios = TestScenarioGenerator().scenariosFor(
            method = method.copy(returnType = "java.lang.String"),
            llmScenarios = listOf(scenario),
        )

        assertTrue(scenarios.any { it.ruleName == "llm-boundary" })
        val emptyStringArgument = 34.toChar().toString().repeat(2)
        assertTrue(scenarios.any { it.arguments == listOf(emptyStringArgument) })
    }

    @Test
    fun generatorsRespectSelectedNumericParameter() {
        val numericMethod = method.copy(
            parameters = listOf(
                ParameterModel("value", "String"),
                ParameterModel("count", "Int"),
            ),
        )
        val numericScenario = scenario.copy(
            testName = "negativeCountIsHandled",
            targetParameter = "count",
            inputStrategy = "negative",
        )

        val scenarios = TestScenarioGenerator().scenariosFor(
            method = numericMethod,
            llmScenarios = listOf(numericScenario),
        )

        val quote = 34.toChar()
        val sampleArgument = quote.toString() + "sample" + quote
        assertTrue(scenarios.any { it.ruleName == "llm-boundary" && it.arguments == listOf(sampleArgument, "-1") })
    }
}
