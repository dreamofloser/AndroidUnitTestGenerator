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
    val returnExpressions: List<String> = emptyList(),
    val conditionExpressions: List<String> = emptyList(),
    val thrownStatementTypes: List<String> = emptyList(),
    val dependencyCalls: List<DependencyCallModel> = emptyList(),
)

data class ParameterModel(
    val name: String,
    val type: String,
)

data class DependencyCallModel(
    val receiverName: String,
    val methodName: String,
    val arguments: List<String>,
)
