package com.example.hotel_booking_app.data.remote;

public class SupabaseAuthSession {
    private final String userId;
    private final String email;
    private final String accessToken;
    private final String refreshToken;

    public SupabaseAuthSession(String userId, String email, String accessToken, String refreshToken) {
        this.userId = userId;
        this.email = email;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
