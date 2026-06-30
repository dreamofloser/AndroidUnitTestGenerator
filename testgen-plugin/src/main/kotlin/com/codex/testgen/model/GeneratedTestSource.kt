package com.codex.testgen.model

data class GeneratedTestSource(
    val source: String,
    val testMethodCount: Int,
    val assertionCount: Int,
    val fallbackMethodCount: Int,
    val ruleMatchedMethodCount: Int,
)
