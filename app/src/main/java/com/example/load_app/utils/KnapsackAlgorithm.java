package com.example.load_app.utils;

import com.example.load_app.models.Package;
import java.util.ArrayList;
import java.util.List;

public class KnapsackAlgorithm {
    public static List<Package> getOptimalPackages(List<Package> availablePackages, double maxCapacity) {
        int n = availablePackages.size();
        int capacity = (int) maxCapacity;
        int[][] dp = new int[n + 1][capacity + 1];

        for (int i = 1; i <= n; i++) {
            Package pkg = availablePackages.get(i - 1);
            int weight = (int) pkg.getWeightKg();
            int value = 10;
            for (int w = 1; w <= capacity; w++) {
                if (weight <= w) {
                    dp[i][w] = Math.max(dp[i - 1][w], dp[i - 1][w - weight] + value);
                } else {
                    dp[i][w] = dp[i - 1][w];
                }
            }
        }

        List<Package> result = new ArrayList<>();
        int w = capacity;
        for (int i = n; i > 0 && w > 0; i--) {
            if (dp[i][w] != dp[i - 1][w]) {
                Package pkg = availablePackages.get(i - 1);
                result.add(pkg);
                w -= (int) pkg.getWeightKg();
            }
        }
        return result;
    }
}