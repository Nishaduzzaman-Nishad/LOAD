package com.example.load.models;

public class Rider extends User {

    private double maxCapacityKg;   // Maximum weight the rider can carry
    private boolean isOnline;        // Online/Offline status
    private double safetyScore;      // 0-100
    private double totalEarnings;    // Total money earned
    private long shiftStartTime;     // When the shift started

    public Rider() {
        super();
    }

    public Rider(String userId, String fullName, String email, String phone,
                 String password, double maxCapacityKg) {
       super(userId, fullName, email, phone, password, "RIDER");
        this.maxCapacityKg = maxCapacityKg;
        this.isOnline = false;
        this.safetyScore = 100.0;
        this.totalEarnings = 0.0;
    }

    public double getMaxCapacityKg() { return maxCapacityKg; }
    public void setMaxCapacityKg(double maxCapacityKg) { this.maxCapacityKg = maxCapacityKg; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public double getSafetyScore() { return safetyScore; }
    public void setSafetyScore(double safetyScore) { this.safetyScore = safetyScore; }

    public double getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(double totalEarnings) { this.totalEarnings = totalEarnings; }

    public long getShiftStartTime() { return shiftStartTime; }
    public void setShiftStartTime(long shiftStartTime) { this.shiftStartTime = shiftStartTime; }
}