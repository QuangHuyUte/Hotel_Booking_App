package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class AppNotification {
    @SerializedName("_id")
    private String id;

    private String userId;
    private String title;
    private String message;
    private String type;
    private boolean isRead;
    private Object data;
    private String createdAt;

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return isRead;
    }
}
