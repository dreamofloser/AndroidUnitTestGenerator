package com.codex.testgen.parser

import com.codex.testgen.model.ClassModel
import com.codex.testgen.model.ConstructorModel
import com.codex.testgen.model.DependencyCallModel
import com.codex.testgen.model.MethodModel
import com.codex.testgen.model.ParameterModel
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.ThrowStmt
import java.io.File

class JavaSourceParser(
    private val javaParser: JavaParser = JavaParser(),
) {
    fun parse(sourceFile: File): List<ClassModel> {
        val result = javaParser.parse(sourceFile)
        if (!result.isSuccessful || result.result.isEmpty) {
            return emptyList()
        }

        val compilationUnit = result.result.get()
        val packageName = compilationUnit.packageDeclaration
            .map { it.nameAsString }
            .orElse("")

        return compilationUnit.types
            .filterIsInstance<ClassOrInterfaceDeclaration>()
            .filter { declaration -> declaration.isPublic && !declaration.isInterface }
            .map { declaration ->
                ClassModel(
                    packageName = packageName,
                    className = declaration.nameAsString,
                    sourceFile = sourceFile,
                    constructors = declaration.constructors.map { it.toModel() },
                    methods = declaration.methods
                        .filter { it.canGenerateTest() }
                        .map { it.toModel() },
                )
            }
    }

    private fun ConstructorDeclaration.toModel(): ConstructorModel {
        return ConstructorModel(
            parameters = parameters.map {
                ParameterModel(
                    name = it.nameAsString,
                    type = it.type.asString(),
                )
            },
        )
    }

    private fun MethodDeclaration.canGenerateTest(): Boolean {
        return isPublic && !isAbstract && !isNative
    }

    private fun MethodDeclaration.toModel(): MethodModel {
        return MethodModel(
            name = nameAsString,
            returnType = type.asString(),
            parameters = parameters.map {
                ParameterModel(
                    name = it.nameAsString,
                    type = it.type.asString(),
                )
            },
            isStatic = isStatic,
            thrownExceptions = thrownExceptions.map { it.asString() },
            returnExpressions = findAll(ReturnStmt::class.java)
                .mapNotNull { returnStmt ->
                    returnStmt.expression.orElse(null)?.toString()
                },
            conditionExpressions = findAll(IfStmt::class.java)
                .map { it.condition.toString() },
            thrownStatementTypes = findAll(ThrowStmt::class.java)
                .mapNotNull { throwStmt ->
                    (throwStmt.expression as? ObjectCreationExpr)?.type?.toString()
                },
            dependencyCalls = findAll(MethodCallExpr::class.java)
                .mapNotNull { callExpr ->
                    val receiverName = callExpr.scope.orElse(null)?.toString() ?: return@mapNotNull null
                    DependencyCallModel(
                        receiverName = receiverName,
                        methodName = callExpr.nameAsString,
                        arguments = callExpr.arguments.map { it.toString() },
                    )
                },
        )
    }
}
