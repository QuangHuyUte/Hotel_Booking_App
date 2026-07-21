package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.CabinImage;
import com.example.hotel_booking_app.data.models.Rate;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.data.remote.SupabaseClient;
import com.example.hotel_booking_app.utils.AppConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CabinService {
    private final SupabaseClient supabaseClient;

    public CabinService() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public void getCabins(SupabaseCallback<List<Cabin>> callback) {
        supabaseClient.getList(AppConstants.TABLE_CABINS, "*", null, "createdAt.desc", null, Cabin[].class, activeCabinsCallback(callback));
    }

    public void getCabinsForHost(String hostId, SupabaseCallback<List<Cabin>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("hostId", hostId);
        supabaseClient.getList(AppConstants.TABLE_CABINS, "*", null, "createdAt.desc", filters, Cabin[].class, activeCabinsCallback(callback));
    }

    public void searchCabins(String locationKeyword, int guests, SupabaseCallback<List<Cabin>> callback) {
        searchCabins(locationKeyword, guests, "", callback);
    }

    public void searchCabins(String locationKeyword, int guests, String amenityKeyword, SupabaseCallback<List<Cabin>> callback) {
        getCabins(new SupabaseCallback<List<Cabin>>() {
            @Override
            public void onSuccess(List<Cabin> cabins) {
                String keyword = locationKeyword == null ? "" : locationKeyword.trim().toLowerCase(Locale.US);
                String amenity = amenityKeyword == null ? "" : amenityKeyword.trim().toLowerCase(Locale.US);
                List<Cabin> result = cabins.stream()
                        .filter(cabin -> guests <= 0 || cabin.getMaxCapacity() >= guests)
                        .filter(cabin -> keyword.isEmpty()
                                || safe(cabin.getLocation()).toLowerCase(Locale.US).contains(keyword)
                                || safe(cabin.getName()).toLowerCase(Locale.US).contains(keyword))
                        .filter(cabin -> amenity.isEmpty()
                                || safe(cabin.getAmenities()).toLowerCase(Locale.US).contains(amenity))
                        .collect(Collectors.toList());
                callback.onSuccess(result);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void getCabinById(String cabinId, SupabaseCallback<Cabin> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", cabinId);
        supabaseClient.getSingle(AppConstants.TABLE_CABINS, filters, Cabin[].class, callback);
    }

    public void getImages(String cabinId, SupabaseCallback<List<CabinImage>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("cabinId", cabinId);
        supabaseClient.getList(AppConstants.TABLE_IMAGES, "*", null, "createdAt.asc", filters, CabinImage[].class, callback);
    }

    public void getRates(String cabinId, SupabaseCallback<List<Rate>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("cabinId", cabinId);
        supabaseClient.getList(AppConstants.TABLE_RATES, "*", null, "createdAt.desc", filters, Rate[].class, callback);
    }

    public void getRatesForBooking(String bookingId, SupabaseCallback<List<Rate>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("bookingId", bookingId);
        supabaseClient.getList(AppConstants.TABLE_RATES, "*", null, "createdAt.desc", filters, Rate[].class, callback);
    }

    public void createRate(Rate rate, SupabaseCallback<Rate> callback) {
        supabaseClient.insert(AppConstants.TABLE_RATES, rate, Rate[].class, callback);
    }

    public void createCabin(Cabin cabin, SupabaseCallback<Cabin> callback) {
        cabin.setActive(true);
        supabaseClient.insert(AppConstants.TABLE_CABINS, cabin, Cabin[].class, callback);
    }

    public void updateCabin(Cabin cabin, SupabaseCallback<Cabin> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", cabin.getId());
        supabaseClient.update(AppConstants.TABLE_CABINS, filters, cabin, Cabin[].class, callback);
    }

    public void deleteCabin(String cabinId, SupabaseCallback<Boolean> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", cabinId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("isActive", false);
        supabaseClient.updateNoReturn(AppConstants.TABLE_CABINS, filters, payload, callback);
    }

    public void testConnection(SupabaseCallback<List<Cabin>> callback) {
        supabaseClient.getList(AppConstants.TABLE_CABINS, "*", 1, Cabin[].class, callback);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private SupabaseCallback<List<Cabin>> activeCabinsCallback(SupabaseCallback<List<Cabin>> callback) {
        return new SupabaseCallback<List<Cabin>>() {
            @Override
            public void onSuccess(List<Cabin> cabins) {
                List<Cabin> active = cabins == null
                        ? java.util.Collections.emptyList()
                        : cabins.stream().filter(Cabin::isActive).collect(Collectors.toList());
                callback.onSuccess(active);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        };
    }
}
