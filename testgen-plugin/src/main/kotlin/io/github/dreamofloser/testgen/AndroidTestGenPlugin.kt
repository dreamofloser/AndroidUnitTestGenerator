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
        configurePortableLlmConventions(project, extension)

        val generateTask = project.tasks.register("generateUnitTests", GenerateUnitTestsTask::class.java) { task ->
            task.group = "verification"
            task.description = "Generates local unit test skeletons from Android Java and Kotlin source code."

            task.sourceDir.set(extension.sourceDir)
            task.testOutputDir.set(extension.testOutputDir)
            task.reportOutputDir.set(extension.reportOutputDir)
            task.coverageReportFile.set(extension.coverageReportFile)
            task.packageIncludes.set(extension.packageIncludes)
            task.packageExcludes.set(extension.packageExcludes)
            task.enableLlm.set(extension.enableLlm)
            task.llmProvider.set(extension.llmProvider)
            task.llmModel.set(extension.llmModel)
            task.llmAgentMode.set(extension.llmAgentMode)
            task.llmEndpoint.set(extension.llmEndpoint)
            task.llmApiKeyEnv.set(extension.llmApiKeyEnv)
            task.guideExpansionIterations.set(extension.guideExpansionIterations)
            task.maxGuidesPerClassPerIteration.set(extension.maxGuidesPerClassPerIteration)
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

    private fun configurePortableLlmConventions(project: Project, extension: TestGenExtension) {
        val providers = project.providers

        extension.enableLlm.convention(
            providers.gradleProperty("testgen.llm.enabled")
                .orElse(providers.environmentVariable("TESTGEN_LLM_ENABLED"))
                .map { it.equals("true", ignoreCase = true) || it == "1" }
                .orElse(false),
        )
        extension.llmProvider.convention(
            providers.gradleProperty("testgen.llm.provider")
                .orElse(providers.environmentVariable("TESTGEN_LLM_PROVIDER"))
                .orElse("mock"),
        )
        extension.llmModel.convention(
            providers.gradleProperty("testgen.llm.model")
                .orElse(providers.environmentVariable("TESTGEN_LLM_MODEL"))
                .orElse("offline-demo"),
        )
        extension.llmAgentMode.convention(
            providers.gradleProperty("testgen.llm.mode")
                .orElse(providers.environmentVariable("TESTGEN_LLM_MODE"))
                .orElse("planning-and-review"),
        )
        extension.llmEndpoint.convention(
            providers.gradleProperty("testgen.llm.endpoint")
                .orElse(providers.environmentVariable("TESTGEN_LLM_ENDPOINT"))
                .orElse(""),
        )
        extension.llmApiKeyEnv.convention(
            providers.gradleProperty("testgen.llm.apiKeyEnv")
                .orElse(providers.environmentVariable("TESTGEN_LLM_API_KEY_ENV"))
                .orElse("LLM_API_KEY"),
        )
    }
}
