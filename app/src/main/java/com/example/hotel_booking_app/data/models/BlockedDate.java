package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class BlockedDate {
    @SerializedName("_id")
    private String id;

    private String cabinId;
    private String roomTypeId;
    private String hostId;
    private int numRooms = 1;
    private int roomUnitNumber;
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

    public void setNumRooms(int numRooms) {
        this.numRooms = Math.max(1, numRooms);
    }

    public void setRoomUnitNumber(int roomUnitNumber) {
        this.roomUnitNumber = Math.max(0, roomUnitNumber);
    }

    public void setRoomTypeId(String roomTypeId) {
        this.roomTypeId = roomTypeId;
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

    public int getNumRooms() {
        return numRooms <= 0 ? 1 : numRooms;
    }

    public int getRoomUnitNumber() {
        return roomUnitNumber;
    }

    public String getRoomTypeId() {
        return roomTypeId;
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
