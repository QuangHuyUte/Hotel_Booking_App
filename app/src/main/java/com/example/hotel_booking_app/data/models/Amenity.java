package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class Amenity {
    @SerializedName("_id")
    private String id;

    private String name;
    private String icon;
    private String createdAt;
    private String updatedAt;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }
}
