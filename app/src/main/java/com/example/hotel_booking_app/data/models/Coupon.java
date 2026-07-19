package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class Coupon {
    @SerializedName("_id")
    private String id;

    private String code;
    private String description;
    private String discountType;
    private double discountValue;
    private Double maxDiscountAmount;
    private double minBookingAmount;
    private String startDate;
    private String endDate;
    private Integer usageLimit;
    private int usedCount;
    private boolean isActive;
    private String createdAt;
    private String updatedAt;

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getDiscountType() {
        return discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public Double getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public double getMinBookingAmount() {
        return minBookingAmount;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public boolean isActive() {
        return isActive;
    }
}
