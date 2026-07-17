package io.github.dreamofloser.testgen.llm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmStructuredSchemaTest {
    @Test
    fun definesValidOllamaJsonSchema() {
        val root = Json.parseToJsonElement(LlmStructuredSchema.ollamaFormat).jsonObject
        val scenarios = root["properties"]!!
            .jsonObject["scenarios"]!!
            .jsonObject

        assertEquals("object", root["type"]!!.jsonPrimitive.content)
        assertEquals("2", scenarios["maxItems"].toString())
        assertTrue(root.containsKey("required"))
    }
}