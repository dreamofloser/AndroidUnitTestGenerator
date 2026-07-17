package io.github.dreamofloser.testgen.llm

import io.github.dreamofloser.testgen.guide.GuideIterationResult

data class LlmAgentConfig(
    val enabled: Boolean,
    val provider: String,
    val model: String,
    val agentMode: String,
    val endpoint: String,
    val apiKeyEnv: String,
)

data class LlmSuggestion(
    val sourceClass: String,
    val methodName: String?,
    val category: String,
    val recommendation: String,
    val rationale: String,
    val requiresMock: Boolean = false,
    val reviewRequired: Boolean = false,
)

data class LlmStructuredTestPlan(
    val sourceClass: String,
    val sourceSummary: String,
    val scenarios: List<LlmTestScenario>,
    val mockStrategies: List<LlmMockStrategy>,
    val manualReviewNotes: List<String>,
    val parseStatus: LlmJsonParseStatus,
    val parseMessage: String? = null,
    val iteration: Int = 0,
)

data class LlmTestScenario(
    val methodName: String?,
    val category: String,
    val testName: String,
    val given: String,
    val whenAction: String,
    val then: String,
    val requiresMock: Boolean,
    val targetParameter: String? = null,
    val inputStrategy: String? = null,
)

data class LlmMockStrategy(
    val dependency: String,
    val approach: String,
    val reason: String,
)

enum class LlmJsonParseStatus {
    SUCCESS,
    FALLBACK,
}

enum class LlmAdoptionStatus {
    GENERATED,
    DUPLICATE,
    RULE_COVERED,
    MANUAL_REVIEW,
    UNSUPPORTED,
    INVALID_METHOD,
}

data class LlmAdoptionDecision(
    val sourceClass: String,
    val methodName: String?,
    val testName: String,
    val category: String,
    val status: LlmAdoptionStatus,
    val reason: String,
    val targetParameter: String? = null,
    val inputStrategy: String? = null,
    val iteration: Int = 0,
)

data class LlmGenerationGuidance(
    val acceptedScenarios: List<LlmTestScenario> = emptyList(),
    val decisions: List<LlmAdoptionDecision> = emptyList(),
)

data class LlmPlanningResult(
    val suggestions: List<LlmSuggestion>,
    val structuredPlans: List<LlmStructuredTestPlan>,
)

data class LlmAgentReport(
    val config: LlmAgentConfig,
    val planningSuggestions: List<LlmSuggestion>,
    val reviewSuggestions: List<LlmSuggestion>,
    val structuredPlans: List<LlmStructuredTestPlan> = emptyList(),
    val adoptionDecisions: List<LlmAdoptionDecision> = emptyList(),
    val guideIterations: List<GuideIterationResult> = emptyList(),
) {
    val totalSuggestions: Int
        get() = planningSuggestions.size + reviewSuggestions.size

    val adoptedScenarioCount: Int
        get() = adoptionDecisions.count { it.status == LlmAdoptionStatus.GENERATED }
}
