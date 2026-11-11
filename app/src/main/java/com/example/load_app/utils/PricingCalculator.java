package com.example.load_app.utils;

public class PricingCalculator {
    public static double calculateCustomerPrice(double weight, String priority) {
        double base = 50.0;
        double weightCharge = weight * 15.0;
        double urgency = "URGENT".equalsIgnoreCase(priority) ? 40.0 : 0.0;
        return base + weightCharge + urgency;
    }

    public static double calculateRiderFee(double customerPrice) {
        return customerPrice * 0.4;
    }

    public static double calculateNetProfit(double customerPrice) {
        return customerPrice * 0.6;
    }
}