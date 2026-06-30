package com.codex.testgen.model

import java.io.File

data class GenerationSummary(
    val scannedFiles: Int,
    val parsedClasses: Int,
    val generatedClasses: List<GeneratedClassResult>,
    val skippedClasses: List<SkippedClassResult>,
)

data class GeneratedClassResult(
    val sourceClass: String,
    val testClass: String,
    val testFile: File,
    val generatedMethodCount: Int,
)

data class SkippedClassResult(
    val sourceClass: String,
    val reason: String,
)
