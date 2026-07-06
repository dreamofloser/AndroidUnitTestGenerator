package io.github.dreamofloser.testgen

import io.github.dreamofloser.testgen.task.GenerateUnitTestsTask
import io.github.dreamofloser.testgen.task.VerifyGeneratedUnitTestsTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidTestGenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "testGen",
            TestGenExtension::class.java,
            project.objects,
            project.layout,
        )

        val generateTask = project.tasks.register("generateUnitTests", GenerateUnitTestsTask::class.java) { task ->
            task.group = "verification"
            task.description = "Generates local unit test skeletons from Android Java and Kotlin source code."

            task.sourceDir.set(extension.sourceDir)
            task.testOutputDir.set(extension.testOutputDir)
            task.reportOutputDir.set(extension.reportOutputDir)
            task.coverageReportFile.set(extension.coverageReportFile)
            task.packageIncludes.set(extension.packageIncludes)
            task.packageExcludes.set(extension.packageExcludes)
        }

        project.tasks.register("verifyGeneratedUnitTests", VerifyGeneratedUnitTestsTask::class.java) { task ->
            task.group = "verification"
            task.description = "Verifies the generated unit test report and quality gates."
            task.dependsOn(generateTask)

            task.reportFile.set(extension.reportOutputDir.file("report.md"))
            task.minimumQualityScore.set(extension.minimumQualityScore)
            task.failOnSkippedClasses.set(extension.failOnSkippedClasses)
            task.failOnFallbackMethods.set(extension.failOnFallbackMethods)
            task.expectedTestTaskName.set(extension.expectedTestTaskName)
        }
    }
}
