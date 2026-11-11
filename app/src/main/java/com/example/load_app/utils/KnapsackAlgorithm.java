package com.example.load_app.utils;

import com.example.load_app.models.Package;

import java.util.ArrayList;
import java.util.List;

public class KnapsackAlgorithm {

    // Helper method to assign value based on Priority
    private static int getValue(Package pkg) {
        if ("URGENT".equalsIgnoreCase(pkg.getPriority()) || "HIGH".equalsIgnoreCase(pkg.getPriority())) {
            return 100; // High value for urgent packages
        }
        return 10; // Normal value
    }

    public static List<Package> getOptimalPackages(List<Package> availablePackages, double maxCapacity) {
        if (availablePackages == null || availablePackages.isEmpty()) return new ArrayList<>();

        int n = availablePackages.size();
        // Scale capacity and weights by 100 to handle decimals (e.g., 2.5 kg -> 250)
        int capacity = (int) (maxCapacity * 100);
        int[][] dp = new int[n + 1][capacity + 1];

        for (int i = 1; i <= n; i++) {
            Package pkg = availablePackages.get(i - 1);
            int weight = (int) (pkg.getWeightKg() * 100);
            int value = getValue(pkg); // Dynamic value based on priority!

            for (int w = 1; w <= capacity; w++) {
                if (weight <= w) {
                    dp[i][w] = Math.max(dp[i - 1][w], dp[i - 1][w - weight] + value);
                } else {
                    dp[i][w] = dp[i - 1][w];
                }
            }
        }

        // Backtrack to find selected packages
        List<Package> result = new ArrayList<>();
        int w = capacity;
        for (int i = n; i > 0 && w > 0; i--) {
            if (dp[i][w] != dp[i - 1][w]) {
                Package pkg = availablePackages.get(i - 1);
                result.add(pkg);
                w -= (int) (pkg.getWeightKg() * 100);
            }
        }
        return result;
    }
}
