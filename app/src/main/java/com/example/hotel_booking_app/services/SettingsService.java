package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.Setting;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.data.remote.SupabaseClient;
import com.example.hotel_booking_app.utils.AppConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsService {
    private final SupabaseClient supabaseClient;

    public SettingsService() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public void getSettings(SupabaseCallback<Setting> callback) {
        supabaseClient.getList(AppConstants.TABLE_SETTINGS, "*", 1, "createdAt.asc", null, Setting[].class, new SupabaseCallback<List<Setting>>() {
            @Override
            public void onSuccess(List<Setting> data) {
                if (data == null || data.isEmpty()) {
                    callback.onSuccess(defaultSettings());
                } else {
                    callback.onSuccess(data.get(0));
                }
            }

            @Override
            public void onError(String message) {
                callback.onSuccess(defaultSettings());
            }
        });
    }

    public void saveSettings(Setting setting, SupabaseCallback<Setting> callback) {
        if (setting.getId() == null || setting.getId().trim().isEmpty()) {
            supabaseClient.insert(AppConstants.TABLE_SETTINGS, setting, Setting[].class, callback);
            return;
        }

        Map<String, String> filters = new HashMap<>();
        filters.put("_id", setting.getId());
        supabaseClient.update(AppConstants.TABLE_SETTINGS, filters, setting, Setting[].class, callback);
    }

    private Setting defaultSettings() {
        Setting setting = new Setting();
        setting.setMiniBookingLength(1);
        setting.setMaxBookingLength(90);
        setting.setMaxNumberOfGuests(10);
        setting.setBreakfastPrice(20);
        return setting;
    }
}
