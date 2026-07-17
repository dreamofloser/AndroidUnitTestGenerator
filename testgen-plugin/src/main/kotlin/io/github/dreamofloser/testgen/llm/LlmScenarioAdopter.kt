package io.github.dreamofloser.testgen.llm

import io.github.dreamofloser.testgen.model.ClassModel
import io.github.dreamofloser.testgen.model.MethodModel
import io.github.dreamofloser.testgen.model.ParameterModel
import io.github.dreamofloser.testgen.model.SourceClassKind
import io.github.dreamofloser.testgen.model.SourceLanguage

class LlmScenarioAdopter {
    fun adopt(model: ClassModel, plan: LlmStructuredTestPlan?): LlmGenerationGuidance {
        if (plan == null || plan.parseStatus != LlmJsonParseStatus.SUCCESS) {
            return LlmGenerationGuidance()
        }

        val outcomes = plan.scenarios.map { scenario -> decide(model, scenario) }
        return LlmGenerationGuidance(
            acceptedScenarios = outcomes.mapNotNull { it.acceptedScenario },
            decisions = outcomes.map { it.decision.copy(iteration = plan.iteration) },
        )
    }

    private fun decide(model: ClassModel, scenario: LlmTestScenario): AdoptionOutcome {
        val methodName = scenario.methodName
        if (methodName.isNullOrBlank()) {
            return rejected(model, scenario, LlmAdoptionStatus.MANUAL_REVIEW, "Class-level advice cannot be converted into a method test safely.")
        }

        val method = model.methods.singleOrNull { it.name == methodName }
            ?: return rejected(model, scenario, LlmAdoptionStatus.INVALID_METHOD, "The suggested method does not exist or is overloaded ambiguously.")

        val category = scenario.category.trim().lowercase()
        if (isCoveredBySpecializedGenerator(model.classKind, category)) {
            return rejected(model, scenario, LlmAdoptionStatus.RULE_COVERED, "The existing specialized generator already covers this scenario family.")
        }

        if (category != "boundary") {
            return decideNonBoundary(model, method, scenario, category)
        }

        if (method.dependencyCalls.isNotEmpty() &&
            (
                model.language != SourceLanguage.KOTLIN ||
                    !method.returnType.startsWith("Result<")
                )
        ) {
            return rejected(model, scenario, LlmAdoptionStatus.RULE_COVERED, "Dependency interaction templates take precedence unless a Kotlin Result<T> wrapper can be stubbed deterministically.")
        }

        val requestedStrategy = LlmInputStrategy.fromWireName(scenario.inputStrategy)
        if (!scenario.inputStrategy.isNullOrBlank() && requestedStrategy == null) {
            return rejected(model, scenario, LlmAdoptionStatus.UNSUPPORTED, "The requested input strategy is not supported.")
        }

        val selected = selectBoundary(method, scenario.targetParameter, requestedStrategy)
            ?: return rejected(model, scenario, LlmAdoptionStatus.UNSUPPORTED, "The target parameter and input strategy cannot be converted to a supported value.")

        val acceptedScenario = scenario.copy(
            category = "boundary",
            targetParameter = selected.parameter.name,
            inputStrategy = selected.strategy.wireName,
        )
        return AdoptionOutcome(
            acceptedScenario = acceptedScenario,
            decision = decision(
                model = model,
                scenario = acceptedScenario,
                status = LlmAdoptionStatus.GENERATED,
                reason = "Accepted as an LLM-selected boundary input; the deterministic generator will emit the test code.",
            ),
        )
    }

    private fun decideNonBoundary(
        model: ClassModel,
        method: MethodModel,
        scenario: LlmTestScenario,
        category: String,
    ): AdoptionOutcome {
        if (category == "happy-path") {
            return rejected(model, scenario, LlmAdoptionStatus.RULE_COVERED, "The existing default or success template already covers the happy path.")
        }
        if (category == "error") {
            val detectsException = method.thrownExceptions.isNotEmpty() || method.thrownStatementTypes.isNotEmpty()
            return if (detectsException) {
                rejected(model, scenario, LlmAdoptionStatus.RULE_COVERED, "The rule engine already generates the detected exception path.")
            } else {
                rejected(model, scenario, LlmAdoptionStatus.MANUAL_REVIEW, "No source-level exception evidence exists for a safe error template.")
            }
        }
        return rejected(model, scenario, LlmAdoptionStatus.UNSUPPORTED, "The category is not mapped to an LLM-guided code template.")
    }

    private fun selectBoundary(
        method: MethodModel,
        targetParameter: String?,
        requestedStrategy: LlmInputStrategy?,
    ): SelectedBoundary? {
        val parameter = if (!targetParameter.isNullOrBlank()) {
            method.parameters.singleOrNull { it.name == targetParameter } ?: return null
        } else {
            method.parameters.firstOrNull { parameter ->
                requestedStrategy?.let { it in parameter.supportedLlmInputStrategies() }
                    ?: parameter.supportedLlmInputStrategies().isNotEmpty()
            } ?: return null
        }

        val supportedStrategies = parameter.supportedLlmInputStrategies()
        val strategy = (
            requestedStrategy?.takeIf { it in supportedStrategies }
                ?: if (requestedStrategy == null) supportedStrategies.firstOrNull() else null
            ) ?: return null

        return SelectedBoundary(parameter, strategy)
    }

    private fun rejected(
        model: ClassModel,
        scenario: LlmTestScenario,
        status: LlmAdoptionStatus,
        reason: String,
    ): AdoptionOutcome {
        return AdoptionOutcome(
            acceptedScenario = null,
            decision = decision(model, scenario, status, reason),
        )
    }

    private fun decision(
        model: ClassModel,
        scenario: LlmTestScenario,
        status: LlmAdoptionStatus,
        reason: String,
    ): LlmAdoptionDecision {
        return LlmAdoptionDecision(
            sourceClass = model.qualifiedName(),
            methodName = scenario.methodName,
            testName = scenario.testName,
            category = scenario.category,
            status = status,
            reason = reason,
            targetParameter = scenario.targetParameter,
            inputStrategy = scenario.inputStrategy,
        )
    }

    private fun isCoveredBySpecializedGenerator(kind: SourceClassKind, category: String): Boolean {
        return when (kind) {
            SourceClassKind.VIEW_MODEL -> category in setOf("state", "coroutine", "error", "boundary")
            SourceClassKind.RETROFIT_API -> category in setOf("network", "error", "boundary")
            SourceClassKind.ROOM_DAO -> category in setOf("database", "error", "boundary")
            SourceClassKind.ACTIVITY, SourceClassKind.FRAGMENT -> category in setOf("lifecycle", "boundary")
            SourceClassKind.COMPOSE_UI -> category in setOf("state", "boundary")
            SourceClassKind.DATA -> category in setOf("happy-path", "boundary")
            SourceClassKind.REGULAR -> false
        }
    }

    private fun ClassModel.qualifiedName(): String {
        return if (packageName.isBlank()) className else "$packageName.$className"
    }

    private data class SelectedBoundary(
        val parameter: ParameterModel,
        val strategy: LlmInputStrategy,
    )

    private data class AdoptionOutcome(
        val acceptedScenario: LlmTestScenario?,
        val decision: LlmAdoptionDecision,
    )
}
