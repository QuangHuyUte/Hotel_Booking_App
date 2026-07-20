package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.data.remote.SupabaseClient;
import com.example.hotel_booking_app.utils.AppConstants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RoomTypeService {
    private final SupabaseClient supabaseClient;

    public RoomTypeService() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public void getRoomTypesForCabin(String cabinId, SupabaseCallback<List<RoomType>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("cabinId", cabinId);
        supabaseClient.getList(AppConstants.TABLE_ROOM_TYPES, "*", null, "basePrice.asc", filters, RoomType[].class, new SupabaseCallback<List<RoomType>>() {
            @Override
            public void onSuccess(List<RoomType> roomTypes) {
                List<RoomType> active = activeRoomTypesForCabin(roomTypes, cabinId);
                if (active.isEmpty()) {
                    loadRoomTypesForCabinFromFullTable(cabinId, callback);
                    return;
                }
                callback.onSuccess(active);
            }

            @Override
            public void onError(String message) {
                loadRoomTypesForCabinFromFullTable(cabinId, callback);
            }
        });
    }

    public void getRoomTypeById(String roomTypeId, SupabaseCallback<RoomType> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", roomTypeId);
        supabaseClient.getSingle(AppConstants.TABLE_ROOM_TYPES, filters, RoomType[].class, callback);
    }

    public void attachRoomTypes(List<Cabin> cabins, SupabaseCallback<List<Cabin>> callback) {
        if (cabins == null || cabins.isEmpty()) {
            callback.onSuccess(cabins == null ? new ArrayList<>() : cabins);
            return;
        }
        Set<String> cabinIds = new LinkedHashSet<>();
        for (Cabin cabin : cabins) {
            cabin.setRoomTypes(new ArrayList<>());
            if (cabin.getId() != null && !cabin.getId().trim().isEmpty()) {
                cabinIds.add(cabin.getId());
            }
        }
        if (cabinIds.isEmpty()) {
            callback.onSuccess(cabins);
            return;
        }

        supabaseClient.getListIn(AppConstants.TABLE_ROOM_TYPES, "*", "basePrice.asc", "cabinId",
                cabinIds, RoomType[].class, new SupabaseCallback<List<RoomType>>() {
                    @Override
                    public void onSuccess(List<RoomType> roomTypes) {
                        if ((roomTypes == null || roomTypes.isEmpty()) && !cabins.isEmpty()) {
                            attachRoomTypesOneByOne(cabins, callback);
                            return;
                        }
                        Map<String, List<RoomType>> byCabinId = new HashMap<>();
                        for (RoomType roomType : roomTypes) {
                            if (!roomType.isActive()) {
                                continue;
                            }
                            String cabinId = roomType.getCabinId();
                            if (cabinId == null || cabinId.trim().isEmpty()) {
                                continue;
                            }
                            List<RoomType> grouped = byCabinId.get(cabinId);
                            if (grouped == null) {
                                grouped = new ArrayList<>();
                                byCabinId.put(cabinId, grouped);
                            }
                            grouped.add(roomType);
                        }
                        int assignedCount = 0;
                        for (Cabin cabin : cabins) {
                            List<RoomType> grouped = byCabinId.get(cabin.getId());
                            if (grouped == null) {
                                grouped = new ArrayList<>();
                            } else {
                                grouped.sort(Comparator.comparingDouble(RoomType::getBasePrice));
                            }
                            assignedCount += grouped.size();
                            cabin.setRoomTypes(grouped);
                        }
                        if (assignedCount == 0 && !cabins.isEmpty()) {
                            attachRoomTypesFromFullTable(cabins, callback);
                            return;
                        }
                        callback.onSuccess(cabins);
                    }

                    @Override
                    public void onError(String message) {
                        attachRoomTypesFromFullTable(cabins, callback);
                    }
                });
    }

    private void loadRoomTypesForCabinFromFullTable(String cabinId, SupabaseCallback<List<RoomType>> callback) {
        supabaseClient.getList(AppConstants.TABLE_ROOM_TYPES, "*", null, "basePrice.asc", null, RoomType[].class, new SupabaseCallback<List<RoomType>>() {
            @Override
            public void onSuccess(List<RoomType> roomTypes) {
                callback.onSuccess(activeRoomTypesForCabin(roomTypes, cabinId));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void attachRoomTypesFromFullTable(List<Cabin> cabins, SupabaseCallback<List<Cabin>> callback) {
        supabaseClient.getList(AppConstants.TABLE_ROOM_TYPES, "*", null, "basePrice.asc", null, RoomType[].class, new SupabaseCallback<List<RoomType>>() {
            @Override
            public void onSuccess(List<RoomType> roomTypes) {
                Map<String, List<RoomType>> byCabinId = new HashMap<>();
                for (Cabin cabin : cabins) {
                    byCabinId.put(cabin.getId(), new ArrayList<>());
                }
                for (RoomType roomType : roomTypes) {
                    if (roomType == null || !roomType.isActive()) {
                        continue;
                    }
                    List<RoomType> grouped = byCabinId.get(roomType.getCabinId());
                    if (grouped != null) {
                        grouped.add(roomType);
                    }
                }
                for (Cabin cabin : cabins) {
                    List<RoomType> grouped = byCabinId.get(cabin.getId());
                    if (grouped == null) {
                        grouped = new ArrayList<>();
                    }
                    grouped.sort(Comparator.comparingDouble(RoomType::getBasePrice));
                    cabin.setRoomTypes(grouped);
                }
                callback.onSuccess(cabins);
            }

            @Override
            public void onError(String message) {
                attachRoomTypesOneByOne(cabins, callback);
            }
        });
    }

    private void attachRoomTypesOneByOne(List<Cabin> cabins, SupabaseCallback<List<Cabin>> callback) {
        final int[] completed = {0};
        for (Cabin cabin : cabins) {
            getRoomTypesForCabin(cabin.getId(), new SupabaseCallback<List<RoomType>>() {
                @Override
                public void onSuccess(List<RoomType> roomTypes) {
                    cabin.setRoomTypes(roomTypes);
                    complete(cabins, completed, callback);
                }

                @Override
                public void onError(String message) {
                    cabin.setRoomTypes(new ArrayList<>());
                    complete(cabins, completed, callback);
                }
            });
        }
    }

    public RoomType findBestRoomType(Cabin cabin, int guests, String sizeOrCategoryQuery) {
        return findBestRoomType(cabin, guests, 0, sizeOrCategoryQuery);
    }

    public RoomType findBestRoomType(Cabin cabin, int guests, int requestedBeds, String sizeOrCategoryQuery) {
        if (cabin == null || cabin.getRoomTypes() == null || cabin.getRoomTypes().isEmpty()) {
            return null;
        }
        String normalizedQuery = normalize(sizeOrCategoryQuery);
        RoomType best = null;
        for (RoomType roomType : cabin.getRoomTypes()) {
            if (!roomType.isActive()) {
                continue;
            }
            if (!fitsGuestsAndBeds(roomType, guests, requestedBeds)) {
                continue;
            }
            if (!matchesSizeOrCategory(roomType, normalizedQuery)) {
                continue;
            }
            if (best == null || roomType.getBasePrice() < best.getBasePrice()) {
                best = roomType;
            }
        }
        return best;
    }

    public boolean fitsGuestsAndBeds(RoomType roomType, int guests, int requestedBeds) {
        if (roomType == null) {
            return false;
        }
        if (guests > 0
                && (roomType.effectiveMaxAdults() < guests || roomType.effectiveSleepingCapacity() < guests)) {
            return false;
        }
        if (guests > 0 && !roomType.fitsRoomSizeForGuests(guests)) {
            return false;
        }
        return requestedBeds <= 0 || roomType.effectiveBedCount() >= requestedBeds;
    }

    public void createRoomType(RoomType roomType, SupabaseCallback<RoomType> callback) {
        supabaseClient.insert(AppConstants.TABLE_ROOM_TYPES, roomType, RoomType[].class, callback);
    }

    public void updateRoomType(RoomType roomType, SupabaseCallback<RoomType> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", roomType.getId());
        supabaseClient.update(AppConstants.TABLE_ROOM_TYPES, filters, roomType, RoomType[].class, callback);
    }

    public void deleteRoomType(String roomTypeId, SupabaseCallback<Boolean> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", roomTypeId);
        supabaseClient.delete(AppConstants.TABLE_ROOM_TYPES, filters, callback);
    }

    private void complete(List<Cabin> cabins, int[] completed, SupabaseCallback<List<Cabin>> callback) {
        completed[0]++;
        if (completed[0] >= cabins.size()) {
            callback.onSuccess(cabins);
        }
    }

    private List<RoomType> activeRoomTypesForCabin(List<RoomType> roomTypes, String cabinId) {
        List<RoomType> active = new ArrayList<>();
        if (roomTypes == null) {
            return active;
        }
        for (RoomType roomType : roomTypes) {
            if (roomType == null || !roomType.isActive()) {
                continue;
            }
            if (cabinId == null || cabinId.equals(roomType.getCabinId())) {
                active.add(roomType);
            }
        }
        active.sort(Comparator.comparingDouble(RoomType::getBasePrice));
        return active;
    }

    private boolean matchesSizeOrCategory(RoomType roomType, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String category = normalize(roomType.getCategory() + " " + roomType.getName());
        if (category.contains(query) || query.contains(category)) {
            return true;
        }
        if (query.contains("standard") && category.contains("standard")) {
            return true;
        }
        if (query.contains("superior") && category.contains("superior")) {
            return true;
        }
        if (query.contains("deluxe") && category.contains("deluxe")) {
            return true;
        }
        if (query.contains("suite") && category.contains("suite")) {
            return true;
        }
        int requestedSize = firstNumber(query);
        if (requestedSize <= 0 || roomType.getSizeM2() <= 0) {
            return true;
        }
        return Math.abs(roomType.getSizeM2() - requestedSize) <= 8 || roomType.getSizeM2() >= requestedSize;
    }

    private int firstNumber(String value) {
        String digits = value.replaceAll("[^0-9]", " ").trim();
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits.split("\\s+")[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US).trim();
    }
}
