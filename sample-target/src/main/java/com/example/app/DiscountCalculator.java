package com.example.app;

public class DiscountCalculator {
    public int finalPrice(int price, int discount) {
        return price - discount;
    }

    public boolean isFreeShipping(int totalPrice) {
        return totalPrice >= 99;
    }

    public void requireValidPrice(int price) {
        if (price <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
    }
}