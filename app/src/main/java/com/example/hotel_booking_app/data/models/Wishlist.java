package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class Wishlist {
    @SerializedName("_id")
    private String id;

    private String userId;
    private String cabinId;
    private String createdAt;

    public Wishlist() {
    }

    public Wishlist(String userId, String cabinId) {
        this.userId = userId;
        this.cabinId = cabinId;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getCabinId() {
        return cabinId;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
