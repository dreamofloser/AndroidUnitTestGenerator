package io.github.dreamofloser.testgen.llm

class MockLlmClient(
    private val config: LlmAgentConfig,
) : LlmClient {
    override fun complete(prompt: String): String {
        val guideLine = prompt.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("- methodName=") }
        val selectedLine = prompt.lineSequence()
            .map { it.trim() }
            .firstOrNull { line ->
                line.startsWith("- ") &&
                    line.contains("| allowed choices:") &&
                    !line.endsWith("allowed choices: none")
            }
        val methodName = guideLine
            ?.substringAfter("methodName=")
            ?.substringBefore(",")
            ?.trim()
            ?: selectedLine
                ?.substringAfter("- ")
                ?.substringBefore("(")
                ?.trim()
        val selectedChoice = selectedLine
            ?.substringAfter("allowed choices:")
            ?.trim()
            ?.substringBefore(",")
        val targetParameter = guideLine
            ?.substringAfter("targetParameter=")
            ?.substringBefore(",")
            ?.trim()
            ?: selectedChoice?.substringBefore("=")?.trim()
        val inputStrategy = guideLine
            ?.substringAfter("inputStrategy=")
            ?.substringBefore(",")
            ?.trim()
            ?: selectedChoice?.substringAfter("=")?.trim()
        val scenarios = if (
            methodName.isNullOrBlank() ||
            targetParameter.isNullOrBlank() ||
            inputStrategy.isNullOrBlank()
        ) {
            "[]"
        } else {
            """
              [{
                "methodName": "$methodName",
                "category": "boundary",
                "testName": "${methodName}_${inputStrategy.replace("-", "_")}",
                "targetParameter": "$targetParameter",
                "inputStrategy": "$inputStrategy",
                "given": "the selected boundary input is used",
                "when": "$methodName is invoked",
                "then": "the result remains observable",
                "requiresMock": false
              }]
            """.trimIndent()
        }

        return """
            {
              "sourceSummary": "Structured planning response from ${config.model}.",
              "scenarios": $scenarios,
              "mockStrategies": [],
              "manualReviewNotes": []
            }
        """.trimIndent()
    }
}
