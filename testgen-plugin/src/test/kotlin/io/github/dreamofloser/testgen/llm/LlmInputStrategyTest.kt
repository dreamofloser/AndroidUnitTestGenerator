package io.github.dreamofloser.testgen.llm

import io.github.dreamofloser.testgen.model.ParameterModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmInputStrategyTest {
    @Test
    fun mapsNumericStrategiesToConcreteValues() {
        val parameter = ParameterModel("count", "Int")

        assertEquals("0", parameter.llmKotlinValue(LlmInputStrategy.ZERO))
        assertEquals("-1", parameter.llmKotlinValue(LlmInputStrategy.NEGATIVE))
        assertTrue(LlmInputStrategy.ZERO in parameter.supportedLlmInputStrategies())
    }

    @Test
    fun mapsStringAndCollectionStrategies() {
        val emptyString = 34.toChar().toString().repeat(2)

        assertEquals(
            emptyString,
            ParameterModel("value", "String").llmJavaValue(LlmInputStrategy.EMPTY_STRING),
        )
        assertEquals(
            "${34.toChar()} ${34.toChar()}",
            ParameterModel("value", "String").llmKotlinValue(LlmInputStrategy.BLANK_STRING),
        )
        assertEquals(
            "emptyList()",
            ParameterModel("values", "List<String>").llmKotlinValue(LlmInputStrategy.EMPTY_LIST),
        )
        assertTrue(LlmInputStrategy.BLANK_STRING in ParameterModel("value", "String").supportedLlmInputStrategies())
    }

    @Test
    fun rejectsMismatchedStrategy() {
        assertEquals(
            null,
            ParameterModel("value", "String").llmKotlinValue(LlmInputStrategy.FALSE),
        )
    }
}
