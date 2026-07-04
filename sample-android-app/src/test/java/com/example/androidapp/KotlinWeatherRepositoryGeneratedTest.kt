package com.example.androidapp

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.Query

class KotlinWeatherRepositoryGeneratedTest {

    private val api = mockk<KotlinWeatherApi>()
    private val target = KotlinWeatherRepository(api)

    @Test
    fun loadForecast_success_returnsSuccess() = runTest {
        coEvery { api.fetchForecast(any()) } returns KotlinForecastResponse()

        val result = target.loadForecast("sample")

        assertTrue(result.isSuccess)
    }

    @Test
    fun loadForecast_failure_returnsFailure() = runTest {
        coEvery { api.fetchForecast(any()) } throws RuntimeException("sample failure")

        val result = target.loadForecast("sample")

        assertTrue(result.isFailure)
    }

}
