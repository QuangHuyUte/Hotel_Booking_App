package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class BlockedDate {
    @SerializedName("_id")
    private String id;

    private String cabinId;
    private String hostId;
    private String startDate;
    private String endDate;
    private String reason;
    private String createdAt;
    private String updatedAt;

    public void setCabinId(String cabinId) {
        this.cabinId = cabinId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getId() {
        return id;
    }

    public String getCabinId() {
        return cabinId;
    }

    public String getHostId() {
        return hostId;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getReason() {
        return reason;
    }
}
