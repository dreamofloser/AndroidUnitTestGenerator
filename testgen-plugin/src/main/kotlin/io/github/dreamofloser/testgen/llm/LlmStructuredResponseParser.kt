package io.github.dreamofloser.testgen.llm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class LlmStructuredResponseParser(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun parse(sourceClass: String, response: String): LlmStructuredTestPlan {
        return runCatching {
            require(!response.isTransportFailure()) { LlmHttpSupport.compactAdvice(response, 240) }
            val root = json.parseToJsonElement(extractJsonObject(response)) as? JsonObject
                ?: error("The LLM response root must be a JSON object.")
            LlmStructuredTestPlan(
                sourceClass = sourceClass,
                sourceSummary = root.optionalString("sourceSummary")
                    ?.takeIf { it.isNotBlank() }
                    ?: "No reliable source summary was returned.",
                scenarios = root.arrayOrEmpty("scenarios")
                    .take(2)
                    .mapNotNull { element -> parseScenario(element as? JsonObject) },
                mockStrategies = root.arrayOrEmpty("mockStrategies")
                    .take(2)
                    .mapNotNull { element -> parseMockStrategy(element as? JsonObject) },
                manualReviewNotes = root.arrayOrEmpty("manualReviewNotes")
                    .take(2)
                    .mapNotNull { element -> (element as? JsonPrimitive)?.contentOrNull },
                parseStatus = LlmJsonParseStatus.SUCCESS,
            )
        }.getOrElse { parseError ->
            LlmStructuredTestPlan(
                sourceClass = sourceClass,
                sourceSummary = "The configured model did not return a valid structured test plan.",
                scenarios = emptyList(),
                mockStrategies = emptyList(),
                manualReviewNotes = listOf("Review the raw model response and retry generation."),
                parseStatus = LlmJsonParseStatus.FALLBACK,
                parseMessage = parseError.message ?: parseError::class.simpleName,
            )
        }
    }

    private fun parseScenario(scenario: JsonObject?): LlmTestScenario? {
        scenario ?: return null
        val methodName = scenario.optionalString("methodName")
            ?.substringBefore("(")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val category = scenario.optionalString("category")?.takeIf { it.isNotBlank() } ?: return null
        val testName = scenario.optionalString("testName")
            ?.takeIf { it.isNotBlank() }
            ?: "${methodName}_${category.replace("-", "_")}_suggested"

        return LlmTestScenario(
            methodName = methodName,
            category = category,
            testName = testName,
            given = scenario.optionalString("given") ?: "Model did not provide a precondition.",
            whenAction = scenario.optionalString("when") ?: "Invoke $methodName.",
            then = scenario.optionalString("then") ?: "Review the observable result.",
            requiresMock = scenario["requiresMock"]?.jsonPrimitive?.booleanOrNull ?: false,
            targetParameter = scenario.optionalString("targetParameter")
                ?.trim()
                ?.takeIf { it.isNotBlank() },
            inputStrategy = scenario.optionalString("inputStrategy")
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotBlank() },
        )
    }

    private fun parseMockStrategy(strategy: JsonObject?): LlmMockStrategy? {
        strategy ?: return null
        val dependency = strategy.optionalString("dependency")?.takeIf { it.isNotBlank() } ?: return null
        return LlmMockStrategy(
            dependency = dependency,
            approach = strategy.optionalString("approach") ?: "manual review",
            reason = strategy.optionalString("reason") ?: "No reason was returned.",
        )
    }

    private fun String.isTransportFailure(): Boolean {
        val value = trimStart()
        return value.startsWith("LLM request failed") ||
            value.startsWith("Ollama LLM request was unavailable") ||
            value.startsWith("OpenAI-compatible LLM request was unavailable")
    }

    private fun extractJsonObject(response: String): String {
        val withoutFence = response.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = withoutFence.indexOf('{')
        val end = withoutFence.lastIndexOf('}')
        require(start >= 0 && end > start) { "No complete JSON object was found in the LLM response." }
        return withoutFence.substring(start, end + 1)
    }

    private fun JsonObject.optionalString(name: String): String? {
        return (this[name] as? JsonPrimitive)?.contentOrNull
    }

    private fun JsonObject.arrayOrEmpty(name: String): JsonArray {
        return this[name] as? JsonArray ?: JsonArray(emptyList())
    }
}