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
            appendLine("| Scanned Java files | ${summary.scannedFiles} |")
            appendLine("| Parsed classes | ${summary.parsedClasses} |")
            appendLine("| Generated test classes | ${summary.generatedClasses.size} |")
            appendLine("| Generated test methods | ${summary.generatedTestMethods} |")
            appendLine("| Generated assertions | ${summary.generatedAssertions} |")
            appendLine("| Rule matched methods | ${summary.ruleMatchedMethods} |")
            appendLine("| Fallback methods | ${summary.fallbackMethods} |")
            appendLine("| Mocked dependencies | ${summary.mockedDependencies} |")
            appendLine("| Mockito stubs | ${summary.mockStubs} |")
            appendLine("| Mockito verifications | ${summary.mockVerifications} |")
            appendLine("| Skipped classes | ${summary.skippedClasses.size} |")
            appendLine()
            appendLine("## Generated Classes")
            appendLine()

            if (summary.generatedClasses.isEmpty()) {
                appendLine("No test classes were generated.")
            } else {
                appendLine("| Source class | Test class | Test methods | Assertions | Mocked dependencies | Mockito stubs | Mockito verifications | Fallback methods | File |")
                appendLine("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |")
                summary.generatedClasses.forEach {
                    appendLine(
                        "| ${it.sourceClass} | ${it.testClass} | ${it.generatedMethodCount} | ${it.assertionCount} | ${it.mockedDependencyCount} | ${it.mockStubCount} | ${it.mockVerificationCount} | ${it.fallbackMethodCount} | `${it.testFile}` |",
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
}
