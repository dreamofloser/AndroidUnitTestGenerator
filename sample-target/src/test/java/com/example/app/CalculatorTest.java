package com.example.app;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CalculatorTest {

    @Test
    public void add_withDefaultInputs_shouldReturnExpectedValue() throws Exception {
        Calculator target = new Calculator();

        int result = target.add(1, 1);

        assertEquals(2, result);
    }

    @Test
    public void subtract_withDefaultInputs_shouldReturnExpectedValue() throws Exception {
        Calculator target = new Calculator();

        int result = target.subtract(1, 1);

        assertEquals(0, result);
    }

    @Test
    public void isAdult_whenAgeIs18_shouldReturnTrue() throws Exception {
        boolean result = Calculator.isAdult(18);

        assertTrue(result);
    }

    @Test
    public void isAdult_whenAgeIs17_shouldReturnFalse() throws Exception {
        boolean result = Calculator.isAdult(17);

        assertFalse(result);
    }

    @Test
    public void multiply_withDefaultInputs_shouldReturnExpectedValue() throws Exception {
        Calculator target = new Calculator();

        int result = target.multiply(1, 1);

        assertEquals(1, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void requirePositive_whenInvalidInput_shouldThrowIllegalArgumentException() throws Exception {
        Calculator target = new Calculator();

        target.requirePositive(0);
    }

}
