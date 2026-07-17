package io.github.dreamofloser.testgen.llm

object LlmStructuredSchema {
    val ollamaFormat: String = """
        {
          "type": "object",
          "properties": {
            "sourceSummary": {"type": "string"},
            "scenarios": {
              "type": "array",
              "maxItems": 2,
              "items": {
                "type": "object",
                "properties": {
                  "methodName": {"type": "string"},
                  "category": {
                    "type": "string",
                    "enum": ["boundary"]
                  },
                  "testName": {"type": "string"},
                  "targetParameter": {"type": "string"},
                  "inputStrategy": {
                    "type": "string",
                    "enum": ["empty-string", "blank-string", "null", "zero", "negative", "false", "empty-list"]
                  },
                  "given": {"type": "string"},
                  "when": {"type": "string"},
                  "then": {"type": "string"},
                  "requiresMock": {"type": "boolean"}
                },
                "required": [
                  "methodName",
                  "category",
                  "testName",
                  "targetParameter",
                  "inputStrategy",
                  "given",
                  "when",
                  "then",
                  "requiresMock"
                ],
                "additionalProperties": false
              }
            },
            "mockStrategies": {
              "type": "array",
              "maxItems": 2,
              "items": {
                "type": "object",
                "properties": {
                  "dependency": {"type": "string"},
                  "approach": {"type": "string"},
                  "reason": {"type": "string"}
                },
                "required": ["dependency", "approach", "reason"],
                "additionalProperties": false
              }
            },
            "manualReviewNotes": {
              "type": "array",
              "maxItems": 2,
              "items": {"type": "string"}
            }
          },
          "required": ["sourceSummary", "scenarios", "mockStrategies", "manualReviewNotes"],
          "additionalProperties": false
        }
    """.trimIndent()
}
