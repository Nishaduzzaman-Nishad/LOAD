package com.example.load_app.models;

public class Package {
    public static final String STATUS_PENDING   = "PENDING";
    public static final String STATUS_ASSIGNED  = "ASSIGNED";
    public static final String STATUS_PICKED_UP = "PICKED_UP";
    public static final String STATUS_DELIVERED = "DELIVERED";

    private String packageId;
    private String pickupAddress;
    private String dropAddress;
    private String receiverPhone;
    private double weightKg;
    private String packageType;
    private String priority;
    private String status;
    private String assignedRiderId;
    private long createdAt;

    public Package() { }

    public Package(String packageId, String pickupAddress, String dropAddress,
                   String receiverPhone, double weightKg, String packageType, String priority) {
        this.packageId = packageId;
        this.pickupAddress = pickupAddress;
        this.dropAddress = dropAddress;
        this.receiverPhone = receiverPhone;
        this.weightKg = weightKg;
        this.packageType = packageType;
        this.priority = priority;
        this.status = STATUS_PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    public String getPackageId() { return packageId; }
    public void setPackageId(String packageId) { this.packageId = packageId; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public String getDropAddress() { return dropAddress; }
    public void setDropAddress(String dropAddress) { this.dropAddress = dropAddress; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }
    public String getPackageType() { return packageType; }
    public void setPackageType(String packageType) { this.packageType = packageType; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAssignedRiderId() { return assignedRiderId; }
    public void setAssignedRiderId(String assignedRiderId) { this.assignedRiderId = assignedRiderId; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}