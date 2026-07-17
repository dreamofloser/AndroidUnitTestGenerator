package io.github.dreamofloser.testgen.guide

import io.github.dreamofloser.testgen.analysis.TestabilityInsight
import io.github.dreamofloser.testgen.llm.LlmAdoptionDecision
import io.github.dreamofloser.testgen.llm.LlmAdoptionStatus
import io.github.dreamofloser.testgen.llm.LlmGenerationGuidance
import io.github.dreamofloser.testgen.llm.LlmPlanningResult
import io.github.dreamofloser.testgen.llm.LlmScenarioAdopter
import io.github.dreamofloser.testgen.llm.LlmTestScenario
import io.github.dreamofloser.testgen.model.ClassModel

class IterativeTestSuiteExpander(
    private val guideGenerator: TestCaseGuideGenerator,
    private val gapAnalyzer: TestCaseGapAnalyzer = TestCaseGapAnalyzer(),
    private val scenarioAdopter: LlmScenarioAdopter = LlmScenarioAdopter(),
) {
    fun expand(
        classes: List<ClassModel>,
        maxIterations: Int = 2,
        maxGuidesPerClass: Int = 1,
        insights: List<TestabilityInsight> = emptyList(),
    ): IterativeGuideExpansionResult {
        require(maxIterations > 0) { "maxIterations must be greater than zero." }
        require(maxGuidesPerClass > 0) { "maxGuidesPerClass must be greater than zero." }

        val acceptedByClass = mutableMapOf<String, MutableList<LlmTestScenario>>()
        val acceptedFingerprints = mutableSetOf<String>()
        val allDecisions = mutableListOf<LlmAdoptionDecision>()
        val allSuggestions = mutableListOf<io.github.dreamofloser.testgen.llm.LlmSuggestion>()
        val allPlans = mutableListOf<io.github.dreamofloser.testgen.llm.LlmStructuredTestPlan>()
        val iterationResults = mutableListOf<GuideIterationResult>()
        val insightsByMethod = insights.associateBy { it.methodKey }

        for (iteration in 1..maxIterations) {
            val requests = classes.mapNotNull { classModel ->
                val candidates = gapAnalyzer.findCandidates(
                    model = classModel,
                    excludedFingerprints = acceptedFingerprints,
                    insightsByMethod = insightsByMethod,
                )
                if (candidates.isEmpty()) {
                    null
                } else {
                    TestCaseGuideRequest(
                        classModel = classModel,
                        candidates = candidates,
                        acceptedFingerprints = acceptedFingerprints.toSet(),
                    )
                }
            }
            val candidateGapCount = requests.sumOf { it.candidates.size }
            if (requests.isEmpty()) {
                iterationResults += GuideIterationResult(
                    iteration = iteration,
                    status = GuideIterationStatus.NO_GAPS,
                    requestedClassCount = 0,
                    candidateGapCount = 0,
                    generatedGuideCount = 0,
                    acceptedGuideCount = 0,
                    duplicateGuideCount = 0,
                    rejectedGuideCount = 0,
                    remainingGapCount = 0,
                    totalAcceptedGuideCount = acceptedFingerprints.size,
                )
                continue
            }

            val generation = guideGenerator.generate(
                iteration = iteration,
                requests = requests,
                maxGuidesPerClass = maxGuidesPerClass,
            )
            allSuggestions += generation.planningResult.suggestions
            allPlans += generation.planningResult.structuredPlans

            var acceptedCount = 0
            var duplicateCount = 0
            var rejectedCount = 0
            requests.forEach { request ->
                val sourceClass = request.classModel.qualifiedName()
                val plan = generation.planningResult.structuredPlans
                    .firstOrNull { it.sourceClass == sourceClass }
                val guidance = scenarioAdopter.adopt(request.classModel, plan)
                val acceptedByTestName = guidance.acceptedScenarios.associateBy { it.testName }

                guidance.decisions.forEach { decision ->
                    val acceptedScenario = acceptedByTestName[decision.testName]
                    if (decision.status == LlmAdoptionStatus.GENERATED && acceptedScenario != null) {
                        val guide = TestCaseGuide(iteration, sourceClass, acceptedScenario)
                        if (!acceptedFingerprints.add(guide.fingerprint)) {
                            duplicateCount++
                            allDecisions += decision.copy(
                                status = LlmAdoptionStatus.DUPLICATE,
                                reason = "The same method, parameter, and input strategy were already accepted in an earlier iteration.",
                            )
                        } else {
                            acceptedCount++
                            acceptedByClass.getOrPut(sourceClass) { mutableListOf() } += acceptedScenario
                            allDecisions += decision
                        }
                    } else {
                        rejectedCount++
                        allDecisions += decision
                    }
                }
            }

            val remainingGapCount = classes.sumOf { classModel ->
                gapAnalyzer.findCandidates(
                    model = classModel,
                    excludedFingerprints = acceptedFingerprints,
                    insightsByMethod = insightsByMethod,
                ).size
            }
            iterationResults += GuideIterationResult(
                iteration = iteration,
                status = if (acceptedCount > 0) GuideIterationStatus.EXPANDED else GuideIterationStatus.NO_NEW_GUIDES,
                requestedClassCount = requests.size,
                candidateGapCount = candidateGapCount,
                generatedGuideCount = generation.guides.size,
                acceptedGuideCount = acceptedCount,
                duplicateGuideCount = duplicateCount,
                rejectedGuideCount = rejectedCount,
                remainingGapCount = remainingGapCount,
                totalAcceptedGuideCount = acceptedFingerprints.size,
            )
        }

        val guidanceByClass = classes.associateWith { classModel ->
            val sourceClass = classModel.qualifiedName()
            LlmGenerationGuidance(
                acceptedScenarios = acceptedByClass[sourceClass].orEmpty(),
                decisions = allDecisions.filter { it.sourceClass == sourceClass },
            )
        }
        return IterativeGuideExpansionResult(
            guidanceByClass = guidanceByClass,
            planningResult = LlmPlanningResult(
                suggestions = allSuggestions.distinctBy {
                    listOf(it.sourceClass, it.methodName.orEmpty(), it.category, it.recommendation)
                        .joinToString("|")
                },
                structuredPlans = allPlans,
            ),
            adoptionDecisions = allDecisions,
            iterations = iterationResults,
        )
    }

    private fun ClassModel.qualifiedName(): String {
        return if (packageName.isBlank()) className else "$packageName.$className"
    }
}
