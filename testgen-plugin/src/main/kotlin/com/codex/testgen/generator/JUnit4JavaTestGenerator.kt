package com.codex.testgen.generator

import com.codex.testgen.model.ClassModel
import com.codex.testgen.model.ConstructorModel
import com.codex.testgen.model.MethodModel

class JUnit4JavaTestGenerator(
    private val sampleValueProvider: SampleValueProvider = SampleValueProvider(),
) {
    fun generate(model: ClassModel): String {
        val builder = StringBuilder()

        if (model.packageName.isNotBlank()) {
            builder.appendLine("package ${model.packageName};")
            builder.appendLine()
        }

        builder.appendLine("import org.junit.Test;")

        if (model.methods.any { it.shouldAssertNotNull() }) {
            builder.appendLine("import static org.junit.Assert.assertNotNull;")
        }

        builder.appendLine()
        builder.appendLine("public class ${model.className}Test {")
        builder.appendLine()

        model.methods.forEach { method ->
            appendTestMethod(builder, model, method)
            builder.appendLine()
        }

        builder.appendLine("}")
        return builder.toString()
    }

    private fun appendTestMethod(builder: StringBuilder, model: ClassModel, method: MethodModel) {
        builder.appendLine("    @Test")
        builder.appendLine("    public void ${method.testName()}() throws Exception {")

        val methodArguments = method.parameters.joinToString(", ") { sampleValueProvider.valueFor(it.type) }

        if (method.isStatic) {
            appendInvocation(builder, model.className, method, methodArguments)
        } else {
            val constructor = model.bestConstructor()
            val constructorArguments = constructor.parameters.joinToString(", ") { sampleValueProvider.valueFor(it.type) }
            builder.appendLine("        ${model.className} target = new ${model.className}($constructorArguments);")
            builder.appendLine()
            appendInvocation(builder, "target", method, methodArguments)
        }

        builder.appendLine("    }")
    }

    private fun appendInvocation(
        builder: StringBuilder,
        receiver: String,
        method: MethodModel,
        methodArguments: String,
    ) {
        val invocation = "$receiver.${method.name}($methodArguments)"

        if (method.returnType == "void") {
            builder.appendLine("        $invocation;")
            return
        }

        builder.appendLine("        ${method.returnType} result = $invocation;")

        if (method.shouldAssertNotNull()) {
            builder.appendLine()
            builder.appendLine("        assertNotNull(result);")
        }
    }

    private fun ClassModel.bestConstructor(): ConstructorModel {
        return constructors
            .filter { it.parameters.isEmpty() }
            .ifEmpty { constructors }
            .minByOrNull { it.parameters.size }
            ?: ConstructorModel(emptyList())
    }

    private fun MethodModel.testName(): String {
        return "${name}_shouldRunWithoutException"
    }

    private fun MethodModel.shouldAssertNotNull(): Boolean {
        return returnType !in primitiveAndVoidTypes
    }

    private companion object {
        val primitiveAndVoidTypes = setOf(
            "void",
            "byte",
            "short",
            "int",
            "long",
            "float",
            "double",
            "boolean",
            "char",
        )
    }
}
