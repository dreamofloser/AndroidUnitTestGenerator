package io.github.dreamofloser.testgen.llm

import io.github.dreamofloser.testgen.model.GenerationSummary

class LlmReviewAdvisor {
    fun review(summary: GenerationSummary): List<LlmSuggestion> {
        val suggestions = mutableListOf<LlmSuggestion>()

        if (summary.fallbackMethods > 0) {
            suggestions += reviewSuggestion(
                category = "Generated test review",
                recommendation = "Review fallback-generated tests first and replace weak assertions with business-specific expectations.",
                rationale = "Fallback templates intentionally prefer compilable structure over deep behavior assertions.",
            )
        }

        if (summary.skippedClasses.isNotEmpty()) {
            suggestions += reviewSuggestion(
                category = "Skipped source review",
                recommendation = "Inspect skipped classes and decide whether they need parser support, template support, or manual tests.",
                rationale = "Skipped classes are outside the current automatic generation coverage.",
                reviewRequired = true,
            )
        }

        if (summary.coverage == null) {
            suggestions += reviewSuggestion(
                category = "Coverage review",
                recommendation = "Run the target test task with JaCoCo and regenerate the report to attach line, branch, instruction, and method coverage.",
                rationale = "Coverage data helps separate generated-test quantity from actually exercised code.",
                reviewRequired = true,
            )
        }

        if (summary.retrofitApiTests > 0) {
            suggestions += reviewSuggestion(
                category = "Network scenario review",
                recommendation = "Extend Retrofit tests with timeout, malformed body, and authentication-error fixtures when the real API contract is known.",
                rationale = "The current generator covers stable local contract checks; richer network behavior depends on project-specific API semantics.",
                requiresMock = true,
            )
        }

        if (summary.roomDaoTests > 0) {
            suggestions += reviewSuggestion(
                category = "Database scenario review",
                recommendation = "Replace placeholder DAO checks with in-memory Room database tests for insert, query, update, delete, and conflict strategies.",
                rationale = "Room behavior is best verified with a controlled local database fixture.",
                requiresMock = true,
                reviewRequired = true,
            )
        }

        if (summary.generatedAssertions < summary.generatedTestMethods) {
            suggestions += reviewSuggestion(
                category = "Assertion density review",
                recommendation = "Inspect generated tests with low assertion density and add domain-specific assertions.",
                rationale = "A test method without a meaningful assertion may only verify that code does not crash.",
                reviewRequired = true,
            )
        }

        if (suggestions.isEmpty()) {
            suggestions += reviewSuggestion(
                category = "Generated test review",
                recommendation = "No major review issue was detected by the LLM agent rule set for this run.",
                rationale = "Generation counts, assertions, skipped classes, and specialized templates are within the current expected range.",
            )
        }

        return suggestions
    }

    private fun reviewSuggestion(
        category: String,
        recommendation: String,
        rationale: String,
        requiresMock: Boolean = false,
        reviewRequired: Boolean = false,
    ): LlmSuggestion {
        return LlmSuggestion(
            sourceClass = "<generation-report>",
            methodName = null,
            category = category,
            recommendation = recommendation,
            rationale = rationale,
            requiresMock = requiresMock,
            reviewRequired = reviewRequired,
        )
    }
}