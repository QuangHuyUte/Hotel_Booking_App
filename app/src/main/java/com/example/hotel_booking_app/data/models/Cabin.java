package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class Cabin {
    @SerializedName("_id")
    private String id;

    private String name;
    private int maxCapacity;
    private double regularPrice;
    private double discount;
    private String image;
    private String description;
    private String location;
    private String amenities;
    private String hostId;
    private String createdAt;
    private String updatedAt;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setRegularPrice(double regularPrice) {
        this.regularPrice = regularPrice;
    }

    public double getRegularPrice() {
        return regularPrice;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getDiscount() {
        return discount;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImage() {
        return image;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    public String getAmenities() {
        return amenities;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getHostId() {
        return hostId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
