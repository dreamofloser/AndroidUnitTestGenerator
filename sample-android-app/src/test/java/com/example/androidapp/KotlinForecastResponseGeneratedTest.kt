package com.example.androidapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.Query

class KotlinForecastResponseGeneratedTest {

    @Test
    fun createsKotlinForecastResponseWithSampleValues() {
        val target = KotlinForecastResponse(
            city = "sample",
            temperature = 1.0
        )

        assertNotNull(target)
        assertEquals("sample", target.city)
        assertEquals(1.0, target.temperature, 0.0)
    }

}
