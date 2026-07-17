package io.github.dreamofloser.testgen.llm

import org.junit.Assert.assertEquals
import org.junit.Test

class MockLlmClientTest {
    @Test
    fun selectsFirstAllowedPromptChoice() {
        val client = MockLlmClient(
            LlmAgentConfig(
                enabled = true,
                provider = "mock",
                model = "offline-demo",
                agentMode = "planning-and-review",
                endpoint = "",
                apiKeyEnv = "LLM_API_KEY",
            ),
        )
        val response = client.complete(
            """
                Methods and allowed choices:
                - normalize(value: String): String | allowed choices: value=empty-string
            """.trimIndent(),
        )

        val scenario = LlmStructuredResponseParser()
            .parse("sample.Formatter", response)
            .scenarios
            .single()

        assertEquals("normalize", scenario.methodName)
        assertEquals("value", scenario.targetParameter)
        assertEquals("empty-string", scenario.inputStrategy)
    }
}
