package io.github.dreamofloser.testgen.llm

interface LlmClient {
    fun complete(prompt: String): String
}

object LlmClientFactory {
    fun create(config: LlmAgentConfig): LlmClient {
        return when (config.provider.lowercase()) {
            "mock", "offline", "offline-demo" -> MockLlmClient(config)
            "ollama", "local-ollama" -> OllamaLlmClient(config)
            "api", "openai", "openai-compatible", "deepseek", "siliconflow" -> OpenAiCompatibleLlmClient(config)
            else -> MockLlmClient(config)
        }
    }
}