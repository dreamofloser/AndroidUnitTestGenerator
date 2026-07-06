package io.github.dreamofloser.testgen.report

import java.io.File

class ReportVerificationReader {
    fun read(reportFile: File): ReportVerificationSummary {
        val report = reportFile.readText()
        return ReportVerificationSummary(
            reportFile = reportFile,
            scannedSourceFiles = report.metricValue("Scanned source files"),
            parsedClasses = report.metricValue("Parsed classes"),
            generatedTestClasses = report.metricValue("Generated test classes"),
            generatedTestMethods = report.metricValue("Generated test methods"),
            generatedAssertions = report.metricValue("Generated assertions"),
            qualityScore = report.qualityScore(),
            fallbackMethods = report.metricValue("Fallback methods"),
            skippedClasses = report.metricValue("Skipped classes"),
        )
    }

    private fun String.metricValue(metricName: String): Int {
        val escapedMetric = Regex.escape(metricName)
        val regex = Regex("""(?m)^\|\s*$escapedMetric\s*\|\s*(\d+)\s*\|""")
        return regex.find(this)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun String.qualityScore(): Int {
        val regex = Regex("""(?m)^\|\s*Generation quality score\s*\|\s*(\d+)/100\s*\|""")
        return regex.find(this)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
}

data class ReportVerificationSummary(
    val reportFile: File,
    val scannedSourceFiles: Int,
    val parsedClasses: Int,
    val generatedTestClasses: Int,
    val generatedTestMethods: Int,
    val generatedAssertions: Int,
    val qualityScore: Int,
    val fallbackMethods: Int,
    val skippedClasses: Int,
)