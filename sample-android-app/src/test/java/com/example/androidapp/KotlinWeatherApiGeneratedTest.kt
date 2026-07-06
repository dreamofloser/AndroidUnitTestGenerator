package com.example.androidapp

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.lang.reflect.Type
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Converter
import retrofit2.Response
import retrofit2.Retrofit
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

    @Test
    fun fetchRawForecast_returnsStubbedValue() = runTest {
        coEvery { target.fetchRawForecast(any()) } returns Response.success(KotlinForecastResponse())

        val result = target.fetchRawForecast("sample")

        assertNotNull(result)
        coVerify { target.fetchRawForecast(any()) }
    }

    @Test
    fun fetchForecast_hasRetrofitEndpointMetadata() {
        val method = KotlinWeatherApi::class.java.declaredMethods.single { it.name == "fetchForecast" }
        val annotation = method.getAnnotation(GET::class.java)

        assertNotNull(annotation)
        assertEquals("forecast", annotation!!.value)
        val queryNames = method.parameterAnnotations
            .flatMap { annotations -> annotations.filterIsInstance<Query>().map { it.value } }
        assertEquals(listOf("city", "units"), queryNames)
    }

    @Test
    fun fetchRawForecast_hasRetrofitEndpointMetadata() {
        val method = KotlinWeatherApi::class.java.declaredMethods.single { it.name == "fetchRawForecast" }
        val annotation = method.getAnnotation(GET::class.java)

        assertNotNull(annotation)
        assertEquals("forecast/raw", annotation!!.value)
        val queryNames = method.parameterAnnotations
            .flatMap { annotations -> annotations.filterIsInstance<Query>().map { it.value } }
        assertEquals(listOf("city"), queryNames)
    }

    @Test
    fun fetchForecast_requestsExpectedUrlWithMockWebServer() = runTest {
        val server = MockWebServer()
        try {
            server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
            server.start()
            val service = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(sampleConverterFactory(KotlinForecastResponse()))
                .build()
                .create(KotlinWeatherApi::class.java)

            val result = service.fetchForecast("sample", "sample")

            assertNotNull(result)
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/forecast?city=sample&units=sample", request.path)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun fetchRawForecast_requestsExpectedUrlWithMockWebServer() = runTest {
        val server = MockWebServer()
        try {
            server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
            server.start()
            val service = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(sampleConverterFactory(KotlinForecastResponse()))
                .build()
                .create(KotlinWeatherApi::class.java)

            val result = service.fetchRawForecast("sample")

            assertNotNull(result)
            assertTrue(result.isSuccessful)
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/forecast/raw?city=sample", request.path)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun fetchRawForecast_returnsHttpErrorWithMockWebServer() = runTest {
        val server = MockWebServer()
        try {
            server.enqueue(MockResponse().setResponseCode(500).setBody("{\"message\":\"sample failure\"}"))
            server.start()
            val service = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(sampleConverterFactory(KotlinForecastResponse()))
                .build()
                .create(KotlinWeatherApi::class.java)

            val result = service.fetchRawForecast("sample")

            assertFalse(result.isSuccessful)
            assertEquals(500, result.code())
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/forecast/raw?city=sample", request.path)
        } finally {
            server.shutdown()
        }
    }

    private fun sampleConverterFactory(value: Any): Converter.Factory {
        return object : Converter.Factory() {
            override fun responseBodyConverter(
                type: Type,
                annotations: Array<Annotation>,
                retrofit: Retrofit,
            ): Converter<ResponseBody, *> {
                return Converter<ResponseBody, Any> { value }
            }
        }
    }

}
