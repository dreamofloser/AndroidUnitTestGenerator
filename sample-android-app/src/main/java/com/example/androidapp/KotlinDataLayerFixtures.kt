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
        return try {
            Result.success(api.fetchForecast(city))
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
