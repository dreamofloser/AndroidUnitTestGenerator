package io.github.dreamofloser.testgen.model

import java.io.File

data class ClassModel(
    val packageName: String,
    val className: String,
    val sourceFile: File,
    val imports: List<String> = emptyList(),
    val constructors: List<ConstructorModel>,
    val methods: List<MethodModel>,
    val properties: List<PropertyModel> = emptyList(),
    val language: SourceLanguage = SourceLanguage.JAVA,
    val classKind: SourceClassKind = SourceClassKind.REGULAR,
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
    val isSuspend: Boolean = false,
    val returnExpressions: List<String> = emptyList(),
    val conditionExpressions: List<String> = emptyList(),
    val thrownStatementTypes: List<String> = emptyList(),
    val dependencyCalls: List<DependencyCallModel> = emptyList(),
    val httpMethod: String? = null,
    val httpPath: String? = null,
    val httpQueryNames: List<String> = emptyList(),
)

data class ParameterModel(
    val name: String,
    val type: String,
)

data class PropertyModel(
    val name: String,
    val type: String,
)

data class DependencyCallModel(
    val receiverName: String,
    val methodName: String,
    val arguments: List<String>,
)

enum class SourceLanguage {
    JAVA,
    KOTLIN,
}

enum class SourceClassKind {
    REGULAR,
    DATA,
    VIEW_MODEL,
    COMPOSE_UI,
    ACTIVITY,
    FRAGMENT,
    ROOM_DAO,
    RETROFIT_API,
}
