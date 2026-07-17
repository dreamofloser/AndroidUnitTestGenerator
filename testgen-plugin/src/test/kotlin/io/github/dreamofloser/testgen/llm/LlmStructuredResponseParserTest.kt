package io.github.dreamofloser.testgen.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmStructuredResponseParserTest {
    private val parser = LlmStructuredResponseParser()

    @Test
    fun parsesStructuredTestPlan() {
        val response = """
            {
              "sourceSummary": "Loads weather and exposes state.",
              "scenarios": [{
                "methodName": "loadWeather",
                "category": "boundary",
                "testName": "emptyCityIsHandled",
                "targetParameter": "city",
                "inputStrategy": "empty-string",
                "given": "the city is empty",
                "when": "loadWeather is called",
                "then": "error state is emitted",
                "requiresMock": true
              }],
              "mockStrategies": [{
                "dependency": "WeatherRepository",
                "approach": "mock",
                "reason": "control success and failure results"
              }],
              "manualReviewNotes": ["Confirm the expected error message."]
            }
        """.trimIndent()

        val plan = parser.parse("sample.WeatherViewModel", response)

        assertEquals(LlmJsonParseStatus.SUCCESS, plan.parseStatus)
        assertEquals("Loads weather and exposes state.", plan.sourceSummary)
        assertEquals(1, plan.scenarios.size)
        assertEquals("loadWeather", plan.scenarios.single().methodName)
        assertEquals("city", plan.scenarios.single().targetParameter)
        assertEquals("empty-string", plan.scenarios.single().inputStrategy)
        assertTrue(plan.scenarios.single().requiresMock)
        assertEquals("WeatherRepository", plan.mockStrategies.single().dependency)
        assertEquals(1, plan.manualReviewNotes.size)
    }

    @Test
    fun acceptsJsonInsideMarkdownFence() {
        val response = """
            ```json
            {
              "sourceSummary": "A simple value object.",
              "scenarios": [],
              "mockStrategies": [],
              "manualReviewNotes": []
            }
            ```
        """.trimIndent()

        val plan = parser.parse("sample.Value", response)

        assertEquals(LlmJsonParseStatus.SUCCESS, plan.parseStatus)
        assertFalse(plan.sourceSummary.isBlank())
    }

    @Test
    fun salvagesScenarioWhenOptionalFieldsAreMissing() {
        val response = """
            {
              "scenarios": [{
                "methodName": "normalize",
                "category": "boundary",
                "given": "an empty string",
                "when": "normalize is called",
                "then": "a result is returned"
              }]
            }
        """.trimIndent()

        val plan = parser.parse("sample.TextFormatter", response)

        assertEquals(LlmJsonParseStatus.SUCCESS, plan.parseStatus)
        assertEquals(1, plan.scenarios.size)
        assertEquals("normalize_boundary_suggested", plan.scenarios.single().testName)
        assertTrue(plan.mockStrategies.isEmpty())
        assertTrue(plan.manualReviewNotes.isEmpty())
    }
    @Test
    fun returnsFallbackInsteadOfFailingGeneration() {
        val plan = parser.parse("sample.Broken", "not valid json")

        assertEquals(LlmJsonParseStatus.FALLBACK, plan.parseStatus)
        assertTrue(plan.scenarios.isEmpty())
        assertFalse(plan.parseMessage.isNullOrBlank())
    }
}