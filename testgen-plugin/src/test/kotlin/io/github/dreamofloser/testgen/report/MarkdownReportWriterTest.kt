package io.github.dreamofloser.testgen.report

import io.github.dreamofloser.testgen.model.GeneratedClassResult
import io.github.dreamofloser.testgen.model.GenerationSummary
import io.github.dreamofloser.testgen.model.SkippedClassResult
import io.github.dreamofloser.testgen.model.SourceClassKind
import io.github.dreamofloser.testgen.model.SourceLanguage
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class MarkdownReportWriterTest {
    @Test
    fun writesQualityAndRiskSections() {
        val outputDir = Files.createTempDirectory("testgen-report").toFile()
        val testFile = outputDir.resolve("ExampleGeneratedTest.kt")
        val summary = GenerationSummary(
            scannedFiles = 2,
            parsedClasses = 2,
            generatedClasses = listOf(
                GeneratedClassResult(
                    sourceClass = "com.example.ExampleRepository",
                    testClass = "com.example.ExampleRepositoryGeneratedTest",
                    testFile = testFile,
                    sourceLanguage = SourceLanguage.KOTLIN,
                    sourceClassKind = SourceClassKind.RETROFIT_API,
                    generatedMethodCount = 2,
                    assertionCount = 2,
                    fallbackMethodCount = 1,
                    ruleMatchedMethodCount = 1,
                    mockedDependencyCount = 1,
                    mockStubCount = 1,
                    mockVerificationCount = 0,
                    liveDataRuleCount = 0,
                    robolectricTestCount = 0,
                    androidImportCount = 0,
                    composeTestCount = 0,
                    roomDaoTestCount = 0,
                    retrofitApiTestCount = 1,
                ),
            ),
            skippedClasses = listOf(
                SkippedClassResult(
                    sourceClass = "com.example.LegacyActivity",
                    reason = "No supported public methods or data-class constructor can be generated.",
                ),
            ),
        )

        val report = MarkdownReportWriter().write(summary, outputDir).readText()

        assertTrue(report.contains("## Quality Gates"))
        assertTrue(report.contains("## Generated Class Mix"))
        assertTrue(report.contains("## Risk Review"))
        assertTrue(report.contains("## Validation Commands"))
        assertTrue(report.contains("Generation quality score"))
        assertTrue(report.contains("Skipped Reason Summary"))
        assertTrue(report.contains("RETROFIT_API"))
        assertTrue(report.contains("verifyGeneratedUnitTests"))
    }
}

