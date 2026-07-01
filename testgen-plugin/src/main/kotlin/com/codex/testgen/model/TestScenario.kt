package com.codex.testgen.model

data class TestScenario(
    val nameSuffix: String,
    val arguments: List<String>,
    val assertion: AssertionModel,
    val expectedException: String? = null,
    val ruleName: String,
    val isFallback: Boolean = false,
    val mockInteractions: List<MockInteractionModel> = emptyList(),
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

data class MockInteractionModel(
    val receiverName: String,
    val methodName: String,
    val arguments: List<String>,
    val returnValue: String? = null,
)
