package com.codex.testgen.generator

import com.codex.testgen.model.ClassModel
import com.codex.testgen.model.MethodModel
import com.codex.testgen.model.ParameterModel
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class JUnit4JavaTestGeneratorTest {
    @Test
    fun generatesJUnit4TestClass() {
        val model = ClassModel(
            packageName = "com.example",
            className = "Calculator",
            sourceFile = File("Calculator.java"),
            constructors = emptyList(),
            methods = listOf(
                MethodModel(
                    name = "add",
                    returnType = "int",
                    parameters = listOf(
                        ParameterModel("a", "int"),
                        ParameterModel("b", "int"),
                    ),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                ),
            ),
        )

        val source = JUnit4JavaTestGenerator().generate(model)

        assertTrue(source.contains("package com.example;"))
        assertTrue(source.contains("public class CalculatorTest"))
        assertTrue(source.contains("Calculator target = new Calculator();"))
        assertTrue(source.contains("int result = target.add(1, 1);"))
    }
}
