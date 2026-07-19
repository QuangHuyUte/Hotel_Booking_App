package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.BookingPolicy;
import com.example.hotel_booking_app.data.models.BlockedDate;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.Coupon;
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
        checkAvailability(cabinId, startDate, endDate, new SupabaseCallback<AvailabilityResult>() {
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
            callback.onError("Số khách không hợp lệ với sức chứa cabin.");
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

        checkAvailability(cabin.getId(), startDate, endDate, new SupabaseCallback<AvailabilityResult>() {
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

    private void loadPolicy(String cabinId, SupabaseCallback<BookingPolicy> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("cabinId", cabinId);
        supabaseClient.getSingle(AppConstants.TABLE_BOOKING_POLICIES, filters, BookingPolicy[].class, callback);
    }

    private void checkAvailability(String cabinId, String startDate, String endDate, SupabaseCallback<AvailabilityResult> callback) {
        loadUnavailableRanges(cabinId, new SupabaseCallback<List<UnavailableRange>>() {
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

    private void loadUnavailableRanges(String cabinId, SupabaseCallback<List<UnavailableRange>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("cabinId", cabinId);
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
            return "Lịch trống: cabin hiện chưa có booking sắp tới.";
        }

        StringBuilder builder = new StringBuilder("Lịch không available:");
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
        builder.append("\nAvailable gần nhất từ: ").append(availableDate);
        return builder.toString();
    }

    private String buildConflictMessage(UnavailableRange conflict, List<UnavailableRange> ranges) {
        return "Cabin không available từ "
                + conflict.getStartDate()
                + " đến "
                + conflict.getEndDate()
                + " vì "
                + conflict.getLabel()
                + ". Available gần nhất từ: "
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
                    "booking " + booking.getStatus()
            );
        }

        static UnavailableRange fromBlockedDate(BlockedDate blockedDate) {
            String reason = blockedDate.getReason() == null || blockedDate.getReason().trim().isEmpty()
                    ? "host blocked"
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
}
