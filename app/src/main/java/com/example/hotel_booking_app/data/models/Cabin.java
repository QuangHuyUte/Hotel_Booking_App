package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

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
    private double latitude;
    private double longitude;
    private String mapPlaceId;
    private String address;
    private String district;
    private String propertyType;
    private int starRating;
    private double reviewScore;
    private int reviewCount;
    private String googleMapsUrl;
    private String amenities;
    private String hostId;
    private String createdAt;
    private String updatedAt;
    private transient List<RoomType> roomTypes = new ArrayList<>();
    private transient RoomType matchedRoomType;

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

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getMapPlaceId() {
        return mapPlaceId;
    }

    public String getAddress() {
        return address;
    }

    public String getDistrict() {
        return district;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public int getStarRating() {
        return starRating;
    }

    public double getReviewScore() {
        return reviewScore;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public String getGoogleMapsUrl() {
        return googleMapsUrl;
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

    public List<RoomType> getRoomTypes() {
        return roomTypes;
    }

    public void setRoomTypes(List<RoomType> roomTypes) {
        this.roomTypes = roomTypes == null ? new ArrayList<>() : roomTypes;
    }

    public RoomType getMatchedRoomType() {
        return matchedRoomType;
    }

    public void setMatchedRoomType(RoomType matchedRoomType) {
        this.matchedRoomType = matchedRoomType;
    }

    public Cabin copyForMatchedRoom(RoomType roomType) {
        Cabin copy = new Cabin();
        copy.id = id;
        copy.name = name;
        copy.maxCapacity = maxCapacity;
        copy.regularPrice = regularPrice;
        copy.discount = discount;
        copy.image = image;
        copy.description = description;
        copy.location = location;
        copy.latitude = latitude;
        copy.longitude = longitude;
        copy.mapPlaceId = mapPlaceId;
        copy.address = address;
        copy.district = district;
        copy.propertyType = propertyType;
        copy.starRating = starRating;
        copy.reviewScore = reviewScore;
        copy.reviewCount = reviewCount;
        copy.googleMapsUrl = googleMapsUrl;
        copy.amenities = amenities;
        copy.hostId = hostId;
        copy.createdAt = createdAt;
        copy.updatedAt = updatedAt;
        copy.roomTypes = roomTypes == null ? new ArrayList<>() : new ArrayList<>(roomTypes);
        copy.matchedRoomType = roomType;
        return copy;
    }

    public double displayPrice() {
        if (matchedRoomType != null && matchedRoomType.getBasePrice() > 0) {
            return matchedRoomType.getBasePrice();
        }
        return Math.max(0, regularPrice - discount);
    }
}
