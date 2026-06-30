package com.codex.testgen

import com.codex.testgen.task.GenerateUnitTestsTask
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

        project.tasks.register("generateUnitTests", GenerateUnitTestsTask::class.java) { task ->
            task.group = "verification"
            task.description = "Generates local unit test skeletons from Android Java source code."

            task.sourceDir.set(extension.sourceDir)
            task.testOutputDir.set(extension.testOutputDir)
            task.reportOutputDir.set(extension.reportOutputDir)
            task.packageIncludes.set(extension.packageIncludes)
            task.packageExcludes.set(extension.packageExcludes)
        }
    }
}
