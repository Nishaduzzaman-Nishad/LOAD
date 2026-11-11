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
    private String receiverName;
    private double weightKg;
    private String packageType;
    private String priority;
    private String status;
    private String assignedRiderId;

    private String customerId;
    private String customerName;
    private String customerPhone;

    private String pickupOtp;
    private String deliveryOtp;

    private String pickupPhotoUrl;
    private String deliveryPhotoUrl;
    private long createdAt;

    private double customerPrice;
    private double riderFee;
    private double netProfit;

    public Package() { }

    public Package(String packageId, String pickupAddress, String dropAddress,
                   String receiverPhone, double weightKg, String packageType, String priority, String customerId) {
        this.packageId = packageId;
        this.pickupAddress = pickupAddress;
        this.dropAddress = dropAddress;
        this.receiverPhone = receiverPhone;
        this.weightKg = weightKg;
        this.packageType = packageType;
        this.priority = priority;
        this.status = STATUS_PENDING;
        this.customerId = customerId;
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
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
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
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getPickupOtp() { return pickupOtp; }
    public void setPickupOtp(String pickupOtp) { this.pickupOtp = pickupOtp; }
    public String getDeliveryOtp() { return deliveryOtp; }
    public void setDeliveryOtp(String deliveryOtp) { this.deliveryOtp = deliveryOtp; }
    public String getPickupPhotoUrl() { return pickupPhotoUrl; }
    public void setPickupPhotoUrl(String pickupPhotoUrl) { this.pickupPhotoUrl = pickupPhotoUrl; }
    public String getDeliveryPhotoUrl() { return deliveryPhotoUrl; }
    public void setDeliveryPhotoUrl(String deliveryPhotoUrl) { this.deliveryPhotoUrl = deliveryPhotoUrl; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public double getCustomerPrice() { return customerPrice; }
    public void setCustomerPrice(double customerPrice) { this.customerPrice = customerPrice; }
    public double getRiderFee() { return riderFee; }
    public void setRiderFee(double riderFee) { this.riderFee = riderFee; }
    public double getNetProfit() { return netProfit; }
    public void setNetProfit(double netProfit) { this.netProfit = netProfit; }
}