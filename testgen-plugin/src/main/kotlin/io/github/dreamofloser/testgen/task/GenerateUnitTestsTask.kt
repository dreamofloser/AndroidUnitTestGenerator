package io.github.dreamofloser.testgen.task

import io.github.dreamofloser.testgen.generator.JUnit4JavaTestGenerator
import io.github.dreamofloser.testgen.generator.KotlinUnitTestGenerator
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

        val sourceFiles = scanner.findSourceFiles(sourceDirectory)
        val parsedClasses = sourceFiles.flatMap { sourceFile ->
            when (sourceFile.extension) {
                "java" -> javaParser.parse(sourceFile)
                "kt" -> kotlinParser.parse(sourceFile)
                else -> emptyList()
            }
        }
            .filter { it.matchesPackageRules(packageIncludes.get(), packageExcludes.get()) }

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

            val generatedSource = when (classModel.language) {
                SourceLanguage.JAVA -> javaGenerator.generate(classModel)
                SourceLanguage.KOTLIN -> kotlinGenerator.generate(classModel)
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
            )
        }

        cleanStaleKotlinGeneratedTests(testDirectory, generated.map { it.testFile }.toSet())

        val coverage = coverageReportFile.orNull
            ?.asFile
            ?.takeIf { it.isFile }
            ?.let { coverageReader.read(it) }

        val summary = GenerationSummary(
            scannedFiles = sourceFiles.size,
            parsedClasses = parsedClasses.size,
            generatedClasses = generated,
            skippedClasses = skipped,
            coverage = coverage,
        )

        val reportFile = reportWriter.write(summary, reportDirectory)
        logger.lifecycle(
            "Generated ${generated.size} test classes and ${summary.generatedTestMethods} test methods. Report: $reportFile",
        )
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




