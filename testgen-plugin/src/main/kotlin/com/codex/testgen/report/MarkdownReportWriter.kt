package com.codex.testgen.report

import com.codex.testgen.model.GenerationSummary
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
            appendCoverage(summary)
            appendLine()
            appendLine("## Generated Classes")
            appendLine()

            if (summary.generatedClasses.isEmpty()) {
                appendLine("No test classes were generated.")
            } else {
                appendLine("| Source class | Test class | Test methods | Assertions | Mocked dependencies | Mockito stubs | Mockito verifications | LiveData rules | Robolectric | Android imports | Compose UI | Room DAO | Retrofit API | Fallback methods | File |")
                appendLine("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |")
                summary.generatedClasses.forEach {
                    appendLine(
                        "| ${it.sourceClass} | ${it.testClass} | ${it.generatedMethodCount} | ${it.assertionCount} | ${it.mockedDependencyCount} | ${it.mockStubCount} | ${it.mockVerificationCount} | ${it.liveDataRuleCount} | ${it.robolectricTestCount} | ${it.androidImportCount} | ${it.composeTestCount} | ${it.roomDaoTestCount} | ${it.retrofitApiTestCount} | ${it.fallbackMethodCount} | `${it.testFile}` |",
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
            }
        }
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
}
