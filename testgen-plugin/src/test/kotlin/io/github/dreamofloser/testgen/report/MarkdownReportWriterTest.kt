package io.github.dreamofloser.testgen.report

import io.github.dreamofloser.testgen.analysis.DifficultyDriver
import io.github.dreamofloser.testgen.analysis.DifficultyEvidence
import io.github.dreamofloser.testgen.analysis.TestStrategy
import io.github.dreamofloser.testgen.analysis.TestabilityInsight
import io.github.dreamofloser.testgen.guide.GuideIterationResult
import io.github.dreamofloser.testgen.guide.GuideIterationStatus
import io.github.dreamofloser.testgen.llm.LlmAgentConfig
import io.github.dreamofloser.testgen.llm.LlmAgentReport
import io.github.dreamofloser.testgen.llm.LlmAdoptionDecision
import io.github.dreamofloser.testgen.llm.LlmAdoptionStatus
import io.github.dreamofloser.testgen.llm.LlmSuggestion
import io.github.dreamofloser.testgen.llm.LlmJsonParseStatus
import io.github.dreamofloser.testgen.llm.LlmStructuredTestPlan
import io.github.dreamofloser.testgen.llm.LlmTestScenario
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
            testabilityInsights = listOf(
                TestabilityInsight(
                    sourceClass = "com.example.ExampleRepository",
                    methodName = "load",
                    difficultyScore = 64,
                    priorityScore = 82,
                    automationConfidence = 72,
                    recommendedStrategy = TestStrategy.MOCKED_UNIT,
                    evidence = listOf(
                        DifficultyEvidence(
                            driver = DifficultyDriver.DEPENDENCIES,
                            points = 18,
                            detail = "two external dependencies",
                        ),
                    ),
                    boundaryFocus = listOf("city=empty-string", "dependency-failure"),
                ),
            ),
            llmAgentReport = LlmAgentReport(
                config = LlmAgentConfig(
                    enabled = true,
                    provider = "mock",
                    model = "offline-demo",
                    agentMode = "planning-and-review",
                    endpoint = "",
                    apiKeyEnv = "LLM_API_KEY",
                ),
                planningSuggestions = listOf(
                    LlmSuggestion(
                        sourceClass = "com.example.ExampleRepository",
                        methodName = "load",
                        category = "Mock strategy",
                        recommendation = "Stub the API dependency and verify repository interaction.",
                        rationale = "Repository classes should isolate external dependencies.",
                        requiresMock = true,
                    ),
                ),
                reviewSuggestions = emptyList(),
                structuredPlans = listOf(
                    LlmStructuredTestPlan(
                        sourceClass = "com.example.ExampleRepository",
                        sourceSummary = "Loads data through an API dependency.",
                        scenarios = listOf(
                            LlmTestScenario(
                                methodName = "load",
                                category = "error",
                                testName = "loadReturnsFailure",
                                given = "the API returns HTTP 500",
                                whenAction = "load is called",
                                then = "a failure result is returned",
                                requiresMock = true,
                            ),
                        ),
                        mockStrategies = emptyList(),
                        manualReviewNotes = emptyList(),
                        parseStatus = LlmJsonParseStatus.SUCCESS,
                        iteration = 1,
                    ),
                ),
                adoptionDecisions = listOf(
                    LlmAdoptionDecision(
                        sourceClass = "com.example.ExampleRepository",
                        methodName = "load",
                        testName = "loadReturnsFailure",
                        category = "error",
                        status = LlmAdoptionStatus.GENERATED,
                        reason = "Accepted by a deterministic template.",
                        targetParameter = "city",
                        inputStrategy = "empty-string",
                        iteration = 1,
                    ),
                ),
                guideIterations = listOf(
                    GuideIterationResult(
                        iteration = 1,
                        status = GuideIterationStatus.EXPANDED,
                        requestedClassCount = 1,
                        candidateGapCount = 2,
                        generatedGuideCount = 1,
                        acceptedGuideCount = 1,
                        duplicateGuideCount = 0,
                        rejectedGuideCount = 0,
                        remainingGapCount = 1,
                        totalAcceptedGuideCount = 1,
                    ),
                ),
            ),
        )

        val report = MarkdownReportWriter().write(summary, outputDir).readText()

        assertTrue(report.contains("## Quality Gates"))
        assertTrue(report.contains("## Generated Class Mix"))
        assertTrue(report.contains("## Risk Review"))
        assertTrue(report.contains("## Validation Commands"))
        assertTrue(report.contains("## LLM Agent Suggestions"))
        assertTrue(report.contains("Mock strategy"))
        assertTrue(report.contains("### Structured JSON Plans"))
        assertTrue(report.contains("### Iterative Test Suite Expansion"))
        assertTrue(report.contains("### Iteration Gain Insight"))
        assertTrue(report.contains("LLM-guided marginal gain"))
        assertTrue(report.contains("EXPANDED"))
        assertTrue(report.contains("### Suggestion Adoption"))
        assertTrue(report.contains("GENERATED"))
        assertTrue(report.contains("loadReturnsFailure"))
        assertTrue(report.contains("Target parameter"))
        assertTrue(report.contains("empty-string"))
        assertTrue(report.contains("SUCCESS"))
        assertTrue(report.contains("Generation quality score"))
        assertTrue(report.contains("Skipped Reason Summary"))
        assertTrue(report.contains("RETROFIT_API"))
        assertTrue(report.contains("verifyGeneratedUnitTests"))
        assertTrue(report.contains("## Testability Insights"))
        assertTrue(report.contains("### Test Value Matrix"))
        assertTrue(report.contains("### Method Ranking"))
        assertTrue(report.contains("### Difficulty Driver Distribution"))
        assertTrue(report.contains("ExampleRepository#load"))
        assertTrue(report.contains("Mockito/MockK unit"))
        assertTrue(report.contains("the score is not generated by the LLM"))
    }
}

