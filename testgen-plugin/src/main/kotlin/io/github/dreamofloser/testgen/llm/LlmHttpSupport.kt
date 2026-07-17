package io.github.dreamofloser.testgen.llm

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object LlmHttpSupport {
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun postJson(endpoint: String, body: String, headers: Map<String, String> = emptyMap()): String {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))

        headers.forEach { (name, value) -> builder.header(name, value) }

        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        return if (response.statusCode() in 200..299) {
            response.body()
        } else {
            "LLM request failed with HTTP ${response.statusCode()}: ${response.body().take(240)}"
        }
    }

    fun escapeJson(value: String): String {
        return buildString {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }

    fun extractStringField(json: String, fieldName: String): String? {
        val marker = "\"$fieldName\""
        val markerIndex = json.indexOf(marker)
        if (markerIndex < 0) return null
        val colonIndex = json.indexOf(':', markerIndex + marker.length)
        if (colonIndex < 0) return null
        val firstQuote = json.indexOf('"', colonIndex + 1)
        if (firstQuote < 0) return null

        val builder = StringBuilder()
        var index = firstQuote + 1
        var escaped = false
        while (index < json.length) {
            val char = json[index]
            if (escaped) {
                builder.append(
                    when (char) {
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        '"' -> '"'
                        '\\' -> '\\'
                        else -> char
                    },
                )
                escaped = false
            } else {
                when (char) {
                    '\\' -> escaped = true
                    '"' -> return builder.toString()
                    else -> builder.append(char)
                }
            }
            index++
        }
        return null
    }

    fun compactAdvice(text: String, maxLength: Int = 500): String {
        val compact = text
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .replace("|", "/")
        return if (compact.length <= maxLength) compact else compact.take(maxLength - 3) + "..."
    }
}