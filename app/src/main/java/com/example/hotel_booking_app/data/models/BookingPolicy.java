package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class BookingPolicy {
    @SerializedName("_id")
    private String id;

    private String cabinId;
    private double breakfastPrice;
    private int miniBookingLength;
    private int maxBookingLength;
    private String createdAt;
    private String updatedAt;

    public String getId() {
        return id;
    }

    public String getCabinId() {
        return cabinId;
    }

    public double getBreakfastPrice() {
        return breakfastPrice;
    }

    public int getMiniBookingLength() {
        return miniBookingLength;
    }

    public int getMaxBookingLength() {
        return maxBookingLength;
    }
}
