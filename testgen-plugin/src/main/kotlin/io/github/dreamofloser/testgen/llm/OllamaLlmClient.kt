package io.github.dreamofloser.testgen.llm

class OllamaLlmClient(
    private val config: LlmAgentConfig,
) : LlmClient {
    override fun complete(prompt: String): String {
        val endpoint = config.endpoint.ifBlank { DEFAULT_ENDPOINT }
        return runCatching {
            val body = """
                {
                  "model": "${LlmHttpSupport.escapeJson(config.model)}",
                  "prompt": "${LlmHttpSupport.escapeJson(prompt)}",
                  "format": ${LlmStructuredSchema.ollamaFormat},
                  "stream": false,
                  "keep_alive": "10m",
                  "options": {
                    "temperature": 0,
                    "seed": 42,
                    "num_ctx": 4096,
                    "num_predict": 512
                  }
                }
            """.trimIndent()
            val response = LlmHttpSupport.postJson(endpoint = endpoint, body = body)
            LlmHttpSupport.extractStringField(response, "response") ?: response.take(500)
        }.getOrElse { error ->
            "Ollama LLM request was unavailable: ${error.message ?: error::class.java.simpleName}"
        }
    }

    private companion object {
        const val DEFAULT_ENDPOINT = "http://localhost:11434/api/generate"
    }
}