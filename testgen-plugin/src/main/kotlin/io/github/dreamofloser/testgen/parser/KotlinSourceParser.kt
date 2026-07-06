package io.github.dreamofloser.testgen.parser

import io.github.dreamofloser.testgen.model.ClassModel
import io.github.dreamofloser.testgen.model.ConstructorModel
import io.github.dreamofloser.testgen.model.DependencyCallModel
import io.github.dreamofloser.testgen.model.MethodModel
import io.github.dreamofloser.testgen.model.ParameterModel
import io.github.dreamofloser.testgen.model.PropertyModel
import io.github.dreamofloser.testgen.model.SourceClassKind
import io.github.dreamofloser.testgen.model.SourceLanguage
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtProperty
import java.io.File

class KotlinSourceParser {
    fun parse(sourceFile: File): List<ClassModel> {
        val source = sourceFile.readText()
        val disposable = Disposer.newDisposable()
        return try {
            val ktFile = sourceFile.toKtFile(disposable, source)
            val sourcePackageName = source.kotlinPackageName()
            val psiPackageName = ktFile.packageFqName.asString()
            val packageName = sourcePackageName.ifBlank {
                psiPackageName.takeUnless { it.isBlank() || it == "<root>" }.orEmpty()
            }
            val imports = (source.kotlinImportPaths() + ktFile.importList?.imports
                .orEmpty()
                .mapNotNull { it.importPath?.pathStr })
                .distinct()

            val classModels = ktFile.declarations
                .filterIsInstance<KtClass>()
                .mapNotNull { declaration -> declaration.toClassModel(sourceFile, packageName, imports) }

            val composeModels = ktFile.declarations
                .filterIsInstance<KtNamedFunction>()
                .mapNotNull { function -> function.toComposeModel(sourceFile, packageName, imports) }
            val fallbackModels = source.textFallbackClassModels(sourceFile, packageName, imports)
            val retrofitModels = source.retrofitInterfaceModels(sourceFile, packageName, imports)
            val retrofitModelNames = retrofitModels.map { it.className }.toSet()
            val parsedModelNames = (classModels + retrofitModels + composeModels).map { it.className }.toSet()

            classModels.filterNot { it.className in retrofitModelNames } +
                retrofitModels +
                composeModels +
                fallbackModels.filterNot { it.className in parsedModelNames }
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private fun File.toKtFile(
        disposable: org.jetbrains.kotlin.com.intellij.openapi.Disposable,
        source: String,
    ): KtFile {
        val configuration = CompilerConfiguration().apply {
            put(CommonConfigurationKeys.MODULE_NAME, "android-testgen-parser")
        }
        val environment = KotlinCoreEnvironment.createForProduction(
            disposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES,
        )
        return KtPsiFactory(environment.project, markGenerated = false)
            .createFile(name, source)
    }

    private fun KtClass.toClassModel(
        sourceFile: File,
        packageName: String,
        imports: List<String>,
    ): ClassModel? {
        if (superTypeNames().any { it.normalizedSuperTypeName() in unsupportedKotlinAndroidBaseTypes }) {
            return null
        }

        val constructorParameters = primaryConstructorParameters.mapNotNull { it.toParameterModel() }
        val dependencyNames = constructorParameters.map { it.name }.toSet()
        val bodyDeclarations = body?.declarations.orEmpty()
        val functions = bodyDeclarations.filterIsInstance<KtNamedFunction>()
        val methods = functions.map { it.toMethodModel(dependencyNames) }
        val properties = bodyDeclarations
            .filterIsInstance<KtProperty>()
            .mapNotNull { it.toPropertyModel() }

        return ClassModel(
            packageName = packageName,
            className = name ?: return null,
            sourceFile = sourceFile,
            imports = imports,
            constructors = if (isInterface()) emptyList() else listOf(ConstructorModel(constructorParameters)),
            methods = methods,
            properties = properties,
            language = SourceLanguage.KOTLIN,
            classKind = classKind(functions, imports),
        )
    }

    private fun KtNamedFunction.toComposeModel(
        sourceFile: File,
        packageName: String,
        imports: List<String>,
    ): ClassModel? {
        if (!hasAnnotation("Composable")) {
            return null
        }

        val parameters = valueParameters.mapNotNull { it.toParameterModel() }
        val hasViewModelParameter = parameters.any { it.type.endsWith("ViewModel") }
        val hasStableLoadingTag = bodyExpression?.text.orEmpty().contains("testTag(\"LoadingSpinner\")")
        if (!hasViewModelParameter || !hasStableLoadingTag) {
            return null
        }

        val functionName = name ?: return null
        return ClassModel(
            packageName = packageName,
            className = "${functionName}Compose",
            sourceFile = sourceFile,
            imports = imports,
            constructors = emptyList(),
            methods = listOf(toMethodModel(emptySet(), isStatic = true)),
            properties = emptyList(),
            language = SourceLanguage.KOTLIN,
            classKind = SourceClassKind.COMPOSE_UI,
        )
    }

    private fun KtClass.classKind(
        functions: List<KtNamedFunction>,
        imports: List<String>,
    ): SourceClassKind {
        return when {
            hasAnnotation("Dao") || functions.any { it.hasAnyAnnotation(roomDaoMethodAnnotations) } -> SourceClassKind.ROOM_DAO
            isInterface() && imports.any { it == "androidx.room.Dao" || it.startsWith("androidx.room.") } && functions.isNotEmpty() -> {
                SourceClassKind.ROOM_DAO
            }
            functions.any { it.hasAnyAnnotation(retrofitHttpAnnotations) } -> SourceClassKind.RETROFIT_API
            isInterface() && imports.any { it in retrofitHttpImports || it == "retrofit2.http.*" } && functions.isNotEmpty() -> {
                SourceClassKind.RETROFIT_API
            }
            hasModifier(KtTokens.DATA_KEYWORD) -> SourceClassKind.DATA
            superTypeNames().any { it.endsWith("ViewModel") } || name.orEmpty().endsWith("ViewModel") -> SourceClassKind.VIEW_MODEL
            else -> SourceClassKind.REGULAR
        }
    }

    private fun KtNamedFunction.toMethodModel(
        dependencyNames: Set<String>,
        isStatic: Boolean = false,
    ): MethodModel {
        val recoveredParameters = valueParameters.mapNotNull { it.toParameterModel() }
            .ifEmpty { text.recoverFunctionParameters() }
        val recoveredReturnType = typeReference?.text?.trim()
            .orEmpty()
            .ifBlank { text.recoverFunctionReturnType() }
            .ifBlank { "Unit" }

        return MethodModel(
            name = name.orEmpty(),
            returnType = recoveredReturnType,
            parameters = recoveredParameters,
            isStatic = isStatic,
            thrownExceptions = emptyList(),
            isSuspend = hasModifier(KtTokens.SUSPEND_KEYWORD),
            dependencyCalls = dependencyCalls(dependencyNames),
            conditionExpressions = emptyList(),
            returnExpressions = emptyList(),
            thrownStatementTypes = emptyList(),
        )
    }

    private fun KtNamedFunction.dependencyCalls(dependencyNames: Set<String>): List<DependencyCallModel> {
        if (dependencyNames.isEmpty()) {
            return emptyList()
        }

        val body = bodyExpression ?: return emptyList()
        return PsiTreeUtil.findChildrenOfType(body, KtCallExpression::class.java)
            .mapNotNull { callExpression ->
                val qualifiedExpression = callExpression.parent as? KtDotQualifiedExpression ?: return@mapNotNull null
                val receiverName = qualifiedExpression.receiverExpression.text
                if (receiverName !in dependencyNames) {
                    return@mapNotNull null
                }

                DependencyCallModel(
                    receiverName = receiverName,
                    methodName = callExpression.calleeExpression?.text.orEmpty(),
                    arguments = callExpression.valueArguments.mapNotNull { it.getArgumentExpression()?.text },
                )
            }
    }

    private fun KtParameter.toParameterModel(): ParameterModel? {
        val parameterName = name ?: return null
        val parameterType = typeReference?.text?.trim().orEmpty().ifBlank { return null }
        return ParameterModel(
            name = parameterName,
            type = parameterType,
        )
    }

    private fun KtProperty.toPropertyModel(): PropertyModel? {
        val propertyName = name ?: return null
        val propertyType = typeReference?.text?.trim().orEmpty().ifBlank { return null }
        return PropertyModel(
            name = propertyName,
            type = propertyType,
        )
    }

    private fun KtClass.superTypeNames(): List<String> {
        return superTypeListEntries.map { it.typeReference?.text.orEmpty() }
    }

    private fun KtClass.hasAnnotation(shortName: String): Boolean {
        return annotationEntries.hasAnnotation(shortName)
    }

    private fun KtNamedFunction.hasAnnotation(shortName: String): Boolean {
        return annotationEntries.hasAnnotation(shortName) || declarationPrefixText().contains("@$shortName")
    }

    private fun KtNamedFunction.declarationPrefixText(): String {
        val functionKeyword = text.indexOf("fun ")
        return if (functionKeyword == -1) text else text.substring(0, functionKeyword)
    }

    private fun String.retrofitInterfaceModels(
        sourceFile: File,
        packageName: String,
        imports: List<String>,
    ): List<ClassModel> {
        return retrofitInterfaceRegex.findAll(this)
            .mapNotNull { match ->
                val interfaceName = match.groupValues[1]
                val body = match.groupValues[2]
                if (!body.containsRetrofitHttpAnnotation()) {
                    return@mapNotNull null
                }

                ClassModel(
                    packageName = packageName,
                    className = interfaceName,
                    sourceFile = sourceFile,
                    imports = imports,
                    constructors = emptyList(),
                    methods = body.retrofitMethods(),
                    properties = emptyList(),
                    language = SourceLanguage.KOTLIN,
                    classKind = SourceClassKind.RETROFIT_API,
                )
            }
            .toList()
    }

    private fun String.textFallbackClassModels(
        sourceFile: File,
        packageName: String,
        imports: List<String>,
    ): List<ClassModel> {
        return topLevelTypeRegex.findAll(this)
            .mapNotNull { match ->
                val modifiers = match.groupValues[1]
                val typeKind = match.groupValues[2]
                val className = match.groupValues[3]
                val declarationLine = substring(match.range.first, lineEndIndex(match.range.last))
                if (declarationLine.hasUnsupportedAndroidBaseType()) {
                    return@mapNotNull null
                }
                val constructorParameters = match.groupValues[4]
                    .takeIf { it.isNotBlank() }
                    ?.recoverParameters()
                    .orEmpty()
                val body = bodyAfter(match.range.last)
                val methods = body.kotlinFunctionModels(constructorParameters.map { it.name }.toSet())

                ClassModel(
                    packageName = packageName,
                    className = className,
                    sourceFile = sourceFile,
                    imports = imports,
                    constructors = if (typeKind == "interface") emptyList() else listOf(ConstructorModel(constructorParameters)),
                    methods = methods,
                    properties = emptyList(),
                    language = SourceLanguage.KOTLIN,
                    classKind = textClassKind(modifiers, typeKind, methods, imports, className),
                )
            }
            .toList()
    }

    private fun String.lineEndIndex(startIndex: Int): Int {
        val lineBreak = indexOf('\n', startIndex = startIndex)
        return if (lineBreak == -1) length else lineBreak
    }

    private fun String.hasUnsupportedAndroidBaseType(): Boolean {
        val superTypeText = substringAfter(':', missingDelimiterValue = "")
        return unsupportedKotlinAndroidBaseTypes.any { typeName ->
            Regex("""\b${Regex.escape(typeName)}\b""").containsMatchIn(superTypeText)
        }
    }

    private fun String.normalizedSuperTypeName(): String {
        return substringBefore('(').substringAfterLast('.').trim()
    }
    private fun String.bodyAfter(typeDeclarationEnd: Int): String {
        val openBrace = indexOf('{', startIndex = typeDeclarationEnd + 1)
        if (openBrace == -1) {
            return ""
        }

        val closeBrace = matchingBraceIndex(openBrace)
        return if (closeBrace == -1) "" else substring(openBrace + 1, closeBrace)
    }

    private fun String.kotlinFunctionModels(dependencyNames: Set<String>): List<MethodModel> {
        val methods = mutableListOf<MethodModel>()
        var searchIndex = 0

        while (searchIndex < length) {
            val functionMatch = functionNameRegex.find(this, searchIndex) ?: break
            val functionTextStart = functionMatch.range.first
            searchIndex = functionMatch.range.last + 1
            val functionName = functionMatch.groupValues[1]
            val openParen = indexOf('(', startIndex = functionMatch.range.last + 1)
            val closeParen = matchingParenIndex(openParen)
            if (openParen == -1 || closeParen == -1) {
                continue
            }

            val parameterText = substring(openParen + 1, closeParen)
            val suffix = substring(closeParen + 1)
            val returnType = Regex("""^\s*:\s*([A-Za-z_][A-Za-z0-9_.<>?]*)""")
                .find(suffix)
                ?.groupValues
                ?.get(1)
                .orEmpty()
                .ifBlank { "Unit" }
            val functionBody = suffix.substringBefore("\nfun ")

            methods += MethodModel(
                name = functionName,
                returnType = returnType,
                parameters = parameterText.recoverParameters(),
                isStatic = false,
                thrownExceptions = emptyList(),
                isSuspend = functionMatch.value.contains("suspend"),
                dependencyCalls = functionBody.textDependencyCalls(dependencyNames),
            )

            searchIndex = maxOf(searchIndex, functionTextStart + 1)
        }

        return methods
    }

    private fun String.textDependencyCalls(dependencyNames: Set<String>): List<DependencyCallModel> {
        if (dependencyNames.isEmpty()) {
            return emptyList()
        }

        return dependencyCallRegex.findAll(this)
            .mapNotNull { match ->
                val receiverName = match.groupValues[1]
                if (receiverName !in dependencyNames) {
                    return@mapNotNull null
                }

                DependencyCallModel(
                    receiverName = receiverName,
                    methodName = match.groupValues[2],
                    arguments = match.groupValues[3]
                        .splitTopLevelParameters()
                        .filter { it.isNotBlank() },
                )
            }
            .toList()
    }

    private fun textClassKind(
        modifiers: String,
        typeKind: String,
        methods: List<MethodModel>,
        imports: List<String>,
        className: String,
    ): SourceClassKind {
        return when {
            methods.any { method -> method.name in roomDaoMethodNames } ||
                imports.any { it == "androidx.room.Dao" || it.startsWith("androidx.room.") } -> SourceClassKind.ROOM_DAO
            typeKind == "interface" && imports.any { it in retrofitHttpImports || it == "retrofit2.http.*" } -> SourceClassKind.RETROFIT_API
            modifiers.contains("data") -> SourceClassKind.DATA
            className.endsWith("ViewModel") -> SourceClassKind.VIEW_MODEL
            else -> SourceClassKind.REGULAR
        }
    }
    private fun String.kotlinPackageName(): String {
        return packageRegex.find(this)?.groupValues?.get(1).orEmpty()
    }

    private fun String.kotlinImportPaths(): List<String> {
        return importRegex.findAll(this)
            .map { it.groupValues[1] }
            .toList()
    }

    private fun String.retrofitMethods(): List<MethodModel> {
        val methods = mutableListOf<MethodModel>()
        var searchIndex = 0

        while (searchIndex < length) {
            val functionMatch = functionNameRegex.find(this, searchIndex) ?: break
            val prefix = substring(searchIndex, functionMatch.range.first)
            searchIndex = functionMatch.range.last + 1
            if (!prefix.containsRetrofitHttpAnnotation()) {
                continue
            }

            val functionName = functionMatch.groupValues[1]
            val endpoint = prefix.retrofitEndpoint()
            val openParen = indexOf('(', startIndex = functionMatch.range.last + 1)
            val closeParen = matchingParenIndex(openParen)
            if (openParen == -1 || closeParen == -1) {
                continue
            }

            val parameterText = substring(openParen + 1, closeParen)
            val suffix = substring(closeParen + 1)
            val returnType = Regex("""^\s*:\s*([A-Za-z_][A-Za-z0-9_.<>?]*)""")
                .find(suffix)
                ?.groupValues
                ?.get(1)
                .orEmpty()
                .ifBlank { "Unit" }

            methods += MethodModel(
                name = functionName,
                returnType = returnType,
                parameters = parameterText.recoverParameters(),
                isStatic = false,
                thrownExceptions = emptyList(),
                isSuspend = functionMatch.value.contains("suspend"),
                httpMethod = endpoint?.method,
                httpPath = endpoint?.path,
                httpQueryNames = parameterText.recoverRetrofitQueryNames(),
            )
        }

        return methods
    }

    private fun KtNamedFunction.hasAnyAnnotation(shortNames: Set<String>): Boolean {
        return shortNames.any { hasAnnotation(it) }
    }

    private fun List<KtAnnotationEntry>.hasAnnotation(shortName: String): Boolean {
        return any { annotation ->
            annotation.shortName?.asString() == shortName ||
                annotation.typeReference?.text?.substringAfterLast('.') == shortName
        }
    }

    private fun String.retrofitEndpoint(): RetrofitEndpoint? {
        val match = retrofitEndpointRegex.find(this) ?: return null
        return RetrofitEndpoint(
            method = match.groupValues[1],
            path = match.groupValues[2],
        )
    }

    private fun String.recoverRetrofitQueryNames(): List<String> {
        return queryAnnotationRegex.findAll(this)
            .map { it.groupValues[1] }
            .toList()
    }
    private fun String.recoverFunctionParameters(): List<ParameterModel> {
        val parameterText = substringBetweenMatchingParentheses() ?: return emptyList()
        if (parameterText.isBlank()) {
            return emptyList()
        }

        return parameterText.recoverParameters()
    }

    private fun String.recoverParameters(): List<ParameterModel> {
        if (isBlank()) {
            return emptyList()
        }

        return lineCommentRegex.replace(this, "")
            .splitTopLevelParameters()
            .mapNotNull { rawParameter ->
                val cleaned = rawParameter
                    .replace(parameterAnnotationRegex, "")
                    .substringBefore('=')
                    .trim()
                val name = cleaned.substringBefore(':').trim().ifBlank { return@mapNotNull null }
                val type = cleaned.substringAfter(':', "").trim().ifBlank { return@mapNotNull null }
                ParameterModel(name = name, type = type)
            }
    }

    private fun String.containsRetrofitHttpAnnotation(): Boolean {
        return retrofitHttpAnnotations.any { contains("@$it") }
    }

    private fun String.recoverFunctionReturnType(): String {
        val closeParen = matchingParenIndex(functionParameterOpenParenIndex())
        if (closeParen == -1) {
            return ""
        }

        val suffix = substring(closeParen + 1)
        return Regex("""^\s*:\s*([A-Za-z_][A-Za-z0-9_.<>?]*)""")
            .find(suffix)
            ?.groupValues
            ?.get(1)
            .orEmpty()
    }

    private fun String.substringBetweenMatchingParentheses(): String? {
        val openParen = functionParameterOpenParenIndex()
        if (openParen == -1) {
            return null
        }

        val closeParen = matchingParenIndex(openParen)
        return if (closeParen == -1) null else substring(openParen + 1, closeParen)
    }

    private fun String.functionParameterOpenParenIndex(): Int {
        val functionMatch = Regex("""\bfun\s+[A-Za-z_][A-Za-z0-9_]*""").find(this)
            ?: return -1
        return indexOf('(', startIndex = functionMatch.range.last + 1)
    }

    private fun String.matchingParenIndex(openParen: Int): Int {
        if (openParen == -1) {
            return -1
        }

        var depth = 0
        var inString = false
        var previous = Char.MIN_VALUE
        for (index in openParen until length) {
            val char = this[index]
            if (char == '"' && previous != '\\') {
                inString = !inString
            }
            if (!inString) {
                when (char) {
                    '(' -> depth += 1
                    ')' -> {
                        depth -= 1
                        if (depth == 0) {
                            return index
                        }
                    }
                }
            }
            previous = char
        }
        return -1
    }

    private fun String.matchingBraceIndex(openBrace: Int): Int {
        if (openBrace == -1) {
            return -1
        }

        var depth = 0
        var inString = false
        var previous = Char.MIN_VALUE
        for (index in openBrace until length) {
            val char = this[index]
            if (char == '"' && previous != '\\') {
                inString = !inString
            }
            if (!inString) {
                when (char) {
                    '{' -> depth += 1
                    '}' -> {
                        depth -= 1
                        if (depth == 0) {
                            return index
                        }
                    }
                }
            }
            previous = char
        }
        return -1
    }
    private fun String.splitTopLevelParameters(): List<String> {
        val parameters = mutableListOf<String>()
        var depth = 0
        var inString = false
        var previous = Char.MIN_VALUE
        var start = 0

        forEachIndexed { index, char ->
            if (char == '"' && previous != '\\') {
                inString = !inString
            }
            if (!inString) {
                when (char) {
                    '<', '(', '[' -> depth += 1
                    '>', ')', ']' -> depth -= 1
                    ',' -> if (depth == 0) {
                        parameters += substring(start, index)
                        start = index + 1
                    }
                }
            }
            previous = char
        }

        parameters += substring(start)
        return parameters.map { it.trim() }.filter { it.isNotBlank() }
    }

    private data class RetrofitEndpoint(
        val method: String,
        val path: String,
    )

    private companion object {
        val unsupportedKotlinAndroidBaseTypes = setOf(
            "Activity",
            "AppCompatActivity",
            "ComponentActivity",
        )
        val roomDaoMethodAnnotations = setOf("Query", "Insert", "Update", "Delete", "Upsert")
        val roomDaoMethodNames = setOf("insert", "update", "delete", "upsert")
        val retrofitHttpAnnotations = setOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "HTTP")
        val retrofitHttpImports = retrofitHttpAnnotations.map { "retrofit2.http.$it" }.toSet()
        val parameterAnnotationRegex = Regex("""@[A-Za-z_][A-Za-z0-9_.]*(?:\([^)]*\))?\s*""")
        val topLevelTypeRegex = Regex("""(?m)^\uFEFF?((?:data\s+|sealed\s+|open\s+|abstract\s+)*)\b(class|interface)\s+([A-Za-z_][A-Za-z0-9_]*)(?:\s*\(([^)]*)\))?""")
        val dependencyCallRegex = Regex("""\b([A-Za-z_][A-Za-z0-9_]*)\.([A-Za-z_][A-Za-z0-9_]*)\(([^)]*)\)""")
        val retrofitEndpointRegex = Regex("""@(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\s*\(\s*"([^"]*)"\s*\)""")
        val queryAnnotationRegex = Regex("""@Query\s*\(\s*"([^"]+)"\s*\)""")
        val retrofitInterfaceRegex = Regex("""(?s)\binterface\s+([A-Za-z_][A-Za-z0-9_]*)\s*\{(.*?)\n\}""")
        val functionNameRegex = Regex("""(?:suspend\s+)?fun\s+([A-Za-z_][A-Za-z0-9_]*)""")
        val lineCommentRegex = Regex("""//.*""")
        val packageRegex = Regex("""(?m)^\uFEFF?\s*package\s+([A-Za-z_][A-Za-z0-9_.]*)""")
        val importRegex = Regex("""(?m)^\s*import\s+([A-Za-z_][A-Za-z0-9_.*]*)""")
    }
}








