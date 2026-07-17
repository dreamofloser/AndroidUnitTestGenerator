package io.github.dreamofloser.testgen.model

data class GeneratedTestSource(
    val source: String,
    val testMethodCount: Int,
    val assertionCount: Int,
    val fallbackMethodCount: Int,
    val ruleMatchedMethodCount: Int,
    val mockedDependencyCount: Int,
    val mockStubCount: Int,
    val mockVerificationCount: Int,
    val liveDataRuleCount: Int = 0,
    val robolectricTestCount: Int = 0,
    val androidImportCount: Int = 0,
    val composeTestCount: Int = 0,
    val roomDaoTestCount: Int = 0,
    val retrofitApiTestCount: Int = 0,
    val llmAdoptedMethodCount: Int = 0,
)
