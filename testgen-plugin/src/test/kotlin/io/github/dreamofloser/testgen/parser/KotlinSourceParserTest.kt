package io.github.dreamofloser.testgen.parser

import io.github.dreamofloser.testgen.model.SourceClassKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KotlinSourceParserTest {
    @Test
    fun parsesDataClassConstructorWithGenericTypes() {
        val file = File.createTempFile("city-model", ".kt").apply {
            writeText(
                """
                package com.example.weather.model

                data class City(
                    val name: String,
                    val coordinates: List<Double>,
                    val tags: MutableList<String> = mutableListOf()
                )
                """.trimIndent(),
            )
            deleteOnExit()
        }

        val models = KotlinSourceParser().parse(file)

        assertEquals(1, models.size)
        assertEquals(SourceClassKind.DATA, models.single().classKind)
        assertEquals(3, models.single().constructors.single().parameters.size)
        assertEquals("List<Double>", models.single().constructors.single().parameters[1].type)
    }

    @Test
    fun parsesViewModelSuperTypeAndDependencyCalls() {
        val file = File.createTempFile("weather-viewmodel", ".kt").apply {
            writeText(
                """
                package com.example.weather.viewmodel

                import androidx.lifecycle.ViewModel
                import kotlinx.coroutines.flow.StateFlow

                class WeatherViewModel(
                    private val repository: WeatherRepository
                ) : ViewModel() {
                    val uiState: StateFlow<WeatherUiState> = TODO()

                    suspend fun fetchWeather(lat: Double, lon: Double): Result<WeatherResponse> {
                        return repository.getWeather(lat, lon)
                    }
                }
                """.trimIndent(),
            )
            deleteOnExit()
        }

        val models = KotlinSourceParser().parse(file)
        val model = models.single()

        assertEquals(SourceClassKind.VIEW_MODEL, model.classKind)
        assertEquals("StateFlow<WeatherUiState>", model.properties.single().type)
        assertEquals("fetchWeather", model.methods.single().name)
        assertEquals("repository", model.methods.single().dependencyCalls.single().receiverName)
        assertEquals("getWeather", model.methods.single().dependencyCalls.single().methodName)
    }

    @Test
    fun parsesRoomDaoInterface() {
        val file = File.createTempFile("room-dao", ".kt").apply {
            writeText(
                """
                package com.example.weather.data

                import androidx.room.Dao
                import androidx.room.Query
                import com.example.weather.viewmodel.City

                @Dao
                interface CityDao {
                    @Query("SELECT * FROM city")
                    suspend fun observeCities(): List<City>
                }
                """.trimIndent(),
            )
            deleteOnExit()
        }

        val models = KotlinSourceParser().parse(file)

        assertEquals(1, models.size)
        assertEquals(SourceClassKind.ROOM_DAO, models.single().classKind)
        assertEquals("observeCities", models.single().methods.single().name)
        assertTrue(models.single().methods.single().isSuspend)
    }

    @Test
    fun parsesRetrofitApiInterface() {
        val file = File.createTempFile("retrofit-api", ".kt").apply {
            writeText(
                """
                package com.example.weather.data

                import retrofit2.Response
                import retrofit2.http.GET
                import retrofit2.http.Query

                interface WeatherApiService {
                    @GET("weather")
                    suspend fun getCurrentWeather(@Query("lat") lat: Double, @Query("lon") lon: Double): Response<WeatherResponse>
                }
                """.trimIndent(),
            )
            deleteOnExit()
        }

        val models = KotlinSourceParser().parse(file)

        assertEquals(1, models.size)
        assertEquals(SourceClassKind.RETROFIT_API, models.single().classKind)
        assertEquals("getCurrentWeather", models.single().methods.single().name)
        assertEquals(2, models.single().methods.single().parameters.size)
        assertEquals("GET", models.single().methods.single().httpMethod)
        assertEquals("weather", models.single().methods.single().httpPath)
        assertEquals(listOf("lat", "lon"), models.single().methods.single().httpQueryNames)
    }

    @Test
    fun parsesRetrofitInterfaceAfterDataClassesWithDefaultParameters() {
        val file = File.createTempFile("weather-api-real-shape", ".kt").apply {
            writeText(
                """
                package com.example.weather.data

                import kotlinx.serialization.Serializable
                import retrofit2.http.GET
                import retrofit2.http.Query

                @Serializable
                data class WeatherResponse(val current: Current? = null)

                @Serializable
                data class Current(val apparent_temperature: Double? = null)

                interface WeatherApiService {
                    @GET("v1/forecast")
                    suspend fun getCurrentWeather(
                        @Query("latitude") lat: Double = 39.9042, // Default Beijing
                        @Query("longitude") lon: Double = 116.4074,
                        @Query("current_weather") currentWeather: Boolean = true,
                        @Query("timezone") timezone: String = "auto"
                    ): WeatherResponse
                }
                """.trimIndent(),
            )
            deleteOnExit()
        }

        val models = KotlinSourceParser().parse(file)
        val api = models.single { it.className == "WeatherApiService" }

        assertEquals(SourceClassKind.RETROFIT_API, api.classKind)
        assertEquals("WeatherResponse", api.methods.single().returnType)
        assertEquals(4, api.methods.single().parameters.size)
        assertEquals("lat", api.methods.single().parameters.first().name)
        assertEquals("GET", api.methods.single().httpMethod)
        assertEquals("v1/forecast", api.methods.single().httpPath)
        assertEquals(listOf("latitude", "longitude", "current_weather", "timezone"), api.methods.single().httpQueryNames)
    }

    @Test
    fun parsesKotlinDataApiAndRepositoryFromOneFile() {
        val file = File.createTempFile("android-kotlin-fixture", ".kt").apply {
            writeText(
                """
                package com.example.androidapp

                import retrofit2.http.GET
                import retrofit2.http.Query

                data class KotlinForecastResponse(
                    val city: String = "sample",
                    val temperature: Double = 20.0,
                )

                interface KotlinWeatherApi {
                    @GET("forecast")
                    suspend fun fetchForecast(
                        @Query("city") city: String = "sample",
                        @Query("units") units: String = "metric",
                    ): KotlinForecastResponse
                }

                class KotlinWeatherRepository(
                    private val api: KotlinWeatherApi,
                ) {
                    suspend fun loadForecast(city: String): Result<KotlinForecastResponse> {
                        return Result.success(api.fetchForecast(city))
                    }
                }
                """.trimIndent(),
            )
            deleteOnExit()
        }

        val models = KotlinSourceParser().parse(file)
        val api = models.single { it.className == "KotlinWeatherApi" }
        val repository = models.single { it.className == "KotlinWeatherRepository" }

        assertEquals(setOf("KotlinForecastResponse", "KotlinWeatherApi", "KotlinWeatherRepository"), models.map { it.className }.toSet())
        assertEquals("com.example.androidapp", api.packageName)
        assertEquals(SourceClassKind.RETROFIT_API, api.classKind)
        assertEquals(SourceClassKind.REGULAR, repository.classKind)
        assertEquals("api", repository.constructors.single().parameters.single().name)
        assertEquals("fetchForecast", repository.methods.single().dependencyCalls.single().methodName)
    }

    @Test
    fun skipsKotlinActivityClassesForDedicatedLifecycleTemplates() {
        val file = File.createTempFile("main-activity", ".kt").apply {
            writeText(
                """
                package com.example.weather

                import androidx.activity.ComponentActivity

                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: android.os.Bundle?) {
                        super.onCreate(savedInstanceState)
                    }
                }
                """.trimIndent(),
            )
            deleteOnExit()
        }

        val models = KotlinSourceParser().parse(file)

        assertTrue(models.none { it.className == "MainActivity" })
    }
}


