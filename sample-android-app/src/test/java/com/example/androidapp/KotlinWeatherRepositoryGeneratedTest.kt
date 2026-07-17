package com.example.androidapp

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
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
        assertNotNull(result.getOrNull())
        coVerify { api.fetchForecast(any()) }
    }

    @Test
    fun loadForecast_failure_returnsFailure() = runTest {
        coEvery { api.fetchForecast(any()) } throws RuntimeException("sample failure")

        val result = target.loadForecast("sample")

        assertTrue(result.isFailure)
        assertEquals("sample failure", result.exceptionOrNull()?.message)
        coVerify { api.fetchForecast(any()) }
    }

    @Test
    fun loadForecast_llm_guide_i1_loadForecast_city_blank_string() = runTest {
        coEvery { api.fetchForecast(any()) } returns KotlinForecastResponse()

        val result = target.loadForecast(" ")

        assertNotNull(result)
        coVerify { api.fetchForecast(any()) }
    }

    @Test
    fun loadForecast_llm_guide_i2_loadForecast_city_empty_string() = runTest {
        coEvery { api.fetchForecast(any()) } returns KotlinForecastResponse()

        val result = target.loadForecast("")

        assertNotNull(result)
        coVerify { api.fetchForecast(any()) }
    }
}
