package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.AppNotification;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.data.remote.SupabaseClient;
import com.example.hotel_booking_app.utils.AppConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationService {
    private final SupabaseClient supabaseClient;

    public NotificationService() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public void getNotifications(String userId, SupabaseCallback<List<AppNotification>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("userId", userId);
        supabaseClient.getList(AppConstants.TABLE_NOTIFICATIONS, "*", null, "createdAt.desc", filters, AppNotification[].class, callback);
    }

    public void createNotification(String userId, String title, String message, String type, SupabaseCallback<AppNotification> callback) {
        AppNotification notification = new AppNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        supabaseClient.insertNoReturn(AppConstants.TABLE_NOTIFICATIONS, notification, new SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                callback.onSuccess(notification);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void markAsRead(String notificationId, SupabaseCallback<AppNotification> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", notificationId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("isRead", true);
        supabaseClient.update(AppConstants.TABLE_NOTIFICATIONS, filters, payload, AppNotification[].class, callback);
    }
}
