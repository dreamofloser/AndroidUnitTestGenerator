package io.github.dreamofloser.testgen.guide

import io.github.dreamofloser.testgen.llm.LlmClient
import io.github.dreamofloser.testgen.llm.LlmPlanningResult
import io.github.dreamofloser.testgen.llm.LlmPromptBuilder
import io.github.dreamofloser.testgen.llm.LlmStructuredResponseParser
import io.github.dreamofloser.testgen.llm.LlmTestAdvisor
import io.github.dreamofloser.testgen.model.ClassModel

interface TestCaseGuideGenerator {
    fun generate(
        iteration: Int,
        requests: List<TestCaseGuideRequest>,
        maxGuidesPerClass: Int,
    ): TestCaseGuideGeneration
}

class LlmTestCaseGuideGenerator(
    client: LlmClient,
    private val promptBuilder: LlmPromptBuilder = LlmPromptBuilder(),
    responseParser: LlmStructuredResponseParser = LlmStructuredResponseParser(),
    progressReporter: (String) -> Unit = {},
) : TestCaseGuideGenerator {
    private val advisor = LlmTestAdvisor(
        client = client,
        promptBuilder = promptBuilder,
        responseParser = responseParser,
        progressReporter = progressReporter,
    )

    override fun generate(
        iteration: Int,
        requests: List<TestCaseGuideRequest>,
        maxGuidesPerClass: Int,
    ): TestCaseGuideGeneration {
        if (requests.isEmpty()) {
            return TestCaseGuideGeneration(
                planningResult = LlmPlanningResult(emptyList(), emptyList()),
                guides = emptyList(),
            )
        }

        val requestByClass = requests.associateBy { it.classModel.qualifiedName() }
        val planningResult = advisor.analyzeWithPrompts(
            classes = requests.map { it.classModel },
            progressLabel = "Guide expansion iteration $iteration",
            promptFactory = { classModel ->
                val request = requestByClass.getValue(classModel.qualifiedName())
                promptBuilder.buildGuidePrompt(
                    classModel = classModel,
                    iteration = iteration,
                    candidates = request.candidates,
                    acceptedFingerprints = request.acceptedFingerprints,
                    maxGuides = maxGuidesPerClass,
                )
            },
        )

        val normalizedPlans = planningResult.structuredPlans.map { plan ->
            val request = requestByClass.getValue(plan.sourceClass)
            val normalizedScenarios = plan.scenarios
                .take(maxGuidesPerClass)
                .map { scenario ->
                    val candidate = request.candidates.singleOrNull {
                        it.methodName == scenario.methodName &&
                            it.targetParameter == scenario.targetParameter &&
                            it.inputStrategy == scenario.inputStrategy
                    }
                    candidate?.scenario(iteration)?.copy(
                        given = scenario.given,
                        whenAction = scenario.whenAction,
                        then = scenario.then,
                        requiresMock = scenario.requiresMock,
                    ) ?: scenario
                }
            plan.copy(
                scenarios = normalizedScenarios,
                iteration = iteration,
            )
        }
        val guides = normalizedPlans.flatMap { plan ->
            plan.scenarios.map { scenario ->
                TestCaseGuide(
                    iteration = iteration,
                    sourceClass = plan.sourceClass,
                    scenario = scenario,
                )
            }
        }
        return TestCaseGuideGeneration(
            planningResult = planningResult.copy(structuredPlans = normalizedPlans),
            guides = guides,
        )
    }

    private fun ClassModel.qualifiedName(): String {
        return if (packageName.isBlank()) className else "$packageName.$className"
    }
}
