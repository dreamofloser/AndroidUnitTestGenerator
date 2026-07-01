package com.codex.testgen.generator

import com.codex.testgen.model.ClassModel
import com.codex.testgen.model.ConstructorModel
import com.codex.testgen.model.DependencyCallModel
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
                    returnExpressions = listOf("a + b"),
                ),
            ),
        )

        val result = JUnit4JavaTestGenerator().generate(model)
        val source = result.source

        assertTrue(source.contains("package com.example;"))
        assertTrue(source.contains("public class CalculatorTest"))
        assertTrue(source.contains("Calculator target = new Calculator();"))
        assertTrue(source.contains("int result = target.add(1, 1);"))
        assertTrue(source.contains("assertEquals(2, result);"))
        assertTrue(result.testMethodCount == 1)
        assertTrue(result.assertionCount == 1)
    }

    @Test
    fun generatesMockitoTestForConstructorDependency() {
        val model = ClassModel(
            packageName = "com.example",
            className = "UserService",
            sourceFile = File("UserService.java"),
            constructors = listOf(
                ConstructorModel(
                    parameters = listOf(ParameterModel("repository", "UserRepository")),
                ),
            ),
            methods = listOf(
                MethodModel(
                    name = "getUserName",
                    returnType = "String",
                    parameters = listOf(ParameterModel("id", "String")),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                    returnExpressions = listOf("repository.getUserName(id)"),
                    dependencyCalls = listOf(
                        DependencyCallModel(
                            receiverName = "repository",
                            methodName = "getUserName",
                            arguments = listOf("id"),
                        ),
                    ),
                ),
            ),
        )

        val result = JUnit4JavaTestGenerator().generate(model)
        val source = result.source

        assertTrue(source.contains("import org.junit.Before;"))
        assertTrue(source.contains("import static org.mockito.Mockito.mock;"))
        assertTrue(source.contains("import static org.mockito.Mockito.when;"))
        assertTrue(source.contains("import static org.mockito.Mockito.verify;"))
        assertTrue(source.contains("private UserRepository repository;"))
        assertTrue(source.contains("repository = mock(UserRepository.class);"))
        assertTrue(source.contains("target = new UserService(repository);"))
        assertTrue(source.contains("when(repository.getUserName(\"sample\")).thenReturn(\"sample\");"))
        assertTrue(source.contains("assertEquals(\"sample\", result);"))
        assertTrue(source.contains("verify(repository).getUserName(\"sample\");"))
        assertTrue(result.mockedDependencyCount == 1)
        assertTrue(result.mockStubCount == 1)
        assertTrue(result.mockVerificationCount == 1)
    }
}
