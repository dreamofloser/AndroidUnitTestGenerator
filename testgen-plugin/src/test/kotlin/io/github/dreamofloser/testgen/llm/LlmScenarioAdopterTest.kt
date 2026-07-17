package io.github.dreamofloser.testgen.llm

import io.github.dreamofloser.testgen.model.ClassModel
import io.github.dreamofloser.testgen.model.ConstructorModel
import io.github.dreamofloser.testgen.model.DependencyCallModel
import io.github.dreamofloser.testgen.model.MethodModel
import io.github.dreamofloser.testgen.model.ParameterModel
import io.github.dreamofloser.testgen.model.SourceClassKind
import io.github.dreamofloser.testgen.model.SourceLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LlmScenarioAdopterTest {
    private val adopter = LlmScenarioAdopter()

    @Test
    fun acceptsModelSelectedBoundaryParameterAndStrategy() {
        val guidance = adopter.adopt(
            regularModel(),
            plan(
                methodName = "normalize",
                category = "boundary",
                targetParameter = "value",
                inputStrategy = "empty-string",
            ),
        )

        val accepted = guidance.acceptedScenarios.single()
        assertEquals("value", accepted.targetParameter)
        assertEquals("empty-string", accepted.inputStrategy)
        assertEquals(LlmAdoptionStatus.GENERATED, guidance.decisions.single().status)
    }

    @Test
    fun suppliesCompatibleDefaultsForOlderStructuredResponses() {
        val guidance = adopter.adopt(regularModel(), plan("normalize", "boundary"))

        val accepted = guidance.acceptedScenarios.single()
        assertEquals("value", accepted.targetParameter)
        assertEquals("empty-string", accepted.inputStrategy)
    }

    @Test
    fun acceptsNegativeStrategyForNumericParameter() {
        val model = regularModel().copy(
            methods = listOf(
                regularModel().methods.single().copy(
                    parameters = listOf(ParameterModel("count", "Int")),
                ),
            ),
        )

        val guidance = adopter.adopt(
            model,
            plan("normalize", "boundary", "count", "negative"),
        )

        assertEquals(LlmAdoptionStatus.GENERATED, guidance.decisions.single().status)
        assertEquals("negative", guidance.acceptedScenarios.single().inputStrategy)
    }

    @Test
    fun rejectsStrategyThatDoesNotMatchParameterType() {
        val guidance = adopter.adopt(
            regularModel(),
            plan("normalize", "boundary", "value", "false"),
        )

        assertTrue(guidance.acceptedScenarios.isEmpty())
        assertEquals(LlmAdoptionStatus.UNSUPPORTED, guidance.decisions.single().status)
    }

    @Test
    fun acceptsKotlinBoundaryScenarioWhenDependencyCallHasRuleTemplate() {
        val base = regularModel()
        val model = base.copy(
            methods = listOf(
                base.methods.single().copy(
                    returnType = "Result<String>",
                    dependencyCalls = listOf(
                        DependencyCallModel(
                            receiverName = "repository",
                            methodName = "normalize",
                            arguments = listOf("value"),
                        ),
                    ),
                ),
            ),
        )

        val guidance = adopter.adopt(
            model,
            plan("normalize", "boundary", "value", "empty-string"),
        )

        assertEquals(LlmAdoptionStatus.GENERATED, guidance.decisions.single().status)
        assertEquals(1, guidance.acceptedScenarios.size)
    }

    @Test
    fun rejectsHallucinatedMethod() {
        val guidance = adopter.adopt(regularModel(), plan("missingMethod", "boundary"))

        assertTrue(guidance.acceptedScenarios.isEmpty())
        assertEquals(LlmAdoptionStatus.INVALID_METHOD, guidance.decisions.single().status)
    }

    @Test
    fun leavesNetworkScenarioToSpecializedGenerator() {
        val model = regularModel().copy(classKind = SourceClassKind.RETROFIT_API)
        val guidance = adopter.adopt(model, plan("normalize", "network"))

        assertTrue(guidance.acceptedScenarios.isEmpty())
        assertEquals(LlmAdoptionStatus.RULE_COVERED, guidance.decisions.single().status)
    }

    private fun regularModel(): ClassModel {
        return ClassModel(
            packageName = "sample",
            className = "TextFormatter",
            sourceFile = File("TextFormatter.kt"),
            constructors = listOf(ConstructorModel(emptyList())),
            methods = listOf(
                MethodModel(
                    name = "normalize",
                    returnType = "String",
                    parameters = listOf(ParameterModel("value", "String")),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                ),
            ),
            language = SourceLanguage.KOTLIN,
            classKind = SourceClassKind.REGULAR,
        )
    }

    private fun plan(
        methodName: String,
        category: String,
        targetParameter: String? = null,
        inputStrategy: String? = null,
    ): LlmStructuredTestPlan {
        return LlmStructuredTestPlan(
            sourceClass = "sample.TextFormatter",
            sourceSummary = "Formats text.",
            scenarios = listOf(
                LlmTestScenario(
                    methodName = methodName,
                    category = category,
                    testName = "selectedBoundaryIsHandled",
                    given = "a selected boundary value",
                    whenAction = "the method is called",
                    then = "a result is returned",
                    requiresMock = false,
                    targetParameter = targetParameter,
                    inputStrategy = inputStrategy,
                ),
            ),
            mockStrategies = emptyList(),
            manualReviewNotes = emptyList(),
            parseStatus = LlmJsonParseStatus.SUCCESS,
        )
    }
}
