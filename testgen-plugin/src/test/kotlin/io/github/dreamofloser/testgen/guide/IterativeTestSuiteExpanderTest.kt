package io.github.dreamofloser.testgen.guide

import io.github.dreamofloser.testgen.generator.KotlinUnitTestGenerator
import io.github.dreamofloser.testgen.llm.LlmAgentConfig
import io.github.dreamofloser.testgen.llm.LlmAdoptionStatus
import io.github.dreamofloser.testgen.llm.LlmJsonParseStatus
import io.github.dreamofloser.testgen.llm.LlmPlanningResult
import io.github.dreamofloser.testgen.llm.LlmStructuredTestPlan
import io.github.dreamofloser.testgen.llm.MockLlmClient
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

class IterativeTestSuiteExpanderTest {
    @Test
    fun expandsTwoDistinctBoundaryGuidesAcrossTwoIterations() {
        val model = kotlinResultRepository()
        val result = IterativeTestSuiteExpander(FirstCandidateGuideGenerator())
            .expand(
                classes = listOf(model),
                maxIterations = 2,
                maxGuidesPerClass = 1,
            )

        assertEquals(2, result.iterations.size)
        assertTrue(result.iterations.all { it.status == GuideIterationStatus.EXPANDED })
        assertEquals(listOf(1, 1), result.iterations.map { it.acceptedGuideCount })
        assertEquals(2, result.guidanceByClass.getValue(model).acceptedScenarios.size)
        assertEquals(
            setOf("empty-string", "blank-string"),
            result.guidanceByClass.getValue(model).acceptedScenarios.map { it.inputStrategy }.toSet(),
        )
        assertTrue(
            result.guidanceByClass.getValue(model).acceptedScenarios
                .map { it.testName }
                .containsAll(
                    listOf(
                        "guide_i1_loadForecast_city_empty_string",
                        "guide_i2_loadForecast_city_blank_string",
                    ),
                ),
        )
        val generated = KotlinUnitTestGenerator().generate(
            model,
            result.guidanceByClass.getValue(model),
        )
        assertEquals(4, generated.testMethodCount)
        assertTrue(generated.source.contains("loadForecast_llm_guide_i1_loadForecast_city_empty_string"))
        assertTrue(generated.source.contains("loadForecast_llm_guide_i2_loadForecast_city_blank_string"))
    }

    @Test
    fun recordsBothIterationsWhenNoSupportedGapExists() {
        val model = kotlinResultRepository().copy(classKind = SourceClassKind.DATA)
        val result = IterativeTestSuiteExpander(FirstCandidateGuideGenerator())
            .expand(listOf(model), maxIterations = 2)

        assertEquals(2, result.iterations.size)
        assertTrue(result.iterations.all { it.status == GuideIterationStatus.NO_GAPS })
        assertTrue(result.adoptionDecisions.isEmpty())
    }

    @Test
    fun mockLlmGuideGeneratorUsesTheRemainingCandidateEachRound() {
        val config = LlmAgentConfig(
            enabled = true,
            provider = "mock",
            model = "guide-test",
            agentMode = "planning-and-review",
            endpoint = "",
            apiKeyEnv = "LLM_API_KEY",
        )
        val result = IterativeTestSuiteExpander(
            LlmTestCaseGuideGenerator(MockLlmClient(config)),
        ).expand(listOf(kotlinResultRepository()), maxIterations = 2)

        assertEquals(2, result.adoptionDecisions.count { it.status == LlmAdoptionStatus.GENERATED })
        assertEquals(listOf(1, 2), result.planningResult.structuredPlans.map { it.iteration })
        assertEquals(
            setOf("empty-string", "blank-string"),
            result.adoptionDecisions.mapNotNull { it.inputStrategy }.toSet(),
        )
    }

    private fun kotlinResultRepository(): ClassModel {
        return ClassModel(
            packageName = "com.example",
            className = "WeatherRepository",
            sourceFile = File("WeatherRepository.kt"),
            constructors = listOf(
                ConstructorModel(listOf(ParameterModel("api", "WeatherApi"))),
            ),
            methods = listOf(
                MethodModel(
                    name = "loadForecast",
                    returnType = "Result<Forecast>",
                    parameters = listOf(ParameterModel("city", "String")),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                    isSuspend = true,
                    dependencyCalls = listOf(
                        DependencyCallModel("api", "fetchForecast", listOf("city")),
                    ),
                ),
            ),
            language = SourceLanguage.KOTLIN,
            classKind = SourceClassKind.REGULAR,
        )
    }

    private class FirstCandidateGuideGenerator : TestCaseGuideGenerator {
        override fun generate(
            iteration: Int,
            requests: List<TestCaseGuideRequest>,
            maxGuidesPerClass: Int,
        ): TestCaseGuideGeneration {
            val plans = requests.map { request ->
                val scenario = request.candidates.first().scenario(iteration)
                LlmStructuredTestPlan(
                    sourceClass = request.candidates.first().sourceClass,
                    sourceSummary = "Selects the first uncovered boundary guide.",
                    scenarios = listOf(scenario),
                    mockStrategies = emptyList(),
                    manualReviewNotes = emptyList(),
                    parseStatus = LlmJsonParseStatus.SUCCESS,
                    iteration = iteration,
                )
            }
            return TestCaseGuideGeneration(
                planningResult = LlmPlanningResult(
                    suggestions = emptyList(),
                    structuredPlans = plans,
                ),
                guides = plans.flatMap { plan ->
                    plan.scenarios.map { scenario ->
                        TestCaseGuide(iteration, plan.sourceClass, scenario)
                    }
                },
            )
        }
    }
}
