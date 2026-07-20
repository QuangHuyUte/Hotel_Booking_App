package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.BookingPolicy;
import com.example.hotel_booking_app.data.models.BlockedDate;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.Coupon;
import com.example.hotel_booking_app.data.models.RoomInventory;
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.models.Setting;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.data.remote.SupabaseClient;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.DateUtils;
import com.example.hotel_booking_app.utils.PriceUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BookingService {
    private final SupabaseClient supabaseClient;
    private final SettingsService settingsService;

    public BookingService() {
        supabaseClient = SupabaseClient.getInstance();
        settingsService = new SettingsService();
    }

    public void getBookingsForUser(String userId, SupabaseCallback<List<Booking>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("userId", userId);
        supabaseClient.getList(AppConstants.TABLE_BOOKINGS, "*", null, "createdAt.desc", filters, Booking[].class, callback);
    }

    public void getAllBookings(SupabaseCallback<List<Booking>> callback) {
        supabaseClient.getList(AppConstants.TABLE_BOOKINGS, "*", null, "createdAt.desc", null, Booking[].class, callback);
    }

    public void getActiveBookingsForCabin(String cabinId, SupabaseCallback<List<Booking>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("cabinId", cabinId);
        supabaseClient.getList(AppConstants.TABLE_BOOKINGS, "*", null, "startDate.asc", filters, Booking[].class, new SupabaseCallback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                callback.onSuccess(filterActiveFutureBookings(bookings));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void getBookingsForCabin(String cabinId, SupabaseCallback<List<Booking>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("cabinId", cabinId);
        supabaseClient.getList(AppConstants.TABLE_BOOKINGS, "*", null, "createdAt.desc", filters, Booking[].class, callback);
    }

    public void getAvailabilitySummary(String cabinId, SupabaseCallback<String> callback) {
        loadUnavailableRanges(cabinId, new SupabaseCallback<List<UnavailableRange>>() {
            @Override
            public void onSuccess(List<UnavailableRange> ranges) {
                callback.onSuccess(buildAvailabilitySummary(ranges));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void ensureDateIsAvailable(String cabinId, String date, SupabaseCallback<Boolean> callback) {
        loadUnavailableRanges(cabinId, new SupabaseCallback<List<UnavailableRange>>() {
            @Override
            public void onSuccess(List<UnavailableRange> ranges) {
                LocalDate chosenDate = LocalDate.parse(date);
                for (UnavailableRange range : ranges) {
                    boolean blocked = !chosenDate.isBefore(range.getStartDate()) && chosenDate.isBefore(range.getEndDate());
                    if (blocked) {
                        callback.onError(buildConflictMessage(range, ranges));
                        return;
                    }
                }
                callback.onSuccess(true);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void ensureRangeIsAvailable(String cabinId, String startDate, String endDate, SupabaseCallback<Boolean> callback) {
        checkAvailability(cabinId, null, startDate, endDate, new SupabaseCallback<AvailabilityResult>() {
            @Override
            public void onSuccess(AvailabilityResult result) {
                if (result.isAvailable()) {
                    callback.onSuccess(true);
                } else {
                    callback.onError(result.getMessage());
                }
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void ensureRangeIsAvailable(String cabinId, String roomTypeId, String startDate, String endDate, SupabaseCallback<Boolean> callback) {
        ensureRangeIsAvailable(cabinId, roomTypeId, startDate, endDate, 1, callback);
    }

    public void ensureRangeIsAvailable(String cabinId, String roomTypeId, String startDate, String endDate, int rooms, SupabaseCallback<Boolean> callback) {
        checkAvailability(cabinId, roomTypeId, startDate, endDate, Math.max(1, rooms), new SupabaseCallback<AvailabilityResult>() {
            @Override
            public void onSuccess(AvailabilityResult result) {
                if (result.isAvailable()) {
                    callback.onSuccess(true);
                } else {
                    callback.onError(result.getMessage());
                }
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void getRoomTypeAvailability(
            String cabinId,
            String roomTypeId,
            String startDate,
            String endDate,
            int rooms,
            SupabaseCallback<AvailabilityStatus> callback
    ) {
        checkAvailability(cabinId, roomTypeId, startDate, endDate, Math.max(1, rooms),
                new SupabaseCallback<AvailabilityResult>() {
                    @Override
                    public void onSuccess(AvailabilityResult result) {
                        callback.onSuccess(new AvailabilityStatus(result.isAvailable(), result.getMessage()));
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
    }

    public void getCabinAvailability(
            String cabinId,
            String startDate,
            String endDate,
            SupabaseCallback<AvailabilityStatus> callback
    ) {
        checkAvailability(cabinId, null, startDate, endDate, 1, new SupabaseCallback<AvailabilityResult>() {
            @Override
            public void onSuccess(AvailabilityResult result) {
                callback.onSuccess(new AvailabilityStatus(result.isAvailable(), result.getMessage()));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void createBooking(
            Cabin cabin,
            String userId,
            String startDate,
            String endDate,
            int guests,
            boolean hasBreakfast,
            String observations,
            SupabaseCallback<Booking> callback
    ) {
        createBooking(cabin, userId, startDate, endDate, guests, hasBreakfast, observations, null, 0, callback);
    }

    public void createBooking(
            Cabin cabin,
            RoomType roomType,
            String userId,
            String startDate,
            String endDate,
            int guests,
            int rooms,
            boolean hasBreakfast,
            String observations,
            Coupon coupon,
            double discountAmount,
            SupabaseCallback<Booking> callback
    ) {
        if (roomType == null) {
            createBooking(cabin, userId, startDate, endDate, guests, hasBreakfast, observations, coupon, discountAmount, callback);
            return;
        }
        if (!DateUtils.isDateRangeValid(startDate, endDate)) {
            callback.onError("Ngày trả phòng phải sau ngày nhận phòng.");
            return;
        }
        if (guests <= 0
                || guests > roomType.effectiveMaxAdults()
                || guests > roomType.effectiveSleepingCapacity()) {
            callback.onError("Số khách không phù hợp với loại phòng này.");
            return;
        }
        if (!roomType.fitsRoomSizeForGuests(guests)) {
            callback.onError("Diện tích phòng chưa đủ cho số khách này.");
            return;
        }

        loadPolicy(cabin.getId(), new SupabaseCallback<BookingPolicy>() {
            @Override
            public void onSuccess(BookingPolicy policy) {
                continueCreateBookingWithRules(cabin, roomType, userId, startDate, endDate, guests, Math.max(1, rooms),
                        hasBreakfast, observations, coupon, discountAmount,
                        policy.getMiniBookingLength(), policy.getMaxBookingLength(), policy.getBreakfastPrice(), callback);
            }

            @Override
            public void onError(String message) {
                settingsService.getSettings(new SupabaseCallback<Setting>() {
                    @Override
                    public void onSuccess(Setting setting) {
                        continueCreateBookingWithRules(cabin, roomType, userId, startDate, endDate, guests, Math.max(1, rooms),
                                hasBreakfast, observations, coupon, discountAmount,
                                setting.getMiniBookingLength(), setting.getMaxBookingLength(), setting.getBreakfastPrice(), callback);
                    }

                    @Override
                    public void onError(String settingsMessage) {
                        continueCreateBookingWithRules(cabin, roomType, userId, startDate, endDate, guests, Math.max(1, rooms),
                                hasBreakfast, observations, coupon, discountAmount, 1, 30, 15, callback);
                    }
                });
            }
        });
    }

    public void createBooking(
            Cabin cabin,
            String userId,
            String startDate,
            String endDate,
            int guests,
            boolean hasBreakfast,
            String observations,
            Coupon coupon,
            double discountAmount,
            SupabaseCallback<Booking> callback
    ) {
        if (!DateUtils.isDateRangeValid(startDate, endDate)) {
            callback.onError("Ngày trả phòng phải sau ngày nhận phòng.");
            return;
        }
        if (guests <= 0 || guests > cabin.getMaxCapacity()) {
            callback.onError("Số khách vượt quá sức chứa của khách sạn.");
            return;
        }

        loadPolicy(cabin.getId(), new SupabaseCallback<BookingPolicy>() {
            @Override
            public void onSuccess(BookingPolicy policy) {
                continueCreateBooking(cabin, userId, startDate, endDate, guests, hasBreakfast, observations, coupon, discountAmount, policy, callback);
            }

            @Override
            public void onError(String message) {
                settingsService.getSettings(new SupabaseCallback<Setting>() {
                    @Override
                    public void onSuccess(Setting setting) {
                        continueCreateBooking(cabin, userId, startDate, endDate, guests, hasBreakfast, observations, coupon, discountAmount, setting, callback);
                    }

                    @Override
                    public void onError(String settingsMessage) {
                        continueCreateBooking(cabin, userId, startDate, endDate, guests, hasBreakfast, observations, coupon, discountAmount, (Setting) null, callback);
                    }
                });
            }
        });
    }

    public void getBookingById(String bookingId, SupabaseCallback<Booking> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", bookingId);
        supabaseClient.getSingle(AppConstants.TABLE_BOOKINGS, filters, Booking[].class, callback);
    }

    public void cancelBooking(String bookingId, SupabaseCallback<Booking> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", bookingId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("status", AppConstants.BOOKING_CANCELLED);
        payload.put("isPaid", false);
        supabaseClient.update(AppConstants.TABLE_BOOKINGS, filters, payload, Booking[].class, callback);
    }

    public void updateStatus(String bookingId, String status, boolean isPaid, SupabaseCallback<Booking> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", bookingId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("status", status);
        payload.put("isPaid", isPaid);
        supabaseClient.update(AppConstants.TABLE_BOOKINGS, filters, payload, Booking[].class, callback);
    }

    public void updateStatusNoReturn(String bookingId, String status, boolean isPaid, SupabaseCallback<Boolean> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", bookingId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("status", status);
        payload.put("isPaid", isPaid);
        supabaseClient.updateNoReturn(AppConstants.TABLE_BOOKINGS, filters, payload, callback);
    }

    public void updateBookingDetails(
            String bookingId,
            int guests,
            boolean hasBreakfast,
            String observations,
            double extrasPrice,
            double totalPrice,
            SupabaseCallback<Booking> callback
    ) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", bookingId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numGuests", guests);
        payload.put("hasBreakfast", hasBreakfast);
        payload.put("observations", observations);
        payload.put("extrasPrice", extrasPrice);
        payload.put("totalPrice", totalPrice);
        supabaseClient.update(AppConstants.TABLE_BOOKINGS, filters, payload, Booking[].class, callback);
    }

    private void continueCreateBooking(
            Cabin cabin,
            String userId,
            String startDate,
            String endDate,
            int guests,
            boolean hasBreakfast,
            String observations,
            Coupon coupon,
            double discountAmount,
            BookingPolicy policy,
            SupabaseCallback<Booking> callback
    ) {
        int minNights = policy != null ? policy.getMiniBookingLength() : 1;
        int maxNights = policy != null ? policy.getMaxBookingLength() : 30;
        double breakfastPrice = policy != null ? policy.getBreakfastPrice() : 15;
        continueCreateBookingWithRules(cabin, userId, startDate, endDate, guests, hasBreakfast, observations,
                coupon, discountAmount, minNights, maxNights, breakfastPrice, callback);
    }

    private void continueCreateBooking(
            Cabin cabin,
            String userId,
            String startDate,
            String endDate,
            int guests,
            boolean hasBreakfast,
            String observations,
            Coupon coupon,
            double discountAmount,
            Setting setting,
            SupabaseCallback<Booking> callback
    ) {
        int minNights = setting != null ? setting.getMiniBookingLength() : 1;
        int maxNights = setting != null ? setting.getMaxBookingLength() : 30;
        double breakfastPrice = setting != null ? setting.getBreakfastPrice() : 15;
        continueCreateBookingWithRules(cabin, userId, startDate, endDate, guests, hasBreakfast, observations,
                coupon, discountAmount, minNights, maxNights, breakfastPrice, callback);
    }

    private void continueCreateBookingWithRules(
            Cabin cabin,
            String userId,
            String startDate,
            String endDate,
            int guests,
            boolean hasBreakfast,
            String observations,
            Coupon coupon,
            double discountAmount,
            int minNights,
            int maxNights,
            double breakfastPrice,
            SupabaseCallback<Booking> callback
    ) {
        int nights = DateUtils.nightsBetween(startDate, endDate);
        if (nights < minNights || nights > maxNights) {
            callback.onError("Số đêm phải từ " + minNights + " đến " + maxNights + ".");
            return;
        }

        checkAvailability(cabin.getId(), null, startDate, endDate, new SupabaseCallback<AvailabilityResult>() {
            @Override
            public void onSuccess(AvailabilityResult result) {
                if (!result.isAvailable()) {
                    callback.onError(result.getMessage());
                    return;
                }

                double nightlyPrice = PriceUtils.priceAfterDiscount(cabin.getRegularPrice(), cabin.getDiscount());
                double cabinPrice = nightlyPrice * nights;
                double extrasPrice = hasBreakfast ? breakfastPrice * guests * nights : 0;
                double safeDiscount = Math.max(0, Math.min(discountAmount, cabinPrice + extrasPrice));

                Booking booking = new Booking();
                booking.setUserId(userId);
                booking.setCabinId(cabin.getId());
                booking.setStartDate(startDate);
                booking.setEndDate(endDate);
                booking.setNumNights(nights);
                booking.setNumGuests(guests);
                booking.setCabinPrice(cabinPrice);
                booking.setExtrasPrice(extrasPrice);
                booking.setDiscountAmount(safeDiscount);
                booking.setCouponId(coupon != null ? coupon.getId() : null);
                booking.setTotalPrice(cabinPrice + extrasPrice - safeDiscount);
                booking.setStatus(AppConstants.BOOKING_PENDING);
                booking.setHasBreakfast(hasBreakfast);
                booking.setPaid(false);
                booking.setObservations(observations);
                supabaseClient.insert(AppConstants.TABLE_BOOKINGS, booking, Booking[].class, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void continueCreateBookingWithRules(
            Cabin cabin,
            RoomType roomType,
            String userId,
            String startDate,
            String endDate,
            int guests,
            int rooms,
            boolean hasBreakfast,
            String observations,
            Coupon coupon,
            double discountAmount,
            int minNights,
            int maxNights,
            double breakfastPrice,
            SupabaseCallback<Booking> callback
    ) {
        int nights = DateUtils.nightsBetween(startDate, endDate);
        if (nights < minNights || nights > maxNights) {
            callback.onError("Số đêm phải từ " + minNights + " đến " + maxNights + ".");
            return;
        }
        if (rooms > roomType.getTotalRooms()) {
            callback.onError("Số phòng yêu cầu vượt quá tổng số phòng của loại này.");
            return;
        }

        checkAvailability(cabin.getId(), roomType.getId(), startDate, endDate, Math.max(1, rooms), new SupabaseCallback<AvailabilityResult>() {
            @Override
            public void onSuccess(AvailabilityResult result) {
                if (!result.isAvailable()) {
                    callback.onError(result.getMessage());
                    return;
                }

                double roomPrice = roomType.getBasePrice() * Math.max(1, rooms) * nights;
                double extrasPrice = hasBreakfast ? breakfastPrice * guests * nights : 0;
                double safeDiscount = Math.max(0, Math.min(discountAmount, roomPrice + extrasPrice));

                Booking booking = new Booking();
                booking.setUserId(userId);
                booking.setCabinId(cabin.getId());
                booking.setRoomTypeId(roomType.getId());
                booking.setNumRooms(Math.max(1, rooms));
                booking.setStartDate(startDate);
                booking.setEndDate(endDate);
                booking.setNumNights(nights);
                booking.setNumGuests(guests);
                booking.setCabinPrice(roomPrice);
                booking.setExtrasPrice(extrasPrice);
                booking.setDiscountAmount(safeDiscount);
                booking.setCouponId(coupon != null ? coupon.getId() : null);
                booking.setTotalPrice(roomPrice + extrasPrice - safeDiscount);
                booking.setStatus(AppConstants.BOOKING_PENDING);
                booking.setHasBreakfast(hasBreakfast);
                booking.setPaid(false);
                booking.setObservations(observations);
                supabaseClient.insert(AppConstants.TABLE_BOOKINGS, booking, Booking[].class, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void loadPolicy(String cabinId, SupabaseCallback<BookingPolicy> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("cabinId", cabinId);
        supabaseClient.getSingle(AppConstants.TABLE_BOOKING_POLICIES, filters, BookingPolicy[].class, callback);
    }

    private void checkAvailability(String cabinId, String startDate, String endDate, SupabaseCallback<AvailabilityResult> callback) {
        checkAvailability(cabinId, null, startDate, endDate, 1, callback);
    }

    private void checkAvailability(String cabinId, String roomTypeId, String startDate, String endDate, SupabaseCallback<AvailabilityResult> callback) {
        checkAvailability(cabinId, roomTypeId, startDate, endDate, 1, callback);
    }

    private void checkAvailability(String cabinId, String roomTypeId, String startDate, String endDate, int requestedRooms, SupabaseCallback<AvailabilityResult> callback) {
        if (roomTypeId != null && !roomTypeId.trim().isEmpty()) {
            checkRoomTypeAvailability(cabinId, roomTypeId, startDate, endDate, Math.max(1, requestedRooms), callback);
            return;
        }
        loadUnavailableRanges(cabinId, roomTypeId, new SupabaseCallback<List<UnavailableRange>>() {
            @Override
            public void onSuccess(List<UnavailableRange> ranges) {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                for (UnavailableRange range : ranges) {
                    boolean overlaps = start.isBefore(range.getEndDate()) && end.isAfter(range.getStartDate());
                    if (overlaps) {
                        callback.onSuccess(AvailabilityResult.notAvailable(buildConflictMessage(range, ranges)));
                        return;
                    }
                }
                callback.onSuccess(AvailabilityResult.available());
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void checkRoomTypeAvailability(String cabinId, String roomTypeId, String startDate, String endDate, int requestedRooms, SupabaseCallback<AvailabilityResult> callback) {
        Map<String, String> roomFilters = new HashMap<>();
        roomFilters.put("_id", roomTypeId);
        supabaseClient.getSingle(AppConstants.TABLE_ROOM_TYPES, roomFilters, RoomType[].class, new SupabaseCallback<RoomType>() {
            @Override
            public void onSuccess(RoomType roomType) {
                Map<String, String> inventoryFilters = new HashMap<>();
                inventoryFilters.put("roomTypeId", roomTypeId);
                supabaseClient.getList(AppConstants.TABLE_ROOM_INVENTORY, "*", null, "date.asc", inventoryFilters, RoomInventory[].class, new SupabaseCallback<List<RoomInventory>>() {
                    @Override
                    public void onSuccess(List<RoomInventory> inventoryRows) {
                        Map<String, String> bookingFilters = new HashMap<>();
                        bookingFilters.put("cabinId", cabinId);
                        supabaseClient.getList(AppConstants.TABLE_BOOKINGS, "*", null, "startDate.asc", bookingFilters, Booking[].class, new SupabaseCallback<List<Booking>>() {
                            @Override
                            public void onSuccess(List<Booking> bookings) {
                                Map<String, String> blockedFilters = new HashMap<>();
                                blockedFilters.put("cabinId", cabinId);
                                supabaseClient.getList(AppConstants.TABLE_BLOCKED_DATES, "*", null, "startDate.asc", blockedFilters, BlockedDate[].class, new SupabaseCallback<List<BlockedDate>>() {
                                    @Override
                                    public void onSuccess(List<BlockedDate> blockedDates) {
                                        callback.onSuccess(evaluateRoomTypeAvailability(roomType, inventoryRows, bookings, blockedDates,
                                                startDate, endDate, requestedRooms));
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

    private AvailabilityResult evaluateRoomTypeAvailability(
            RoomType roomType,
            List<RoomInventory> inventoryRows,
            List<Booking> bookings,
            List<BlockedDate> blockedDates,
            String startDate,
            String endDate,
            int requestedRooms
    ) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            if (!end.isAfter(start)) {
                return AvailabilityResult.notAvailable("Khoảng ngày đã chọn chưa hợp lệ.");
            }

            Map<String, RoomInventory> inventoryByDate = new HashMap<>();
            for (RoomInventory inventory : inventoryRows) {
                if (sameRoomType(roomType.getId(), inventory.getRoomTypeId())) {
                    inventoryByDate.put(inventory.getDate(), inventory);
                }
            }

            for (LocalDate day = start; day.isBefore(end); day = day.plusDays(1)) {
                if (isRoomBlockedOn(day, roomType.getId(), blockedDates)) {
                    return AvailabilityResult.notAvailable("Loại phòng đang bị chặn vào ngày " + day + ".");
                }

                int capacity = Math.max(0, roomType.getTotalRooms());
                RoomInventory inventory = inventoryByDate.get(day.toString());
                if (inventory != null) {
                    if (inventory.isClosed()) {
                        return AvailabilityResult.notAvailable("Loại phòng đã đóng bán vào ngày " + day + ".");
                    }
                    capacity = Math.min(capacity, Math.max(0, inventory.getAvailableRooms()));
                }

                int bookedRooms = bookedRoomsOn(day, roomType.getId(), bookings);
                int roomsLeft = capacity - bookedRooms;
                if (roomsLeft < requestedRooms) {
                    return AvailabilityResult.notAvailable("Chỉ còn " + Math.max(0, roomsLeft)
                            + " phòng vào ngày " + day + ".");
                }
            }
            return AvailabilityResult.available();
        } catch (Exception e) {
            return AvailabilityResult.notAvailable("Khoảng ngày đã chọn chưa hợp lệ.");
        }
    }

    private int bookedRoomsOn(LocalDate day, String roomTypeId, List<Booking> bookings) {
        int total = 0;
        for (Booking booking : filterActiveFutureBookings(bookings)) {
            if (!sameRoomType(roomTypeId, booking.getRoomTypeId())) {
                continue;
            }
            if (overlapsDay(day, booking.getStartDate(), booking.getEndDate())) {
                total += Math.max(1, booking.getNumRooms());
            }
        }
        return total;
    }

    private boolean isRoomBlockedOn(LocalDate day, String roomTypeId, List<BlockedDate> blockedDates) {
        for (BlockedDate blockedDate : blockedDates) {
            String blockedRoomTypeId = blockedDate.getRoomTypeId();
            if (blockedRoomTypeId != null && !blockedRoomTypeId.trim().isEmpty()
                    && !sameRoomType(roomTypeId, blockedRoomTypeId)) {
                continue;
            }
            if (overlapsDay(day, blockedDate.getStartDate(), blockedDate.getEndDate())) {
                return true;
            }
        }
        return false;
    }

    private boolean overlapsDay(LocalDate day, String startDate, String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            return !day.isBefore(start) && day.isBefore(end);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean sameRoomType(String left, String right) {
        return left != null && right != null && left.trim().equals(right.trim());
    }

    private void loadUnavailableRanges(String cabinId, SupabaseCallback<List<UnavailableRange>> callback) {
        loadUnavailableRanges(cabinId, null, callback);
    }

    private void loadUnavailableRanges(String cabinId, String roomTypeId, SupabaseCallback<List<UnavailableRange>> callback) {
        Map<String, String> filters = new HashMap<>();
        if (roomTypeId != null && !roomTypeId.trim().isEmpty()) {
            filters.put("roomTypeId", roomTypeId);
        } else {
            filters.put("cabinId", cabinId);
        }
        supabaseClient.getList(AppConstants.TABLE_BOOKINGS, "*", null, "startDate.asc", filters, Booking[].class, new SupabaseCallback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                List<UnavailableRange> ranges = new ArrayList<>();
                for (Booking booking : filterActiveFutureBookings(bookings)) {
                    ranges.add(UnavailableRange.fromBooking(booking));
                }

                supabaseClient.getList(AppConstants.TABLE_BLOCKED_DATES, "*", null, "startDate.asc", filters, BlockedDate[].class, new SupabaseCallback<List<BlockedDate>>() {
                    @Override
                    public void onSuccess(List<BlockedDate> blockedDates) {
                        for (BlockedDate blockedDate : blockedDates) {
                            if (isFutureOrCurrent(blockedDate.getEndDate())) {
                                ranges.add(UnavailableRange.fromBlockedDate(blockedDate));
                            }
                        }
                        ranges.sort(Comparator.comparing(UnavailableRange::getStartDate));
                        callback.onSuccess(ranges);
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

    private List<Booking> filterActiveFutureBookings(List<Booking> bookings) {
        LocalDate today = LocalDate.now();
        return bookings.stream()
                .filter(booking -> !AppConstants.BOOKING_CANCELLED.equals(booking.getStatus()))
                .filter(booking -> {
                    try {
                        return !LocalDate.parse(booking.getEndDate()).isBefore(today);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .sorted(Comparator.comparing(booking -> LocalDate.parse(booking.getStartDate())))
                .collect(Collectors.toList());
    }

    private boolean isFutureOrCurrent(String endDate) {
        try {
            return !LocalDate.parse(endDate).isBefore(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private String buildAvailabilitySummary(List<UnavailableRange> ranges) {
        if (ranges.isEmpty()) {
            return "Lịch còn trống: chỗ nghỉ này chưa có lượt đặt phòng sắp tới.";
        }

        StringBuilder builder = new StringBuilder("Lịch đã được đặt:");
        int limit = Math.min(ranges.size(), 4);
        for (int i = 0; i < limit; i++) {
            UnavailableRange range = ranges.get(i);
            builder.append("\n- ")
                    .append(range.getStartDate())
                    .append(" đến ")
                    .append(range.getEndDate())
                    .append(" (")
                    .append(range.getLabel())
                    .append(")");
        }

        LocalDate availableDate = findNextAvailableDate(ranges);
        builder.append("\nNgày gần nhất còn trống: ").append(availableDate);
        return builder.toString();
    }

    private String buildConflictMessage(UnavailableRange conflict, List<UnavailableRange> ranges) {
        return "Chỗ nghỉ không còn trống từ "
                + conflict.getStartDate()
                + " đến "
                + conflict.getEndDate()
                + " vì "
                + conflict.getLabel()
                + ". Ngày gần nhất còn trống từ: "
                + findNextAvailableDate(ranges)
                + ".";
    }

    private LocalDate findNextAvailableDate(List<UnavailableRange> ranges) {
        LocalDate availableDate = LocalDate.now();
        List<UnavailableRange> normalized = new ArrayList<>(ranges);
        normalized.sort(Comparator.comparing(UnavailableRange::getStartDate));

        for (UnavailableRange range : normalized) {
            LocalDate start = range.getStartDate();
            LocalDate end = range.getEndDate();
            if (availableDate.isBefore(start)) {
                return availableDate;
            }
            if (!availableDate.isAfter(end)) {
                availableDate = end;
            }
        }
        return availableDate;
    }

    private static class UnavailableRange {
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final String label;

        private UnavailableRange(LocalDate startDate, LocalDate endDate, String label) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.label = label;
        }

        static UnavailableRange fromBooking(Booking booking) {
            return new UnavailableRange(
                    LocalDate.parse(booking.getStartDate()),
                    LocalDate.parse(booking.getEndDate()),
                    bookingStatusLabel(booking.getStatus())
            );
        }

        private static String bookingStatusLabel(String status) {
            if (AppConstants.BOOKING_PENDING.equals(status)) {
                return "Đang chờ xác nhận";
            }
            if (AppConstants.BOOKING_CONFIRMED.equals(status)) {
                return "Đã có đặt phòng";
            }
            if ("checked-in".equals(status)) {
                return "Khách đang lưu trú";
            }
            if ("checked-out".equals(status)) {
                return "Khách đã trả phòng";
            }
            return "Đã được giữ lịch";
        }

        static UnavailableRange fromBlockedDate(BlockedDate blockedDate) {
            String reason = blockedDate.getReason() == null || blockedDate.getReason().trim().isEmpty()
                    ? "quản lý đã chặn lịch"
                    : blockedDate.getReason();
            return new UnavailableRange(
                    LocalDate.parse(blockedDate.getStartDate()),
                    LocalDate.parse(blockedDate.getEndDate()),
                    reason
            );
        }

        LocalDate getStartDate() {
            return startDate;
        }

        LocalDate getEndDate() {
            return endDate;
        }

        String getLabel() {
            return label;
        }
    }

    private static class AvailabilityResult {
        private final boolean available;
        private final String message;

        private AvailabilityResult(boolean available, String message) {
            this.available = available;
            this.message = message;
        }

        static AvailabilityResult available() {
            return new AvailabilityResult(true, "");
        }

        static AvailabilityResult notAvailable(String message) {
            return new AvailabilityResult(false, message);
        }

        boolean isAvailable() {
            return available;
        }

        String getMessage() {
            return message;
        }
    }

    public static class AvailabilityStatus {
        private final boolean available;
        private final String message;

        private AvailabilityStatus(boolean available, String message) {
            this.available = available;
            this.message = message == null ? "" : message;
        }

        public boolean isAvailable() {
            return available;
        }

        public String getMessage() {
            return message;
        }
    }
}
