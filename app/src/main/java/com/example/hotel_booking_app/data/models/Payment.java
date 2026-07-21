package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class Payment {
    @SerializedName("_id")
    private String id;

    private String bookingId;
    private String userId;
    private double amount;
    private String method;
    private String provider;
    private String transactionId;
    private String status;
    private String paidAt;
    private String createdAt;
    private String updatedAt;

    public void setId(String id) {
        this.id = id;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPaidAt(String paidAt) {
        this.paidAt = paidAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getUserId() {
        return userId;
    }

    public double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getMethod() {
        return method;
    }

    public String getProvider() {
        return provider;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getPaidAt() {
        return paidAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
