package io.github.dreamofloser.testgen.task

import io.github.dreamofloser.testgen.analysis.TestabilityAnalyzer
import io.github.dreamofloser.testgen.generator.JUnit4JavaTestGenerator
import io.github.dreamofloser.testgen.generator.KotlinUnitTestGenerator
import io.github.dreamofloser.testgen.guide.IterativeTestSuiteExpander
import io.github.dreamofloser.testgen.guide.LlmTestCaseGuideGenerator
import io.github.dreamofloser.testgen.llm.LlmAgentConfig
import io.github.dreamofloser.testgen.llm.LlmAgentReport
import io.github.dreamofloser.testgen.llm.LlmClientFactory
import io.github.dreamofloser.testgen.llm.LlmReviewAdvisor
import io.github.dreamofloser.testgen.llm.LlmPlanningResult
import io.github.dreamofloser.testgen.llm.LlmGenerationGuidance
import io.github.dreamofloser.testgen.model.ClassModel
import io.github.dreamofloser.testgen.model.GeneratedClassResult
import io.github.dreamofloser.testgen.model.GenerationSummary
import io.github.dreamofloser.testgen.model.SkippedClassResult
import io.github.dreamofloser.testgen.model.SourceClassKind
import io.github.dreamofloser.testgen.model.SourceLanguage
import io.github.dreamofloser.testgen.parser.JavaSourceParser
import io.github.dreamofloser.testgen.parser.KotlinSourceParser
import io.github.dreamofloser.testgen.report.CoverageReportReader
import io.github.dreamofloser.testgen.report.MarkdownReportWriter
import io.github.dreamofloser.testgen.scanner.SourceScanner
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateUnitTestsTask : DefaultTask() {
    @get:Optional
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val testOutputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val reportOutputDir: DirectoryProperty

    @get:Input
    abstract val packageIncludes: ListProperty<String>

    @get:Input
    abstract val packageExcludes: ListProperty<String>

    @get:Internal
    abstract val coverageReportFile: RegularFileProperty

    @get:Input
    abstract val enableLlm: Property<Boolean>

    @get:Input
    abstract val llmProvider: Property<String>

    @get:Input
    abstract val llmModel: Property<String>

    @get:Input
    abstract val llmAgentMode: Property<String>

    @get:Input
    abstract val llmEndpoint: Property<String>

    @get:Input
    abstract val llmApiKeyEnv: Property<String>

    @get:Input
    abstract val guideExpansionIterations: Property<Int>

    @get:Input
    abstract val maxGuidesPerClassPerIteration: Property<Int>

    @TaskAction
    fun generate() {
        val sourceDirectory = sourceDir.asFile.get()
        val testDirectory = testOutputDir.asFile.get()
        val reportDirectory = reportOutputDir.asFile.get()

        val scanner = SourceScanner()
        val javaParser = JavaSourceParser()
        val kotlinParser = KotlinSourceParser()
        val javaGenerator = JUnit4JavaTestGenerator()
        val kotlinGenerator = KotlinUnitTestGenerator()
        val coverageReader = CoverageReportReader()
        val reportWriter = MarkdownReportWriter()
        val llmConfig = LlmAgentConfig(
            enabled = enableLlm.get(),
            provider = llmProvider.get(),
            model = llmModel.get(),
            agentMode = llmAgentMode.get(),
            endpoint = llmEndpoint.get(),
            apiKeyEnv = llmApiKeyEnv.get(),
        )

        if (llmConfig.enabled) {
            logger.lifecycle(
                "LLM agent configuration: provider=${llmConfig.provider}, model=${llmConfig.model}, mode=${llmConfig.agentMode}",
            )
        }

        val sourceFiles = scanner.findSourceFiles(sourceDirectory)
        val parsedClasses = sourceFiles.flatMap { sourceFile ->
            when (sourceFile.extension) {
                "java" -> javaParser.parse(sourceFile)
                "kt" -> kotlinParser.parse(sourceFile)
                else -> emptyList()
            }
        }
            .filter { it.matchesPackageRules(packageIncludes.get(), packageExcludes.get()) }
        val testabilityInsights = TestabilityAnalyzer().analyze(parsedClasses)
        val highPriorityTargets = testabilityInsights.count { it.priorityScore >= 60 }
        logger.lifecycle(
            "Testability analysis completed for ${testabilityInsights.size} targets; " +
                "$highPriorityTargets are high priority.",
        )

        val guideExpansion = if (llmConfig.enabled) {
            IterativeTestSuiteExpander(
                guideGenerator = LlmTestCaseGuideGenerator(
                    client = LlmClientFactory.create(llmConfig),
                    progressReporter = { message -> logger.lifecycle(message) },
                ),
            ).expand(
                classes = parsedClasses,
                maxIterations = guideExpansionIterations.get(),
                maxGuidesPerClass = maxGuidesPerClassPerIteration.get(),
                insights = testabilityInsights,
            )
        } else {
            null
        }

        val planningResult = guideExpansion?.planningResult
            ?: LlmPlanningResult(suggestions = emptyList(), structuredPlans = emptyList())
        val generationGuidance = guideExpansion?.guidanceByClass
            ?: parsedClasses.associateWith { LlmGenerationGuidance() }
        val adoptionDecisions = guideExpansion?.adoptionDecisions.orEmpty()

        val generated = mutableListOf<GeneratedClassResult>()
        val skipped = mutableListOf<SkippedClassResult>()

        parsedClasses.forEach { classModel ->
            if (classModel.methods.isEmpty() && classModel.classKind !in generatableClassKindsWithoutPublicMethods) {
                skipped += SkippedClassResult(
                    sourceClass = classModel.qualifiedName(),
                    reason = "No supported public methods or data-class constructor can be generated.",
                )
                return@forEach
            }

            val guidance = generationGuidance.getValue(classModel)
            val generatedSource = when (classModel.language) {
                SourceLanguage.JAVA -> javaGenerator.generate(classModel, guidance)
                SourceLanguage.KOTLIN -> kotlinGenerator.generate(classModel, guidance)
            }
            val testFile = classModel.testFileIn(testDirectory)
            testFile.parentFile.mkdirs()
            testFile.writeText(generatedSource.source)

            generated += GeneratedClassResult(
                sourceClass = classModel.qualifiedName(),
                testClass = "${classModel.qualifiedName()}${classModel.testClassSuffix()}",
                testFile = testFile,
                sourceLanguage = classModel.language,
                sourceClassKind = classModel.classKind,
                generatedMethodCount = generatedSource.testMethodCount,
                assertionCount = generatedSource.assertionCount,
                fallbackMethodCount = generatedSource.fallbackMethodCount,
                ruleMatchedMethodCount = generatedSource.ruleMatchedMethodCount,
                mockedDependencyCount = generatedSource.mockedDependencyCount,
                mockStubCount = generatedSource.mockStubCount,
                mockVerificationCount = generatedSource.mockVerificationCount,
                liveDataRuleCount = generatedSource.liveDataRuleCount,
                robolectricTestCount = generatedSource.robolectricTestCount,
                androidImportCount = generatedSource.androidImportCount,
                composeTestCount = generatedSource.composeTestCount,
                roomDaoTestCount = generatedSource.roomDaoTestCount,
                retrofitApiTestCount = generatedSource.retrofitApiTestCount,
                llmAdoptedMethodCount = generatedSource.llmAdoptedMethodCount,
            )
        }

        cleanStaleKotlinGeneratedTests(testDirectory, generated.map { it.testFile }.toSet())

        val coverage = coverageReportFile.orNull
            ?.asFile
            ?.takeIf { it.isFile }
            ?.let { coverageReader.read(it) }

        val baseSummary = GenerationSummary(
            scannedFiles = sourceFiles.size,
            parsedClasses = parsedClasses.size,
            generatedClasses = generated,
            skippedClasses = skipped,
            coverage = coverage,
            testabilityInsights = testabilityInsights,
        )
        val llmAgentReport = if (llmConfig.enabled) {
            LlmAgentReport(
                config = llmConfig,
                planningSuggestions = planningResult.suggestions,
                structuredPlans = planningResult.structuredPlans,
                adoptionDecisions = adoptionDecisions,
                guideIterations = guideExpansion?.iterations.orEmpty(),
                reviewSuggestions = LlmReviewAdvisor().review(baseSummary),
            )
        } else {
            null
        }
        val summary = baseSummary.copy(llmAgentReport = llmAgentReport)

        val reportFile = reportWriter.write(summary, reportDirectory)
        logger.lifecycle(
            "Generated ${generated.size} test classes and ${summary.generatedTestMethods} test methods. Report: $reportFile",
        )
        if (llmAgentReport != null) {
            logger.lifecycle("LLM agent produced ${llmAgentReport.totalSuggestions} suggestions and adopted ${llmAgentReport.adoptedScenarioCount} scenarios.")
            logger.lifecycle(
                "Guide expansion completed ${llmAgentReport.guideIterations.size} iterations with " +
                    "${llmAgentReport.guideIterations.sumOf { it.acceptedGuideCount }} accepted guides.",
            )
        }
    }

    private fun cleanStaleKotlinGeneratedTests(testDirectory: File, generatedFiles: Set<File>) {
        if (!testDirectory.exists()) {
            return
        }

        val generatedCanonicalFiles = generatedFiles.map { it.canonicalFile }.toSet()
        testDirectory.walkTopDown()
            .filter { it.isFile && it.name.endsWith("GeneratedTest.kt") }
            .filterNot { it.canonicalFile in generatedCanonicalFiles }
            .forEach { staleFile -> staleFile.delete() }
    }
    private fun ClassModel.matchesPackageRules(includes: List<String>, excludes: List<String>): Boolean {
        val qualifiedName = qualifiedName()
        val included = includes.isEmpty() || includes.any { qualifiedName.startsWith(it) }
        val excluded = excludes.any { qualifiedName.startsWith(it) }
        return included && !excluded
    }

    private fun ClassModel.qualifiedName(): String {
        return if (packageName.isBlank()) className else "$packageName.$className"
    }

    private fun ClassModel.testFileIn(testDirectory: File): File {
        val packagePath = packageName.replace('.', File.separatorChar)
        val directory = if (packagePath.isBlank()) testDirectory else testDirectory.resolve(packagePath)
        return directory.resolve("${className}${testClassSuffix()}.${testFileExtension()}")
    }

    private fun ClassModel.testClassSuffix(): String {
        return when (language) {
            SourceLanguage.JAVA -> "Test"
            SourceLanguage.KOTLIN -> "GeneratedTest"
        }
    }

    private fun ClassModel.testFileExtension(): String {
        return when (language) {
            SourceLanguage.JAVA -> "java"
            SourceLanguage.KOTLIN -> "kt"
        }
    }

    private companion object {
        val generatableClassKindsWithoutPublicMethods = setOf(
            SourceClassKind.DATA,
            SourceClassKind.ACTIVITY,
            SourceClassKind.FRAGMENT,
            SourceClassKind.COMPOSE_UI,
            SourceClassKind.ROOM_DAO,
            SourceClassKind.RETROFIT_API,
        )
    }
}
