package com.codex.testgen.scanner

import java.io.File

class SourceScanner {
    fun findSourceFiles(sourceDir: File): List<File> {
        if (!sourceDir.exists()) {
            return emptyList()
        }

        return sourceDir
            .walkTopDown()
            .filter { it.isFile && it.extension in supportedExtensions }
            .sortedBy { it.absolutePath }
            .toList()
    }

    fun findJavaFiles(sourceDir: File): List<File> {
        if (!sourceDir.exists()) {
            return emptyList()
        }

        return sourceDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .sortedBy { it.absolutePath }
            .toList()
    }

    private companion object {
        val supportedExtensions = setOf("java", "kt")
    }
}
