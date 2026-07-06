package io.github.dreamofloser.testgen.report

import io.github.dreamofloser.testgen.model.GeneratedClassResult
import io.github.dreamofloser.testgen.model.GenerationSummary
import java.io.File

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
            appendLine("| Skipped classes | ${summary.skippedClasses.size} |")
            appendLine()
            appendQualityGates(summary)
            appendLine()
            appendGeneratedClassMix(summary)
            appendLine()
            appendRiskReview(summary)
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
        }

        if (risks.isEmpty()) {
            appendLine("No major generation risks were detected by the current rule set.")
            return
        }

        risks.forEach { appendLine("- $it") }
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
