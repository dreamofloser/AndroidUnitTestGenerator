package com.codex.testgen.generator

import com.codex.testgen.model.ClassModel
import com.codex.testgen.model.ConstructorModel
import com.codex.testgen.model.DependencyCallModel
import com.codex.testgen.model.MethodModel
import com.codex.testgen.model.ParameterModel
import com.codex.testgen.model.PropertyModel
import com.codex.testgen.model.SourceClassKind
import com.codex.testgen.model.SourceLanguage
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KotlinUnitTestGeneratorTest {
    @Test
    fun generatesDataClassTest() {
        val model = ClassModel(
            packageName = "com.example.weather.viewmodel",
            className = "City",
            sourceFile = File("City.kt"),
            constructors = listOf(
                ConstructorModel(
                    parameters = listOf(
                        ParameterModel("name", "String"),
                        ParameterModel("lat", "Double"),
                        ParameterModel("lon", "Double"),
                    ),
                ),
            ),
            methods = emptyList(),
            language = SourceLanguage.KOTLIN,
            classKind = SourceClassKind.DATA,
        )

        val result = KotlinUnitTestGenerator().generate(model)
        val source = result.source

        assertTrue(source.contains("class CityGeneratedTest"))
        assertTrue(source.contains("val target = City("))
        assertTrue(source.contains("name = \"sample\""))
        assertTrue(source.contains("assertEquals(\"sample\", target.name)"))
        assertTrue(result.testMethodCount == 1)
    }

    @Test
    fun generatesSuspendRepositoryTestWithMockK() {
        val model = ClassModel(
            packageName = "com.example.weather.repository",
            className = "WeatherRepository",
            sourceFile = File("WeatherRepository.kt"),
            imports = listOf(
                "com.example.weather.data.WeatherApiService",
                "com.example.weather.data.WeatherResponse",
            ),
            constructors = listOf(
                ConstructorModel(
                    parameters = listOf(ParameterModel("apiService", "WeatherApiService")),
                ),
            ),
            methods = listOf(
                MethodModel(
                    name = "getWeather",
                    returnType = "Result<WeatherResponse>",
                    parameters = listOf(
                        ParameterModel("lat", "Double"),
                        ParameterModel("lon", "Double"),
                    ),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                    isSuspend = true,
                    dependencyCalls = listOf(
                        DependencyCallModel(
                            receiverName = "apiService",
                            methodName = "getCurrentWeather",
                            arguments = listOf("lat", "lon"),
                        ),
                    ),
                ),
            ),
            language = SourceLanguage.KOTLIN,
        )

        val result = KotlinUnitTestGenerator().generate(model)
        val source = result.source

        assertTrue(source.contains("import io.mockk.coEvery"))
        assertTrue(source.contains("import kotlinx.coroutines.test.runTest"))
        assertTrue(source.contains("class WeatherRepositoryGeneratedTest"))
        assertTrue(source.contains("private val apiService = mockk<WeatherApiService>()"))
        assertTrue(source.contains("coEvery { apiService.getCurrentWeather(any(), any()) } returns WeatherResponse()"))
        assertTrue(source.contains("coEvery { apiService.getCurrentWeather(any(), any()) } throws RuntimeException(\"sample failure\")"))
        assertTrue(source.contains("assertTrue(result.isSuccess)"))
        assertTrue(source.contains("assertTrue(result.isFailure)"))
        assertTrue(result.testMethodCount == 2)
    }

    @Test
    fun generatesViewModelStateFlowTestWithDispatcher() {
        val model = ClassModel(
            packageName = "com.example.weather.viewmodel",
            className = "WeatherViewModel",
            sourceFile = File("WeatherViewModel.kt"),
            imports = listOf("com.example.weather.repository.WeatherRepository"),
            constructors = listOf(
                ConstructorModel(
                    parameters = listOf(ParameterModel("repository", "WeatherRepository")),
                ),
            ),
            methods = listOf(
                MethodModel(
                    name = "fetchWeather",
                    returnType = "Unit",
                    parameters = emptyList(),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                ),
                MethodModel(
                    name = "selectCity",
                    returnType = "Unit",
                    parameters = listOf(ParameterModel("city", "City")),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                ),
                MethodModel(
                    name = "refreshWeather",
                    returnType = "Unit",
                    parameters = emptyList(),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                ),
            ),
            properties = listOf(
                PropertyModel("uiState", "StateFlow<WeatherUiState>"),
                PropertyModel("selectedCity", "StateFlow<City>"),
                PropertyModel("isRefreshing", "StateFlow<Boolean>"),
            ),
            language = SourceLanguage.KOTLIN,
            classKind = SourceClassKind.VIEW_MODEL,
        )

        val result = KotlinUnitTestGenerator().generate(model)
        val source = result.source

        assertTrue(source.contains("@OptIn(ExperimentalCoroutinesApi::class)"))
        assertTrue(source.contains("Dispatchers.setMain(testDispatcher)"))
        assertTrue(source.contains("Dispatchers.resetMain()"))
        assertTrue(source.contains("private val repository = mockk<WeatherRepository>()"))
        assertTrue(source.contains("coEvery { repository.getWeather(any(), any()) } returns Result.failure"))
        assertTrue(source.contains("testDispatcher.scheduler.advanceUntilIdle()"))
        assertTrue(source.contains("assertTrue(target.uiState.value is WeatherUiState.Error)"))
        assertTrue(source.contains("assertEquals(city, target.selectedCity.value)"))
        assertTrue(source.contains("assertFalse(target.isRefreshing.value)"))
        assertTrue(result.testMethodCount == 4)
    }

    @Test
    fun generatesComposeUiTestWithComposeRule() {
        val model = ClassModel(
            packageName = "com.example.weather",
            className = "WeatherScreenCompose",
            sourceFile = File("MainActivity.kt"),
            imports = listOf(
                "com.example.weather.viewmodel.City",
                "com.example.weather.viewmodel.WeatherUiState",
                "com.example.weather.viewmodel.WeatherViewModel",
            ),
            constructors = emptyList(),
            methods = listOf(
                MethodModel(
                    name = "WeatherScreen",
                    returnType = "Unit",
                    parameters = listOf(ParameterModel("viewModel", "WeatherViewModel")),
                    isStatic = true,
                    thrownExceptions = emptyList(),
                ),
            ),
            language = SourceLanguage.KOTLIN,
            classKind = SourceClassKind.COMPOSE_UI,
        )

        val result = KotlinUnitTestGenerator().generate(model)
        val source = result.source

        assertTrue(source.contains("class WeatherScreenComposeGeneratedTest"))
        assertTrue(source.contains("@RunWith(RobolectricTestRunner::class)"))
        assertTrue(source.contains("val composeTestRule = createComposeRule()"))
        assertTrue(source.contains("val viewModel = mockk<WeatherViewModel>(relaxed = true)"))
        assertTrue(source.contains("MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)"))
        assertTrue(source.contains("WeatherScreen(viewModel = viewModel)"))
        assertTrue(source.contains("onNodeWithTag(\"LoadingSpinner\").assertExists()"))
        assertTrue(result.composeTestCount == 1)
    }

    @Test
    fun generatesRoomDaoContractTestWithMockK() {
        val model = ClassModel(
            packageName = "com.example.weather.data",
            className = "CityDao",
            sourceFile = File("CityDao.kt"),
            imports = listOf("com.example.weather.viewmodel.City"),
            constructors = emptyList(),
            methods = listOf(
                MethodModel(
                    name = "observeCities",
                    returnType = "List<City>",
                    parameters = emptyList(),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                    isSuspend = true,
                ),
                MethodModel(
                    name = "insertCity",
                    returnType = "Unit",
                    parameters = listOf(ParameterModel("city", "City")),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                    isSuspend = true,
                ),
            ),
            language = SourceLanguage.KOTLIN,
            classKind = SourceClassKind.ROOM_DAO,
        )

        val result = KotlinUnitTestGenerator().generate(model)
        val source = result.source

        assertTrue(source.contains("class CityDaoGeneratedTest"))
        assertTrue(source.contains("private val target = mockk<CityDao>()"))
        assertTrue(source.contains("coEvery { target.observeCities() } returns emptyList<City>()"))
        assertTrue(source.contains("coVerify { target.observeCities() }"))
        assertTrue(source.contains("coEvery { target.insertCity(any()) } returns Unit"))
        assertTrue(source.contains("coVerify { target.insertCity(any()) }"))
        assertTrue(result.roomDaoTestCount == 1)
        assertTrue(result.testMethodCount == 2)
    }

    @Test
    fun generatesRetrofitApiContractTestWithResponse() {
        val model = ClassModel(
            packageName = "com.example.weather.data",
            className = "WeatherApiService",
            sourceFile = File("WeatherApiService.kt"),
            imports = listOf(
                "com.example.weather.data.WeatherResponse",
                "retrofit2.Response",
            ),
            constructors = emptyList(),
            methods = listOf(
                MethodModel(
                    name = "getCurrentWeather",
                    returnType = "Response<WeatherResponse>",
                    parameters = listOf(
                        ParameterModel("lat", "Double"),
                        ParameterModel("lon", "Double"),
                    ),
                    isStatic = false,
                    thrownExceptions = emptyList(),
                    isSuspend = true,
                ),
            ),
            language = SourceLanguage.KOTLIN,
            classKind = SourceClassKind.RETROFIT_API,
        )

        val result = KotlinUnitTestGenerator().generate(model)
        val source = result.source

        assertTrue(source.contains("class WeatherApiServiceGeneratedTest"))
        assertTrue(source.contains("import retrofit2.Response"))
        assertTrue(source.contains("coEvery { target.getCurrentWeather(any(), any()) } returns Response.success(WeatherResponse())"))
        assertTrue(source.contains("val result = target.getCurrentWeather(1.0, 1.0)"))
        assertTrue(source.contains("coVerify { target.getCurrentWeather(any(), any()) }"))
        assertTrue(result.retrofitApiTestCount == 1)
    }
}
