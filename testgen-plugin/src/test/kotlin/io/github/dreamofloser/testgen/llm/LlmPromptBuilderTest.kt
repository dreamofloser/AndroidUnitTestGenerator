package io.github.dreamofloser.testgen.llm

import io.github.dreamofloser.testgen.model.ClassModel
import io.github.dreamofloser.testgen.model.ConstructorModel
import io.github.dreamofloser.testgen.model.MethodModel
import io.github.dreamofloser.testgen.model.ParameterModel
import io.github.dreamofloser.testgen.model.SourceClassKind
import io.github.dreamofloser.testgen.model.SourceLanguage
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LlmPromptBuilderTest {
    @Test
    fun exposesOnlySupportedGenerationChoices() {
        val model = ClassModel(
            packageName = "sample",
            className = "Formatter",
            sourceFile = File("Formatter.kt"),
            constructors = listOf(ConstructorModel(emptyList())),
            methods = listOf(
                MethodModel(
                    name = "format",
                    returnType = "String",
                    parameters = listOf(
                        ParameterModel("value", "String"),
                        ParameterModel("count", "Int"),
                    ),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                ),
            ),
            language = SourceLanguage.KOTLIN,
            classKind = SourceClassKind.REGULAR,
        )

        val prompt = LlmPromptBuilder().buildPlanningPrompt(model)

        assertTrue(prompt.contains("value=empty-string"))
        assertTrue(prompt.contains("value=blank-string"))
        assertTrue(prompt.contains("count=zero"))
        assertTrue(prompt.contains("count=negative"))
        assertTrue(prompt.contains("targetParameter"))
        assertTrue(prompt.contains("inputStrategy"))
    }
}
