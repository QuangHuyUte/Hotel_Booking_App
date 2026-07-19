package com.example.hotel_booking_app.data.models;

import com.google.gson.annotations.SerializedName;

public class Message {
    @SerializedName("_id")
    private String id;

    private String conversationId;
    private String senderId;
    private String message;
    private boolean isRead;
    private String createdAt;

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return isRead;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
