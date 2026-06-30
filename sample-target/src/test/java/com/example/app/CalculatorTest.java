package com.example.app;

import org.junit.Test;

public class CalculatorTest {

    @Test
    public void add_shouldRunWithoutException() throws Exception {
        Calculator target = new Calculator();

        int result = target.add(1, 1);
    }

    @Test
    public void subtract_shouldRunWithoutException() throws Exception {
        Calculator target = new Calculator();

        int result = target.subtract(1, 1);
    }

    @Test
    public void isAdult_shouldRunWithoutException() throws Exception {
        boolean result = Calculator.isAdult(1);
    }

}
