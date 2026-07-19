package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.Amenity;
import com.example.hotel_booking_app.data.models.CabinAmenity;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.data.remote.SupabaseClient;
import com.example.hotel_booking_app.utils.AppConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AmenityService {
    private final SupabaseClient supabaseClient;

    public AmenityService() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public void getAmenities(SupabaseCallback<List<Amenity>> callback) {
        supabaseClient.getList(AppConstants.TABLE_AMENITIES, "*", null, "name.asc", null, Amenity[].class, callback);
    }

    public void getCabinAmenities(String cabinId, SupabaseCallback<List<CabinAmenity>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("cabinId", cabinId);
        supabaseClient.getList(AppConstants.TABLE_CABIN_AMENITIES, "*", null, "createdAt.asc", filters, CabinAmenity[].class, callback);
    }

    public void addAmenityToCabin(String cabinId, String amenityId, SupabaseCallback<CabinAmenity> callback) {
        supabaseClient.insert(AppConstants.TABLE_CABIN_AMENITIES, new CabinAmenity(cabinId, amenityId), CabinAmenity[].class, callback);
    }

    public void removeAmenityFromCabin(String cabinId, String amenityId, SupabaseCallback<Boolean> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("cabinId", cabinId);
        filters.put("amenityId", amenityId);
        supabaseClient.delete(AppConstants.TABLE_CABIN_AMENITIES, filters, callback);
    }

    public void getAmenityNamesForCabin(String cabinId, SupabaseCallback<String> callback) {
        getCabinAmenities(cabinId, new SupabaseCallback<List<CabinAmenity>>() {
            @Override
            public void onSuccess(List<CabinAmenity> cabinAmenities) {
                if (cabinAmenities.isEmpty()) {
                    callback.onSuccess("");
                    return;
                }

                getAmenities(new SupabaseCallback<List<Amenity>>() {
                    @Override
                    public void onSuccess(List<Amenity> amenities) {
                        String names = amenities.stream()
                                .filter(amenity -> cabinAmenities.stream()
                                        .anyMatch(link -> amenity.getId().equals(link.getAmenityId())))
                                .map(Amenity::getName)
                                .collect(Collectors.joining(", "));
                        callback.onSuccess(names);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
