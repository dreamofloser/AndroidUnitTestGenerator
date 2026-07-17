package io.github.dreamofloser.testgen.guide

import io.github.dreamofloser.testgen.analysis.TestabilityInsight
import io.github.dreamofloser.testgen.generator.TestScenarioGenerator
import io.github.dreamofloser.testgen.llm.supportedLlmInputStrategies
import io.github.dreamofloser.testgen.model.ClassModel
import io.github.dreamofloser.testgen.model.MethodModel
import io.github.dreamofloser.testgen.model.SourceClassKind
import io.github.dreamofloser.testgen.model.SourceLanguage

class TestCaseGapAnalyzer(
    private val scenarioGenerator: TestScenarioGenerator = TestScenarioGenerator(),
) {
    fun findCandidates(
        model: ClassModel,
        excludedFingerprints: Set<String> = emptySet(),
        insightsByMethod: Map<String, TestabilityInsight> = emptyMap(),
    ): List<TestCaseGuideCandidate> {
        if (model.classKind != SourceClassKind.REGULAR) {
            return emptyList()
        }

        return model.methods
            .filter { method -> model.methods.count { it.name == method.name } == 1 }
            .filter { method -> canGenerateBoundary(model, method) }
            .flatMap { method ->
                method.parameters.flatMap { parameter ->
                    parameter.supportedLlmInputStrategies().map { strategy ->
                        val insight = insightsByMethod[model.methodKey(method)]
                        TestCaseGuideCandidate(
                            sourceClass = model.qualifiedName(),
                            methodName = method.name,
                            targetParameter = parameter.name,
                            inputStrategy = strategy.wireName,
                            difficultyScore = insight?.difficultyScore ?: 0,
                            priorityScore = insight?.priorityScore ?: 0,
                            automationConfidence = insight?.automationConfidence ?: 0,
                            guidePriorityScore = insight?.guidePriorityScore ?: 0,
                        )
                    }
                }
            }
            .filterNot { it.fingerprint in excludedFingerprints }
            .filter { candidate -> expandsGeneratedSuite(model, candidate) }
            .distinctBy { it.fingerprint }
            .sortedWith(
                compareByDescending<TestCaseGuideCandidate> { it.guidePriorityScore }
                    .thenByDescending { it.priorityScore }
                    .thenBy { it.methodName }
                    .thenBy { it.targetParameter },
            )
    }

    private fun canGenerateBoundary(model: ClassModel, method: MethodModel): Boolean {
        if (method.dependencyCalls.isEmpty()) {
            return true
        }
        return model.language == SourceLanguage.KOTLIN && method.returnType.startsWith("Result<")
    }

    private fun expandsGeneratedSuite(
        model: ClassModel,
        candidate: TestCaseGuideCandidate,
    ): Boolean {
        if (model.language == SourceLanguage.KOTLIN) {
            return true
        }

        val method = model.methods.single { it.name == candidate.methodName }
        val baseline = scenarioGenerator.scenariosFor(method)
        val expanded = scenarioGenerator.scenariosFor(
            method = method,
            llmScenarios = listOf(candidate.scenario(iteration = 0)),
        )
        return expanded.size > baseline.size
    }

    private fun ClassModel.qualifiedName(): String {
        return if (packageName.isBlank()) className else "$packageName.$className"
    }

    private fun ClassModel.methodKey(method: MethodModel): String {
        return "${qualifiedName()}#${method.name}".lowercase()
    }
}
