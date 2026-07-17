package io.github.dreamofloser.testgen.llm

class OpenAiCompatibleLlmClient(
    private val config: LlmAgentConfig,
) : LlmClient {
    override fun complete(prompt: String): String {
        val endpoint = config.endpoint.ifBlank { DEFAULT_ENDPOINT }
        val apiKey = System.getenv(config.apiKeyEnv).orEmpty()
        if (apiKey.isBlank()) {
            return "OpenAI-compatible LLM client is configured, but environment variable ${config.apiKeyEnv} is not set."
        }

        return runCatching {
            val escapedSystem = LlmHttpSupport.escapeJson(SYSTEM_PROMPT)
            val escapedPrompt = LlmHttpSupport.escapeJson(prompt)
            val body = """
                {
                  "model": "${LlmHttpSupport.escapeJson(config.model)}",
                  "temperature": 0.2,
                  "messages": [
                    {"role": "system", "content": "$escapedSystem"},
                    {"role": "user", "content": "$escapedPrompt"}
                  ]
                }
            """.trimIndent()
            val response = LlmHttpSupport.postJson(
                endpoint = endpoint,
                body = body,
                headers = mapOf("Authorization" to "Bearer $apiKey"),
            )
            LlmHttpSupport.extractStringField(response, "content") ?: response.take(500)
        }.getOrElse { error ->
            "OpenAI-compatible LLM request was unavailable: ${error.message}"
        }
    }

    private companion object {
        const val DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions"
        const val SYSTEM_PROMPT = "You are a unit-test planning agent. Return concise test scenario and mock strategy advice."
    }
}