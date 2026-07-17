package io.github.dreamofloser.testgen.llm

import org.junit.Assert.assertEquals
import org.junit.Test

class LlmHttpSupportTest {
    @Test
    fun extractsOpenAiCompatibleContentField() {
        val json = """
            {"choices":[{"message":{"content":"Plan boundary tests\nand mock dependencies."}}]}
        """.trimIndent()

        assertEquals("Plan boundary tests\nand mock dependencies.", LlmHttpSupport.extractStringField(json, "content"))
    }

    @Test
    fun extractsOllamaResponseField() {
        val json = """
            {"response":"Use MockWebServer for HTTP 500."}
        """.trimIndent()

        assertEquals("Use MockWebServer for HTTP 500.", LlmHttpSupport.extractStringField(json, "response"))
    }
}