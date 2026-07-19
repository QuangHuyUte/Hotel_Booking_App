package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class Promotion {
    @SerializedName("_id")
    private String id;

    private String cabinId;
    private double discountPercent;
    private String startDate;
    private String endDate;
    private boolean isActive;
    private String createdAt;
    private String updatedAt;

    public String getId() {
        return id;
    }

    public String getCabinId() {
        return cabinId;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public boolean isActive() {
        return isActive;
    }
}
