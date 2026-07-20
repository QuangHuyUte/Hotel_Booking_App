package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.Wishlist;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.data.remote.SupabaseClient;
import com.example.hotel_booking_app.utils.AppConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WishlistService {
    private final SupabaseClient supabaseClient;

    public WishlistService() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public void getWishlist(String userId, SupabaseCallback<List<Wishlist>> callback) {
        if (!hasValue(userId)) {
            callback.onError("Vui lòng đăng nhập để xem danh sách đã lưu.");
            return;
        }
        Map<String, String> filters = new HashMap<>();
        filters.put("userId", userId);
        supabaseClient.getList(AppConstants.TABLE_WISHLISTS, "*", null, "createdAt.desc", filters, Wishlist[].class, callback);
    }

    public void addToWishlist(String userId, String cabinId, SupabaseCallback<Wishlist> callback) {
        if (!hasValue(userId) || !hasValue(cabinId)) {
            callback.onError("Missing user or cabin information to save favorite.");
            return;
        }
        supabaseClient.insert(AppConstants.TABLE_WISHLISTS, new Wishlist(userId, cabinId), Wishlist[].class, callback);
    }

    public void getWishlistForCabin(String cabinId, SupabaseCallback<List<Wishlist>> callback) {
        if (!hasValue(cabinId)) {
            callback.onError("Missing cabinId to count wishlist items.");
            return;
        }
        Map<String, String> filters = new HashMap<>();
        filters.put("cabinId", cabinId);
        supabaseClient.getList(AppConstants.TABLE_WISHLISTS, "*", null, "createdAt.desc", filters, Wishlist[].class, callback);
    }

    public void isFavorite(String userId, String cabinId, SupabaseCallback<Boolean> callback) {
        if (!hasValue(userId) || !hasValue(cabinId)) {
            callback.onSuccess(false);
            return;
        }
        Map<String, String> filters = new HashMap<>();
        filters.put("userId", userId);
        filters.put("cabinId", cabinId);
        supabaseClient.getList(AppConstants.TABLE_WISHLISTS, "*", 1, null, filters, Wishlist[].class, new SupabaseCallback<List<Wishlist>>() {
            @Override
            public void onSuccess(List<Wishlist> data) {
                callback.onSuccess(!data.isEmpty());
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void removeFromWishlist(String userId, String cabinId, SupabaseCallback<Boolean> callback) {
        if (!hasValue(userId) || !hasValue(cabinId)) {
            callback.onError("Missing user or cabin information to remove favorite.");
            return;
        }
        Map<String, String> filters = new HashMap<>();
        filters.put("userId", userId);
        filters.put("cabinId", cabinId);
        supabaseClient.delete(AppConstants.TABLE_WISHLISTS, filters, callback);
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
