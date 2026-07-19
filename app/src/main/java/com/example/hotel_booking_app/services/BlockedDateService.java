package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.BlockedDate;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.data.remote.SupabaseClient;
import com.example.hotel_booking_app.utils.AppConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockedDateService {
    private final SupabaseClient supabaseClient;

    public BlockedDateService() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public void getBlockedDates(String cabinId, SupabaseCallback<List<BlockedDate>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("cabinId", cabinId);
        supabaseClient.getList(AppConstants.TABLE_BLOCKED_DATES, "*", null, "startDate.asc", filters, BlockedDate[].class, callback);
    }

    public void blockDates(String cabinId, String hostId, String startDate, String endDate, String reason, SupabaseCallback<BlockedDate> callback) {
        BlockedDate blockedDate = new BlockedDate();
        blockedDate.setCabinId(cabinId);
        blockedDate.setHostId(hostId);
        blockedDate.setStartDate(startDate);
        blockedDate.setEndDate(endDate);
        blockedDate.setReason(reason);
        supabaseClient.insert(AppConstants.TABLE_BLOCKED_DATES, blockedDate, BlockedDate[].class, callback);
    }

    public void deleteBlockedDate(String blockedDateId, SupabaseCallback<Boolean> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", blockedDateId);
        supabaseClient.delete(AppConstants.TABLE_BLOCKED_DATES, filters, callback);
    }
}
