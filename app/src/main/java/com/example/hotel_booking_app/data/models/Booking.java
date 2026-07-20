package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class Booking {
    @SerializedName("_id")
    private String id;

    private String userId;
    private String cabinId;
    private String roomTypeId;
    private String startDate;
    private String endDate;
    private int numNights;
    private int numGuests;
    private int numRooms = 1;
    private double cabinPrice;
    private double extrasPrice;
    private double totalPrice;
    private String status;
    private boolean hasBreakfast;
    private boolean isPaid;
    private String observations;
    private String couponId;
    private double discountAmount;
    private String createdAt;
    private String updatedAt;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setCabinId(String cabinId) {
        this.cabinId = cabinId;
    }

    public String getCabinId() {
        return cabinId;
    }

    public void setRoomTypeId(String roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getRoomTypeId() {
        return roomTypeId;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setNumNights(int numNights) {
        this.numNights = numNights;
    }

    public int getNumNights() {
        return numNights;
    }

    public void setNumGuests(int numGuests) {
        this.numGuests = numGuests;
    }

    public int getNumGuests() {
        return numGuests;
    }

    public void setNumRooms(int numRooms) {
        this.numRooms = numRooms;
    }

    public int getNumRooms() {
        return numRooms;
    }

    public void setCabinPrice(double cabinPrice) {
        this.cabinPrice = cabinPrice;
    }

    public double getCabinPrice() {
        return cabinPrice;
    }

    public void setExtrasPrice(double extrasPrice) {
        this.extrasPrice = extrasPrice;
    }

    public double getExtrasPrice() {
        return extrasPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setHasBreakfast(boolean hasBreakfast) {
        this.hasBreakfast = hasBreakfast;
    }

    public boolean hasBreakfast() {
        return hasBreakfast;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public String getObservations() {
        return observations;
    }

    public void setCouponId(String couponId) {
        this.couponId = couponId;
    }

    public String getCouponId() {
        return couponId;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
