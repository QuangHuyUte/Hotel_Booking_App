package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class Otp {
    @SerializedName("_id")
    private String id;

    private String email;
    private String otp;
    private String expiresAt;
    private String userId;
    private String createdAt;

    public String getEmail() {
        return email;
    }

    public String getOtp() {
        return otp;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public String getUserId() {
        return userId;
    }
}
