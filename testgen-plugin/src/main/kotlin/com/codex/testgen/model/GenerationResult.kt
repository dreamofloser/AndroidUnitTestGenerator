package com.codex.testgen.model

import java.io.File

data class GenerationSummary(
    val scannedFiles: Int,
    val parsedClasses: Int,
    val generatedClasses: List<GeneratedClassResult>,
    val skippedClasses: List<SkippedClassResult>,
    val coverage: CoverageSummary? = null,
) {
    val generatedTestMethods: Int
        get() = generatedClasses.sumOf { it.generatedMethodCount }

    val generatedAssertions: Int
        get() = generatedClasses.sumOf { it.assertionCount }

    val fallbackMethods: Int
        get() = generatedClasses.sumOf { it.fallbackMethodCount }

    val ruleMatchedMethods: Int
        get() = generatedClasses.sumOf { it.ruleMatchedMethodCount }

    val mockedDependencies: Int
        get() = generatedClasses.sumOf { it.mockedDependencyCount }

    val mockStubs: Int
        get() = generatedClasses.sumOf { it.mockStubCount }

    val mockVerifications: Int
        get() = generatedClasses.sumOf { it.mockVerificationCount }

    val liveDataRules: Int
        get() = generatedClasses.sumOf { it.liveDataRuleCount }

    val robolectricTests: Int
        get() = generatedClasses.sumOf { it.robolectricTestCount }

    val androidImports: Int
        get() = generatedClasses.sumOf { it.androidImportCount }

    val composeTests: Int
        get() = generatedClasses.sumOf { it.composeTestCount }

    val roomDaoTests: Int
        get() = generatedClasses.sumOf { it.roomDaoTestCount }

    val retrofitApiTests: Int
        get() = generatedClasses.sumOf { it.retrofitApiTestCount }
}

data class GeneratedClassResult(
    val sourceClass: String,
    val testClass: String,
    val testFile: File,
    val generatedMethodCount: Int,
    val assertionCount: Int,
    val fallbackMethodCount: Int,
    val ruleMatchedMethodCount: Int,
    val mockedDependencyCount: Int,
    val mockStubCount: Int,
    val mockVerificationCount: Int,
    val liveDataRuleCount: Int,
    val robolectricTestCount: Int,
    val androidImportCount: Int,
    val composeTestCount: Int,
    val roomDaoTestCount: Int,
    val retrofitApiTestCount: Int,
)

data class SkippedClassResult(
    val sourceClass: String,
    val reason: String,
)

data class CoverageSummary(
    val reportFile: File,
    val metrics: List<CoverageMetric>,
) {
    fun metric(type: String): CoverageMetric? = metrics.firstOrNull { it.type == type }
}

data class CoverageMetric(
    val type: String,
    val missed: Int,
    val covered: Int,
) {
    val total: Int
        get() = missed + covered

    val percentage: Double
        get() = if (total == 0) 0.0 else covered.toDouble() * 100.0 / total.toDouble()
}
