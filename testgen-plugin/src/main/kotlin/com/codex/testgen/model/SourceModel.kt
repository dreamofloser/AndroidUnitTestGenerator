package com.codex.testgen.model

import java.io.File

data class ClassModel(
    val packageName: String,
    val className: String,
    val sourceFile: File,
    val constructors: List<ConstructorModel>,
    val methods: List<MethodModel>,
)

data class ConstructorModel(
    val parameters: List<ParameterModel>,
)

data class MethodModel(
    val name: String,
    val returnType: String,
    val parameters: List<ParameterModel>,
    val isStatic: Boolean,
    val thrownExceptions: List<String>,
)

data class ParameterModel(
    val name: String,
    val type: String,
)
