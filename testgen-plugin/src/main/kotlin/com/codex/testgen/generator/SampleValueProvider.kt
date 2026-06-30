package com.codex.testgen.generator

class SampleValueProvider {
    fun valueFor(type: String): String {
        return when (type.removeSuffix("?")) {
            "byte", "Byte" -> "(byte) 1"
            "short", "Short" -> "(short) 1"
            "int", "Integer" -> "1"
            "long", "Long" -> "1L"
            "float", "Float" -> "1.0f"
            "double", "Double" -> "1.0d"
            "boolean", "Boolean" -> "true"
            "char", "Character" -> "'a'"
            "String", "java.lang.String" -> "\"sample\""
            else -> complexValueFor(type)
        }
    }

    private fun complexValueFor(type: String): String {
        if (type.endsWith("[]")) {
            return "new ${type.removeSuffix("[]")}[0]"
        }

        if (type.startsWith("List<") || type.startsWith("java.util.List<")) {
            return "java.util.Collections.emptyList()"
        }

        if (type.startsWith("Set<") || type.startsWith("java.util.Set<")) {
            return "java.util.Collections.emptySet()"
        }

        if (type.startsWith("Map<") || type.startsWith("java.util.Map<")) {
            return "java.util.Collections.emptyMap()"
        }

        return "null"
    }
}
