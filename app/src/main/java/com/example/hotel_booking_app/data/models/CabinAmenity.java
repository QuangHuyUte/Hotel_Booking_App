package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class CabinAmenity {
    @SerializedName("_id")
    private String id;

    private String cabinId;
    private String amenityId;
    private String createdAt;

    public CabinAmenity() {
    }

    public CabinAmenity(String cabinId, String amenityId) {
        this.cabinId = cabinId;
        this.amenityId = amenityId;
    }

    public String getId() {
        return id;
    }

    public String getCabinId() {
        return cabinId;
    }

    public String getAmenityId() {
        return amenityId;
    }
}
