package com.codex.testgen.scanner

import java.io.File

class SourceScanner {
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
}
