package com.codex.testgen.model

data class TestScenario(
    val nameSuffix: String,
    val arguments: List<String>,
    val assertion: AssertionModel,
    val expectedException: String? = null,
    val ruleName: String,
    val isFallback: Boolean = false,
)

data class AssertionModel(
    val kind: AssertionKind,
    val expectedValue: String? = null,
)

enum class AssertionKind {
    NONE,
    EQUALS,
    TRUE,
    FALSE,
    NOT_NULL,
}
