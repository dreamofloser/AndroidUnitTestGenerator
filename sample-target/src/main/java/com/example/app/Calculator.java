package com.example.app;

public class Calculator {
    public int add(int a, int b) {
        return a + b;
    }

    public int subtract(int a, int b) {
        return a - b;
    }

    public static boolean isAdult(int age) {
        return age >= 18;
    }

    public int multiply(int a, int b) {
        return a * b;
    }

    public void requirePositive(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("value must be positive");
        }
    }
}
