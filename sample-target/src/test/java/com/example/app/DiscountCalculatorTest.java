package com.example.app;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiscountCalculatorTest {

    @Test
    public void finalPrice_withDefaultInputs_shouldReturnExpectedValue() throws Exception {
        DiscountCalculator target = new DiscountCalculator();

        int result = target.finalPrice(1, 1);

        assertEquals(0, result);
    }

    @Test
    public void isFreeShipping_whenTotalPriceIs99_shouldReturnTrue() throws Exception {
        DiscountCalculator target = new DiscountCalculator();

        boolean result = target.isFreeShipping(99);

        assertTrue(result);
    }

    @Test
    public void isFreeShipping_whenTotalPriceIs98_shouldReturnFalse() throws Exception {
        DiscountCalculator target = new DiscountCalculator();

        boolean result = target.isFreeShipping(98);

        assertFalse(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireValidPrice_whenInvalidInput_shouldThrowIllegalArgumentException() throws Exception {
        DiscountCalculator target = new DiscountCalculator();

        target.requireValidPrice(0);
    }

}
