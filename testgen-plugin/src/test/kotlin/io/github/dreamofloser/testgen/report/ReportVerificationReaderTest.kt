package io.github.dreamofloser.testgen.report

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class ReportVerificationReaderTest {
    @Test
    fun readsVerificationMetricsFromMarkdownReport() {
        val report = Files.createTempFile("testgen-report", ".md").toFile().apply {
            writeText(
                """
                # Android Unit Test Generator Report

                ## Summary

                | Metric | Value |
                | --- | ---: |
                | Scanned source files | 9 |
                | Parsed classes | 10 |
                | Generated test classes | 10 |
                | Generated test methods | 18 |
                | Generated assertions | 24 |
                | Generation quality score | 91/100 |
                | Fallback methods | 1 |
                | Skipped classes | 0 |
                """.trimIndent(),
            )
        }

        val summary = ReportVerificationReader().read(report)

        assertEquals(9, summary.scannedSourceFiles)
        assertEquals(10, summary.parsedClasses)
        assertEquals(10, summary.generatedTestClasses)
        assertEquals(18, summary.generatedTestMethods)
        assertEquals(24, summary.generatedAssertions)
        assertEquals(91, summary.qualityScore)
        assertEquals(1, summary.fallbackMethods)
        assertEquals(0, summary.skippedClasses)
    }

    @Test
    fun returnsZeroForMissingMetrics() {
        val report = Files.createTempFile("testgen-report-empty", ".md").toFile().apply {
            writeText("# Empty Report")
        }

        val summary = ReportVerificationReader().read(report)

        assertEquals(0, summary.generatedTestClasses)
        assertEquals(0, summary.qualityScore)
    }
}