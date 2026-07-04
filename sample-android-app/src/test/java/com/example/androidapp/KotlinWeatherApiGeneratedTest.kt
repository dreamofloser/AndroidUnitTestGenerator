package com.example.androidapp

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.Query

class KotlinWeatherApiGeneratedTest {
    private val target = mockk<KotlinWeatherApi>()

    @Test
    fun fetchForecast_returnsStubbedValue() = runTest {
        coEvery { target.fetchForecast(any(), any()) } returns KotlinForecastResponse()

        val result = target.fetchForecast("sample", "sample")

        assertNotNull(result)
        coVerify { target.fetchForecast(any(), any()) }
    }

}
