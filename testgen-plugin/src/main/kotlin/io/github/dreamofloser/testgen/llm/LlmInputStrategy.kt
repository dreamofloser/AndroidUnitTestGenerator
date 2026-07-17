package io.github.dreamofloser.testgen.llm

import io.github.dreamofloser.testgen.model.ParameterModel

enum class LlmInputStrategy(val wireName: String) {
    EMPTY_STRING("empty-string"),
    BLANK_STRING("blank-string"),
    NULL("null"),
    ZERO("zero"),
    NEGATIVE("negative"),
    FALSE("false"),
    EMPTY_LIST("empty-list");

    companion object {
        fun fromWireName(value: String?): LlmInputStrategy? {
            return entries.firstOrNull { it.wireName == value?.trim()?.lowercase() }
        }
    }
}

fun ParameterModel.supportedLlmInputStrategies(): List<LlmInputStrategy> {
    val normalized = llmNormalizedType()
    return buildList {
        if (type.trim().endsWith("?")) {
            add(LlmInputStrategy.NULL)
        }
        when {
            normalized in stringTypes -> {
                add(LlmInputStrategy.EMPTY_STRING)
                add(LlmInputStrategy.BLANK_STRING)
            }
            normalized in numericTypes -> {
                add(LlmInputStrategy.ZERO)
                add(LlmInputStrategy.NEGATIVE)
            }
            normalized in booleanTypes -> add(LlmInputStrategy.FALSE)
            normalized.startsWith("List<") ||
                normalized.startsWith("MutableList<") ||
                normalized.startsWith("java.util.List<") -> add(LlmInputStrategy.EMPTY_LIST)
        }
    }.distinct()
}

fun ParameterModel.llmJavaValue(strategy: LlmInputStrategy): String? {
    if (strategy !in supportedLlmInputStrategies()) {
        return null
    }
    return when (strategy) {
        LlmInputStrategy.EMPTY_STRING -> 34.toChar().toString().repeat(2)
        LlmInputStrategy.BLANK_STRING -> "${34.toChar()} ${34.toChar()}"
        LlmInputStrategy.NULL -> "null"
        LlmInputStrategy.ZERO -> javaNumericValue(negative = false)
        LlmInputStrategy.NEGATIVE -> javaNumericValue(negative = true)
        LlmInputStrategy.FALSE -> "false"
        LlmInputStrategy.EMPTY_LIST -> "java.util.Collections.emptyList()"
    }
}

fun ParameterModel.llmKotlinValue(strategy: LlmInputStrategy): String? {
    if (strategy !in supportedLlmInputStrategies()) {
        return null
    }
    return when (strategy) {
        LlmInputStrategy.EMPTY_STRING -> 34.toChar().toString().repeat(2)
        LlmInputStrategy.BLANK_STRING -> "${34.toChar()} ${34.toChar()}"
        LlmInputStrategy.NULL -> "null"
        LlmInputStrategy.ZERO -> kotlinNumericValue(negative = false)
        LlmInputStrategy.NEGATIVE -> kotlinNumericValue(negative = true)
        LlmInputStrategy.FALSE -> "false"
        LlmInputStrategy.EMPTY_LIST -> {
            if (llmNormalizedType().startsWith("MutableList<")) "mutableListOf()" else "emptyList()"
        }
    }
}

private fun ParameterModel.javaNumericValue(negative: Boolean): String? {
    val value = if (negative) "-1" else "0"
    return when (llmNormalizedType()) {
        "byte", "Byte" -> "(byte) $value"
        "short", "Short" -> "(short) $value"
        "int", "Integer", "Int" -> value
        "long", "Long" -> "${value}L"
        "float", "Float" -> "${value}.0f"
        "double", "Double" -> "${value}.0d"
        else -> null
    }
}

private fun ParameterModel.kotlinNumericValue(negative: Boolean): String? {
    val value = if (negative) "-1" else "0"
    return when (llmNormalizedType()) {
        "Byte", "Short", "Int", "int", "Integer" -> value
        "Long", "long" -> "${value}L"
        "Float", "float" -> "${value}.0f"
        "Double", "double" -> "${value}.0"
        else -> null
    }
}

private fun ParameterModel.llmNormalizedType(): String {
    return type.removeSuffix("?")
        .substringBefore('=')
        .trim()
}

private val stringTypes = setOf("String", "java.lang.String")
private val numericTypes = setOf(
    "Byte", "byte",
    "Short", "short",
    "Int", "int", "Integer",
    "Long", "long",
    "Float", "float",
    "Double", "double",
)
private val booleanTypes = setOf("Boolean", "boolean")
