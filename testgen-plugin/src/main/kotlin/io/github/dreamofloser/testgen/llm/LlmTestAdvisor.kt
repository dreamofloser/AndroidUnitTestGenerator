package io.github.dreamofloser.testgen.llm

import io.github.dreamofloser.testgen.model.ClassModel
import io.github.dreamofloser.testgen.model.MethodModel
import io.github.dreamofloser.testgen.model.SourceClassKind

class LlmTestAdvisor(
    private val client: LlmClient,
    private val promptBuilder: LlmPromptBuilder = LlmPromptBuilder(),
    private val responseParser: LlmStructuredResponseParser = LlmStructuredResponseParser(),
    private val progressReporter: (String) -> Unit = {},
) {
    fun advise(classes: List<ClassModel>): List<LlmSuggestion> = analyze(classes).suggestions

    fun analyze(classes: List<ClassModel>): LlmPlanningResult {
        return analyzeWithPrompts(
            classes = classes,
            progressLabel = "LLM analysis",
            promptFactory = { classModel -> promptBuilder.buildPlanningPrompt(classModel) },
        )
    }

    fun analyzeWithPrompts(
        classes: List<ClassModel>,
        progressLabel: String,
        promptFactory: (ClassModel) -> String,
    ): LlmPlanningResult {
        val structuredPlans = mutableListOf<LlmStructuredTestPlan>()
        val suggestions = classes.flatMapIndexed { index, classModel ->
            val progress = "[${index + 1}/${classes.size}]"
            progressReporter("$progressLabel $progress requesting ${classModel.qualifiedName()}")
            val response = client.complete(promptFactory(classModel))
            val structuredPlan = responseParser.parse(classModel.qualifiedName(), response)
            structuredPlans += structuredPlan
            progressReporter("$progressLabel $progress ${structuredPlan.parseStatus}: ${structuredPlan.scenarios.size} scenarios")

            structuredPlanSuggestions(structuredPlan, response) +
                classLevelSuggestions(classModel) +
                classModel.methods.flatMap { method -> methodLevelSuggestions(classModel, method) }
        }.distinctBy { listOf(it.sourceClass, it.methodName ?: "", it.category, it.recommendation).joinToString("|") }

        return LlmPlanningResult(
            suggestions = suggestions,
            structuredPlans = structuredPlans,
        )
    }

    private fun structuredPlanSuggestions(plan: LlmStructuredTestPlan, rawResponse: String): List<LlmSuggestion> {
        if (plan.parseStatus == LlmJsonParseStatus.FALLBACK) {
            return listOf(
                suggestion(
                    sourceClass = plan.sourceClass,
                    methodName = null,
                    category = "JSON fallback",
                    recommendation = LlmHttpSupport.compactAdvice(rawResponse),
                    rationale = plan.parseMessage ?: "The structured LLM response could not be parsed.",
                    reviewRequired = true,
                ),
            )
        }

        val scenarioSuggestions = plan.scenarios.map { scenario ->
            suggestion(
                sourceClass = plan.sourceClass,
                methodName = scenario.methodName,
                category = "LLM ${scenario.category}",
                recommendation = "Test ${scenario.testName}: Given ${scenario.given}; when ${scenario.whenAction}; then ${scenario.then}.",
                rationale = plan.sourceSummary,
                requiresMock = scenario.requiresMock,
            )
        }
        val manualReviewSuggestions = plan.manualReviewNotes.map { note ->
            suggestion(
                sourceClass = plan.sourceClass,
                methodName = null,
                category = "LLM manual review",
                recommendation = note,
                rationale = "The structured LLM plan marked this item for developer review.",
                reviewRequired = true,
            )
        }
        return scenarioSuggestions + manualReviewSuggestions
    }

    private fun classLevelSuggestions(classModel: ClassModel): List<LlmSuggestion> {
        val sourceClass = classModel.qualifiedName()
        return when (classModel.classKind) {
            SourceClassKind.VIEW_MODEL -> listOf(
                suggestion(sourceClass, null, "State planning", "Verify initial state, loading state, success state, and error state transitions.", "ViewModel tests should focus on observable state changes rather than implementation details.", requiresMock = true),
                suggestion(sourceClass, null, "Coroutine control", "Use a test dispatcher and deterministic coroutine scheduling for asynchronous state updates.", "Coroutine timing must be controlled to make generated tests stable."),
            )
            SourceClassKind.RETROFIT_API -> listOf(
                suggestion(sourceClass, null, "Network contract", "Verify HTTP method, path, query names, and local MockWebServer request paths.", "Retrofit APIs are interface contracts, so generated tests should validate annotations and request metadata.", requiresMock = true),
                suggestion(sourceClass, null, "HTTP error path", "Add server error scenarios such as HTTP 500 and empty response body handling when return type allows it.", "Network clients require explicit error-path checks beyond successful responses.", requiresMock = true),
            )
            SourceClassKind.ROOM_DAO -> listOf(
                suggestion(sourceClass, null, "Database fixture", "Prefer an in-memory Room database fixture for insert, query, update, delete, and empty-result cases.", "DAO behavior is more meaningful when verified against a real in-memory database.", reviewRequired = true),
            )
            SourceClassKind.COMPOSE_UI -> listOf(
                suggestion(sourceClass, null, "UI semantics", "Verify stable semantics tags and visible text for key UI states.", "Compose UI tests depend on stable semantics; missing test tags should be reviewed manually.", reviewRequired = true),
            )
            SourceClassKind.ACTIVITY, SourceClassKind.FRAGMENT -> listOf(
                suggestion(sourceClass, null, "Lifecycle", "Verify create, start, resume, pause, stop, destroy, and recreation paths where safe.", "Lifecycle components often fail around recreation and argument restoration.", reviewRequired = true),
            )
            SourceClassKind.DATA -> listOf(
                suggestion(sourceClass, null, "Data contract", "Verify constructor property mapping, equality, copy, and representative boundary values.", "Data classes are usually best tested through value semantics."),
            )
            SourceClassKind.REGULAR -> emptyList()
        }
    }

    private fun methodLevelSuggestions(classModel: ClassModel, method: MethodModel): List<LlmSuggestion> {
        val sourceClass = classModel.qualifiedName()
        val suggestions = mutableListOf<LlmSuggestion>()

        if (method.isSuspend) {
            suggestions += suggestion(sourceClass, method.name, "Coroutine path", "Generate success and failure coroutine paths using runTest.", "Suspend functions need deterministic coroutine execution and exception-path coverage.", requiresMock = method.dependencyCalls.isNotEmpty())
        }

        if (method.parameters.any { it.type.contains("String", ignoreCase = true) }) {
            suggestions += suggestion(sourceClass, method.name, "Boundary input", "Add blank string, empty string, and representative valid string cases.", "String inputs commonly carry validation and formatting edge cases.")
        }

        if (method.parameters.any { it.type.endsWith("?") || it.type.contains("Nullable", ignoreCase = true) }) {
            suggestions += suggestion(sourceClass, method.name, "Nullability", "Add a null-input scenario or mark the method for manual review if null is not accepted.", "Nullable parameters should be covered explicitly to avoid hidden NPE paths.", reviewRequired = true)
        }

        if (method.returnType.contains("Response<") || method.returnType.startsWith("Response")) {
            suggestions += suggestion(sourceClass, method.name, "HTTP response", "Cover successful response, HTTP 500 response, and empty-body response when feasible.", "Retrofit Response<T> exposes status and body separately, so both should be tested.", requiresMock = true)
        }

        if (method.httpMethod != null) {
            suggestions += suggestion(sourceClass, method.name, "Endpoint metadata", "Check @${method.httpMethod} path '${method.httpPath.orEmpty()}' and query parameters ${method.httpQueryNames.joinToString()}.", "Endpoint annotation metadata is part of the API contract.", requiresMock = true)
        }

        if (method.dependencyCalls.isNotEmpty()) {
            suggestions += suggestion(sourceClass, method.name, "Mock strategy", "Stub dependency calls and verify important interactions after invoking this method.", "Dependency interactions are a practical unit-test boundary for service and repository classes.", requiresMock = true)
        }

        if (method.thrownExceptions.isNotEmpty() || method.thrownStatementTypes.isNotEmpty()) {
            suggestions += suggestion(sourceClass, method.name, "Exception path", "Generate assertThrows coverage for declared or detected exception branches.", "Detected throw statements should be represented in generated tests.")
        }

        return suggestions
    }

    private fun suggestion(
        sourceClass: String,
        methodName: String?,
        category: String,
        recommendation: String,
        rationale: String,
        requiresMock: Boolean = false,
        reviewRequired: Boolean = false,
    ): LlmSuggestion {
        return LlmSuggestion(
            sourceClass = sourceClass,
            methodName = methodName,
            category = category,
            recommendation = recommendation,
            rationale = rationale,
            requiresMock = requiresMock,
            reviewRequired = reviewRequired,
        )
    }

    private fun ClassModel.qualifiedName(): String {
        return if (packageName.isBlank()) className else "$packageName.$className"
    }
}
