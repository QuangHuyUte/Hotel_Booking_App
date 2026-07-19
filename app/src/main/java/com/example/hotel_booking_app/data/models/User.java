package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("_id")
    private String id;

    private String fullName;
    private String email;
    private String password;
    private String phone;
    private String nationalId;
    private String dateOfBirth;
    private String gender;
    private String address;
    private String nationality;
    private String role;
    private String createdAt;
    private String updatedAt;
    private transient String authUserId;
    private transient String authAccessToken;
    private transient String authRefreshToken;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getRole() {
        return role;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthAccessToken(String authAccessToken) {
        this.authAccessToken = authAccessToken;
    }

    public String getAuthAccessToken() {
        return authAccessToken;
    }

    public void setAuthRefreshToken(String authRefreshToken) {
        this.authRefreshToken = authRefreshToken;
    }

    public String getAuthRefreshToken() {
        return authRefreshToken;
    }
}
