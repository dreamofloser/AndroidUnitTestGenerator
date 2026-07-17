package io.github.dreamofloser.testgen.analysis

import io.github.dreamofloser.testgen.generator.TestScenarioGenerator
import io.github.dreamofloser.testgen.llm.supportedLlmInputStrategies
import io.github.dreamofloser.testgen.model.ClassModel
import io.github.dreamofloser.testgen.model.MethodModel
import io.github.dreamofloser.testgen.model.SourceClassKind

class TestabilityAnalyzer(
    private val scenarioGenerator: TestScenarioGenerator = TestScenarioGenerator(),
) {
    fun analyze(classes: List<ClassModel>): List<TestabilityInsight> {
        return classes.flatMap(::analyze)
    }

    fun analyze(model: ClassModel): List<TestabilityInsight> {
        val targets = model.methods.ifEmpty {
            listOf(
                MethodModel(
                    name = "<class>",
                    returnType = model.className,
                    parameters = model.constructors.firstOrNull()?.parameters.orEmpty(),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                ),
            )
        }
        return targets.map { method -> analyze(model, method) }
    }

    private fun analyze(model: ClassModel, method: MethodModel): TestabilityInsight {
        val dependencyNames = dependencyNames(model, method)
        val evidence = buildList {
            controlFlowEvidence(method)?.let(::add)
            dependencyEvidence(dependencyNames, method)?.let(::add)
            asyncEvidence(model, method)?.let(::add)
            androidFrameworkEvidence(model, method)?.let(::add)
            externalResourceEvidence(model, method)?.let(::add)
            generatorLimitationEvidence(model, method)?.let(::add)
        }
        val difficulty = evidence.sumOf { it.points }.coerceIn(0, 100)
        val priority = priorityScore(model, method, dependencyNames)
        val confidence = automationConfidence(
            model = model,
            method = method,
            dependencyNames = dependencyNames,
            evidence = evidence,
            difficulty = difficulty,
        )
        return TestabilityInsight(
            sourceClass = model.qualifiedName(),
            methodName = method.name,
            difficultyScore = difficulty,
            priorityScore = priority,
            automationConfidence = confidence,
            recommendedStrategy = strategyFor(model, method, dependencyNames, confidence),
            evidence = evidence,
            boundaryFocus = boundaryFocus(model, method, dependencyNames),
        )
    }

    private fun controlFlowEvidence(method: MethodModel): DifficultyEvidence? {
        val conditionCount = method.conditionExpressions.size
        val exceptionCount = (method.thrownExceptions + method.thrownStatementTypes).distinct().size
        val additionalReturns = (method.returnExpressions.size - 1).coerceAtLeast(0)
        val points = (conditionCount * 5 + exceptionCount * 4 + additionalReturns * 2)
            .coerceAtMost(25)
        if (points == 0) {
            return null
        }
        return DifficultyEvidence(
            driver = DifficultyDriver.CONTROL_FLOW,
            points = points,
            detail = "$conditionCount conditions, $exceptionCount exception paths, $additionalReturns additional returns",
        )
    }

    private fun dependencyEvidence(
        dependencyNames: Set<String>,
        method: MethodModel,
    ): DifficultyEvidence? {
        val dependencyCallCount = method.dependencyCalls.count { it.receiverName in dependencyNames }
        if (dependencyNames.isEmpty() && dependencyCallCount == 0) {
            return null
        }
        val points = (dependencyNames.size * 7 + dependencyCallCount * 2)
            .coerceAtMost(20)
        return DifficultyEvidence(
            driver = DifficultyDriver.DEPENDENCIES,
            points = points,
            detail = "${dependencyNames.size} dependencies and $dependencyCallCount dependency calls",
        )
    }

    private fun asyncEvidence(model: ClassModel, method: MethodModel): DifficultyEvidence? {
        val asyncReturn = asyncTypeMarkers.any { marker ->
            method.returnType.contains(marker, ignoreCase = true)
        }
        val observableState = usesObservableState(model, method)
        val points = (
            (if (method.isSuspend) 10 else 0) +
                (if (asyncReturn) 5 else 0) +
                (if (observableState && !asyncReturn) 5 else 0)
            )
            .coerceAtMost(15)
        if (points == 0) {
            return null
        }
        val features = buildList {
            if (method.isSuspend) add("suspend")
            if (asyncReturn) add("async state return")
            if (observableState && !asyncReturn) add("observable state interaction")
        }
        return DifficultyEvidence(
            driver = DifficultyDriver.ASYNC_STATE,
            points = points,
            detail = features.joinToString(),
        )
    }

    private fun androidFrameworkEvidence(
        model: ClassModel,
        method: MethodModel,
    ): DifficultyEvidence? {
        val kindPoints = when (model.classKind) {
            SourceClassKind.ACTIVITY,
            SourceClassKind.FRAGMENT,
            SourceClassKind.COMPOSE_UI,
            -> 15
            SourceClassKind.VIEW_MODEL -> 12
            else -> 0
        }
        val usesFrameworkType = (
            method.parameters.map { it.type } +
                model.constructors.flatMap { constructor -> constructor.parameters.map { it.type } }
            ).any(::isAndroidFrameworkType)
        val importPoints = if (
            model.imports.any { it.startsWith("android.") || it.startsWith("androidx.") } ||
            usesFrameworkType
        ) {
            8
        } else {
            0
        }
        val points = maxOf(kindPoints, importPoints).coerceAtMost(15)
        if (points == 0) {
            return null
        }
        return DifficultyEvidence(
            driver = DifficultyDriver.ANDROID_FRAMEWORK,
            points = points,
            detail = "framework coupling detected for ${model.classKind}",
        )
    }

    private fun externalResourceEvidence(
        model: ClassModel,
        method: MethodModel,
    ): DifficultyEvidence? {
        val specializedPoints = when (model.classKind) {
            SourceClassKind.RETROFIT_API,
            SourceClassKind.ROOM_DAO,
            -> 15
            else -> 0
        }
        val dependencyTypes = model.constructors
            .flatMap { it.parameters }
            .map { it.type }
            .filter(::isExternalResourceType)
            .distinct()
        val namePoints = if (
            externalClassMarkers.any { marker -> model.className.contains(marker, ignoreCase = true) }
        ) {
            5
        } else {
            0
        }
        val callPoints = if (method.httpMethod != null || method.httpPath != null) 8 else 0
        val points = maxOf(
            specializedPoints,
            (dependencyTypes.size * 6 + namePoints + callPoints).coerceAtMost(15),
        )
        if (points == 0) {
            return null
        }
        return DifficultyEvidence(
            driver = DifficultyDriver.EXTERNAL_RESOURCES,
            points = points,
            detail = "network, database, storage, or service boundary detected",
        )
    }

    private fun generatorLimitationEvidence(
        model: ClassModel,
        method: MethodModel,
    ): DifficultyEvidence? {
        val overloaded = model.methods.count { it.name == method.name } > 1
        val unsupportedParameters = method.parameters.count { !isSupportedValueType(it.type) }
        val noObservableResult = method.returnType.normalizedType() in setOf("void", "Unit") &&
            method.dependencyCalls.isEmpty()
        val unsupportedReturn = model.classKind == SourceClassKind.REGULAR &&
            !isSupportedReturnType(method.returnType)
        val points = (
            (if (overloaded) 5 else 0) +
                (unsupportedParameters * 3).coerceAtMost(6) +
                (if (noObservableResult) 3 else 0) +
                (if (unsupportedReturn) 3 else 0)
            ).coerceAtMost(10)
        if (points == 0) {
            return null
        }
        val details = buildList {
            if (overloaded) add("overloaded method")
            if (unsupportedParameters > 0) add("$unsupportedParameters complex parameters")
            if (noObservableResult) add("no direct observable result")
            if (unsupportedReturn) add("complex return type")
        }
        return DifficultyEvidence(
            driver = DifficultyDriver.GENERATOR_LIMITATIONS,
            points = points,
            detail = details.joinToString(),
        )
    }

    private fun priorityScore(
        model: ClassModel,
        method: MethodModel,
        dependencyNames: Set<String>,
    ): Int {
        val conditionPoints = (method.conditionExpressions.size * 6).coerceAtMost(20)
        val exceptionPoints = (
            (method.thrownExceptions + method.thrownStatementTypes).distinct().size * 8
            ).coerceAtMost(15)
        val dependencyPoints = if (dependencyNames.isNotEmpty()) 15 else 0
        val asyncPoints = if (
            method.isSuspend ||
                asyncTypeMarkers.any { method.returnType.contains(it, ignoreCase = true) } ||
                usesObservableState(model, method)
        ) {
            10
        } else {
            0
        }
        val externalPoints = if (externalResourceEvidence(model, method) != null) 15 else 0
        val boundaryPoints = method.parameters
            .count { it.supportedLlmInputStrategies().isNotEmpty() }
            .times(5)
            .coerceAtMost(10)
        val criticalNamePoints = if (
            criticalMethodMarkers.any { marker -> method.name.contains(marker, ignoreCase = true) }
        ) {
            15
        } else {
            0
        }
        val observablePoints = if (
            method.returnType.normalizedType() !in setOf("void", "Unit") ||
            method.dependencyCalls.isNotEmpty()
        ) {
            5
        } else {
            0
        }
        val specializedPoints = if (
            model.classKind in setOf(
                SourceClassKind.VIEW_MODEL,
                SourceClassKind.ACTIVITY,
                SourceClassKind.FRAGMENT,
                SourceClassKind.COMPOSE_UI,
                SourceClassKind.ROOM_DAO,
                SourceClassKind.RETROFIT_API,
            ) || model.className.contains("ViewModel", ignoreCase = true)
        ) {
            10
        } else {
            0
        }
        return (
            10 +
                conditionPoints +
                exceptionPoints +
                dependencyPoints +
                asyncPoints +
                externalPoints +
                boundaryPoints +
                criticalNamePoints +
                observablePoints +
                specializedPoints
            ).coerceIn(0, 100)
    }

    private fun automationConfidence(
        model: ClassModel,
        method: MethodModel,
        dependencyNames: Set<String>,
        evidence: List<DifficultyEvidence>,
        difficulty: Int,
    ): Int {
        val limitationPoints = evidence
            .firstOrNull { it.driver == DifficultyDriver.GENERATOR_LIMITATIONS }
            ?.points
            ?: 0
        val recognizedTemplate = if (method.name == "<class>") {
            model.classKind != SourceClassKind.REGULAR
        } else {
            scenarioGenerator.scenariosFor(method, dependencyNames)
                .any { !it.isFallback }
        }
        val specializedTemplate = model.classKind != SourceClassKind.REGULAR
        var confidence = 92
        confidence -= limitationPoints * 2
        confidence -= dependencyNames.size.coerceAtMost(3) * 4
        if (method.isSuspend) confidence -= 8
        if (evidence.any { it.driver == DifficultyDriver.ANDROID_FRAMEWORK }) confidence -= 8
        if (evidence.any { it.driver == DifficultyDriver.EXTERNAL_RESOURCES }) confidence -= 8
        if (recognizedTemplate) confidence += 5 else confidence -= 15
        if (specializedTemplate) confidence += 5
        if (difficulty < 30 && dependencyNames.isEmpty()) confidence += 5
        return confidence.coerceIn(0, 100)
    }

    private fun strategyFor(
        model: ClassModel,
        method: MethodModel,
        dependencyNames: Set<String>,
        confidence: Int,
    ): TestStrategy {
        if (confidence < 50) {
            return TestStrategy.MANUAL_REVIEW
        }
        if (
            model.className.contains("ViewModel", ignoreCase = true) ||
            usesObservableState(model, method)
        ) {
            return TestStrategy.VIEW_MODEL_STATE
        }
        return when (model.classKind) {
            SourceClassKind.RETROFIT_API -> TestStrategy.RETROFIT_CONTRACT
            SourceClassKind.ROOM_DAO -> TestStrategy.ROOM_IN_MEMORY
            SourceClassKind.COMPOSE_UI -> TestStrategy.COMPOSE_UI
            SourceClassKind.ACTIVITY,
            SourceClassKind.FRAGMENT,
            -> TestStrategy.ROBOLECTRIC
            SourceClassKind.VIEW_MODEL -> TestStrategy.VIEW_MODEL_STATE
            else -> when {
                method.isSuspend ||
                    asyncTypeMarkers.any { method.returnType.contains(it, ignoreCase = true) } ->
                    TestStrategy.COROUTINE_UNIT
                dependencyNames.isNotEmpty() ->
                    TestStrategy.MOCKED_UNIT
                else -> TestStrategy.PURE_UNIT
            }
        }
    }

    private fun boundaryFocus(
        model: ClassModel,
        method: MethodModel,
        dependencyNames: Set<String>,
    ): List<String> {
        return buildList {
            method.parameters.forEach { parameter ->
                parameter.supportedLlmInputStrategies().forEach { strategy ->
                    add("${parameter.name}=${strategy.wireName}")
                }
            }
            if ((method.thrownExceptions + method.thrownStatementTypes).isNotEmpty()) {
                add("exception-path")
            }
            if (dependencyNames.isNotEmpty()) {
                add("dependency-failure")
            }
            if (method.isSuspend) {
                add("coroutine-failure")
            }
            when (model.classKind) {
                SourceClassKind.ACTIVITY,
                SourceClassKind.FRAGMENT,
                -> add("lifecycle-transition")
                SourceClassKind.COMPOSE_UI -> add("semantics-interaction")
                SourceClassKind.ROOM_DAO -> add("crud-and-empty-result")
                SourceClassKind.RETROFIT_API -> add("http-success-and-error")
                SourceClassKind.VIEW_MODEL -> add("state-transition")
                else -> Unit
            }
        }.distinct().ifEmpty { listOf("happy-path") }
    }

    private fun dependencyNames(model: ClassModel, method: MethodModel): Set<String> {
        val constructorDependencies = model.constructors
            .flatMap { it.parameters }
            .filter { parameter -> isLikelyDependencyType(parameter.type) }
            .map { it.name }
            .toSet()
        return constructorDependencies +
            method.dependencyCalls
                .map { it.receiverName }
                .filter { it in constructorDependencies }
    }

    private fun usesObservableState(model: ClassModel, method: MethodModel): Boolean {
        if (asyncTypeMarkers.any { method.returnType.contains(it, ignoreCase = true) }) {
            return true
        }
        if (
            method.dependencyCalls.any { call ->
                stateMutationMethods.any { it.equals(call.methodName, ignoreCase = true) }
            }
        ) {
            return true
        }
        val observableProperties = model.properties
            .filter { property ->
                asyncTypeMarkers.any { marker -> property.type.contains(marker, ignoreCase = true) }
            }
            .map { it.name }
            .toSet()
        return method.dependencyCalls.any { it.receiverName in observableProperties }
    }

    private fun isLikelyDependencyType(type: String): Boolean {
        val normalized = type.normalizedType()
        return dependencyTypeMarkers.any { normalized.contains(it, ignoreCase = true) } ||
            isAndroidFrameworkType(normalized)
    }

    private fun isExternalResourceType(type: String): Boolean {
        val normalized = type.normalizedType()
        return externalTypeMarkers.any { normalized.contains(it, ignoreCase = true) }
    }

    private fun isAndroidFrameworkType(type: String): Boolean {
        val normalized = type.normalizedType()
        return androidTypeMarkers.any { marker ->
            normalized == marker || normalized.endsWith(".$marker")
        }
    }

    private fun isSupportedValueType(type: String): Boolean {
        val normalized = type.normalizedType()
        return normalized in simpleValueTypes ||
            normalized.endsWith("[]") ||
            collectionTypeMarkers.any { normalized.startsWith(it) }
    }

    private fun isSupportedReturnType(type: String): Boolean {
        val normalized = type.normalizedType()
        return isSupportedValueType(normalized) ||
            supportedReturnMarkers.any { normalized.startsWith(it) }
    }

    private fun String.normalizedType(): String = trim().removeSuffix("?")

    private fun ClassModel.qualifiedName(): String {
        return if (packageName.isBlank()) className else "$packageName.$className"
    }

    private companion object {
        val asyncTypeMarkers = listOf("Flow<", "StateFlow<", "LiveData<", "Deferred<", "Job")
        val stateMutationMethods = listOf("setValue", "postValue", "emit", "tryEmit", "update")
        val criticalMethodMarkers = listOf(
            "login",
            "auth",
            "token",
            "load",
            "fetch",
            "save",
            "delete",
            "update",
            "create",
            "submit",
            "purchase",
            "payment",
        )
        val externalClassMarkers = listOf("Repository", "Service", "Store", "Gateway")
        val dependencyTypeMarkers = listOf(
            "Repository",
            "Service",
            "Dao",
            "Api",
            "Client",
            "Database",
            "DataSource",
            "Context",
            "Resources",
            "SharedPreferences",
        )
        val externalTypeMarkers = listOf(
            "Repository",
            "Service",
            "Dao",
            "Api",
            "Client",
            "Database",
            "DataSource",
            "SharedPreferences",
        )
        val androidTypeMarkers = listOf(
            "Context",
            "Resources",
            "SharedPreferences",
            "Intent",
            "Bundle",
            "Activity",
            "Fragment",
            "ViewModel",
            "LiveData",
        )
        val simpleValueTypes = setOf(
            "void",
            "Unit",
            "byte",
            "Byte",
            "short",
            "Short",
            "int",
            "Integer",
            "long",
            "Long",
            "float",
            "Float",
            "double",
            "Double",
            "boolean",
            "Boolean",
            "char",
            "Character",
            "String",
            "java.lang.String",
        )
        val collectionTypeMarkers = listOf(
            "List<",
            "Set<",
            "Map<",
            "Collection<",
            "java.util.List<",
            "java.util.Set<",
            "java.util.Map<",
        )
        val supportedReturnMarkers = listOf(
            "Result<",
            "Response<",
            "Flow<",
            "StateFlow<",
            "LiveData<",
        )
    }
}
