package com.codex.testgen.parser

import com.codex.testgen.model.ClassModel
import com.codex.testgen.model.ConstructorModel
import com.codex.testgen.model.MethodModel
import com.codex.testgen.model.ParameterModel
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
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
        )
    }
}
