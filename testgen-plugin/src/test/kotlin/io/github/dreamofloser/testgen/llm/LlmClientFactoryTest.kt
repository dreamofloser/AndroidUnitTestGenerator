package io.github.dreamofloser.testgen.llm

import org.junit.Assert.assertTrue
import org.junit.Test

class LlmClientFactoryTest {
    @Test
    fun createsMockClientByDefault() {
        val client = LlmClientFactory.create(config(provider = "mock"))

        assertTrue(client is MockLlmClient)
    }

    @Test
    fun createsOllamaClient() {
        val client = LlmClientFactory.create(config(provider = "ollama"))

        assertTrue(client is OllamaLlmClient)
    }

    @Test
    fun createsOpenAiCompatibleClient() {
        val client = LlmClientFactory.create(config(provider = "openai-compatible"))

        assertTrue(client is OpenAiCompatibleLlmClient)
    }

    private fun config(provider: String): LlmAgentConfig {
        return LlmAgentConfig(
            enabled = true,
            provider = provider,
            model = "offline-demo",
            agentMode = "planning-and-review",
            endpoint = "",
            apiKeyEnv = "LLM_API_KEY",
        )
    }
}