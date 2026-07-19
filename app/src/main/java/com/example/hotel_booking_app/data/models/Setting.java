package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class Setting {
    @SerializedName("_id")
    private String id;

    private int miniBookingLength;
    private int maxBookingLength;
    private int maxNumberOfGuests;
    private double breakfastPrice;
    private String createdAt;
    private String updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMiniBookingLength() {
        return miniBookingLength;
    }

    public void setMiniBookingLength(int miniBookingLength) {
        this.miniBookingLength = miniBookingLength;
    }

    public int getMaxBookingLength() {
        return maxBookingLength;
    }

    public void setMaxBookingLength(int maxBookingLength) {
        this.maxBookingLength = maxBookingLength;
    }

    public int getMaxNumberOfGuests() {
        return maxNumberOfGuests;
    }

    public void setMaxNumberOfGuests(int maxNumberOfGuests) {
        this.maxNumberOfGuests = maxNumberOfGuests;
    }

    public double getBreakfastPrice() {
        return breakfastPrice;
    }

    public void setBreakfastPrice(double breakfastPrice) {
        this.breakfastPrice = breakfastPrice;
    }
}
