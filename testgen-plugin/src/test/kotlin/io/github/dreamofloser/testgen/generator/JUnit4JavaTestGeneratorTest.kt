package io.github.dreamofloser.testgen.generator

import io.github.dreamofloser.testgen.model.ClassModel
import io.github.dreamofloser.testgen.model.ConstructorModel
import io.github.dreamofloser.testgen.model.DependencyCallModel
import io.github.dreamofloser.testgen.model.MethodModel
import io.github.dreamofloser.testgen.model.ParameterModel
import io.github.dreamofloser.testgen.model.SourceClassKind
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

    @Test
    fun importsAndroidTypesAndAddsLiveDataRule() {
        val model = ClassModel(
            packageName = "com.example.androidapp",
            className = "AndroidPresenter",
            sourceFile = File("AndroidPresenter.java"),
            imports = listOf(
                "android.content.Context",
                "androidx.lifecycle.LiveData",
            ),
            constructors = listOf(
                ConstructorModel(
                    parameters = listOf(ParameterModel("context", "Context")),
                ),
            ),
            methods = listOf(
                MethodModel(
                    name = "getTitle",
                    returnType = "String",
                    parameters = emptyList(),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                    returnExpressions = listOf("context.getString(R.string.app_name)"),
                    dependencyCalls = listOf(
                        DependencyCallModel(
                            receiverName = "context",
                            methodName = "getString",
                            arguments = listOf("R.string.app_name"),
                        ),
                    ),
                ),
                MethodModel(
                    name = "state",
                    returnType = "LiveData<String>",
                    parameters = emptyList(),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                ),
            ),
        )

        val result = JUnit4JavaTestGenerator().generate(model)
        val source = result.source

        assertTrue(source.contains("import org.junit.Rule;"))
        assertTrue(source.contains("import org.junit.runner.RunWith;"))
        assertTrue(source.contains("import org.robolectric.annotation.Config;"))
        assertTrue(source.contains("import androidx.arch.core.executor.testing.InstantTaskExecutorRule;"))
        assertTrue(source.contains("import android.content.Context;"))
        assertTrue(source.contains("import androidx.lifecycle.LiveData;"))
        assertTrue(source.contains("@RunWith(RobolectricTestRunner.class)"))
        assertTrue(source.contains("@Config(sdk = 34)"))
        assertTrue(source.contains("public InstantTaskExecutorRule instantTaskExecutorRule"))
        assertTrue(source.contains("private Context context;"))
        assertTrue(source.contains("context = mock(Context.class);"))
        assertTrue(source.contains("LiveData<String> result = target.state();"))
        assertTrue(result.liveDataRuleCount == 1)
        assertTrue(result.robolectricTestCount == 1)
        assertTrue(result.androidImportCount == 2)
    }

    @Test
    fun generatesActivityLifecycleTest() {
        val model = ClassModel(
            packageName = "com.example.androidapp",
            className = "LifecycleDemoActivity",
            sourceFile = File("LifecycleDemoActivity.java"),
            imports = listOf("android.app.Activity"),
            constructors = emptyList(),
            methods = listOf(
                MethodModel("isCreated", "boolean", emptyList(), false, emptyList()),
                MethodModel("isStarted", "boolean", emptyList(), false, emptyList()),
            ),
            classKind = SourceClassKind.ACTIVITY,
        )

        val result = JUnit4JavaTestGenerator().generate(model)
        val source = result.source

        assertTrue(source.contains("Robolectric.buildActivity(LifecycleDemoActivity.class)"))
        assertTrue(source.contains("controller.create().start().resume().get()"))
        assertTrue(source.contains("assertTrue(activity.isCreated());"))
        assertTrue(source.contains("assertTrue(activity.isStarted());"))
        assertTrue(result.robolectricTestCount == 1)
    }

    @Test
    fun generatesFragmentLifecycleTest() {
        val model = ClassModel(
            packageName = "com.example.androidapp",
            className = "LifecycleDemoFragment",
            sourceFile = File("LifecycleDemoFragment.java"),
            imports = listOf("android.app.Fragment"),
            constructors = emptyList(),
            methods = listOf(
                MethodModel("isCreated", "boolean", emptyList(), false, emptyList()),
            ),
            classKind = SourceClassKind.FRAGMENT,
        )

        val result = JUnit4JavaTestGenerator().generate(model)
        val source = result.source

        assertTrue(source.contains("ActivityController<Activity> controller"))
        assertTrue(source.contains("new LifecycleDemoFragment()"))
        assertTrue(source.contains("beginTransaction().add(fragment, \"target\").commitNow()"))
        assertTrue(source.contains("assertTrue(fragment.isAdded());"))
        assertTrue(source.contains("assertTrue(fragment.isCreated());"))
        assertTrue(result.robolectricTestCount == 1)
    }
}
