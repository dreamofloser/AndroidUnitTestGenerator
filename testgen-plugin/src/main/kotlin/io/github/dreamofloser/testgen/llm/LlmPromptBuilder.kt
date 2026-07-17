package io.github.dreamofloser.testgen.llm

import io.github.dreamofloser.testgen.guide.TestCaseGuideCandidate
import io.github.dreamofloser.testgen.model.ClassModel
import io.github.dreamofloser.testgen.model.MethodModel
import io.github.dreamofloser.testgen.model.SourceClassKind
import io.github.dreamofloser.testgen.model.SourceLanguage

class LlmPromptBuilder {
    fun buildGuidePrompt(
        classModel: ClassModel,
        iteration: Int,
        candidates: List<TestCaseGuideCandidate>,
        acceptedFingerprints: Set<String>,
        maxGuides: Int,
    ): String {
        val candidateText = candidates.joinToString(separator = "\n") { candidate ->
            "- methodName=${candidate.methodName}, targetParameter=${candidate.targetParameter}, " +
                "inputStrategy=${candidate.inputStrategy}, priority=${candidate.priorityScore}, " +
                "difficulty=${candidate.difficultyScore}, confidence=${candidate.automationConfidence}, " +
                "guideValue=${candidate.guidePriorityScore}"
        }
        val acceptedText = acceptedFingerprints.sorted().joinToString().ifBlank { "none" }

        return """
            You are a test-case guide generator expanding an existing Android unit-test suite.

            This is expansion iteration $iteration of 2.
            Select exactly ${maxGuides.coerceAtMost(candidates.size)} high-value guide from the supplied uncovered choices.
            The choices were prevalidated against real source methods and generator capabilities.
            Prefer a higher guideValue. It combines test priority, automation confidence, and generation difficulty.
            Use methodName, targetParameter, and inputStrategy exactly as supplied.
            Do not reuse an accepted guide and do not invent methods, parameters, strategies, dependencies, or behavior.
            Only use category "boundary".
            Return JSON only. Do not output Markdown, comments, explanations, or reasoning.

            Required JSON shape:
            {
              "sourceSummary": "short factual summary",
              "scenarios": [{
                "methodName": "exact supplied method name",
                "category": "boundary",
                "testName": "short valid identifier",
                "targetParameter": "exact supplied parameter name",
                "inputStrategy": "exact supplied strategy",
                "given": "selected input condition",
                "when": "method invocation",
                "then": "observable result",
                "requiresMock": false
              }],
              "mockStrategies": [],
              "manualReviewNotes": []
            }

            Source class: ${classModel.packageName}.${classModel.className}
            Source kind: ${classModel.classKind}
            Language: ${classModel.language}
            Previously accepted guide fingerprints: $acceptedText
            Uncovered choices:
            $candidateText
        """.trimIndent()
    }

    fun buildPlanningPrompt(classModel: ClassModel): String {
        val methods = classModel.methods.joinToString(separator = "\n") { method ->
            val parameters = method.parameters.joinToString { "${it.name}: ${it.type}" }
            val choices = if (classModel.canGenerateLlmBoundary(method)) {
                method.parameters.flatMap { parameter ->
                    parameter.supportedLlmInputStrategies().map { strategy ->
                        "${parameter.name}=${strategy.wireName}"
                    }
                }
            } else {
                emptyList()
            }
            val choiceText = choices.joinToString().ifBlank { "none" }
            "- ${method.name}($parameters): ${method.returnType} | allowed choices: $choiceText"
        }.ifBlank { "- No public methods were detected." }

        return """
            You are an Android unit-test scenario selection agent.

            Select at most 2 useful boundary scenarios from the supplied allowed choices.
            Every selected scenario will be converted into real Java or Kotlin test code.
            Use methodName, targetParameter, and inputStrategy exactly as supplied.
            Do not invent methods, parameters, strategies, dependencies, exceptions, or behavior.
            Only use category "boundary".
            Do not select a method whose allowed choices are "none".
            Prefer choices that add useful coverage beyond a normal happy path.
            If no reliable allowed choice exists, return an empty scenarios array.
            Return JSON only. Do not output Markdown, comments, explanations, or reasoning.

            Required JSON shape:
            {
              "sourceSummary": "short factual summary",
              "scenarios": [{
                "methodName": "exact supplied method name",
                "category": "boundary",
                "testName": "valid identifier",
                "targetParameter": "exact supplied parameter name",
                "inputStrategy": "exact supplied strategy",
                "given": "selected boundary input",
                "when": "method invocation",
                "then": "observable result",
                "requiresMock": false
              }],
              "mockStrategies": [],
              "manualReviewNotes": []
            }

            Source class: ${classModel.packageName}.${classModel.className}
            Source kind: ${classModel.classKind}
            Language: ${classModel.language}
            Methods and allowed choices:
            $methods
        """.trimIndent()
    }

    private fun ClassModel.canGenerateLlmBoundary(method: MethodModel): Boolean {
        if (classKind != SourceClassKind.REGULAR) {
            return false
        }
        if (methods.count { it.name == method.name } != 1) {
            return false
        }
        if (method.dependencyCalls.isEmpty()) {
            return true
        }
        return language == SourceLanguage.KOTLIN && method.returnType.startsWith("Result<")
    }
}
