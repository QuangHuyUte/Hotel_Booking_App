package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class RoomInventory {
    @SerializedName("_id")
    private String id;

    private String roomTypeId;
    private String date;
    private int availableRooms;
    private double priceOverride;
    private boolean isClosed;
    private String createdAt;
    private String updatedAt;

    public String getId() {
        return id;
    }

    public String getRoomTypeId() {
        return roomTypeId;
    }

    public String getDate() {
        return date;
    }

    public int getAvailableRooms() {
        return availableRooms;
    }

    public double getPriceOverride() {
        return priceOverride;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
