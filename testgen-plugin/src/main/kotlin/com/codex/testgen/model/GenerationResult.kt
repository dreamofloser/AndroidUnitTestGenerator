package com.codex.testgen.model

import java.io.File

data class GenerationSummary(
    val scannedFiles: Int,
    val parsedClasses: Int,
    val generatedClasses: List<GeneratedClassResult>,
    val skippedClasses: List<SkippedClassResult>,
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
)

data class SkippedClassResult(
    val sourceClass: String,
    val reason: String,
)
