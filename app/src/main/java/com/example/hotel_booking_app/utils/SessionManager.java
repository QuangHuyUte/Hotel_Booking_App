package com.example.hotel_booking_app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.hotel_booking_app.data.models.User;
import com.example.hotel_booking_app.data.remote.SupabaseAuthClient;
import com.example.hotel_booking_app.data.remote.SupabaseAuthSession;
import com.example.hotel_booking_app.data.remote.SupabaseClient;

import java.io.IOException;

public class SessionManager {
    private static final String PREF_NAME = "hotel_booking_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_AUTH_USER_ID = "auth_user_id";
    private static final String KEY_AUTH_ACCESS_TOKEN = "auth_access_token";
    private static final String KEY_AUTH_REFRESH_TOKEN = "auth_refresh_token";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SupabaseClient.getInstance().setAccessToken(getAuthAccessToken());
        SupabaseClient.getInstance().setTokenRefresher(this::refreshAccessTokenBlocking);
    }

    public void saveUser(User user) {
        preferences.edit()
                .putString(KEY_USER_ID, user.getId())
                .putString(KEY_FULL_NAME, user.getFullName())
                .putString(KEY_EMAIL, user.getEmail())
                .putString(KEY_ROLE, user.getRole())
                .putString(KEY_AUTH_USER_ID, user.getAuthUserId())
                .putString(KEY_AUTH_ACCESS_TOKEN, user.getAuthAccessToken())
                .putString(KEY_AUTH_REFRESH_TOKEN, user.getAuthRefreshToken())
                .apply();
        SupabaseClient.getInstance().setAccessToken(user.getAuthAccessToken());
    }

    public boolean isLoggedIn() {
        return getUserId() != null && hasAuthSession();
    }

    public void logout() {
        preferences.edit().clear().apply();
        SupabaseClient.getInstance().setAccessToken(null);
    }

    public String getUserId() {
        return preferences.getString(KEY_USER_ID, null);
    }

    public String getFullName() {
        return preferences.getString(KEY_FULL_NAME, "");
    }

    public String getEmail() {
        return preferences.getString(KEY_EMAIL, "");
    }

    public String getRole() {
        return preferences.getString(KEY_ROLE, AppConstants.ROLE_CUSTOMER);
    }

    public String getAuthUserId() {
        return preferences.getString(KEY_AUTH_USER_ID, null);
    }

    public String getAuthAccessToken() {
        return preferences.getString(KEY_AUTH_ACCESS_TOKEN, null);
    }

    public String getAuthRefreshToken() {
        return preferences.getString(KEY_AUTH_REFRESH_TOKEN, null);
    }

    public boolean hasAuthSession() {
        return getAuthUserId() != null
                && !getAuthUserId().trim().isEmpty()
                && getAuthAccessToken() != null
                && !getAuthAccessToken().trim().isEmpty();
    }

    private String refreshAccessTokenBlocking() throws IOException {
        String refreshToken = getAuthRefreshToken();
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IOException("Phiên đăng nhập đã hết hạn. Vui lòng đăng xuất rồi đăng nhập lại.");
        }

        SupabaseAuthSession session = new SupabaseAuthClient().refreshSessionBlocking(refreshToken);
        preferences.edit()
                .putString(KEY_AUTH_USER_ID, session.getUserId())
                .putString(KEY_AUTH_ACCESS_TOKEN, session.getAccessToken())
                .putString(KEY_AUTH_REFRESH_TOKEN, session.getRefreshToken())
                .apply();
        SupabaseClient.getInstance().setAccessToken(session.getAccessToken());
        return session.getAccessToken();
    }

    public boolean isHostOrAdmin() {
        String role = getRole();
        return AppConstants.ROLE_HOST.equals(role) || AppConstants.ROLE_ADMIN.equals(role);
    }
}
