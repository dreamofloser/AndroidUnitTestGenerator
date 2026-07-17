package io.github.dreamofloser.testgen.analysis

import io.github.dreamofloser.testgen.guide.TestCaseGapAnalyzer
import io.github.dreamofloser.testgen.model.ClassModel
import io.github.dreamofloser.testgen.model.ConstructorModel
import io.github.dreamofloser.testgen.model.DependencyCallModel
import io.github.dreamofloser.testgen.model.MethodModel
import io.github.dreamofloser.testgen.model.ParameterModel
import io.github.dreamofloser.testgen.model.PropertyModel
import io.github.dreamofloser.testgen.model.SourceClassKind
import io.github.dreamofloser.testgen.model.SourceLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TestabilityAnalyzerTest {
    @Test
    fun scoresSuspendRepositoryAsHighPriorityCoroutineTarget() {
        val model = repositoryModel()

        val insight = TestabilityAnalyzer().analyze(model).single()

        assertTrue(insight.difficultyScore >= 30)
        assertTrue(insight.priorityScore >= 60)
        assertTrue(insight.automationConfidence >= 60)
        assertEquals(TestStrategy.COROUTINE_UNIT, insight.recommendedStrategy)
        assertEquals(TestValueQuadrant.PRIORITY_AUTOMATION, insight.quadrant)
        assertTrue(insight.evidence.any { it.driver == DifficultyDriver.DEPENDENCIES })
        assertTrue(insight.evidence.any { it.driver == DifficultyDriver.ASYNC_STATE })
        assertTrue(insight.evidence.any { it.driver == DifficultyDriver.EXTERNAL_RESOURCES })
        assertTrue(insight.boundaryFocus.contains("city=empty-string"))
        assertTrue(insight.boundaryFocus.contains("coroutine-failure"))
    }

    @Test
    fun keepsPureArithmeticTargetEasyAndAutomationReady() {
        val model = ClassModel(
            packageName = "com.example",
            className = "Calculator",
            sourceFile = File("Calculator.java"),
            constructors = listOf(ConstructorModel(emptyList())),
            methods = listOf(
                MethodModel(
                    name = "add",
                    returnType = "int",
                    parameters = listOf(
                        ParameterModel("left", "int"),
                        ParameterModel("right", "int"),
                    ),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                    returnExpressions = listOf("left + right"),
                ),
            ),
        )

        val insight = TestabilityAnalyzer().analyze(model).single()

        assertEquals(InsightScoreLevel.LOW, insight.difficultyLevel)
        assertTrue(insight.automationConfidence >= 90)
        assertEquals(TestStrategy.PURE_UNIT, insight.recommendedStrategy)
        assertEquals(TestValueQuadrant.BATCH_AUTOMATION, insight.quadrant)
    }

    @Test
    fun ordersGuideCandidatesByTestabilityValue() {
        val lowValueMethod = MethodModel(
            name = "format",
            returnType = "String",
            parameters = listOf(ParameterModel("value", "String")),
            isStatic = false,
            thrownExceptions = emptyList(),
        )
        val highValueMethod = MethodModel(
            name = "load",
            returnType = "String",
            parameters = listOf(ParameterModel("id", "String")),
            isStatic = false,
            thrownExceptions = emptyList(),
        )
        val model = ClassModel(
            packageName = "com.example",
            className = "LookupService",
            sourceFile = File("LookupService.kt"),
            constructors = listOf(ConstructorModel(emptyList())),
            methods = listOf(lowValueMethod, highValueMethod),
            language = SourceLanguage.KOTLIN,
            classKind = SourceClassKind.REGULAR,
        )
        val insights = listOf(
            testInsight("format", guideValue = 35),
            testInsight("load", guideValue = 88),
        ).associateBy { it.methodKey }

        val candidates = TestCaseGapAnalyzer().findCandidates(
            model = model,
            insightsByMethod = insights,
        )

        assertEquals("load", candidates.first().methodName)
        assertTrue(candidates.first().guidePriorityScore > candidates.last().guidePriorityScore)
    }

    @Test
    fun recommendsStateTestingForObservablePropertyInteraction() {
        val model = ClassModel(
            packageName = "com.example",
            className = "LoginViewModel",
            sourceFile = File("LoginViewModel.java"),
            imports = listOf("androidx.lifecycle.MutableLiveData"),
            constructors = listOf(
                ConstructorModel(listOf(ParameterModel("repository", "LoginRepository"))),
            ),
            methods = listOf(
                MethodModel(
                    name = "loadDisplayName",
                    returnType = "void",
                    parameters = listOf(ParameterModel("userId", "String")),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                    dependencyCalls = listOf(
                        DependencyCallModel("repository", "loadDisplayName", listOf("userId")),
                        DependencyCallModel("displayName", "setValue", listOf("name")),
                    ),
                ),
            ),
            properties = listOf(PropertyModel("displayName", "MutableLiveData<String>")),
        )

        val insight = TestabilityAnalyzer().analyze(model).single()

        assertEquals(TestStrategy.VIEW_MODEL_STATE, insight.recommendedStrategy)
        assertTrue(insight.evidence.any { it.driver == DifficultyDriver.ASYNC_STATE })
    }

    @Test
    fun doesNotTreatMethodParameterCallsAsInjectedDependencies() {
        val model = ClassModel(
            packageName = "com.example",
            className = "IntentInspector",
            sourceFile = File("IntentInspector.java"),
            imports = listOf("android.content.Intent"),
            constructors = listOf(ConstructorModel(emptyList())),
            methods = listOf(
                MethodModel(
                    name = "isShareIntent",
                    returnType = "boolean",
                    parameters = listOf(ParameterModel("intent", "Intent")),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                    dependencyCalls = listOf(
                        DependencyCallModel("intent", "getAction", emptyList()),
                    ),
                ),
            ),
        )

        val insight = TestabilityAnalyzer().analyze(model).single()

        assertTrue(insight.evidence.none { it.driver == DifficultyDriver.DEPENDENCIES })
        assertTrue(insight.boundaryFocus.none { it == "dependency-failure" })
    }

    private fun repositoryModel(): ClassModel {
        return ClassModel(
            packageName = "com.example",
            className = "WeatherRepository",
            sourceFile = File("WeatherRepository.kt"),
            constructors = listOf(
                ConstructorModel(listOf(ParameterModel("api", "WeatherApi"))),
            ),
            methods = listOf(
                MethodModel(
                    name = "loadForecast",
                    returnType = "Result<Forecast>",
                    parameters = listOf(ParameterModel("city", "String")),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                    isSuspend = true,
                    dependencyCalls = listOf(
                        DependencyCallModel("api", "fetchForecast", listOf("city")),
                    ),
                ),
            ),
            language = SourceLanguage.KOTLIN,
            classKind = SourceClassKind.REGULAR,
        )
    }

    private fun testInsight(methodName: String, guideValue: Int): TestabilityInsight {
        val priority = guideValue.coerceIn(0, 100)
        return TestabilityInsight(
            sourceClass = "com.example.LookupService",
            methodName = methodName,
            difficultyScore = 20,
            priorityScore = priority,
            automationConfidence = 90,
            recommendedStrategy = TestStrategy.PURE_UNIT,
            evidence = emptyList(),
            boundaryFocus = listOf("empty-string"),
        )
    }
}
