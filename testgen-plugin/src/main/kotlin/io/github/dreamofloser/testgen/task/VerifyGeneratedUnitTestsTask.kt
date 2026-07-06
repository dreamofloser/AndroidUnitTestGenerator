package io.github.dreamofloser.testgen.task

import io.github.dreamofloser.testgen.report.ReportVerificationReader
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

abstract class VerifyGeneratedUnitTestsTask : DefaultTask() {
    @get:InputFile
    abstract val reportFile: RegularFileProperty

    @get:Input
    abstract val minimumQualityScore: Property<Int>

    @get:Input
    abstract val failOnSkippedClasses: Property<Boolean>

    @get:Input
    abstract val failOnFallbackMethods: Property<Boolean>

    @get:Optional
    @get:Input
    abstract val expectedTestTaskName: Property<String>

    @TaskAction
    fun verify() {
        val report = reportFile.asFile.get()
        if (!report.isFile) {
            throw GradleException("Generated test report was not found: $report")
        }

        val summary = ReportVerificationReader().read(report)
        val failures = mutableListOf<String>()

        if (summary.generatedTestClasses <= 0) {
            failures += "No generated test classes were found in the report."
        }
        if (summary.generatedTestMethods <= 0) {
            failures += "No generated test methods were found in the report."
        }
        if (summary.generatedAssertions <= 0) {
            failures += "No generated assertions were found in the report."
        }
        if (summary.qualityScore < minimumQualityScore.get()) {
            failures += "Generation quality score ${summary.qualityScore}/100 is below the required ${minimumQualityScore.get()}/100."
        }
        if (failOnSkippedClasses.get() && summary.skippedClasses > 0) {
            failures += "${summary.skippedClasses} classes were skipped."
        }
        if (failOnFallbackMethods.get() && summary.fallbackMethods > 0) {
            failures += "${summary.fallbackMethods} fallback methods were generated."
        }

        if (failures.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Generated unit test verification failed:")
                    failures.forEach { appendLine("- $it") }
                    appendLine("Report: $report")
                },
            )
        }

        val testTaskHint = expectedTestTaskName.orNull?.takeIf { it.isNotBlank() }
            ?: "the target module's test task"
        logger.lifecycle(
            "Generated unit test verification passed: ${summary.generatedTestClasses} classes, " +
                "${summary.generatedTestMethods} methods, quality ${summary.qualityScore}/100. " +
                "Next run $testTaskHint to compile and execute generated tests. Report: $report",
        )
    }
}