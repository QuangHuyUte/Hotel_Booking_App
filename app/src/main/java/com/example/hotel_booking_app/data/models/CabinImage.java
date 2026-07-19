package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class CabinImage {
    @SerializedName("_id")
    private String id;

    private String cabinId;
    private String imageUrl;
    private String name;
    private boolean isCover;
    private String createdAt;

    public String getId() {
        return id;
    }

    public String getCabinId() {
        return cabinId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getName() {
        return name;
    }

    public boolean isCover() {
        return isCover;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
