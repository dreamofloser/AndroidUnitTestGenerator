package com.codex.testgen.task

import com.codex.testgen.generator.JUnit4JavaTestGenerator
import com.codex.testgen.model.ClassModel
import com.codex.testgen.model.GeneratedClassResult
import com.codex.testgen.model.GenerationSummary
import com.codex.testgen.model.SkippedClassResult
import com.codex.testgen.parser.JavaSourceParser
import com.codex.testgen.report.MarkdownReportWriter
import com.codex.testgen.scanner.SourceScanner
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
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

    @TaskAction
    fun generate() {
        val sourceDirectory = sourceDir.asFile.get()
        val testDirectory = testOutputDir.asFile.get()
        val reportDirectory = reportOutputDir.asFile.get()

        val scanner = SourceScanner()
        val parser = JavaSourceParser()
        val generator = JUnit4JavaTestGenerator()
        val reportWriter = MarkdownReportWriter()

        val javaFiles = scanner.findJavaFiles(sourceDirectory)
        val parsedClasses = javaFiles.flatMap { parser.parse(it) }
            .filter { it.matchesPackageRules(packageIncludes.get(), packageExcludes.get()) }

        val generated = mutableListOf<GeneratedClassResult>()
        val skipped = mutableListOf<SkippedClassResult>()

        parsedClasses.forEach { classModel ->
            if (classModel.methods.isEmpty()) {
                skipped += SkippedClassResult(
                    sourceClass = classModel.qualifiedName(),
                    reason = "No public methods can be generated in Stage 1.",
                )
                return@forEach
            }

            val generatedSource = generator.generate(classModel)
            val testFile = classModel.testFileIn(testDirectory)
            testFile.parentFile.mkdirs()
            testFile.writeText(generatedSource.source)

            generated += GeneratedClassResult(
                sourceClass = classModel.qualifiedName(),
                testClass = "${classModel.qualifiedName()}Test",
                testFile = testFile,
                generatedMethodCount = generatedSource.testMethodCount,
                assertionCount = generatedSource.assertionCount,
                fallbackMethodCount = generatedSource.fallbackMethodCount,
                ruleMatchedMethodCount = generatedSource.ruleMatchedMethodCount,
            )
        }

        val summary = GenerationSummary(
            scannedFiles = javaFiles.size,
            parsedClasses = parsedClasses.size,
            generatedClasses = generated,
            skippedClasses = skipped,
        )

        val reportFile = reportWriter.write(summary, reportDirectory)
        logger.lifecycle(
            "Generated ${generated.size} test classes and ${summary.generatedTestMethods} test methods. Report: $reportFile",
        )
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
        return directory.resolve("${className}Test.java")
    }
}
