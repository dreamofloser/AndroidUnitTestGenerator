package io.github.dreamofloser.testgen.report

import io.github.dreamofloser.testgen.analysis.DifficultyDriver
import io.github.dreamofloser.testgen.analysis.TestValueQuadrant
import io.github.dreamofloser.testgen.analysis.TestabilityInsight
import io.github.dreamofloser.testgen.llm.LlmSuggestion
import io.github.dreamofloser.testgen.model.GeneratedClassResult
import io.github.dreamofloser.testgen.model.GenerationSummary
import java.io.File
import java.util.Locale

class MarkdownReportWriter {
    fun write(summary: GenerationSummary, outputDir: File): File {
        outputDir.mkdirs()

        val reportFile = outputDir.resolve("report.md")
        reportFile.writeText(buildMarkdown(summary))
        return reportFile
    }

    private fun buildMarkdown(summary: GenerationSummary): String {
        return buildString {
            appendLine("# Android Unit Test Generator Report")
            appendLine()
            appendLine("## Summary")
            appendLine()
            appendLine("| Metric | Value |")
            appendLine("| --- | ---: |")
            appendLine("| Scanned source files | ${summary.scannedFiles} |")
            appendLine("| Parsed classes | ${summary.parsedClasses} |")
            appendLine("| Generated test classes | ${summary.generatedClasses.size} |")
            appendLine("| Generated test methods | ${summary.generatedTestMethods} |")
            appendLine("| Generated assertions | ${summary.generatedAssertions} |")
            appendLine("| Generation quality score | ${summary.qualityScore()}/100 |")
            appendLine("| Rule matched methods | ${summary.ruleMatchedMethods} |")
            appendLine("| Fallback methods | ${summary.fallbackMethods} |")
            appendLine("| Mocked dependencies | ${summary.mockedDependencies} |")
            appendLine("| Mockito stubs | ${summary.mockStubs} |")
            appendLine("| Mockito verifications | ${summary.mockVerifications} |")
            appendLine("| LiveData rules | ${summary.liveDataRules} |")
            appendLine("| Robolectric test classes | ${summary.robolectricTests} |")
            appendLine("| Android imports | ${summary.androidImports} |")
            appendLine("| Compose UI test classes | ${summary.composeTests} |")
            appendLine("| Room DAO test classes | ${summary.roomDaoTests} |")
            appendLine("| Retrofit API test classes | ${summary.retrofitApiTests} |")
            appendLine("| LLM agent suggestions | ${summary.llmAgentReport?.totalSuggestions ?: 0} |")
            appendLine("| LLM adopted test methods | ${summary.llmAdoptedMethods} |")
            appendLine("| Guide expansion iterations | ${summary.llmAgentReport?.guideIterations?.size ?: 0} |")
            appendLine("| Accepted test-case guides | ${summary.llmAgentReport?.guideIterations?.sumOf { it.acceptedGuideCount } ?: 0} |")
            appendLine("| Analyzed test targets | ${summary.testabilityInsights.size} |")
            appendLine("| Average generation difficulty | ${summary.averageDifficulty()} |")
            appendLine("| High-priority test targets | ${summary.testabilityInsights.count { it.priorityScore >= 60 }} |")
            appendLine("| Automation-ready targets | ${summary.testabilityInsights.count { it.automationConfidence >= 70 }} |")
            appendLine("| Skipped classes | ${summary.skippedClasses.size} |")
            appendLine()
            appendQualityGates(summary)
            appendLine()
            appendGeneratedClassMix(summary)
            appendLine()
            appendTestabilityInsights(summary)
            appendLine()
            appendRiskReview(summary)
            appendLine()
            appendLlmAgentSuggestions(summary)
            appendLine()
            appendValidationCommands()
            appendLine()
            appendCoverage(summary)
            appendLine()
            appendLine("## Generated Classes")
            appendLine()

            if (summary.generatedClasses.isEmpty()) {
                appendLine("No test classes were generated.")
            } else {
                appendLine("| Source class | Language | Kind | Test class | Test methods | Assertions | Mocked dependencies | Mockito stubs | Mockito verifications | LiveData rules | Robolectric | Android imports | Compose UI | Room DAO | Retrofit API | Fallback methods | File |")
                appendLine("| --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |")
                summary.generatedClasses.forEach {
                    appendLine(
                        "| ${it.sourceClass} | ${it.sourceLanguage} | ${it.sourceClassKind} | ${it.testClass} | ${it.generatedMethodCount} | ${it.assertionCount} | ${it.mockedDependencyCount} | ${it.mockStubCount} | ${it.mockVerificationCount} | ${it.liveDataRuleCount} | ${it.robolectricTestCount} | ${it.androidImportCount} | ${it.composeTestCount} | ${it.roomDaoTestCount} | ${it.retrofitApiTestCount} | ${it.fallbackMethodCount} | `${it.testFile}` |",
                    )
                }
            }

            appendLine()
            appendLine("## Skipped Classes")
            appendLine()

            if (summary.skippedClasses.isEmpty()) {
                appendLine("No classes were skipped.")
            } else {
                appendLine("| Source class | Reason |")
                appendLine("| --- | --- |")
                summary.skippedClasses.forEach {
                    appendLine("| ${it.sourceClass} | ${it.reason} |")
                }
                appendLine()
                appendLine("### Skipped Reason Summary")
                appendLine()
                appendLine("| Reason | Count |")
                appendLine("| --- | ---: |")
                summary.skippedClasses
                    .groupingBy { it.reason }
                    .eachCount()
                    .toSortedMap()
                    .forEach { (reason, count) -> appendLine("| $reason | $count |") }
            }
        }
    }

    private fun StringBuilder.appendQualityGates(summary: GenerationSummary) {
        appendLine("## Quality Gates")
        appendLine()
        appendLine("| Gate | Status | Detail |")
        appendLine("| --- | --- | --- |")
        appendLine("| Source parsing | ${status(summary.parsedClasses > 0)} | Parsed ${summary.parsedClasses} classes from ${summary.scannedFiles} source files. |")
        appendLine("| Test generation | ${status(summary.generatedClasses.isNotEmpty())} | Generated ${summary.generatedClasses.size} test classes. |")
        appendLine("| Assertions | ${status(summary.generatedAssertions > 0)} | Generated ${summary.generatedAssertions} assertions. |")
        appendLine("| Fallback usage | ${status(summary.fallbackMethods == 0)} | ${summary.fallbackMethods} fallback methods were generated. |")
        appendLine("| Skipped classes | ${status(summary.skippedClasses.isEmpty())} | ${summary.skippedClasses.size} classes were skipped. |")
        appendLine("| Coverage input | ${status(summary.coverage != null)} | ${if (summary.coverage == null) "No JaCoCo XML report was available." else "JaCoCo XML report was loaded."} |")
    }

    private fun StringBuilder.appendGeneratedClassMix(summary: GenerationSummary) {
        appendLine("## Generated Class Mix")
        appendLine()
        if (summary.generatedClasses.isEmpty()) {
            appendLine("No generated classes are available for mix analysis.")
            return
        }

        appendLine("### By Language")
        appendLine()
        appendCountTable(summary.generatedClasses.groupingBy { it.sourceLanguage.name }.eachCount())
        appendLine()
        appendLine("### By Source Kind")
        appendLine()
        appendCountTable(summary.generatedClasses.groupingBy { it.sourceClassKind.name }.eachCount())
    }

    private fun StringBuilder.appendRiskReview(summary: GenerationSummary) {
        appendLine("## Risk Review")
        appendLine()
        val risks = buildList {
            if (summary.fallbackMethods > 0) {
                add("Fallback templates were used. Review those generated tests first because assertions may be intentionally conservative.")
            }
            if (summary.skippedClasses.isNotEmpty()) {
                add("Some source classes were skipped. Check the skipped section before treating the run as complete.")
            }
            if (summary.coverage == null) {
                add("Coverage was not attached. Run the project test task with JaCoCo before using this report as a coverage artifact.")
            }
            if (summary.composeTests > 0) {
                add("Compose UI tests depend on stable semantics and test tags. UI behavior beyond the detected nodes still needs manual review.")
            }
            if (summary.retrofitApiTests > 0) {
                add("Retrofit API tests include mocked contract checks, annotation metadata checks, MockWebServer request checks, and Response<T> HTTP error checks. Non-Response<T> exception paths, rich response bodies, and JSON fixtures are still limited.")
            }
            if (summary.roomDaoTests > 0) {
                add("Room DAO tests should eventually be backed by an in-memory database fixture.")
            }
            val guidedTargets = summary.testabilityInsights.count {
                it.priorityScore >= 60 && it.automationConfidence < 60
            }
            if (guidedTargets > 0) {
                add("$guidedTargets high-priority targets have automation confidence below 60 and should receive guided generation or manual review.")
            }
        }

        if (risks.isEmpty()) {
            appendLine("No major generation risks were detected by the current rule set.")
            return
        }

        risks.forEach { appendLine("- $it") }
    }

    private fun StringBuilder.appendTestabilityInsights(summary: GenerationSummary) {
        appendLine("## Testability Insights")
        appendLine()
        val insights = summary.testabilityInsights
        if (insights.isEmpty()) {
            appendLine("No source methods were available for testability analysis.")
            return
        }

        val hardest = insights.maxBy { it.difficultyScore }
        val highestPriority = insights.maxBy { it.priorityScore }
        val priorityAutomation = insights.count {
            it.quadrant == TestValueQuadrant.PRIORITY_AUTOMATION
        }
        val guidedGeneration = insights.count {
            it.quadrant == TestValueQuadrant.GUIDED_GENERATION
        }
        val dominantDriver = insights
            .flatMap { it.evidence }
            .groupBy { it.driver }
            .maxByOrNull { (_, evidence) -> evidence.sumOf { it.points } }
            ?.key

        appendLine("### Key Findings")
        appendLine()
        appendLine(
            "- Hardest target: `${hardest.sourceClass}#${hardest.methodName}` " +
                "${scoreBar(hardest.difficultyScore)} ${hardest.difficultyLevel}.",
        )
        appendLine(
            "- Highest test priority: `${highestPriority.sourceClass}#${highestPriority.methodName}` " +
                "${scoreBar(highestPriority.priorityScore)} ${highestPriority.priorityLevel}.",
        )
        appendLine("- Priority automation opportunities: $priorityAutomation.")
        appendLine("- Guided-generation targets: $guidedGeneration.")
        appendLine("- Dominant difficulty driver: ${dominantDriver?.displayName ?: "None"}.")
        appendLine()

        appendLine("### Test Value Matrix")
        appendLine()
        appendLine("| Quadrant | Targets | Meaning | Recommended action |")
        appendLine("| --- | ---: | --- | --- |")
        TestValueQuadrant.values().forEach { quadrant ->
            appendLine(
                "| ${quadrant.displayName} | ${insights.count { it.quadrant == quadrant }} | " +
                    "${quadrantMeaning(quadrant)} | ${quadrantAction(quadrant)} |",
            )
        }
        appendLine()

        appendLine("### Method Ranking")
        appendLine()
        appendLine("| Source target | Difficulty | Priority | Confidence | Value quadrant | Recommended strategy | Main evidence | Boundary focus |")
        appendLine("| --- | ---: | ---: | ---: | --- | --- | --- | --- |")
        insights
            .sortedWith(
                compareByDescending<TestabilityInsight> { it.priorityScore }
                    .thenByDescending { it.difficultyScore }
                    .thenBy { it.sourceClass }
                    .thenBy { it.methodName },
            )
            .take(25)
            .forEach { insight ->
                val evidence = insight.evidence
                    .joinToString("; ") { "${it.driver.displayName} +${it.points}" }
                    .ifBlank { "No structural difficulty drivers" }
                appendLine(
                    "| `${insight.sourceClass}#${insight.methodName}` | " +
                        "${insight.difficultyScore} ${insight.difficultyLevel} | " +
                        "${insight.priorityScore} ${insight.priorityLevel} | " +
                        "${insight.automationConfidence} | ${insight.quadrant.displayName} | " +
                        "${insight.recommendedStrategy.displayName} | $evidence | " +
                        "${insight.boundaryFocus.take(5).joinToString(", ")} |",
                )
            }
        if (insights.size > 25) {
            appendLine()
            appendLine("Only the first 25 ranked targets are shown. Total analyzed targets: ${insights.size}.")
        }
        appendLine()

        appendLine("### Difficulty Driver Distribution")
        appendLine()
        appendLine("| Driver | Score contribution | Affected targets |")
        appendLine("| --- | ---: | ---: |")
        val evidenceByDriver = insights.flatMap { it.evidence }.groupBy { it.driver }
        DifficultyDriver.values()
            .map { driver ->
                val evidence = evidenceByDriver[driver].orEmpty()
                Triple(driver, evidence.sumOf { it.points }, evidence.size)
            }
            .filter { (_, points, _) -> points > 0 }
            .sortedByDescending { (_, points, _) -> points }
            .forEach { (driver, points, affectedTargets) ->
                appendLine("| ${driver.displayName} | $points | $affectedTargets |")
            }
        appendLine()
        appendLine(
            "Scores are deterministic static-analysis heuristics. Every difficulty point is backed by parsed source evidence; " +
                "the score is not generated by the LLM.",
        )
    }

    private fun StringBuilder.appendLlmAgentSuggestions(summary: GenerationSummary) {
        appendLine("## LLM Agent Suggestions")
        appendLine()

        val report = summary.llmAgentReport
        if (report == null) {
            appendLine("LLM agent was disabled for this generation run.")
            return
        }

        appendLine("| Config | Value |")
        appendLine("| --- | --- |")
        appendLine("| Provider | ${report.config.provider} |")
        appendLine("| Model | ${report.config.model} |")
        appendLine("| Agent mode | ${report.config.agentMode} |")
        appendLine("| Endpoint configured | ${yesNo(report.config.endpoint.isNotBlank())} |")
        appendLine("| API key env | ${report.config.apiKeyEnv} |")
        appendLine()

        appendLine("### Iterative Test Suite Expansion")
        appendLine()
        if (report.guideIterations.isEmpty()) {
            appendLine("No iterative test-case guide expansion was executed.")
        } else {
            appendLine("| Iteration | Status | Requested classes | Candidate gaps | Returned guides | Accepted | Duplicates | Rejected | Remaining gaps | Total accepted |")
            appendLine("| ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
            report.guideIterations.forEach { iteration ->
                appendLine(
                    "| ${iteration.iteration} | ${iteration.status} | ${iteration.requestedClassCount} | " +
                        "${iteration.candidateGapCount} | ${iteration.generatedGuideCount} | " +
                        "${iteration.acceptedGuideCount} | ${iteration.duplicateGuideCount} | " +
                        "${iteration.rejectedGuideCount} | ${iteration.remainingGapCount} | " +
                        "${iteration.totalAcceptedGuideCount} |",
                )
            }
        }
        appendLine()
        appendIterationGain(summary)
        appendLine()

        appendLine("### Structured JSON Plans")
        appendLine()
        if (report.structuredPlans.isEmpty()) {
            appendLine("No structured JSON plans were produced.")
        } else {
            appendLine("| Iteration | Source | Parse status | Scenarios | Mock strategies | Manual review notes |")
            appendLine("| ---: | --- | --- | ---: | ---: | ---: |")
            report.structuredPlans.forEach { plan ->
                appendLine("| ${plan.iteration} | ${plan.sourceClass} | ${plan.parseStatus} | ${plan.scenarios.size} | ${plan.mockStrategies.size} | ${plan.manualReviewNotes.size} |")
            }
            appendLine()
            report.structuredPlans.take(10).forEach { plan ->
                appendLine("#### Iteration ${plan.iteration}: ${plan.sourceClass}")
                appendLine()
                appendLine("**Source understanding:** ${plan.sourceSummary}")
                appendLine()
                if (plan.parseMessage != null) {
                    appendLine("**JSON parse message:** ${plan.parseMessage}")
                    appendLine()
                }
                if (plan.scenarios.isNotEmpty()) {
                    appendLine("| Method | Target parameter | Input strategy | Category | Test name | Given | When | Then | Mock |")
                    appendLine("| --- | --- | --- | --- | --- | --- | --- | --- | --- |")
                    plan.scenarios.forEach { scenario ->
                        appendLine("| ${scenario.methodName ?: "-"} | ${scenario.targetParameter ?: "-"} | ${scenario.inputStrategy ?: "-"} | ${scenario.category} | ${scenario.testName} | ${scenario.given} | ${scenario.whenAction} | ${scenario.then} | ${yesNo(scenario.requiresMock)} |")
                    }
                    appendLine()
                }
                if (plan.mockStrategies.isNotEmpty()) {
                    appendLine("**Mock strategy**")
                    appendLine()
                    plan.mockStrategies.forEach { strategy ->
                        appendLine("- ${strategy.dependency}: ${strategy.approach}. ${strategy.reason}")
                    }
                    appendLine()
                }
                if (plan.manualReviewNotes.isNotEmpty()) {
                    appendLine("**Manual review**")
                    appendLine()
                    plan.manualReviewNotes.forEach { note -> appendLine("- $note") }
                    appendLine()
                }
            }
        }
        appendLine()

        appendLine("### Suggestion Adoption")
        appendLine()
        if (report.adoptionDecisions.isEmpty()) {
            appendLine("No structured LLM scenarios were available for adoption.")
        } else {
            appendLine("| Iteration | Source | Method | Target parameter | Input strategy | Test name | Category | Status | Reason |")
            appendLine("| ---: | --- | --- | --- | --- | --- | --- | --- | --- |")
            report.adoptionDecisions.forEach { decision ->
                appendLine("| ${decision.iteration} | ${decision.sourceClass} | ${decision.methodName ?: "-"} | ${decision.targetParameter ?: "-"} | ${decision.inputStrategy ?: "-"} | ${decision.testName} | ${decision.category} | ${decision.status} | ${decision.reason} |")
            }
            appendLine()
            appendLine("Adoption summary: " + report.adoptionDecisions
                .groupingBy { it.status }
                .eachCount()
                .entries
                .sortedBy { it.key.name }
                .joinToString(", ") { "${it.key}=${it.value}" })
        }
        appendLine()

        appendLine("### Test Planning")
        appendLine()
        appendSuggestions(report.planningSuggestions)
        appendLine()

        appendLine("### Generation Review")
        appendLine()
        appendSuggestions(report.reviewSuggestions)
    }

    private fun StringBuilder.appendSuggestions(suggestions: List<LlmSuggestion>) {
        if (suggestions.isEmpty()) {
            appendLine("No LLM agent suggestions were produced.")
            return
        }

        appendLine("| Source | Method | Category | Recommendation | Rationale | Mock | Review |")
        appendLine("| --- | --- | --- | --- | --- | --- | --- |")
        suggestions.take(20).forEach { suggestion ->
            appendLine(
                "| ${suggestion.sourceClass} | ${suggestion.methodName ?: "-"} | ${suggestion.category} | ${suggestion.recommendation} | ${suggestion.rationale} | ${yesNo(suggestion.requiresMock)} | ${yesNo(suggestion.reviewRequired)} |",
            )
        }
        if (suggestions.size > 20) {
            appendLine()
            appendLine("Only the first 20 suggestions are shown in detail. Total suggestions: ${suggestions.size}.")
        }
    }

    private fun StringBuilder.appendIterationGain(summary: GenerationSummary) {
        val iterations = summary.llmAgentReport?.guideIterations.orEmpty()
        appendLine("### Iteration Gain Insight")
        appendLine()
        if (iterations.isEmpty()) {
            appendLine("No iterative gain data was available.")
            return
        }

        val baselineMethods = (summary.generatedTestMethods - summary.llmAdoptedMethods).coerceAtLeast(0)
        var cumulativeAdded = 0
        appendLine("| Stage | Added tests | Cumulative generated tests | Marginal status |")
        appendLine("| --- | ---: | ---: | --- |")
        appendLine("| Deterministic baseline | 0 | $baselineMethods | BASELINE |")
        iterations.forEach { iteration ->
            cumulativeAdded += iteration.acceptedGuideCount
            val status = if (iteration.acceptedGuideCount > 0) "PRODUCTIVE" else "NO_GAIN"
            appendLine(
                "| Iteration ${iteration.iteration} | ${iteration.acceptedGuideCount} | " +
                    "${baselineMethods + cumulativeAdded} | $status |",
            )
        }
        appendLine()
        val productiveRounds = iterations.count { it.acceptedGuideCount > 0 }
        appendLine(
            "Productive rounds: $productiveRounds/${iterations.size}; " +
                "LLM-guided marginal gain: +$cumulativeAdded test methods over the deterministic baseline.",
        )
    }
    private fun StringBuilder.appendValidationCommands() {
        appendLine("## Validation Commands")
        appendLine()
        appendLine("Generate tests, verify the generated report, then run the target project's normal Gradle test task:")
        appendLine()
        appendLine("```powershell")
        appendLine(".\\gradlew.bat :sample-android-app:generateUnitTests --rerun-tasks")
        appendLine(".\\gradlew.bat :sample-android-app:verifyGeneratedUnitTests --rerun-tasks")
        appendLine(".\\gradlew.bat :sample-android-app:testDebugUnitTest --rerun-tasks")
        appendLine("```")
        appendLine()
        appendLine("For a real Android app module, replace `sample-android-app` with the app module name, such as `app`.")
    }

    private fun StringBuilder.appendCoverage(summary: GenerationSummary) {
        appendLine("## Coverage")
        appendLine()

        val coverage = summary.coverage
        if (coverage == null) {
            appendLine("No JaCoCo XML report was found when this report was generated.")
            return
        }

        appendLine("Source: `${coverage.reportFile}`")
        appendLine()
        appendLine("| Metric | Covered | Missed | Total | Coverage |")
        appendLine("| --- | ---: | ---: | ---: | ---: |")
        coverage.metrics.forEach { metric ->
            appendLine(
                "| ${metric.type.lowercase().replaceFirstChar { it.uppercase() }} | ${metric.covered} | ${metric.missed} | ${metric.total} | ${"%.2f".format(metric.percentage)}% |",
            )
        }
    }

    private fun StringBuilder.appendCountTable(counts: Map<String, Int>) {
        appendLine("| Name | Count |")
        appendLine("| --- | ---: |")
        counts.toSortedMap().forEach { (name, count) -> appendLine("| $name | $count |") }
    }

    private fun status(passed: Boolean): String {
        return if (passed) "PASS" else "REVIEW"
    }

    private fun yesNo(value: Boolean): String {
        return if (value) "YES" else "NO"
    }

    private fun GenerationSummary.averageDifficulty(): String {
        if (testabilityInsights.isEmpty()) {
            return "0.0"
        }
        return String.format(
            Locale.US,
            "%.1f",
            testabilityInsights.map { it.difficultyScore }.average(),
        )
    }

    private fun scoreBar(score: Int): String {
        val filled = (score.coerceIn(0, 100) + 9) / 10
        return "[${"#".repeat(filled)}${".".repeat(10 - filled)}] $score/100"
    }

    private fun quadrantMeaning(quadrant: TestValueQuadrant): String {
        return when (quadrant) {
            TestValueQuadrant.PRIORITY_AUTOMATION -> "High value with practical automation confidence"
            TestValueQuadrant.GUIDED_GENERATION -> "High value but difficult or uncertain to automate"
            TestValueQuadrant.BATCH_AUTOMATION -> "Lower-risk target that can be generated in bulk"
            TestValueQuadrant.DEFER_OR_REVIEW -> "Automation cost is high relative to current priority"
        }
    }

    private fun quadrantAction(quadrant: TestValueQuadrant): String {
        return when (quadrant) {
            TestValueQuadrant.PRIORITY_AUTOMATION -> "Generate first"
            TestValueQuadrant.GUIDED_GENERATION -> "Use LLM guidance and review"
            TestValueQuadrant.BATCH_AUTOMATION -> "Batch generate"
            TestValueQuadrant.DEFER_OR_REVIEW -> "Defer or test manually"
        }
    }

    private fun GenerationSummary.qualityScore(): Int {
        if (parsedClasses == 0) {
            return 0
        }

        val generationRatio = generatedClasses.size.toDouble() / parsedClasses.toDouble()
        val assertionRatio = generatedAssertions.toDouble() / generatedClasses.size.coerceAtLeast(1).toDouble()
        val baseScore = (generationRatio * 60).toInt()
        val assertionScore = minOf(20, (assertionRatio * 8).toInt())
        val specializationScore = if (composeTests + roomDaoTests + retrofitApiTests + liveDataRules + robolectricTests > 0) 15 else 5
        val coverageScore = if (coverage != null) 5 else 0
        val fallbackPenalty = minOf(20, fallbackMethods * 3)
        val skippedPenalty = minOf(20, skippedClasses.size * 5)

        return (baseScore + assertionScore + specializationScore + coverageScore - fallbackPenalty - skippedPenalty)
            .coerceIn(0, 100)
    }
}
