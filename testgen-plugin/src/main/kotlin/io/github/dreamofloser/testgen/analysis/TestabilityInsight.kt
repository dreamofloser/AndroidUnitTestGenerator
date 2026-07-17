package io.github.dreamofloser.testgen.analysis

enum class InsightScoreLevel {
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH;

    companion object {
        fun fromScore(score: Int): InsightScoreLevel {
            return when (score.coerceIn(0, 100)) {
                in 0..29 -> LOW
                in 30..59 -> MEDIUM
                in 60..79 -> HIGH
                else -> VERY_HIGH
            }
        }
    }
}

enum class TestValueQuadrant(val displayName: String) {
    PRIORITY_AUTOMATION("Priority automation"),
    GUIDED_GENERATION("Guided generation"),
    BATCH_AUTOMATION("Batch automation"),
    DEFER_OR_REVIEW("Defer or review"),
}

enum class TestStrategy(val displayName: String) {
    PURE_UNIT("JUnit pure unit"),
    MOCKED_UNIT("Mockito/MockK unit"),
    COROUTINE_UNIT("Coroutine runTest"),
    VIEW_MODEL_STATE("ViewModel state"),
    ROBOLECTRIC("Robolectric"),
    COMPOSE_UI("Compose UI"),
    ROOM_IN_MEMORY("Room in-memory"),
    RETROFIT_CONTRACT("Retrofit contract"),
    MANUAL_REVIEW("Manual review"),
}

enum class DifficultyDriver(val displayName: String) {
    CONTROL_FLOW("Control flow"),
    DEPENDENCIES("Dependencies"),
    ASYNC_STATE("Async and state"),
    ANDROID_FRAMEWORK("Android framework"),
    EXTERNAL_RESOURCES("External resources"),
    GENERATOR_LIMITATIONS("Generator limitations"),
}

data class DifficultyEvidence(
    val driver: DifficultyDriver,
    val points: Int,
    val detail: String,
)

data class TestabilityInsight(
    val sourceClass: String,
    val methodName: String,
    val difficultyScore: Int,
    val priorityScore: Int,
    val automationConfidence: Int,
    val recommendedStrategy: TestStrategy,
    val evidence: List<DifficultyEvidence>,
    val boundaryFocus: List<String>,
) {
    val difficultyLevel: InsightScoreLevel
        get() = InsightScoreLevel.fromScore(difficultyScore)

    val priorityLevel: InsightScoreLevel
        get() = InsightScoreLevel.fromScore(priorityScore)

    val quadrant: TestValueQuadrant
        get() = when {
            priorityScore >= 60 && (difficultyScore >= 60 || automationConfidence < 60) ->
                TestValueQuadrant.GUIDED_GENERATION
            priorityScore >= 60 -> TestValueQuadrant.PRIORITY_AUTOMATION
            difficultyScore < 60 && automationConfidence >= 60 -> TestValueQuadrant.BATCH_AUTOMATION
            else -> TestValueQuadrant.DEFER_OR_REVIEW
        }

    val guidePriorityScore: Int
        get() = (
            priorityScore * 5 +
                automationConfidence * 3 +
                (100 - difficultyScore) * 2
            ) / 10

    val methodKey: String
        get() = "$sourceClass#$methodName".lowercase()
}
