package io.github.dreamofloser.testgen.guide

import io.github.dreamofloser.testgen.llm.LlmAdoptionDecision
import io.github.dreamofloser.testgen.llm.LlmGenerationGuidance
import io.github.dreamofloser.testgen.llm.LlmPlanningResult
import io.github.dreamofloser.testgen.llm.LlmTestScenario
import io.github.dreamofloser.testgen.model.ClassModel

data class TestCaseGuideCandidate(
    val sourceClass: String,
    val methodName: String,
    val targetParameter: String,
    val inputStrategy: String,
    val difficultyScore: Int = 0,
    val priorityScore: Int = 0,
    val automationConfidence: Int = 0,
    val guidePriorityScore: Int = 0,
) {
    val fingerprint: String
        get() = listOf(sourceClass, methodName, targetParameter, inputStrategy)
            .joinToString("|")
            .lowercase()

    fun scenario(iteration: Int): LlmTestScenario {
        val strategyName = inputStrategy.replace("-", "_")
        val safeMethodName = methodName.replace(Regex("[^A-Za-z0-9_]"), "_")
        val safeParameterName = targetParameter.replace(Regex("[^A-Za-z0-9_]"), "_")
        return LlmTestScenario(
            methodName = methodName,
            category = "boundary",
            testName = "guide_i${iteration}_${safeMethodName}_${safeParameterName}_$strategyName",
            given = "$targetParameter uses $inputStrategy",
            whenAction = "$methodName is invoked",
            then = "the observable result remains testable",
            requiresMock = false,
            targetParameter = targetParameter,
            inputStrategy = inputStrategy,
        )
    }
}

data class TestCaseGuideRequest(
    val classModel: ClassModel,
    val candidates: List<TestCaseGuideCandidate>,
    val acceptedFingerprints: Set<String>,
)

data class TestCaseGuide(
    val iteration: Int,
    val sourceClass: String,
    val scenario: LlmTestScenario,
) {
    val fingerprint: String
        get() = listOf(
            sourceClass,
            scenario.methodName.orEmpty(),
            scenario.targetParameter.orEmpty(),
            scenario.inputStrategy.orEmpty(),
        ).joinToString("|").lowercase()
}

data class TestCaseGuideGeneration(
    val planningResult: LlmPlanningResult,
    val guides: List<TestCaseGuide>,
)

enum class GuideIterationStatus {
    EXPANDED,
    NO_GAPS,
    NO_NEW_GUIDES,
}

data class GuideIterationResult(
    val iteration: Int,
    val status: GuideIterationStatus,
    val requestedClassCount: Int,
    val candidateGapCount: Int,
    val generatedGuideCount: Int,
    val acceptedGuideCount: Int,
    val duplicateGuideCount: Int,
    val rejectedGuideCount: Int,
    val remainingGapCount: Int,
    val totalAcceptedGuideCount: Int,
)

data class IterativeGuideExpansionResult(
    val guidanceByClass: Map<ClassModel, LlmGenerationGuidance>,
    val planningResult: LlmPlanningResult,
    val adoptionDecisions: List<LlmAdoptionDecision>,
    val iterations: List<GuideIterationResult>,
)
