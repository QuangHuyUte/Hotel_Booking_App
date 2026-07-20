package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class Conversation {
    @SerializedName("_id")
    private String id;

    private String guestId;
    private String hostId;
    private String cabinId;
    private String bookingId;
    private String createdAt;
    private String updatedAt;

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public void setCabinId(String cabinId) {
        this.cabinId = cabinId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getId() {
        return id;
    }

    public String getGuestId() {
        return guestId;
    }

    public String getHostId() {
        return hostId;
    }

    public String getCabinId() {
        return cabinId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
